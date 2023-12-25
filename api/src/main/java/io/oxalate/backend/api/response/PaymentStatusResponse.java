package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UpdateStatusEnum;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    @JsonProperty("userId")
    private long userId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private UpdateStatusEnum status;

    @JsonProperty("payments")
    private Set<PaymentResponse> payments;
}
