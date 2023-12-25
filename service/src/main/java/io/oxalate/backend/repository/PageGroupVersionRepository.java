package io.oxalate.backend.repository;

import io.oxalate.backend.model.PageGroupVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageGroupVersionRepository extends CrudRepository<PageGroupVersion, Long> {
    List<PageGroupVersion> findAllByPageGroupIdOrderByLanguageAsc(Long pageGroupId);

    Optional<PageGroupVersion> findByPageGroupIdAndLanguage(Long id, String language);
}
