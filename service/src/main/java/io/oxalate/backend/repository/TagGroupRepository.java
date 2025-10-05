package io.oxalate.backend.repository;

import io.oxalate.backend.api.TagGroupEnum;
import io.oxalate.backend.model.TagGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagGroupRepository extends JpaRepository<TagGroup, Long> {
    Optional<TagGroup> findByCode(String code);
    boolean existsByCode(String code);

    @Query("SELECT tg FROM TagGroup tg LEFT JOIN FETCH tg.translations WHERE tg.id = :id")
    Optional<TagGroup> findByIdWithTranslations(@Param("id") Long id);

    @Query("SELECT tg FROM TagGroup tg LEFT JOIN FETCH tg.translations")
    List<TagGroup> findAllWithTranslations();

    @Query("SELECT DISTINCT tg FROM TagGroup tg " +
           "LEFT JOIN FETCH tg.translations " +
           "LEFT JOIN FETCH tg.tags t " +
           "LEFT JOIN FETCH t.translations")
    List<TagGroup> findAllWithTagsAndTranslations();

    List<TagGroup> findByType(TagGroupEnum type);

    @Query("SELECT tg FROM TagGroup tg LEFT JOIN FETCH tg.translations WHERE tg.type = :type")
    List<TagGroup> findByTypeWithTranslations(@Param("type") TagGroupEnum type);

    @Query("SELECT DISTINCT tg FROM TagGroup tg " +
            "LEFT JOIN FETCH tg.translations " +
            "LEFT JOIN FETCH tg.tags t " +
            "LEFT JOIN FETCH t.translations " +
            "WHERE tg.type = :type")
    List<TagGroup> findByTypeWithTagsAndTranslations(@Param("type") TagGroupEnum type);
}
