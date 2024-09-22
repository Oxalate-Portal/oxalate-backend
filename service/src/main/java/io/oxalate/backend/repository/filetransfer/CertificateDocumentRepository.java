package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.CertificateFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateDocumentRepository extends ListCrudRepository<CertificateFile, Long> {
    Optional<CertificateFile> findByCertificateId(Long certificateId);

    @Query(value = "SELECT * FROM certificate_files c WHERE c.creator = :creator", nativeQuery = true)
    List<CertificateFile> findByCreator(long creator);

    @Modifying
    void deleteByCertificateId(Long certificateId);
}
