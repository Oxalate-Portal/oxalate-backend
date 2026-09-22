package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Portal configuration request")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PortalConfigurationRequest {
    private long id;
    private String value;
}
