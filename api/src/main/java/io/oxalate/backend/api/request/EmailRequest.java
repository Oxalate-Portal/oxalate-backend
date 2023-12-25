package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Schema(description = "Email request")
@Data
public class EmailRequest {
    @Schema(description = "Email address to whom the message should be sent", example = "someone@somewhere.tld", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email
    @JsonProperty("email")
    private String email;
}
