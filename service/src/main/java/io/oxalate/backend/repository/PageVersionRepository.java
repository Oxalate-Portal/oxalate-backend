package io.oxalate.backend.repository;

import io.oxalate.backend.model.PageVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageVersionRepository extends JpaRepository<PageVersion, Long> {
    List<PageVersion> findAllByPageIdOrderByLanguage(long pageId);

    Optional<PageVersion> findByPageIdAndLanguage(Long pageId, String language);
}
