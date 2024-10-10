package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.ERROR;
import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.api.response.CertificateResponse;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_ADD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_ADD_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_ADD_START;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_DELETE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_DELETE_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_DELETE_START;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_DELETE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_ALL_USER_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_ALL_USER_START;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_ALL_USER_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_GET_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_UNAUTHORIZED;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.CertificateAPI;
import io.oxalate.backend.service.CertificateService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class CertificateController implements CertificateAPI {

    private final CertificateService certificateService;
    private static final String AUDIT_NAME = "CertificateController";
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CertificateResponse>> getAllCertificates(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(CERTIFICATES_GET_ALL_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(CERTIFICATES_GET_ALL_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK).body(certificateService.findAll());
        }

        log.warn("User {} is not allowed to see all certificates", AuthTools.getCurrentUserId());
        appEventPublisher.publishAuditEvent(CERTIFICATES_GET_ALL_UNAUTHORIZED, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<CertificateResponse>> getUserCertificates(long userId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(CERTIFICATES_GET_ALL_USER_START + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        // Check if user is allowed to see this user's certificates. ADMIN and ORGANIZER can see any, USER can see only their own
        if (AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) || AuthTools.isUserIdCurrentUser(userId)) {
            appEventPublisher.publishAuditEvent(CERTIFICATES_GET_ALL_USER_OK + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK).body(certificateService.findByUserId(userId));
        }

        log.warn("User {} is not allowed to see user {}'s certificates", AuthTools.getCurrentUserId(), userId);
        appEventPublisher.publishAuditEvent(CERTIFICATES_GET_ALL_USER_UNAUTHORIZED + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CertificateResponse> getCertificate(long certificateId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(CERTIFICATES_GET_START + certificateId, INFO, request,
                AUDIT_NAME, AuthTools.getCurrentUserId());

        var certificateResponse = certificateService.findById(certificateId);
        // Check if user is allowed to see this user's certificates. ADMIN and ORGANIZER can see any, USER can see only their own
        if (AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) || AuthTools.isUserIdCurrentUser(certificateResponse.getUserId())) {

            if (certificateResponse != null) {
                appEventPublisher.publishAuditEvent(CERTIFICATES_GET_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
                return ResponseEntity.status(HttpStatus.OK)
                                     .body(certificateResponse);
            } else {
                appEventPublisher.publishAuditEvent(CERTIFICATES_GET_NOT_FOUND + certificateId, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                        auditUuid);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .body(null);
            }
        }

        appEventPublisher.publishAuditEvent(CERTIFICATES_GET_UNAUTHORIZED + certificateResponse.getUserId(), ERROR, request, AUDIT_NAME,
                AuthTools.getCurrentUserId(), auditUuid);
        log.warn("User {} is not allowed to see user {}'s certificate ID {}", AuthTools.getCurrentUserId(), certificateResponse.getUserId(), certificateId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CertificateResponse> addCertificate(CertificateRequest certificateRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(CERTIFICATES_ADD_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        // Any user can add certificates only to their own profile. We disregard the userId given in the request and use instead the one from the session
        var userId = AuthTools.getCurrentUserId();

        var certificateDto = certificateService.addCertificate(userId, certificateRequest);

        if (certificateDto != null) {
            appEventPublisher.publishAuditEvent(CERTIFICATES_ADD_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(certificateDto);
        }

        appEventPublisher.publishAuditEvent(CERTIFICATES_ADD_FAIL, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CertificateResponse> updateCertificate(CertificateRequest certificateRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(CERTIFICATES_UPDATE_START + certificateRequest.getId(), INFO, request, AUDIT_NAME,
                AuthTools.getCurrentUserId());
        // Any user can update certificates only to their own profile
        if (AuthTools.isUserIdCurrentUser(certificateRequest.getUserId())) {
            var certificateDto = certificateService.updateCertificate(certificateRequest.getUserId(), certificateRequest);

            if (certificateDto != null) {
                appEventPublisher.publishAuditEvent(CERTIFICATES_UPDATE_OK + certificateRequest.getId(), INFO, request, AUDIT_NAME,
                        AuthTools.getCurrentUserId(), auditUuid);
                return ResponseEntity.status(HttpStatus.OK)
                                     .body(certificateDto);
            } else {
                appEventPublisher.publishAuditEvent(CERTIFICATES_UPDATE_FAIL + certificateRequest.getId(), INFO, request, AUDIT_NAME,
                        AuthTools.getCurrentUserId(),
                        auditUuid);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .body(null);
            }
        }

        appEventPublisher.publishAuditEvent(CERTIFICATES_UPDATE_UNAUTHORIZED + certificateRequest.getId(), INFO, request, AUDIT_NAME,
                AuthTools.getCurrentUserId(),
                auditUuid);
        log.warn("User {} is not allowed to update certificates belonging to user {}", AuthTools.getCurrentUserId(), certificateRequest.getUserId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> deleteCertificate(long certificateId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(CERTIFICATES_DELETE_START + certificateId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        var userId = AuthTools.getCurrentUserId();

        var certificate = certificateService.findById(certificateId);
        // Any user can delete only their own certificates
        if (certificate.getUserId() == userId) {
            if (certificateService.deleteCertificate(certificateId)) {
                appEventPublisher.publishAuditEvent(CERTIFICATES_DELETE_OK + certificateId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
                return ResponseEntity.status(HttpStatus.OK)
                                     .body(null);
            } else {
                appEventPublisher.publishAuditEvent(CERTIFICATES_DELETE_FAIL + certificateId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                        auditUuid);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .body(null);
            }
        }

        appEventPublisher.publishAuditEvent(CERTIFICATES_DELETE_UNAUTHORIZED + certificateId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                auditUuid);
        log.warn("User {} is not allowed to delete user {}'s certificates", AuthTools.getCurrentUserId(), userId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(null);
    }
}
