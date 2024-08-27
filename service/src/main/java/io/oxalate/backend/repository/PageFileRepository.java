package io.oxalate.backend.repository;

import io.oxalate.backend.model.PageFile;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageFileRepository extends ListCrudRepository<PageFile, Long> {
    Optional<PageFile> findByPageIdAndLanguageAndFileName(long pageId, String language, String fileName);
}
