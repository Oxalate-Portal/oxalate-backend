package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(description = "Event dive list request")
@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDiveListRequest {
    @Schema(description = "Set of event dive requests that should be updated", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("dives")
    Set<EventDiveRequest> dives;
}
