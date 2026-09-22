package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.model.Certificate;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.CertificateFile;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.CertificateDocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CertificateFileTransferServiceUTC {

    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CertificateDocumentRepository certificateDocumentRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private CertificateFileTransferService fileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "uploadMainDirectory", "target/test-uploads");
        ReflectionTestUtils.setField(fileService, "backendUrl", "http://localhost");
    }

    @Test
    void findAllCertificateFilesPagedMapsCertificateIdSortAndBuildsUrlOk() {
        var certificateFile = CertificateFile.builder()
                                             .id(5L)
                                             .fileName("card.png")
                                             .mimeType("image/png")
                                             .fileSize(10L)
                                             .fileChecksum("abc")
                                             .createdAt(Instant.now())
                                             .creator(User.builder()
                                                          .id(2L)
                                                          .firstName("Ann")
                                                          .lastName("Diver")
                                                          .build())
                                             .certificate(Certificate.builder()
                                                                     .id(7L)
                                                                     .build())
                                             .build();
        when(certificateDocumentRepository.findAll(ArgumentMatchers.<Specification<CertificateFile>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(certificateFile)));

        var page = fileService.findAllCertificateFilesPaged(PagedRequest.builder()
                                                                        .sortBy("certificate_id")
                                                                        .direction(Sort.Direction.ASC)
                                                                        .build());

        assertEquals("http://localhost/api/files/certificates/7", page.getContent()
                                                                      .getFirst()
                                                                      .getUrl());
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(certificateDocumentRepository).findAll(ArgumentMatchers.<Specification<CertificateFile>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("certificate.id");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void findAllCertificateFilesPagedUnknownSortFallsBackOk() {
        when(certificateDocumentRepository.findAll(ArgumentMatchers.<Specification<CertificateFile>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        fileService.findAllCertificateFilesPaged(PagedRequest.builder()
                                                             .sortBy("certificate.user_id")
                                                             .build());

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(certificateDocumentRepository).findAll(ArgumentMatchers.<Specification<CertificateFile>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void uploadMissingCertificateFailsNotFound() {
        when(certificateRepository.findById(7L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadCertificateFile(null, 2L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void uploadCertificateRejectsDifferentOwner() {
        when(certificateRepository.findById(7L)).thenReturn(Optional.of(Certificate.builder()
                                                                                   .id(7L)
                                                                                   .userId(3L)
                                                                                   .build()));

        var exception = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadCertificateFile(null, 2L, 7L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).findById(2L);
    }

    @Test
    void downloadMissingCertificateUsesForbiddenResponse() {
        when(certificateRepository.findById(7L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class,
                () -> fileService.downloadCertificateFile(7L, 2L, Set.of()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void downloadRejectsUnauthorizedUserWithoutPrivilegedRole() {
        when(certificateRepository.findById(7L)).thenReturn(Optional.of(Certificate.builder()
                                                                                   .id(7L)
                                                                                   .userId(3L)
                                                                                   .build()));

        var exception = assertThrows(ResponseStatusException.class,
                () -> fileService.downloadCertificateFile(7L, 2L, Set.of(RoleEnum.ROLE_USER)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(certificateDocumentRepository, never()).findByCertificateId(7L);
    }

    @Test
    void removeMissingDocumentFailsNotFound() {
        when(certificateDocumentRepository.findByCertificateId(7L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class,
                () -> fileService.removeCertificateFile(7L, 2L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void removeDocumentRejectsDifferentOwner() {
        var certificate = Certificate.builder()
                                     .id(7L)
                                     .userId(3L)
                                     .build();
        var document = CertificateFile.builder()
                                      .certificate(certificate)
                                      .fileName("certificate.jpg")
                                      .build();
        when(certificateDocumentRepository.findByCertificateId(7L)).thenReturn(Optional.of(document));

        var exception = assertThrows(ResponseStatusException.class,
                () -> fileService.removeCertificateFile(7L, 2L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(certificateDocumentRepository, never()).delete(document);
    }
}
