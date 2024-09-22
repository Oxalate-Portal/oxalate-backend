package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalConfigurationResponse {
    @JsonProperty("id")
    private long id;

    @JsonProperty("groupKey")
    private String groupKey;

    @JsonProperty("settingKey")
    private String settingKey;

    @JsonProperty("valueType")
    private String valueType;

    @JsonProperty("defaultValue")
    private String defaultValue;

    @JsonProperty("runtimeValue")
    private String runtimeValue;

    @JsonProperty("requiredRuntime")
    private Boolean requiredRuntime;

    @JsonProperty("description")
    private String description;
}
