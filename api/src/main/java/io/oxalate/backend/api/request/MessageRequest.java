package io.oxalate.backend.api.request;

import io.oxalate.backend.api.AbstractMessage;
import io.oxalate.backend.api.NotificationGroupEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Message request")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageRequest extends AbstractMessage {

    @Schema(description = "List of user ID to which the message should be sent", example = "[1,2,3,4,5]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<Long> recipients;

    @Schema(description = "Alternative toggle to send everyone the message", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean sendAll;

    @Schema(description = "User group targeting for bulk notifications (if neither sendAll nor recipients are specified)", example = "inactive_days", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private NotificationGroupEnum notificationGroup;

    @Schema(description = "Number of days of inactivity (required if notificationGroup is INACTIVE_DAYS)", example = "30", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer inactiveDays;

    @Schema(description = "ID of the dive event that the message relates to, used to generate a direct link in the notification email", example = "42", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long eventId;
}
