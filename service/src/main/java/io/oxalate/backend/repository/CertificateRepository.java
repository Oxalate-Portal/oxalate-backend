package io.oxalate.backend.repository;

import io.oxalate.backend.model.Certificate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateRepository extends CrudRepository<Certificate, Long> {

    Optional<Certificate> findByUserIdAndOrganizationAndAndCertificateName(long userId, String organization, String certificateName);
    List<Certificate> findByUserIdOrderByCertificationDateAsc(long userId);
}