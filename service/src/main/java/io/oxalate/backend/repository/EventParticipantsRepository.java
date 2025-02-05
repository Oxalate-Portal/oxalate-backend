package io.oxalate.backend.repository;

import io.oxalate.backend.model.EventsParticipant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface EventParticipantsRepository extends CrudRepository<EventsParticipant, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM event_participants ep WHERE ep.event_id = :eventId ORDER BY user_id ASC")
    Set<EventsParticipant> findEventDives(@Param("eventId") long eventId);

    @Query(nativeQuery = true, value = "SELECT ep.dive_count FROM event_participants ep WHERE ep.user_id = :userId AND ep.event_id = :eventId")
    long countDivesByUserIdAndEventId(@Param("userId") long userId, @Param("eventId") long eventId);

    EventsParticipant findByEventIdAndUserId(long eventId, Long userId);

    List<EventsParticipant> findAllByEventId(long eventId);

    @Query(nativeQuery = true, value = """
            SELECT DISTINCT ep.event_id
            FROM event_participants ep, events e\s
            WHERE ep.user_id = 100
              AND ep.event_id = e.id
              AND ep.payment_type = 'ONE_TIME'
              AND e.start_time + (e.event_duration * INTERVAL '1 hour') > NOW()
            ORDER BY ep.event_id
            """)
	List<Long> findOneTimeFutureEventParticipantsByUserId(long userId);
}
