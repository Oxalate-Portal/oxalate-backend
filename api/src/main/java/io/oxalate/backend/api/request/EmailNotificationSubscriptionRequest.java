package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationSubscriptionRequest {
    @Schema(description = "Type of subscription", example = "EVENT_NEW", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("subscriptionList")
    private List<EmailNotificationTypeEnum> subscriptionList;
}
