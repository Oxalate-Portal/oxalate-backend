package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "User status request")
@Data
public class UserStatusRequest {
    @NotBlank
    @Schema(description = "User status to be set", example = "LOCKED", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    private UserStatus status;
}
