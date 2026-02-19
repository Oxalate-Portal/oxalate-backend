package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.response.download.DownloadCertificateResponse;
import io.oxalate.backend.api.response.download.DownloadDiveResponse;
import io.oxalate.backend.api.response.download.DownloadPaymentResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_CERTIFICATES_OK;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_CERTIFICATES_START;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_CERTIFICATES_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_DIVES_OK;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_DIVES_START;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_DIVES_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_PAYMENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_PAYMENTS_START;
import static io.oxalate.backend.events.AppAuditMessages.DATA_DOWNLOAD_PAYMENTS_UNAUTHORIZED;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.rest.DataDownloadAPI;
import io.oxalate.backend.service.DataDownloadService;
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
@AuditSource("DataDownloadController")
public class DataDownloadController implements DataDownloadAPI {
    private final DataDownloadService dataDownloadService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = DATA_DOWNLOAD_CERTIFICATES_START, okMessage = DATA_DOWNLOAD_CERTIFICATES_OK)
    public ResponseEntity<List<DownloadCertificateResponse>> downloadCertificates() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.warn("User {} is not allowed to download certificates", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(DATA_DOWNLOAD_CERTIFICATES_UNAUTHORIZED, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(dataDownloadService.downloadCertificates());
    }

    @Override
    @Audited(startMessage = DATA_DOWNLOAD_DIVES_START, okMessage = DATA_DOWNLOAD_DIVES_OK)
    public ResponseEntity<List<DownloadDiveResponse>> downloadDives() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.warn("User {} is not allowed to download dives", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(DATA_DOWNLOAD_DIVES_UNAUTHORIZED, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(dataDownloadService.downloadDives());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = DATA_DOWNLOAD_PAYMENTS_START, okMessage = DATA_DOWNLOAD_PAYMENTS_OK)
    public ResponseEntity<List<DownloadPaymentResponse>> downloadPayments() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.error("User ID {} tried to download payment information without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(DATA_DOWNLOAD_PAYMENTS_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var downloadPaymentResponse = dataDownloadService.downloadPayments();
        return ResponseEntity.status(HttpStatus.OK).body(downloadPaymentResponse);
    }
}
