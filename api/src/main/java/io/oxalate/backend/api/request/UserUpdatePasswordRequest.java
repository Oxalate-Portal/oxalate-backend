package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "User password update request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserUpdatePasswordRequest {
    @Size(min = 6)
    @Schema(description = "Current password", example = "NotSoSecret", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @Size(min = 10)
    @Schema(description = "New password", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    @Size(min = 10)
    @Schema(description = "New password, again", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;
}
