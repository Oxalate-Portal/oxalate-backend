package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "Login request")
@Data
@AllArgsConstructor
public class LoginRequest {
    @NotBlank
    @Schema(description = "Username/email of the user logging in", example = "someone@somewhere.tld", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("username")
    private String username;

    @NotBlank
    @Schema(description = "Password of the user logging in", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("password")
    private String password;
}
