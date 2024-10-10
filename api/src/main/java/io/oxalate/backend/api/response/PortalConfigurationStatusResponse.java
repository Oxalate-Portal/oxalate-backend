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
public class PortalConfigurationStatusResponse {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;
}
