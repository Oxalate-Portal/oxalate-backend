package io.oxalate.backend.service;

import io.oxalate.backend.api.request.CertificateClassificationRequest;
import io.oxalate.backend.model.CertificateClassification;
import io.oxalate.backend.repository.CertificateClassificationRepository;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.filetransfer.CertificateDocumentRepository;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificateServiceUTC {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CertificateDocumentRepository certificateDocumentRepository;

    @Mock
    private PortalConfigurationService portalConfigurationService;

    @Mock
    private CertificateClassificationRepository classificationRepository;

    @InjectMocks
    private CertificateService certificateService;

    @Test
    void createsClassificationWhenFrontendUsesZeroId() {
        var request = new CertificateClassificationRequest();
        request.setId(0L);
        request.setTitles(Map.of("en", "Open water"));
        request.setDescription("Description");

        var saved = new CertificateClassification();
        saved.setId(1L);
        when(classificationRepository.save(any(CertificateClassification.class))).thenReturn(saved);

        var response = certificateService.saveClassification(request);

        assertEquals(1L, response.getId());
        verify(classificationRepository, never()).findById(0L);
        verify(classificationRepository).save(any(CertificateClassification.class));
    }

    @Test
    void assignsClassificationToAllSelectedCertificateNames() {
        var request = new io.oxalate.backend.api.request.CertificateClassificationAssignmentRequest();
        request.setCertificateNames(List.of("Open Water", "Advanced Open Water"));
        request.setClassificationId(2L);
        when(classificationRepository.existsById(2L)).thenReturn(true);
        when(certificateRepository.updateClassificationByNames(request.getCertificateNames(), 2L)).thenReturn(2);

        assertEquals(2, certificateService.updateClassification(request));
        verify(certificateRepository).updateClassificationByNames(request.getCertificateNames(), 2L);
    }

    @Test
    void returnsClassificationsInDescendingOrder() {
        var least = new CertificateClassification();
        least.setId(1L);
        least.setOrder(1);
        var middle = new CertificateClassification();
        middle.setId(2L);
        middle.setOrder(2);
        var most = new CertificateClassification();
        most.setId(3L);
        most.setOrder(3);
        when(classificationRepository.findAllByOrderByOrderDescIdAsc()).thenReturn(List.of(most, middle, least));

        var result = certificateService.findAllClassifications();

        assertEquals(List.of(3, 2, 1), result.stream()
                                             .map(response -> response.getOrder())
                                             .toList());
        verify(classificationRepository).findAllByOrderByOrderDescIdAsc();
    }
}
