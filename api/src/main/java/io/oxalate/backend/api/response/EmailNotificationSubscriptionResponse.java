package io.oxalate.backend.api.response;

import io.oxalate.backend.api.EmailNotificationTypeEnum;
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
public class EmailNotificationSubscriptionResponse {
    private long id;
    private EmailNotificationTypeEnum emailNotificationType;
    private long userId;
}
