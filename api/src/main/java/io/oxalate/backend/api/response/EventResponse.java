package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractEvent;
import io.oxalate.backend.api.EventStatusEnum;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class EventResponse extends AbstractEvent {

    @JsonProperty("status")
    private EventStatusEnum status;

    @JsonProperty("organizer")
    private UserResponse organizer;

    @JsonProperty("participants")
    private List<ListUserResponse> participants;
}
