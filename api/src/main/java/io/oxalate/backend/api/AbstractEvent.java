package io.oxalate.backend.api;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public abstract class AbstractEvent {

    private long id;

    private DiveTypeEnum type;

    @Size(min = 4, message = "Event title must be longer than 4 characters long")
    private String title;

    @Size(min = 20, max = 15_000, message = "Event description must be between 20 and 15000 characters long")
    private String description;

    private Instant startTime;

    private int eventDuration;

    private int maxDuration;

    private int maxDepth;

    private int maxParticipants;
}
