package io.oxalate.backend.controller;

import static io.oxalate.backend.api.PortalConfigEnum.FILES;
import static io.oxalate.backend.api.PortalConfigEnum.FileConfigEnum.DOCUMENTS_SUPPORTED;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.response.filetransfer.DocumentFileResponse;
import io.oxalate.backend.service.PortalConfigurationService;
import io.oxalate.backend.service.filetransfer.AvatarFileTransferService;
import io.oxalate.backend.service.filetransfer.CertificateFileTransferService;
import io.oxalate.backend.service.filetransfer.DiveFileTransferService;
import io.oxalate.backend.service.filetransfer.DocumentFileTransferService;
import io.oxalate.backend.service.filetransfer.PageFileTransferService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void findAllDocumentFiles_adminWithCreatorId_returnsCreatorScopedFiles() {
        var expected = List.of(DocumentFileResponse.builder()
                                                   .id(11L)
                                                   .build());
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(true);
        when(documentFileTransferService.findDocumentFilesByCreatorId(15L)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(7L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(true);

            var response = controller.findAllDocumentFiles(15L);

            assertEquals(expected, response.getBody());
            verify(documentFileTransferService).findDocumentFilesByCreatorId(15L);
            verify(documentFileTransferService, never()).findAllDocumentFiles();
        }
    }

    @Test
    void findAllDocumentFiles_nonAdminWithoutCreatorId_returnsOwnFiles() {
        var expected = List.of(DocumentFileResponse.builder()
                                                   .id(12L)
                                                   .build());
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(true);
        when(documentFileTransferService.findDocumentFilesByCreatorId(9L)).thenReturn(expected);

        try (MockedStatic<AuthTools> authTools = mockStatic(AuthTools.class)) {
            authTools.when(AuthTools::getCurrentUserId)
                     .thenReturn(9L);
            authTools.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                     .thenReturn(false);

            var response = controller.findAllDocumentFiles(null);

            assertEquals(expected, response.getBody());
            verify(documentFileTransferService).findDocumentFilesByCreatorId(9L);
            verify(documentFileTransferService, never()).findAllDocumentFiles();
        }
    }

    @Test
    void findAllDocumentFiles_whenFeatureDisabled_returnsEmptyListWithoutServiceCall() {
        when(portalConfigurationService.getBooleanConfiguration(FILES.group, DOCUMENTS_SUPPORTED.key)).thenReturn(false);

        var response = controller.findAllDocumentFiles(null);

        assertEquals(List.of(), response.getBody());
        verify(documentFileTransferService, never()).findAllDocumentFiles();
        verify(documentFileTransferService, never()).findDocumentFilesByCreatorId(anyLong());
    }
}


