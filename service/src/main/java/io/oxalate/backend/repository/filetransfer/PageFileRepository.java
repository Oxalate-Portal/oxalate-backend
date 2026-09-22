package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.PageFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PageFileRepository extends JpaRepository<PageFile, Long>, JpaSpecificationExecutor<PageFile> {
    Optional<PageFile> findByPageIdAndLanguageAndFileName(long pageId, String language, String fileName);
}
