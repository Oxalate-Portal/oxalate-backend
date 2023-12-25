package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Token request")
@Data
public class TokenRequest {
    @Schema(description = "Token to be validated", example = "b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("token")
    private String token;
}
