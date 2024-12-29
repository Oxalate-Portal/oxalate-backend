package io.oxalate.backend.model;

import io.oxalate.backend.api.ParticipantTypeEnum;
import io.oxalate.backend.api.PaymentTypeEnum;
import io.oxalate.backend.api.response.EventDiveResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_participants")
@Entity
@IdClass(EventsParticipantId.class)
public class EventsParticipant {
    @Id
    @Column(name = "user_id")
    private long userId;

    @Id
    @Column(name = "event_id")
    private long eventId;

    @Column(name = "dive_count")
    private int diveCount;

    @Column(name = "participant_type")
    @Enumerated(EnumType.STRING)
    private ParticipantTypeEnum participantType;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private PaymentTypeEnum paymentType;

    @Column(name = "created_at")
    private Instant createdAt;

    public EventDiveResponse toEventDiveResponse(String name) {
        return EventDiveResponse.builder()
                .userId(userId)
                .name(name)
                .diveCount(diveCount)
                .build();
    }
}
