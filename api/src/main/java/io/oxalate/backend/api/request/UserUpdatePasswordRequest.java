package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "User password update request")
@Data
public class UserUpdatePasswordRequest {
    @Size(min = 6)
    @Schema(description = "Current password", example = "NotSoSecret", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("oldPassword")
    private String oldPassword;

    @Size(min = 10)
    @Schema(description = "New password", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("newPassword")
    private String newPassword;

    @Size(min = 10)
    @Schema(description = "New password, again", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("confirmPassword")
    private String confirmPassword;
}
