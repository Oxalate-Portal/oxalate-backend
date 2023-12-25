package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "User password reset request")
@Data
public class UserResetPasswordRequest {
    @Size(min = 10)
    @Schema(description = "New password", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("newPassword")
    private String newPassword;

    @Size(min = 10)
    @Schema(description = "New password, again", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("confirmPassword")
    private String confirmPassword;

    @Schema(description = "Validation token", example = "b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("token")
    private String token;
}
