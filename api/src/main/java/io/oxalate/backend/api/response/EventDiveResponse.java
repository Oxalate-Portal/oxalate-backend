package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.request.EventDiveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event dive response")
public class EventDiveResponse extends EventDiveRequest {
    @Schema(description = "User name", example = "Toivonen Erkki", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    private String name;
}
