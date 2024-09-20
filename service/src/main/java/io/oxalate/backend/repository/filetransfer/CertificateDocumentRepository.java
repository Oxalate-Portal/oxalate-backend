package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.CertificateFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateDocumentRepository extends ListCrudRepository<CertificateFile, Long> {
    Optional<CertificateFile> findByCertificateId(Long certificateId);

    @Modifying
    void deleteByCertificateId(Long certificateId);
}
