package io.oxalate.backend.repository;

import io.oxalate.backend.model.CertificateDocument;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateDocumentRepository extends ListCrudRepository<CertificateDocument, Long> {
}
