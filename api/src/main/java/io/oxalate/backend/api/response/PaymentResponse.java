package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse extends AbstractPayment {

    @Schema(description = "If the type is ONE_TIME, then this is the list of future event IDs they've been used in", example = "[3, 12]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("boundEvents")
    private List<Long> boundEvents;

    @Schema(description = "When was the periodic payment done", example = "2023-04-12T11:22:33.542Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("created")
    private Instant created;
}
