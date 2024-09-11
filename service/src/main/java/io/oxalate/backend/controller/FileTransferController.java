package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import io.oxalate.backend.api.response.UploadErrorResponse;
import io.oxalate.backend.api.response.UploadResponse;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_CERTIFICATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_CERTIFICATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_CERTIFICATE_START;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_PAGE_FILE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_PAGE_FILE_OK;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_PAGE_FILE_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.FileTransferAPI;
import io.oxalate.backend.service.FileTransferService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@RestController
public class FileTransferController implements FileTransferAPI {

    private static final String AUDIT_NAME = "UploadController";
    private final AppEventPublisher appEventPublisher;
    private final FileTransferService fileTransferService;

    /* Page */
    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> uploadPageFile(MultipartFile uploadFile, String language, long pageId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(UPLOAD_PAGE_FILE_START, INFO, request, AUDIT_NAME, userId);
        UploadResponse uploadResponse;

        try {
            uploadResponse = fileTransferService.uploadPageFile(uploadFile, language, pageId, userId);
        } catch (Exception e) {
            appEventPublisher.publishAuditEvent(UPLOAD_PAGE_FILE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse();
            return ResponseEntity.ok(errorMessage);
        }

        return getResponseEntity(request, userId, uploadResponse, auditUuid, UPLOAD_PAGE_FILE_FAIL, UPLOAD_PAGE_FILE_OK);
    }

    @Override
    public ResponseEntity<byte[]> downloadPageFile(long pageId, String language, String fileName, HttpServletRequest request) {
        // We do not audit this endpoint because it is expected to be quite high-traffic
        // However we do check the allowed roles the page the file belongs to.
        log.debug("Downloading file: {} {} {}", pageId, language, fileName);
        var roles = AuthTools.getUserRoles();
        return fileTransferService.downloadPageFile(pageId, language, fileName, roles);
    }

    /* Certificate */
    @PreAuthorize("hasAnyRole('USER')")
    @Override
    public ResponseEntity<?> uploadCertificateFile(MultipartFile uploadFile, long certificateId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        UploadResponse uploadResponse;
        var auditUuid = appEventPublisher.publishAuditEvent(UPLOAD_CERTIFICATE_START, INFO, request, AUDIT_NAME, userId);

        try {
            uploadResponse = fileTransferService.uploadCertificateFile(uploadFile, userId, certificateId);
        } catch (Exception e) {
            appEventPublisher.publishAuditEvent(UPLOAD_CERTIFICATE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse();
            return ResponseEntity.ok(errorMessage);
        }

        return getResponseEntity(request, userId, uploadResponse, auditUuid, UPLOAD_CERTIFICATE_FAIL, UPLOAD_CERTIFICATE_OK);
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<byte[]> downloadCertificateFile(long certificateId, HttpServletRequest request) {
        // We do not audit this endpoint because it is expected to be quite high-traffic
        // However we do check the allowed roles the page the file belongs to.
        var userId = AuthTools.getCurrentUserId();
        log.debug("Downloading certificate file: {}", certificateId);
        var roles = AuthTools.getUserRoles();

        // If there were no roles to retrieve, of the only role is anonymous, then we return a 403
        if (roles.isEmpty() || (roles.size() == 1 && roles.contains("ROLE_ANONYMOUS"))) {
            log.error("Could not retrieve any non-anonymous roles when accessing certificate ID: {}", certificateId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request from unauthorized user");
        }

        return fileTransferService.downloadCertificateFile(certificateId, userId, roles);
    }

    /* Document */
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Override
    public ResponseEntity<?> uploadDocumentFile(MultipartFile uploadFile, HttpServletRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<byte[]> downloadDocumentFile(long documentId, HttpServletRequest request) {
        return null;
    }

    /* DivePlan */
    @PreAuthorize("hasAnyRole('USER')")
    @Override
    public ResponseEntity<?> uploadDivePlanFile(MultipartFile uploadFile, long pageId, HttpServletRequest request) {
        return null;
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<byte[]> downloadDivePlanFile(long divePlanId, HttpServletRequest request) {
        return null;
    }

    /* Avatar */
    @PreAuthorize("hasRole('USER')")
    @Override
    public ResponseEntity<?> uploadAvatarFile(MultipartFile uploadFile, HttpServletRequest request) {
        return null;
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<byte[]> downloadAvatarFile(long avatarId, HttpServletRequest request) {
        return null;
    }

    private static UploadErrorResponse getUploadErrorResponse() {
        var errorMessage = UploadErrorResponse.builder()
                                              .error(UploadErrorResponse.UploadErrorMessage.builder()
                                                                                           .message("Could not upload file")
                                                                                           .build())
                                              .build();
        return errorMessage;
    }

    private ResponseEntity<?> getResponseEntity(HttpServletRequest request, long userId, UploadResponse uploadResponse, UUID auditUuid,
            String failMessage, String okMessage) {
        if (uploadResponse == null) {
            appEventPublisher.publishAuditEvent(failMessage, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse();
            return ResponseEntity.ok(errorMessage);
        }

        appEventPublisher.publishAuditEvent(okMessage, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(uploadResponse);
    }
}
