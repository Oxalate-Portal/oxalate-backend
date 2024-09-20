package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.response.FileRemovalResponse;
import io.oxalate.backend.api.response.UploadErrorResponse;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.AvatarFileResponse;
import io.oxalate.backend.api.response.filetransfer.CertificateFileResponse;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
import io.oxalate.backend.api.response.filetransfer.DocumentFileResponse;
import io.oxalate.backend.api.response.filetransfer.PageFileResponse;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_GET_ALL_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_REMOVE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_REMOVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_REMOVE_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_UPLOAD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_UPLOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_AVATAR_UPLOAD_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_DOWNLOAD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_DOWNLOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_DOWNLOAD_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_DOWNLOAD_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_GET_ALL_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_REMOVE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_REMOVE_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_UPLOAD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_UPLOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_CERTIFICATE_UPLOAD_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DIVE_FILE_GET_ALL_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DIVE_FILE_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DIVE_FILE_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DIVE_FILE_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DIVE_FILE_UPLOAD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DIVE_FILE_UPLOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DIVE_FILE_UPLOAD_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_GET_ALL_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_REMOVE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_REMOVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_REMOVE_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_UPLOAD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_UPLOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_DOCUMENT_UPLOAD_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_GET_ALL_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_REMOVE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_REMOVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_REMOVE_START;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_UPLOAD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_UPLOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.FILE_PAGE_FILE_UPLOAD_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.FileTransferAPI;
import io.oxalate.backend.service.filetransfer.AvatarFileTransferService;
import io.oxalate.backend.service.filetransfer.CertificateFileTransferService;
import io.oxalate.backend.service.filetransfer.DiveFileTransferService;
import io.oxalate.backend.service.filetransfer.DocumentFileTransferService;
import io.oxalate.backend.service.filetransfer.PageFileTransferService;
import io.oxalate.backend.tools.AuthTools;
import static io.oxalate.backend.tools.AuthTools.currentUserHasRole;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
    private final AvatarFileTransferService avatarFileTransferService;
    private final CertificateFileTransferService certificateFileTransferService;
    private final DiveFileTransferService diveFileTransferService;
    private final DocumentFileTransferService documentFileTransferService;
    private final PageFileTransferService pageFileTransferService;

    /* Avatar */
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<AvatarFileResponse>> findAllAvatarFiles(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_AVATAR_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (userId < 0 || !currentUserHasRole(ROLE_ADMIN)) {
            log.error("Unauthorized access with user ID: {}", userId);
            appEventPublisher.publishAuditEvent(FILE_AVATAR_GET_ALL_UNAUTHORIZED, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized request");
        }

        var allAvatarFiles = avatarFileTransferService.findAllAvatarFiles();

        if (allAvatarFiles == null) {
            appEventPublisher.publishAuditEvent(FILE_AVATAR_GET_ALL_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.internalServerError()
                                 .build();
        }

        appEventPublisher.publishAuditEvent(FILE_AVATAR_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(allAvatarFiles);
    }

    @PreAuthorize("hasRole('USER')")
    @Override
    public ResponseEntity<?> uploadAvatarFile(MultipartFile uploadFile, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_AVATAR_UPLOAD_START, INFO, request, AUDIT_NAME, userId);

        UploadResponse uploadResponse;

        try {
            uploadResponse = avatarFileTransferService.uploadAvatarFile(uploadFile, userId);
        } catch (Exception e) {
            log.debug("Failed to upload avatar file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_AVATAR_UPLOAD_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse("Avatar");
            return ResponseEntity.ok(errorMessage);
        }

        return getResponseEntity(request, userId, uploadResponse, auditUuid, FILE_AVATAR_UPLOAD_OK, FILE_CERTIFICATE_UPLOAD_OK);
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<byte[]> downloadAvatarFile(long avatarId, HttpServletRequest request) {
        log.debug("Downloading avatar file: {}", avatarId);
        return avatarFileTransferService.downloadAvatarFile(avatarId);
    }

    @PreAuthorize("hasRole('USER')")
    @Override
    public ResponseEntity<FileRemovalResponse> removeAvatarFile(long avatarId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        log.debug("Removing avatar file: {} by user: {}", avatarId, userId);
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_AVATAR_REMOVE_START, INFO, request, AUDIT_NAME, userId);

        FileRemovalResponse response;

        try {
            response = avatarFileTransferService.removeAvatarFile(avatarId, userId);
        } catch (Exception e) {
            log.error("Failed to remove avatar file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_AVATAR_REMOVE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw e;
        }

        appEventPublisher.publishAuditEvent(FILE_AVATAR_REMOVE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(response);
    }

    /* Certificate */
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<CertificateFileResponse>> findAllCertificateFiles(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (userId < 0 || !currentUserHasRole(ROLE_ADMIN)) {
            log.error("Unauthorized access with user ID: {}", userId);
            appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_GET_ALL_UNAUTHORIZED, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized request");
        }

        var allCertificateFiles = certificateFileTransferService.findAllCertificateFiles();

        if (allCertificateFiles == null) {
            appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_GET_ALL_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.internalServerError()
                                 .build();
        }

        appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(allCertificateFiles);
    }

    @PreAuthorize("hasRole('USER')")
    @Override
    public ResponseEntity<?> uploadCertificateFile(MultipartFile uploadFile, long certificateId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        UploadResponse uploadResponse;
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_UPLOAD_START, INFO, request, AUDIT_NAME, userId);

        try {
            uploadResponse = certificateFileTransferService.uploadCertificateFile(uploadFile, userId, certificateId);
        } catch (Exception e) {
            log.debug("Failed to upload certificate file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_UPLOAD_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse("Certificate");
            return ResponseEntity.ok(errorMessage);
        }

        return getResponseEntity(request, userId, uploadResponse, auditUuid, FILE_CERTIFICATE_UPLOAD_OK, FILE_CERTIFICATE_UPLOAD_OK);
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<byte[]> downloadCertificateFile(long certificateId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_DOWNLOAD_START, INFO, request, AUDIT_NAME, userId);
        log.debug("Downloading certificate file: {} by user ID: {}", certificateId, userId);
        var roles = AuthTools.getUserRoles();

        // If there were no roles to retrieve, of the only role is anonymous, then we return a 403
        if (roles.isEmpty() || (roles.size() == 1 && roles.contains(RoleEnum.ROLE_ANONYMOUS))) {
            log.error("Could not retrieve any non-anonymous roles when accessing certificate ID: {}", certificateId);
            appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_DOWNLOAD_UNAUTHORIZED, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request from unauthorized user");
        }

        var response = certificateFileTransferService.downloadCertificateFile(certificateId, userId, roles);

        if (response == null) {
            log.error("Failed to download certificate file: {}", certificateId);
            appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_DOWNLOAD_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download certificate file");
        }

        appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_DOWNLOAD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return response;
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<FileRemovalResponse> removeCertificateFile(long certificateId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        log.debug("Removing certificate file: {}", certificateId);
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_REMOVE_START, INFO, request, AUDIT_NAME, userId);

        FileRemovalResponse response;

        try {
            response = certificateFileTransferService.removeCertificateFile(certificateId, userId);
        } catch (Exception e) {
            log.error("Failed to remove certificate file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_CERTIFICATE_REMOVE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw e;
        }

        return ResponseEntity.ok(response);
    }

    /* DiveFile */
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<DiveFileResponse>> findAllDiveFiles(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_DIVE_FILE_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (userId < 0 || !currentUserHasRole(ROLE_ADMIN)) {
            log.error("Unauthorized access with user ID: {}", userId);
            appEventPublisher.publishAuditEvent(FILE_DIVE_FILE_GET_ALL_UNAUTHORIZED, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized request");
        }

        var allDiveFiles = diveFileTransferService.findAllDiveFiles();

        if (allDiveFiles == null) {
            appEventPublisher.publishAuditEvent(FILE_DIVE_FILE_GET_ALL_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.internalServerError()
                                 .build();
        }

        appEventPublisher.publishAuditEvent(FILE_DIVE_FILE_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(allDiveFiles);
    }

    @PreAuthorize("hasRole('USER')")
    @Override
    public ResponseEntity<?> uploadDiveFile(MultipartFile uploadFile, long eventId, long diveGroupId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_DIVE_FILE_UPLOAD_START, INFO, request, AUDIT_NAME, userId);
        UploadResponse uploadResponse;

        try {
            uploadResponse = diveFileTransferService.uploadDiveFile(uploadFile, eventId, diveGroupId, userId);
        } catch (Exception e) {
            log.debug("Failed to upload dive file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_DIVE_FILE_UPLOAD_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse("Dive file");
            return ResponseEntity.ok(errorMessage);
        }

        return getResponseEntity(request, userId, uploadResponse, auditUuid, FILE_DIVE_FILE_UPLOAD_OK, FILE_CERTIFICATE_UPLOAD_OK);
    }

    // TODO implement these later when we work on the dive group functionality, see https://github.com/Oxalate-Portal/oxalate-backend/issues/57

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<byte[]> downloadDiveFile(long diveFileId, HttpServletRequest request) {
        return null;
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<FileRemovalResponse> removeDiveFile(long diveFileId, HttpServletRequest request) {
        return null;
    }

    /* Document */
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<DocumentFileResponse>> findAllDocumentFiles(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_DOCUMENT_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (userId < 0 || !currentUserHasRole(ROLE_ADMIN)) {
            log.error("Unauthorized access with user ID: {}", userId);
            appEventPublisher.publishAuditEvent(FILE_DOCUMENT_GET_ALL_UNAUTHORIZED, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized request");
        }

        var allDocumentFiles = documentFileTransferService.findAllDocumentFiles();

        if (allDocumentFiles == null) {
            appEventPublisher.publishAuditEvent(FILE_DOCUMENT_GET_ALL_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.internalServerError()
                                 .build();
        }

        appEventPublisher.publishAuditEvent(FILE_DOCUMENT_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(allDocumentFiles);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> uploadDocumentFile(MultipartFile uploadFile, HttpServletRequest request) {
        log.debug("Uploading document file: {}", uploadFile.getOriginalFilename());
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_DOCUMENT_UPLOAD_START, INFO, request, AUDIT_NAME, userId);

        UploadResponse uploadResponse;

        try {
            uploadResponse = documentFileTransferService.uploadDocumentFile(uploadFile, userId);
        } catch (Exception e) {
            log.warn("Failed to upload document file: {}", e.getMessage(), e);
            appEventPublisher.publishAuditEvent(FILE_DOCUMENT_UPLOAD_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw e;
        }

        return getResponseEntity(request, userId, uploadResponse, auditUuid, FILE_DOCUMENT_UPLOAD_OK, FILE_CERTIFICATE_UPLOAD_OK);
    }

    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<byte[]> downloadDocumentFile(long documentId, HttpServletRequest request) {
        // We do not audit this endpoint because it is expected to be quite high-traffic
        log.debug("Downloading document ID: {}", documentId);
        return documentFileTransferService.downloadDocumentFile(documentId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<FileRemovalResponse> removeDocumentFile(long documentId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        log.debug("Removing document file: {} by user: {}", documentId, userId);
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_DOCUMENT_REMOVE_START, INFO, request, AUDIT_NAME, userId);

        FileRemovalResponse response;

        try {
            response = documentFileTransferService.removeDocumentFile(documentId, userId);
        } catch (Exception e) {
            log.error("Failed to remove document file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_DOCUMENT_REMOVE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw e;
        }

        appEventPublisher.publishAuditEvent(FILE_DOCUMENT_REMOVE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(response);
    }

    /* Page */
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<PageFileResponse>> findAllPageFiles(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (userId < 0 || !currentUserHasRole(ROLE_ADMIN)) {
            log.error("Unauthorized access with user ID: {}", userId);
            appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_GET_ALL_UNAUTHORIZED, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized request");
        }

        var allPageFiles = pageFileTransferService.findAllPageFiles();

        if (allPageFiles == null) {
            appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_GET_ALL_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.internalServerError()
                                 .build();
        }

        appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(allPageFiles);
    }

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<?> uploadPageFile(MultipartFile uploadFile, String language, long pageId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        if (userId < 0) {
            log.error("User ID is invalid: {}", userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User ID is invalid");
        }

        var auditUuid = appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_UPLOAD_START, INFO, request, AUDIT_NAME, userId);
        UploadResponse uploadResponse;

        try {
            uploadResponse = pageFileTransferService.uploadPageFile(uploadFile, language, pageId, userId);
        } catch (Exception e) {
            log.debug("Failed to upload page file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_UPLOAD_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse("Page file");
            return ResponseEntity.ok(errorMessage);
        }

        return getResponseEntity(request, userId, uploadResponse, auditUuid, FILE_PAGE_FILE_UPLOAD_FAIL, FILE_PAGE_FILE_UPLOAD_OK);
    }

    @Override
    public ResponseEntity<byte[]> downloadPageFile(long pageId, String language, String fileName, HttpServletRequest request) {
        // We do not audit this endpoint because it is expected to be quite high-traffic
        // However we do check the allowed roles the page the file belongs to.
        log.debug("Downloading file: {} {} {}", pageId, language, fileName);
        var roles = AuthTools.getUserRoles();
        return pageFileTransferService.downloadPageFile(pageId, language, fileName, roles);
    }

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Override
    public ResponseEntity<FileRemovalResponse> removePageFile(long pageId, String language, String fileName, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        log.debug("Removing page file: {} language: {} filename: {} by user: {}", pageId, language, fileName, userId);
        var auditUuid = appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_REMOVE_START, INFO, request, AUDIT_NAME, userId);

        FileRemovalResponse response;

        try {
            response = pageFileTransferService.removePageFile(pageId, language, fileName);
        } catch (Exception e) {
            log.error("Failed to remove page file: {}", e.getMessage());
            appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_REMOVE_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            throw e;
        }

        appEventPublisher.publishAuditEvent(FILE_PAGE_FILE_REMOVE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(response);
    }

    private static UploadErrorResponse getUploadErrorResponse(String uploadFileType) {
        log.error("Failed to upload {} file", uploadFileType);
        return UploadErrorResponse.builder()
                                  .error(UploadErrorResponse.UploadErrorMessage.builder()
                                                                               .message("Could not upload file")
                                                                               .build())
                                  .build();
    }

    private ResponseEntity<?> getResponseEntity(HttpServletRequest request, long userId, UploadResponse uploadResponse, UUID auditUuid,
            String failMessage, String okMessage) {
        if (uploadResponse == null) {
            appEventPublisher.publishAuditEvent(failMessage, WARN, request, AUDIT_NAME, userId, auditUuid);
            var errorMessage = getUploadErrorResponse(request.getPathInfo());
            return ResponseEntity.ok(errorMessage);
        }

        appEventPublisher.publishAuditEvent(okMessage, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(uploadResponse);
    }
}
