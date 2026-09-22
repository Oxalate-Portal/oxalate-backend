package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.DocumentFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long>, JpaSpecificationExecutor<DocumentFile> {
    Optional<DocumentFile> findByFileName(String fileName);
}
