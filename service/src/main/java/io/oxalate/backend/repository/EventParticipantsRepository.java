package io.oxalate.backend.repository;

import io.oxalate.backend.model.EventsParticipant;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface EventParticipantsRepository extends CrudRepository<EventsParticipant, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM event_participants ep WHERE ep.event_id = :eventId ORDER BY user_id ASC")
    Set<EventsParticipant> findEventDives(@Param("eventId") long eventId);

    @Query(nativeQuery = true, value = "SELECT ep.dive_count FROM event_participants ep WHERE ep.user_id = :userId AND ep.event_id = :eventId")
    long countDivesByUserIdAndEventId(@Param("userId") long userId, @Param("eventId") long eventId);
}