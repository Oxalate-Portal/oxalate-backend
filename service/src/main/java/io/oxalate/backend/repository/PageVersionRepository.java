package io.oxalate.backend.repository;

import io.oxalate.backend.model.PageVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageVersionRepository extends CrudRepository<PageVersion, Long> {
    List<PageVersion> findAllByPageIdOrderByLanguage(long pageId);

    Optional<PageVersion> findByPageIdAndLanguage(Long pageId, String language);
}
