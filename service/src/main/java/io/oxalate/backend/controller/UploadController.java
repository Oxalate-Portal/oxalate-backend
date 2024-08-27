package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import io.oxalate.backend.api.response.UploadErrorResponse;
import io.oxalate.backend.api.response.UploadResponse;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_FILE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_FILE_OK;
import static io.oxalate.backend.events.AppAuditMessages.UPLOAD_FILE_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.UploadAPI;
import io.oxalate.backend.service.UploadService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
public class UploadController implements UploadAPI {

    private static final String AUDIT_NAME = "UploadController";
    private final AppEventPublisher appEventPublisher;
    private final UploadService uploadService;

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> uploadFile(MultipartFile file, String language, long pageId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(UPLOAD_FILE_START, INFO, request, AUDIT_NAME, userId);

        UploadResponse uploadResponse = null;
        try {
            uploadResponse = uploadService.uploadFile(file, language, pageId, userId);
        } catch (Exception e) {
            appEventPublisher.publishAuditEvent(UPLOAD_FILE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse();
            return ResponseEntity.ok(errorMessage);
        }

        if (uploadResponse == null) {
            appEventPublisher.publishAuditEvent(UPLOAD_FILE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse();
            return ResponseEntity.ok(errorMessage);
        }

        appEventPublisher.publishAuditEvent(UPLOAD_FILE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(uploadResponse);
    }

    private static UploadErrorResponse getUploadErrorResponse() {
        var errorMessage = UploadErrorResponse.builder()
                                              .error(UploadErrorResponse.UploadErrorMessage.builder()
                                                                                           .message("Could not upload file")
                                                                                           .build())
                                              .build();
        return errorMessage;
    }

    @Override
    public ResponseEntity<byte[]> downloadFile(long pageId, String language, String fileName, HttpServletRequest request) {
        log.debug("Downloading file: {} {} {}", pageId, language, fileName);
        // We do not audit this endpoint because it is expected to be quite high-traffic
        // However we do check the allowed roles the page the file belongs to.
       var roles = AuthTools.getUserRoles();
        return uploadService.downloadFile(pageId, language, fileName, roles);
    }
}
