package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
public class EventListResponse extends AbstractEvent {

    @JsonProperty("organizerName")
    private String organizerName;

    @JsonProperty("participantCount")
    private int participantCount;
}
