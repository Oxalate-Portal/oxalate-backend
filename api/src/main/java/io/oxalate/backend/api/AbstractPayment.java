package io.oxalate.backend.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractPayment {
    @Schema(description = "Payment ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    private long id;

    @Schema(description = "User ID to which the payment belongs to", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("userId")
    private long userId;

    @Schema(description = "Type of payment, can either be a ONE_TIME or PERIOD", example = "ONE_TIME", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("paymentType")
    private PaymentTypeEnum paymentType;

    @Schema(description = "If type is ONE_TIME then this has to be positive integer", example = "4", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("paymentCount")
    private Long paymentCount;
}
