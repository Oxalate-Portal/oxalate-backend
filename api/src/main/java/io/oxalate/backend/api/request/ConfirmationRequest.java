package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Confirmation request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationRequest {
    @NotBlank
    @Schema(description = "User answer to whether they accept the terms and conditions, is either yes or no", example = "yes", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("confirmationAnswer")
    private boolean confirmationAnswer;
}
