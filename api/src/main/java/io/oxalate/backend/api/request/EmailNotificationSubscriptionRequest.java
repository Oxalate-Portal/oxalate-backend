package io.oxalate.backend.api.request;

import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class EmailNotificationSubscriptionRequest {
    @Schema(description = "Type of subscription", example = "EVENT_NEW", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EmailNotificationTypeEnum> subscriptionList;
}
