package io.oxalate.backend.service;

import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.filetransfer.CertificateDocumentRepository;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificateSearchServiceUTC {

    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CertificateDocumentRepository certificateDocumentRepository;
    @Mock
    private PortalConfigurationService portalConfigurationService;
    @Mock
    private io.oxalate.backend.repository.CertificateClassificationRepository classificationRepository;

    @InjectMocks
    private CertificateService certificateService;

    @Test
    void findsDistinctMatchingValuesAndTrimsSearchTerm() {
        when(certificateRepository.findDistinctCertificateNamesMatching("open")).thenReturn(List.of("Open Water"));
        when(certificateRepository.findDistinctOrganizationsMatching("padi")).thenReturn(List.of("PADI"));

        assertEquals(List.of("Open Water"), certificateService.findCertificateNames("  open "));
        assertEquals(List.of("PADI"), certificateService.findOrganizations("padi"));
        verify(certificateRepository).findDistinctCertificateNamesMatching("open");
        verify(certificateRepository).findDistinctOrganizationsMatching("padi");
    }

    @Test
    void blankOrNullSearchReturnsNoValuesWithoutQueryingRepository() {
        assertEquals(List.of(), certificateService.findCertificateNames("  "));
        assertEquals(List.of(), certificateService.findOrganizations(null));
        verify(certificateRepository, never()).findDistinctCertificateNamesMatching(org.mockito.ArgumentMatchers.anyString());
        verify(certificateRepository, never()).findDistinctOrganizationsMatching(org.mockito.ArgumentMatchers.anyString());
    }
}
