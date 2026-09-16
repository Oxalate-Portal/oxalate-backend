package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.api.response.CertificateResponse;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.service.CertificateService;
import io.oxalate.backend.tools.AuthTools;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;

class CertificateControllerUTC {

    private final CertificateService certificateService = mock(CertificateService.class);
    private final CertificateController controller = new CertificateController(certificateService);

    @Test
    void adminCanListAndUserCanReadOwnCertificate() {
        var certificate = CertificateResponse.builder()
                                             .id(4L)
                                             .userId(7L)
                                             .build();
        when(certificateService.findAll()).thenReturn(java.util.List.of(certificate));
        when(certificateService.findById(4L)).thenReturn(certificate);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(true);
            assertEquals(1, controller.getAllCertificates()
                                      .getBody()
                                      .size());
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN))
                .thenReturn(false);
            auth.when(() -> AuthTools.isUserIdCurrentUser(7L))
                .thenReturn(true);
            assertEquals(certificate, controller.getCertificate(4L)
                                                .getBody());
        }
    }

    @Test
    void certificateReadsRejectUnauthorizedAndMissingValues() {
        when(certificateService.findById(4L)).thenReturn(null);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            assertThrows(OxalateNotFoundException.class, () -> controller.getCertificate(4L));
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(false);
            assertThrows(OxalateUnauthorizedException.class, controller::getAllCertificates);
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN))
                .thenReturn(false);
            auth.when(() -> AuthTools.isUserIdCurrentUser(7L))
                .thenReturn(false);
            when(certificateService.findById(4L)).thenReturn(CertificateResponse.builder()
                                                                                .userId(7L)
                                                                                .build());
            assertThrows(OxalateUnauthorizedException.class, () -> controller.getCertificate(4L));
            assertThrows(OxalateUnauthorizedException.class, () -> controller.getUserCertificates(8L));
        }
    }

    @Test
    void userCertificateListAllowsOwnerAndOrganizer() {
        var certificates = java.util.List.of(CertificateResponse.builder()
                                                                .id(1L)
                                                                .build());
        when(certificateService.findByUserId(7L)).thenReturn(certificates);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN))
                .thenReturn(false);
            auth.when(() -> AuthTools.isUserIdCurrentUser(7L))
                .thenReturn(true);
            assertEquals(certificates, controller.getUserCertificates(7L)
                                                 .getBody());
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN))
                .thenReturn(true);
            assertEquals(certificates, controller.getUserCertificates(7L)
                                                 .getBody());
        }
    }

    @Test
    void addAndUpdateCertificateHandleSuccessAndFailures() {
        var request = new CertificateRequest();
        request.setId(4L);
        var certificate = CertificateResponse.builder()
                                             .id(4L)
                                             .userId(7L)
                                             .build();
        when(certificateService.addCertificate(7L, request)).thenReturn(certificate);
        when(certificateService.findById(4L)).thenReturn(certificate);
        when(certificateService.updateCertificate(7L, request)).thenReturn(certificate);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(AuthTools::getCurrentUserId)
                .thenReturn(7L);
            assertEquals(certificate, controller.addCertificate(request)
                                                .getBody());
            assertEquals(certificate, controller.updateCertificate(request)
                                                .getBody());
            when(certificateService.addCertificate(7L, request)).thenReturn(null);
            assertThrows(OxalateValidationException.class, () -> controller.addCertificate(request));
            when(certificateService.updateCertificate(7L, request)).thenReturn(null);
            assertThrows(OxalateValidationException.class, () -> controller.updateCertificate(request));
        }
    }

    @Test
    void updateCertificateChecksOwnershipAndExistence() {
        var request = new CertificateRequest();
        request.setId(4L);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(AuthTools::getCurrentUserId)
                .thenReturn(7L);
            when(certificateService.findById(4L)).thenReturn(null);
            assertThrows(OxalateNotFoundException.class, () -> controller.updateCertificate(request));
            when(certificateService.findById(4L)).thenReturn(CertificateResponse.builder()
                                                                                .userId(8L)
                                                                                .build());
            assertThrows(OxalateUnauthorizedException.class, () -> controller.updateCertificate(request));
        }
    }

    @Test
    void deleteCertificateChecksOwnershipAndServiceResult() {
        var certificate = CertificateResponse.builder()
                                             .id(4L)
                                             .userId(7L)
                                             .build();
        when(certificateService.findById(4L)).thenReturn(certificate);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(AuthTools::getCurrentUserId)
                .thenReturn(7L);
            when(certificateService.deleteCertificate(4L)).thenReturn(true);
            assertEquals(HttpStatus.OK, controller.deleteCertificate(4L)
                                                  .getStatusCode());
            when(certificateService.deleteCertificate(4L)).thenReturn(false);
            assertThrows(OxalateValidationException.class, () -> controller.deleteCertificate(4L));
            when(certificateService.findById(4L)).thenReturn(null);
            assertThrows(OxalateNotFoundException.class, () -> controller.deleteCertificate(4L));
        }
    }

    @Test
    void adminClassificationOperationsReturnSuccessAndMapFailures() {
        var request = mock(io.oxalate.backend.api.request.CertificateClassificationAssignmentRequest.class);
        var replacement = mock(io.oxalate.backend.api.request.CertificateValueReplacementRequest.class);
        when(certificateService.updateClassification(request)).thenReturn(1);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(true);
            assertEquals(HttpStatus.OK, controller.updateClassification(request)
                                                  .getStatusCode());
            assertEquals(HttpStatus.OK, controller.replaceOrganizations(replacement)
                                                  .getStatusCode());
            assertEquals(HttpStatus.OK, controller.replaceCertificateNames(replacement)
                                                  .getStatusCode());
            when(certificateService.updateClassification(request)).thenReturn(0);
            assertThrows(OxalateNotFoundException.class, () -> controller.updateClassification(request));
            when(certificateService.updateClassification(request)).thenThrow(new IllegalArgumentException("bad"));
            assertThrows(OxalateValidationException.class, () -> controller.updateClassification(request));
        }
    }

    @Test
    void adminClassificationRequiresRoleAndReplacementMapsFailure() {
        var request = mock(io.oxalate.backend.api.request.CertificateValueReplacementRequest.class);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(false);
            assertThrows(OxalateUnauthorizedException.class, () -> controller.replaceOrganizations(request));
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(true);
            when(certificateService.replaceOrganizations(request)).thenThrow(new IllegalArgumentException("bad"));
            assertThrows(OxalateValidationException.class, () -> controller.replaceOrganizations(request));
            when(certificateService.replaceCertificateNames(request)).thenThrow(new IllegalArgumentException("bad"));
            assertThrows(OxalateValidationException.class, () -> controller.replaceCertificateNames(request));
        }
    }

    @Test
    void certificateSearchDelegates() {
        when(certificateService.findCertificateNames("div")).thenReturn(java.util.List.of("Diver"));
        when(certificateService.findOrganizations("club")).thenReturn(java.util.List.of("Club"));

        assertEquals(java.util.List.of("Diver"), controller.findCertificateNames("div")
                                                           .getBody());
        assertEquals(java.util.List.of("Club"), controller.findOrganizations("club")
                                                          .getBody());
    }
}
