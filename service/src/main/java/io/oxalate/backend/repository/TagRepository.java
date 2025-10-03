package io.oxalate.backend.repository;

import io.oxalate.backend.model.Tag;
import io.oxalate.backend.model.TagGroup;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByCode(String code);
    List<Tag> findByTagGroup(TagGroup tagGroup);
    List<Tag> findByTagGroupId(Long tagGroupId);
    boolean existsByCode(String code);

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.translations WHERE t.id = :id")
    Optional<Tag> findByIdWithTranslations(@Param("id") Long id);

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.translations")
    List<Tag> findAllWithTranslations();

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.translations WHERE t.tagGroup.id = :tagGroupId")
    List<Tag> findByTagGroupIdWithTranslations(@Param("tagGroupId") Long tagGroupId);

    @Query("SELECT t FROM Tag t JOIN t.users u WHERE u.id = :userId")
    Set<Tag> findByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Tag t JOIN t.events e WHERE e.id = :eventId")
    Set<Tag> findByEventId(@Param("eventId") Long eventId);
}

