package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Email change request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmailChangeRequest {
    @NotBlank
    @Size(max = 80)
    @Email
    @Schema(description = "New email address", example = "new.email@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newEmail;

    @NotBlank
    @Size(min = 6, max = 120)
    @Schema(description = "Current password for authorization", example = "Avery^S3curePasswd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

