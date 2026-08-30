package io.oxalate.backend.repository;

import io.oxalate.backend.model.CertificateClassification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateClassificationRepository extends JpaRepository<CertificateClassification, Long> {
    @EntityGraph(attributePaths = "translations")
    List<CertificateClassification> findAllByOrderByOrderDescIdAsc();

    @EntityGraph(attributePaths = "translations")
    Optional<CertificateClassification> findById(Long id);
}
