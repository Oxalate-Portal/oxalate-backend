package io.oxalate.backend.service;

import static io.oxalate.backend.api.PortalConfigEnum.FRONTEND;
import static io.oxalate.backend.api.PortalConfigEnum.FrontendConfigEnum.MAX_CERTIFICATES;
import static io.oxalate.backend.api.UploadDirectoryConstants.CERTIFICATES;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.api.response.CertificateResponse;
import io.oxalate.backend.model.Certificate;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.filetransfer.CertificateDocumentRepository;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateDocumentRepository certificateDocumentRepository;
    private final PortalConfigurationService portalConfigurationService;

    @Value("${oxalate.app.backend-url}")
    private String backendUrl;
    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;

    public List<CertificateResponse> findAll() {
        var certificates = certificateRepository.findAll();
        var certificateResponses = new ArrayList<CertificateResponse>();

        for (var certificate : certificates) {
            var userId = certificate.getUserId();
            var certificateResponse = certificate.toCertificateResponse();
            attachCertificateUrl(certificate.getId(), certificateResponse);
            certificateResponses.add(certificateResponse);
        }

        return certificateResponses;
    }

    public List<CertificateResponse> findByUserId(long userId) {
        var certificates = certificateRepository.findByUserIdOrderByCertificationDateAsc(userId);
        var certificateResponses = new ArrayList<CertificateResponse>();

        for (Certificate certificate : certificates) {
            var certificateResponse = certificate.toCertificateResponse();
            attachCertificateUrl(certificate.getId(), certificateResponse);
            certificateResponses.add(certificateResponse);
        }

        return certificateResponses;
    }

    public CertificateResponse addCertificate(long userId, CertificateRequest certificateRequest) {
        try {
            verifyCertificateRequest(certificateRequest);
        } catch (IllegalArgumentException e) {
            log.warn("Certificate request is invalid: {}", e.getMessage());
            return null;
        }

        // Check first whether the user has already 50 certificates
        var certificates = certificateRepository.findByUserIdOrderByCertificationDateAsc(userId);

        if (certificates.size() >= portalConfigurationService.getNumericConfiguration(FRONTEND.group, MAX_CERTIFICATES.key)) {
            log.warn("User ID {} attempted to add more than 50 certificates", userId);
            return null;
        }

        var certificate = Certificate.builder()
                .userId(userId)
                .organization(certificateRequest.getOrganization())
                .certificateName(certificateRequest.getCertificateName())
                .certificateId(certificateRequest.getCertificateId())
                .diverId(certificateRequest.getDiverId())
                .certificationDate(certificateRequest.getCertificationDate())
                .build();

        var savedCertificate = certificateRepository.save(certificate);

        return savedCertificate.toCertificateResponse();
    }

    public CertificateResponse updateCertificate(long userId, CertificateRequest certificateRequest) {
        try {
            verifyCertificateRequest(certificateRequest);
        } catch (IllegalArgumentException e) {
            log.warn("Certificate request is invalid: {}", e.getMessage());
            return null;
        }

        var optionalCertificate = certificateRepository.findById(certificateRequest.getId());

        if (optionalCertificate.isEmpty()) {
            log.warn("User ID {} attempted to updated certificate with ID {}, which does not exist", userId, certificateRequest.getId());
            return null;
        }

        var certificate = optionalCertificate.get();
        certificate.setCertificateId(certificateRequest.getCertificateId());
        certificate.setDiverId(certificateRequest.getDiverId());
        certificate.setOrganization(certificateRequest.getOrganization());
        certificate.setCertificateName(certificateRequest.getCertificateName());
        certificate.setCertificationDate(certificateRequest.getCertificationDate());

        var savedCertificate = certificateRepository.save(certificate);

        return savedCertificate.toCertificateResponse();
    }

    @Transactional
    public boolean deleteCertificate(long certificateId) {
        try {
            certificateRepository.deleteById(certificateId);
            certificateDocumentRepository.deleteByCertificateId(certificateId);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete certificate: {}", e.getMessage());
        }

        return false;
    }

    public CertificateResponse findById(long certificateId) {
        var optionalCertificate = certificateRepository.findById(certificateId);

        return optionalCertificate.map(Certificate::toCertificateResponse).orElse(null);
    }


    public CertificateResponse findCertificateByUserOrgAndCertification(Long userId, String organization, String certificateName) {
        var optionalCertificate = certificateRepository.findByUserIdAndOrganizationAndAndCertificateName(userId, organization, certificateName);
        return optionalCertificate.map(Certificate::toCertificateResponse).orElse(null);
    }

    @Transactional
    public void anonymize(long userId) {
        var certificates = certificateRepository.findByUserIdOrderByCertificationDateAsc(userId);

        for (Certificate certificate : certificates) {
            certificate.setDiverId(UUID.randomUUID().toString());
            certificate.setCertificateId(UUID.randomUUID().toString());
            certificate.setCertificateName(UUID.randomUUID().toString());
            certificate.setCertificationDate(Instant.ofEpochSecond(31337));
        }

        certificateRepository.saveAll(certificates);
    }

    private void verifyCertificateRequest(CertificateRequest certificateRequest) {
        if (certificateRequest.getOrganization() == null || certificateRequest.getOrganization().isEmpty()) {
            throw new IllegalArgumentException("Organization cannot be empty");
        }

        if (certificateRequest.getCertificateName() == null || certificateRequest.getCertificateName().isEmpty()) {
            throw new IllegalArgumentException("Certificate name cannot be empty");
        }

        // Either certificate ID or diver ID can be empty, but not both
        if ((certificateRequest.getCertificateId() == null || certificateRequest.getCertificateId().isEmpty()) &&
                (certificateRequest.getDiverId() == null || certificateRequest.getDiverId().isEmpty())) {
            throw new IllegalArgumentException("Both Diver ID and certificate ID cannot be empty");
        }

        if (certificateRequest.getCertificationDate() == null) {
            throw new IllegalArgumentException("Certification date cannot be empty");
        }
    }

    private void attachCertificateUrl(long certificateId, CertificateResponse certificateResponse) {
        // Check if there is a certificate document related to this certificate
        var optionalCertificateDocument = certificateDocumentRepository.findByCertificateId(certificateId);

        if (optionalCertificateDocument.isPresent()) {
            var certificateDocument = optionalCertificateDocument.get();
            // Url path only contains the certificate ID, as user ID is pulled from the session data
            var urlPath = backendUrl + FILES_URL + "/" + CERTIFICATES + "/" + certificateDocument.getCertificate().getId();
            // Physical path on the other hand contains the user ID as a path segment while the filename is the certificate ID
            var physicalPath = Paths.get(uploadMainDirectory, CERTIFICATES, String.valueOf(certificateDocument.getCreator().getId()), certificateDocument.getFileName());

            var file = physicalPath.toFile();

            if (!file.exists()) {
                log.error("File not found on filesystem to be attached: {}", file);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            certificateResponse.setCertificatePhotoUrl(urlPath);
        }
    }
}
