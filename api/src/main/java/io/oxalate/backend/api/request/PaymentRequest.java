package io.oxalate.backend.api.request;

import io.oxalate.backend.api.AbstractPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Payment request")
@SuperBuilder
@Data
@ToString(callSuper = true)
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PaymentRequest extends AbstractPayment {
}
