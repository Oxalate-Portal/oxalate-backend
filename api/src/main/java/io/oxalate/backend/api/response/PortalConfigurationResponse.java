package io.oxalate.backend.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PortalConfigurationResponse {
    private long id;

    private String groupKey;

    private String settingKey;

    private String valueType;

    private String defaultValue;

    private String runtimeValue;

    private Boolean requiredRuntime;

    private String description;
}
