package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.EmailNotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationSubscriptionResponse {
    @JsonProperty("id")
    private long id;
    @JsonProperty("emailNotificationType")
    private EmailNotificationTypeEnum emailNotificationType;
    @JsonProperty("userId")
    private long userId;
}
