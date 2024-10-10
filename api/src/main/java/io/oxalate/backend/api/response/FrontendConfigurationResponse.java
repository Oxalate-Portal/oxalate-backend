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
public class FrontendConfigurationResponse {
    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private String value;
}
