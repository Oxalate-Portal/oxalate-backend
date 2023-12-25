package io.oxalate.backend.api.request;

import io.oxalate.backend.api.AbstractPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Schema(description = "Payment request")
@SuperBuilder
@Data
@AllArgsConstructor
public class PaymentRequest extends AbstractPayment {
}
