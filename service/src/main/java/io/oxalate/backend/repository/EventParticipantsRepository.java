package io.oxalate.backend.repository;

import io.oxalate.backend.model.EventsParticipant;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface EventParticipantsRepository extends CrudRepository<EventsParticipant, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM event_participants ep WHERE ep.event_id = :eventId ORDER BY user_id ASC")
    Set<EventsParticipant> findEventDives(@Param("eventId") long eventId);

    @Query(nativeQuery = true, value = "SELECT ep.dive_count FROM event_participants ep WHERE ep.user_id = :userId AND ep.event_id = :eventId")
    long countDivesByUserIdAndEventId(@Param("userId") long userId, @Param("eventId") long eventId);

    EventsParticipant findByEventIdAndUserId(long eventId, Long userId);

    @Query(nativeQuery = true, value = "SELECT * FROM event_participants ep WHERE ep.event_id = :eventId AND ep.participant_type = 'WAITING_LIST' ORDER BY ep.created_at ASC")
    List<EventsParticipant> findWaitingListByEventId(@Param("eventId") long eventId);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM event_participants ep WHERE ep.event_id = :eventId AND ep.participant_type = 'WAITING_LIST'")
    long countWaitingListByEventId(@Param("eventId") long eventId);

    @Query(nativeQuery = true, value = "SELECT * FROM event_participants ep WHERE ep.event_id = :eventId AND ep.user_id = :userId AND ep.participant_type = 'WAITING_LIST'")
    Optional<EventsParticipant> findWaitingListEntryByEventIdAndUserId(@Param("eventId") long eventId, @Param("userId") long userId);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE event_participants SET participant_type = 'USER', payment_type = :paymentType, notified_at = NULL WHERE event_id = :eventId AND user_id = :userId AND participant_type = 'WAITING_LIST'")
    void promoteWaitingListUser(@Param("eventId") long eventId, @Param("userId") long userId, @Param("paymentType") String paymentType);

    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM event_participants ep
            WHERE ep.participant_type = 'WAITING_LIST'
              AND ep.event_id IN (
                SELECT e.id FROM events e WHERE e.start_time < :now
              )
            """)
    void removeWaitingListForPastEvents(@Param("now") Instant now);


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
