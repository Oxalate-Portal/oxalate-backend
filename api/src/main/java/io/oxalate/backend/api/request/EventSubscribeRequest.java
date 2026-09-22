package io.oxalate.backend.api.request;

import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Event subscription request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventSubscribeRequest {
    @Min(value = 1L)
    @Schema(description = "ID of the dive event to subscribe to", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long diveEventId;

    @Min(value = 1L)
    @Schema(description = "In what capacity the user takes part in the event", example = "NON_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserTypeEnum userType;
}
