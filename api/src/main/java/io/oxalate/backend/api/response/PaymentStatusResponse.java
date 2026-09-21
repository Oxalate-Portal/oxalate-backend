package io.oxalate.backend.api.response;

import io.oxalate.backend.api.UpdateStatusEnum;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PaymentStatusResponse {

    private long userId;

    private String name;

    private UpdateStatusEnum status;

    private List<PaymentResponse> payments;
}
