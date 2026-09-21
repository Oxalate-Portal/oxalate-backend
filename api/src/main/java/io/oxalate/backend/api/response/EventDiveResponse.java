package io.oxalate.backend.api.response;

import io.oxalate.backend.api.request.EventDiveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Event dive response")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventDiveResponse extends EventDiveRequest {
    @Schema(description = "User name", example = "Toivonen Erkki", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
