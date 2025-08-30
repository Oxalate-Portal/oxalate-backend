package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Event subscription request")
@Data
@Builder
@AllArgsConstructor
public class EventSubscribeRequest {
    @Min(value = 1L)
    @Schema(description = "ID of the dive event to subscribe to", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("diveEventId")
    private long diveEventId;

    @Min(value = 1L)
    @Schema(description = "In what capacity the user takes part in the event", example = "NON_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("userType")
    private UserTypeEnum userType;
}
