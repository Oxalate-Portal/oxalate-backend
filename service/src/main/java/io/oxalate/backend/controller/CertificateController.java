package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.api.response.CertificateResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
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
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATES_UPDATE_UNAUTHORIZED;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.rest.CertificateAPI;
import io.oxalate.backend.service.CertificateService;
import io.oxalate.backend.tools.AuthTools;
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
@AuditSource("CertificateController")
public class CertificateController implements CertificateAPI {

    private final CertificateService certificateService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = CERTIFICATES_GET_ALL_START, okMessage = CERTIFICATES_GET_ALL_OK)
    public ResponseEntity<List<CertificateResponse>> getAllCertificates() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.warn("User {} is not allowed to see all certificates", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(CERTIFICATES_GET_ALL_UNAUTHORIZED, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(certificateService.findAll());
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = CERTIFICATES_GET_ALL_USER_START, okMessage = CERTIFICATES_GET_ALL_USER_OK)
    public ResponseEntity<List<CertificateResponse>> getUserCertificates(long userId) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) && !AuthTools.isUserIdCurrentUser(userId)) {
            log.warn("User {} is not allowed to see user {}'s certificates", AuthTools.getCurrentUserId(), userId);
            throw new OxalateUnauthorizedException(CERTIFICATES_GET_ALL_USER_UNAUTHORIZED + userId, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(certificateService.findByUserId(userId));
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = CERTIFICATES_GET_START, okMessage = CERTIFICATES_GET_OK)
    public ResponseEntity<CertificateResponse> getCertificate(long certificateId) {
        var certificateResponse = certificateService.findById(certificateId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) && !AuthTools.isUserIdCurrentUser(certificateResponse.getUserId())) {
            log.warn("User {} is not allowed to see user {}'s certificate ID {}", AuthTools.getCurrentUserId(), certificateResponse.getUserId(), certificateId);
            throw new OxalateUnauthorizedException(CERTIFICATES_GET_UNAUTHORIZED + certificateResponse.getUserId(), HttpStatus.BAD_REQUEST);
        }

        if (certificateResponse == null) {
            throw new OxalateNotFoundException(CERTIFICATES_GET_NOT_FOUND + certificateId, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(certificateResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = CERTIFICATES_ADD_START, okMessage = CERTIFICATES_ADD_OK)
    public ResponseEntity<CertificateResponse> addCertificate(CertificateRequest certificateRequest) {
        var userId = AuthTools.getCurrentUserId();
        var certificateDto = certificateService.addCertificate(userId, certificateRequest);

        if (certificateDto == null) {
            throw new OxalateValidationException(CERTIFICATES_ADD_FAIL, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(certificateDto);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = CERTIFICATES_UPDATE_START, okMessage = CERTIFICATES_UPDATE_OK)
    public ResponseEntity<CertificateResponse> updateCertificate(CertificateRequest certificateRequest) {
        var userId = AuthTools.getCurrentUserId();
        var certificate = certificateService.findById(certificateRequest.getId());

        if (certificate == null) {
            throw new OxalateNotFoundException(CERTIFICATES_UPDATE_NOT_FOUND + certificateRequest.getId());
        }

        log.info("User {} is updating certificate with request: {}", userId, certificateRequest);

        if (userId != certificate.getUserId()) {
            log.warn("User {} is not allowed to update certificates belonging to user {}", userId, certificateRequest.getUserId());
            throw new OxalateUnauthorizedException(CERTIFICATES_UPDATE_UNAUTHORIZED + certificateRequest.getId(), HttpStatus.BAD_REQUEST);
        }

        var certificateResponse = certificateService.updateCertificate(certificateRequest.getUserId(), certificateRequest);

        if (certificateResponse == null) {
            throw new OxalateValidationException(CERTIFICATES_UPDATE_FAIL + certificateRequest.getId(), HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(certificateResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = CERTIFICATES_DELETE_START, okMessage = CERTIFICATES_DELETE_OK)
    public ResponseEntity<Void> deleteCertificate(long certificateId) {
        var userId = AuthTools.getCurrentUserId();
        var certificate = certificateService.findById(certificateId);

        if (certificate.getUserId() != userId) {
            log.warn("User {} is not allowed to delete user {}'s certificates", AuthTools.getCurrentUserId(), userId);
            throw new OxalateUnauthorizedException(CERTIFICATES_DELETE_UNAUTHORIZED + certificateId, HttpStatus.BAD_REQUEST);
        }

        if (!certificateService.deleteCertificate(certificateId)) {
            throw new OxalateValidationException(CERTIFICATES_DELETE_FAIL + certificateId, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(null);
    }
}
