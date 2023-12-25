package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Schema(description = "Event dive request")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDiveRequest {
    @Schema(description = "ID of the user whose dive count is to be set", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("userId")
    private long userId;

    @Schema(description = "New count of the user in a particular event", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("diveCount")
    private long diveCount;
}
