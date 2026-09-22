package io.oxalate.backend.api.request;

import io.oxalate.backend.api.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "User status request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserStatusRequest {
    @NotBlank
    @Schema(description = "User status to be set", example = "LOCKED", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserStatusEnum status;
}
