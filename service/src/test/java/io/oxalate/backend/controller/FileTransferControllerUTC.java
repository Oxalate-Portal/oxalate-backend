package io.oxalate.backend.controller;

import static io.oxalate.backend.api.PortalConfigEnum.FILES;
import static io.oxalate.backend.api.PortalConfigEnum.FileConfigEnum.DIVE_FILES_SUPPORTED;
import static io.oxalate.backend.api.PortalConfigEnum.FileConfigEnum.DOCUMENTS_SUPPORTED;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.UpdateStatusEnum.OK;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.PagedResponse;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.CertificateFileResponse;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
import io.oxalate.backend.api.response.filetransfer.DocumentFileResponse;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.service.PortalConfigurationService;
import io.oxalate.backend.service.filetransfer.AvatarFileTransferService;
import io.oxalate.backend.service.filetransfer.CertificateFileTransferService;
import io.oxalate.backend.service.filetransfer.DiveFileTransferService;
import io.oxalate.backend.service.filetransfer.DocumentFileTransferService;
import io.oxalate.backend.service.filetransfer.PageFileTransferService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class FileTransferControllerUTC {

    private final AvatarFileTransferService avatarFileTransferService = mock(AvatarFileTransferService.class);
    private final CertificateFileTransferService certificateFileTransferService = mock(CertificateFileTransferService.class);
    private final DiveFileTransferService diveFileTransferService = mock(DiveFileTransferService.class);
    private final DocumentFileTransferService documentFileTransferService = mock(DocumentFileTransferService.class);
    private final PageFileTransferService pageFileTransferService = mock(PageFileTransferService.class);
    private final PortalConfigurationService portalConfigurationService = mock(PortalConfigurationService.class);

    private final FileTransferController controller = new FileTransferController(
            avatarFileTransferService,
            certificateFileTransferService,
            diveFileTransferService,
            documentFileTransferService,
            pageFileTransferService,
            portalConfigurationService
    );

    private static final PagedRequest PAGED_REQUEST = PagedRequest.builder()
                                                                  .page(0)
                                                                  .size(10)
                                                                  .build();

    @Test
    void findAllDocumentFiles_adminWithCreatorId_returnsCreatorScopedFiles() {
        var expected = pageOf(DocumentFileResponse.builder()
                                                  .id(11L)
                                                  .build());
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(true);
        when(documentFileTransferService.findAllDocumentFilesPaged(PAGED_REQUEST, 15L)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(true);

            var response = controller.findAllDocumentFiles(PAGED_REQUEST, 15L);

            assertEquals(expected, response.getBody());
            verify(documentFileTransferService).findAllDocumentFilesPaged(PAGED_REQUEST, 15L);
        }
    }

    @Test
    void findAllDocumentFiles_adminWithoutCreatorId_returnsAllFiles() {
        var expected = pageOf(DocumentFileResponse.builder()
                                                  .id(13L)
                                                  .build());
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(true);
        when(documentFileTransferService.findAllDocumentFilesPaged(PAGED_REQUEST, null)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(true);

            var response = controller.findAllDocumentFiles(PAGED_REQUEST, null);

            assertEquals(expected, response.getBody());
            verify(documentFileTransferService).findAllDocumentFilesPaged(PAGED_REQUEST, null);
        }
    }

    @Test
    void findAllDocumentFiles_nonAdminWithoutCreatorId_returnsOwnFiles() {
        var expected = pageOf(DocumentFileResponse.builder()
                                                  .id(12L)
                                                  .build());
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(true);
        when(documentFileTransferService.findAllDocumentFilesPaged(PAGED_REQUEST, 9L)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(9L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(false);

            var response = controller.findAllDocumentFiles(PAGED_REQUEST, null);

            assertEquals(expected, response.getBody());
            verify(documentFileTransferService).findAllDocumentFilesPaged(PAGED_REQUEST, 9L);
        }
    }

    @Test
    void findAllDocumentFiles_nonAdminWithOtherCreatorId_throwsUnauthorized() {
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(true);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(9L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(false);

            assertThrows(OxalateUnauthorizedException.class, () -> controller.findAllDocumentFiles(PAGED_REQUEST, 15L));
            verify(documentFileTransferService, never()).findAllDocumentFilesPaged(any(), any());
        }
    }

    @Test
    void findAllDocumentFiles_whenFeatureDisabled_returnsEmptyPageWithoutServiceCall() {
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(false);

        var response = controller.findAllDocumentFiles(PAGED_REQUEST, null);

        assertTrue(response.getBody()
                           .isEmpty());
        assertEquals(10, response.getBody()
                                 .getSize());
        verify(documentFileTransferService, never()).findAllDocumentFilesPaged(any(), any());
    }

    private static <T> PagedResponse<T> pageOf(T item) {
        return PagedResponse.of(List.of(item), 0, 10, 1);
    }

    @Test
    void downloadDiveFile_delegatesToService() {
        var expected = ResponseEntity.ok(new byte[] { 1, 2, 3 });
        when(diveFileTransferService.downloadDiveFile(11L)).thenReturn(expected);

        var response = controller.downloadDiveFile(11L);

        assertEquals(expected, response);
        verify(diveFileTransferService).downloadDiveFile(11L);
    }

    @Test
    void removeDiveFile_delegatesToServiceWithCurrentUser() {
        var expected = ActionResponse.builder()
                                     .status(OK)
                                     .message("Dive file removed")
                                     .build();
        when(diveFileTransferService.removeDiveFile(11L, 7L)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);

            var response = controller.removeDiveFile(11L);

            assertEquals(expected, response.getBody());
            verify(diveFileTransferService).removeDiveFile(11L, 7L);
        }
    }

    @Test
    void findAllDiveFiles_asAdmin_returnsAllFiles() {
        var expected = pageOf(DiveFileResponse.builder()
                                              .id(21L)
                                              .build());
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DIVE_FILES_SUPPORTED.key)).thenReturn(true);
        when(diveFileTransferService.findAllDiveFilesPaged(PAGED_REQUEST, 5L)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(true);

            var response = controller.findAllDiveFiles(PAGED_REQUEST, 5L);

            assertEquals(expected, response.getBody());
        }
    }

    @Test
    void findAllDiveFiles_asNonAdmin_throwsUnauthorized() {
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DIVE_FILES_SUPPORTED.key)).thenReturn(true);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(false);

            assertThrows(OxalateUnauthorizedException.class, () -> controller.findAllDiveFiles(PAGED_REQUEST, null));
        }
    }

    @Test
    void findAllDiveFiles_whenFeatureDisabled_returnsEmptyPageWithoutServiceCall() {
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DIVE_FILES_SUPPORTED.key)).thenReturn(false);

        var response = controller.findAllDiveFiles(PAGED_REQUEST, null);

        assertTrue(response.getBody()
                           .isEmpty());
        verify(diveFileTransferService, never()).findAllDiveFilesPaged(any(), any());
    }

    @Test
    void uploadDiveFile_returnsUploadResponseFromService() {
        var uploadFile = new MockMultipartFile("upload_file", "plan.pdf", "application/pdf", new byte[] { 1 });
        var expected = UploadResponse.builder()
                                     .url("http://localhost/api/files/dive-files/21")
                                     .build();
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DIVE_FILES_SUPPORTED.key)).thenReturn(true);
        when(diveFileTransferService.uploadDiveFile(uploadFile, 42L, 7L, 9L)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(9L);

            var response = controller.uploadDiveFile(uploadFile, 42L, 7L);

            assertEquals(expected, response.getBody());
        }
    }

    @Test
    void uploadDiveFile_whenServiceThrows_throwsValidationException() {
        var uploadFile = new MockMultipartFile("upload_file", "plan.pdf", "application/pdf", new byte[] { 1 });
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DIVE_FILES_SUPPORTED.key)).thenReturn(true);
        when(diveFileTransferService.uploadDiveFile(uploadFile, 42L, 7L, 9L)).thenThrow(new RuntimeException("boom"));

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(9L);

            assertThrows(OxalateValidationException.class, () -> controller.uploadDiveFile(uploadFile, 42L, 7L));
        }
    }

    @Test
    void uploadDiveFile_whenServiceReturnsNull_throwsValidationException() {
        var uploadFile = new MockMultipartFile("upload_file", "plan.pdf", "application/pdf", new byte[] { 1 });
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DIVE_FILES_SUPPORTED.key)).thenReturn(true);
        when(diveFileTransferService.uploadDiveFile(uploadFile, 42L, 7L, 9L)).thenReturn(null);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(9L);

            assertThrows(OxalateValidationException.class, () -> controller.uploadDiveFile(uploadFile, 42L, 7L));
        }
    }

    @Test
    void uploadDiveFile_whenFeatureDisabled_throwsValidationException() {
        var uploadFile = new MockMultipartFile("upload_file", "plan.pdf", "application/pdf", new byte[] { 1 });
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DIVE_FILES_SUPPORTED.key)).thenReturn(false);

        assertThrows(OxalateValidationException.class, () -> controller.uploadDiveFile(uploadFile, 42L, 7L));
        verify(diveFileTransferService, never()).uploadDiveFile(uploadFile, 42L, 7L, 9L);
    }

    @Test
    void findAllCertificateFiles_nonAdmin_throwsUnauthorized() {
        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(false);

            assertThrows(OxalateUnauthorizedException.class, () -> controller.findAllCertificateFiles(PAGED_REQUEST));
        }
    }

    @Test
    void findAllCertificateFiles_adminReturnsFiles() {
        var expected = pageOf(CertificateFileResponse.builder()
                                                     .id(8L)
                                                     .build());
        when(certificateFileTransferService.findAllCertificateFilesPaged(PAGED_REQUEST)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(true);

            assertEquals(expected, controller.findAllCertificateFiles(PAGED_REQUEST)
                                             .getBody());
        }
    }

    @Test
    void uploadCertificateFile_serviceFailureReturnsValidationError() {
        var uploadFile = new MockMultipartFile("upload_file", "cert.jpg", "image/jpeg", new byte[] { 1 });
        when(certificateFileTransferService.uploadCertificateFile(uploadFile, 7L, 8L))
                .thenThrow(new RuntimeException("storage failed"));

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);

            assertThrows(OxalateValidationException.class, () -> controller.uploadCertificateFile(uploadFile, 8L));
        }
    }

    @Test
    void downloadCertificateFile_rejectsAnonymousRoles() {
        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(AuthTools::getUserRoles)
                     .thenReturn(java.util.Set.of(io.oxalate.backend.api.RoleEnum.ROLE_ANONYMOUS));

            assertThrows(OxalateUnauthorizedException.class, () -> controller.downloadCertificateFile(8L));
        }
    }
}

