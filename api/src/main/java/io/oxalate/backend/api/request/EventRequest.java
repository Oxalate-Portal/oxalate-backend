package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractEvent;
import io.oxalate.backend.api.EventStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Schema(description = "Event update request")
@SuperBuilder
@Data
@AllArgsConstructor
public class EventRequest extends AbstractEvent {

    @Schema(description = "Boolean to either show or hide the event, is effective only for upcoming events", example = "yes", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    private EventStatusEnum status;

    @Schema(description = "User ID of the organizer", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("organizer")
    private long organizerId;

    @Schema(description = "List of user ID of participating to the event", example = "[123, 234, 345]", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("participants")
    private Set<Long> participants;
}
