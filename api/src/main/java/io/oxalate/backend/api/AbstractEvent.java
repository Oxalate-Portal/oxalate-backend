package io.oxalate.backend.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractEvent {

    @JsonProperty("id")
    private long id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    @Size(min = 4, message = "Event title must be longer than 4 characters long")
    private String title;

    @JsonProperty("description")
    @Size(min = 20, max = 15_000, message = "Event description must be between 20 and 15000 characters long")
    private String description;

    @JsonProperty("startTime")
    private Instant startTime;

    @JsonProperty("eventDuration")
    private int eventDuration;

    @JsonProperty("maxDuration")
    private int maxDuration;

    @JsonProperty("maxDepth")
    private int maxDepth;

    @JsonProperty("maxParticipants")
    private int maxParticipants;
}
