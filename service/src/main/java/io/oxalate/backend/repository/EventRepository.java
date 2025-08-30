package io.oxalate.backend.repository;

import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.model.Event;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends CrudRepository<Event, Long> {
    List<Event> findByStartTimeAfterOrderByStartTimeAsc(Instant instant);

    List<Event> findByStatusAndStartTimeAfterOrderByStartTimeAsc(EventStatusEnum status, Instant instant);

    @Query(nativeQuery = true, value = "SELECT * FROM events e WHERE e.start_time < :until ORDER BY e.start_time DESC")
    List<Event> findAllEventsBefore(Instant until);

    @Query(nativeQuery = true, value =
            "SELECT e.* FROM events e, event_participants ep WHERE e.id = ep.event_id AND ep.user_id = :userId ORDER BY e.start_time ASC")
    List<Event> findByUserId(@Param("userId") long userId);

    @Query(nativeQuery = true, value =
            "SELECT e.* FROM events e, event_participants ep WHERE e.id = ep.event_id AND ep.user_id = :userId AND e.start_time > NOW() ORDER BY e.start_time ASC")
    List<Event> findFutureEventsByUserId(@Param("userId") long userId);

    @Query(nativeQuery = true, value = "SELECT * FROM events e WHERE e.organizer_id = :userId ORDER BY e.start_time ASC")
    List<Event> findByOrganizer(@Param("userId") long userId);

    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO event_participants (user_id, event_id, participant_type, payment_type, created_at, event_user_type)  " +
            "VALUES(:userId, :eventId, :participantType, :paymentType, :createTime, :eventUserType)")
    void addParticipantToEvent(@Param("userId") long userId, @Param("eventId") long eventId, @Param("participantType") String participantType,
            @Param("paymentType") String paymentType, @Param("createTime") Instant createTime, @Param("eventUserType") String eventUserType);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM event_participants WHERE user_id = :userId AND event_id = :eventId")
    void removeParticipantFromEvent(@Param("userId") long userId, @Param("eventId") long eventId);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM event_participants WHERE event_id = :eventId AND participant_type = :participantType")
    void removeAllParticipantsFromEvent(@Param("eventId") long eventId, @Param("participantType") String participantType);

    @Query(nativeQuery = true, value = "SELECT COALESCE(SUM(ep.dive_count), 0) AS dive_count FROM event_participants ep WHERE ep.user_id = :userId")
    long countDivesByUserId(@Param("userId") long userId);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE event_participants ep SET dive_count = :diveCount WHERE ep.event_id = :eventId AND ep.user_id = :userId")
    void updateEventUserDiveCount(@Param("eventId") long eventId, @Param("userId") long userId, @Param("diveCount") long diveCount);

    @Query(nativeQuery = true, value = "SELECT * FROM events e WHERE e.start_time < NOW() AND NOW() < (e.start_time + e.event_duration * INTERVAL '1 hour') ORDER BY e.start_time ASC")
    List<Event> findAllCurrentEvents();

    @Query(value = "SELECT ep.user_id AS id, " +
            "u.first_name AS first_name, " +
            "u.last_name AS last_name, " +
            "COALESCE(SUM(ep.dive_count), 0) AS dive_count " +
            "FROM event_participants ep, users u " +
            "WHERE u.id = ep.user_id " +
            "GROUP BY ep.user_id, u.first_name, u.last_name " +
            "ORDER BY dive_count DESC", nativeQuery = true)
    List<Object[]> getMemberDiveCount();

    @Query(nativeQuery = true, value = "SELECT * FROM events WHERE status = 'PUBLISHED' AND (start_time + (event_duration * interval '1 hour')) < NOW()")
    List<Event> findEventsToMarkAsHeld();

    @Modifying
    @Query("UPDATE Event e SET e.status = :status WHERE e.id = :eventId")
    void updateEventStatus(@Param("eventId") long eventId, @Param("status") EventStatusEnum status);
}
