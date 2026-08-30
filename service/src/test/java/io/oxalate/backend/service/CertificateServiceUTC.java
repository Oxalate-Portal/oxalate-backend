package io.oxalate.backend.service;

import io.oxalate.backend.api.request.CertificateClassificationRequest;
import io.oxalate.backend.model.CertificateClassification;
import io.oxalate.backend.repository.CertificateClassificationRepository;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.filetransfer.CertificateDocumentRepository;
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
}
