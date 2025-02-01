package io.oxalate.backend.api.request;

import io.oxalate.backend.api.AbstractPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Schema(description = "Payment request")
@SuperBuilder
@Data
@ToString(callSuper = true)
@AllArgsConstructor
public class PaymentRequest extends AbstractPayment {
}
