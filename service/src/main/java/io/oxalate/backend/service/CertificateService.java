package io.oxalate.backend.service;

import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.api.response.CertificateResponse;
import io.oxalate.backend.model.Certificate;
import io.oxalate.backend.repository.CertificateRepository;
import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;

    public List<CertificateResponse> findByUserId(long userId) {
        var certificates = certificateRepository.findByUserIdOrderByCertificationDateAsc(userId);
        var certificateDtos = new ArrayList<CertificateResponse>();

        for (Certificate certificate : certificates) {
            var certificateDto = CertificateResponse.builder()
													.id(certificate.getId())
													.organization(certificate.getOrganization())
													.certificateName(certificate.getCertificateName())
													.certificateId(certificate.getCertificateId())
													.diverId(certificate.getDiverId())
													.certificationDate(certificate.getCertificationDate().toInstant())
													.build();

            certificateDtos.add(certificateDto);
        }

        return certificateDtos;
    }

    public CertificateResponse addCertificate(long userId, CertificateRequest certificateRequest) {
        try {
            verifyCertificateRequest(certificateRequest);
        } catch (IllegalArgumentException e) {
            log.warn("Certificate request is invalid: {}", e.getMessage());
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

    public boolean deleteCertificate(long certificateId) {
        try {
            certificateRepository.deleteById(certificateId);
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
            certificate.setCertificationDate(Timestamp.from(Instant.ofEpochSecond(31337)));
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
}
