package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Event dive list request")
@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventDiveListRequest {
    @Schema(description = "Set of event dive requests that should be updated", requiredMode = Schema.RequiredMode.REQUIRED)
    Set<EventDiveRequest> dives;
}
