package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse extends AbstractPayment {
    @Schema(description = "When was the periodic payment done", example = "2023-04-12T11:22:33.542Z\"", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("createdAt")
    private Instant createdAt;
    @Schema(description = "When was the periodic payment done", example = "2023-04-12T11:22:33.542Z\"", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("expiresAt")
    private Instant expiresAt;
}
