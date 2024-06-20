package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.ERROR;
import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.response.download.DownloadCertificateResponse;
import io.oxalate.backend.api.response.download.DownloadDiveResponse;
import io.oxalate.backend.api.response.download.DownloadPaymentResponse;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_CERTIFICATES_OK;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_CERTIFICATES_START;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_CERTIFICATES_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_DIVES_OK;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_DIVES_START;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_DIVES_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_PAYMENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_PAYMENTS_START;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_PAYMENTS_UNAUTHORIZED;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.DataDownloadAPI;
import io.oxalate.backend.service.DataDownloadService;
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
public class DataDownloadController implements DataDownloadAPI {
    private static final String AUDIT_NAME = "CertificateController";
    private final AppEventPublisher appEventPublisher;
    private final DataDownloadService dataDownloadService;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<DownloadCertificateResponse>> downloadCertificates(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_CERTIFICATES_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_CERTIFICATES_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK).body(dataDownloadService.downloadCertificates());
        }

        log.warn("User {} is not allowed to download certificates", AuthTools.getCurrentUserId());
        appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_CERTIFICATES_UNAUTHORIZED, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @Override
    public ResponseEntity<List<DownloadDiveResponse>> downloadDives(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_DIVES_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_DIVES_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK).body(dataDownloadService.downloadDives());
        }

        log.warn("User {} is not allowed to download dives", AuthTools.getCurrentUserId());
        appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_DIVES_UNAUTHORIZED, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<DownloadPaymentResponse>> downloadPayments(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_PAYMENTS_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_PAYMENTS_UNAUTHORIZED, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to download payment information without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        var downloadPaymentResponse = dataDownloadService.downloadPayments();

        appEventPublisher.publishAuditEvent(DATA_DOWNLOAD_PAYMENTS_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(downloadPaymentResponse);
    }
}
