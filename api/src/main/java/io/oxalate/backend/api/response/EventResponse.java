package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractEvent;
import java.util.Set;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class EventResponse extends AbstractEvent {

    @JsonProperty("published")
    private boolean published;

    @JsonProperty("organizer")
    private UserResponse organizer;

    @JsonProperty("participants")
    private Set<EventUserResponse> participants;
}
