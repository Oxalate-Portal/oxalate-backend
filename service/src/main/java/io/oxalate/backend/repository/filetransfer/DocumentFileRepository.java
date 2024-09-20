package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.DocumentFile;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentFileRepository extends ListCrudRepository<DocumentFile, Long> {
    Optional<DocumentFile> findByFileName(String fileName);
}
