package io.oxalate.backend.repository;

import io.oxalate.backend.model.DiveGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiveGroupRepository extends JpaRepository<DiveGroup, Long> {

    List<DiveGroup> findAllByEventIdOrderByCreatedAtAsc(long eventId);

    Optional<DiveGroup> findByEventIdAndOwnerId(long eventId, long ownerId);

    void deleteAllByEventId(long eventId);
}
