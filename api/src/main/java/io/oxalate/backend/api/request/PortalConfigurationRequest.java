package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Portal configuration request")
public class PortalConfigurationRequest {
    private long id;
    private String value;
}
