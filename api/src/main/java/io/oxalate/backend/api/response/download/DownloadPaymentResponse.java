package io.oxalate.backend.api.response.download;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.PaymentTypeEnum;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadPaymentResponse {

    @JsonProperty("id")
    private long id;

    @JsonProperty("userId")
    private long userId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("paymentCount")
    private Integer paymentCount;

    @JsonProperty("paymentType")
    private PaymentTypeEnum paymentType;

    @JsonProperty("createdAt")
    private Instant createdAt;
}
