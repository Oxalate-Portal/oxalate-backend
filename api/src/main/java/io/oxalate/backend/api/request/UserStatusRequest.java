package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "User status request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusRequest {
    @NotBlank
    @Schema(description = "User status to be set", example = "LOCKED", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    private UserStatusEnum status;
}
