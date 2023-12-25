package io.oxalate.backend.model;

import io.oxalate.backend.api.response.EventListResponse;
import io.oxalate.backend.api.response.EventResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    private String type;

    @Column(name = "title")
    @Size(min = 4, message = "Event title must be longer than 4 characters long")
    private String title;

    @Size(min = 20, max = 15_000, message = "Event description must be between 20 and 15000 characters long")
    @Column(name = "description")
    private String description;

    @Column(name = "start_time")
    private Timestamp startTime;

    @Column(name = "event_duration")
    private int eventDuration;

    @Column(name = "max_duration")
    private int maxDuration;

    @Column(name = "max_depth")
    private int maxDepth;

    @Column(name = "max_participants")
    private int maxParticipants;

    @Column(name = "organizer_id")
    private long organizerId;

    @Column(name = "published")
    private boolean published;

    /**
     * Converts this Event to EventResponse. Note that organizer and participants are not populated.
     *
     * @return EventResponse without the organizer and participants populated
     */
    public EventResponse toEventResponse() {

        return EventResponse.builder()
                .id(this.id)
                .description(this.description)
                .eventDuration(this.eventDuration)
                .maxDepth(this.maxDepth)
                .maxDuration(this.maxDuration)
                .maxParticipants(this.maxParticipants)
                .organizer(null)
                .participants(null)
                .published(this.published)
                .startTime(this.startTime)
                .title(this.title)
                .type(this.type)
                .build();
    }

    public EventListResponse toEventListResponse() {
        return EventListResponse.builder()
                .id(this.id)
                .description(this.description)
                .eventDuration(this.eventDuration)
                .maxDepth(this.maxDepth)
                .maxDuration(this.maxDuration)
                .maxParticipants(this.maxParticipants)
                .organizerName(null)
                .startTime(this.startTime)
                .title(this.title)
                .type(this.type)
                .build();
    }
}
