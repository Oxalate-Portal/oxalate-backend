package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(description = "Message request")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest extends AbstractMessage {

    @Schema(description = "List of user ID to which the message should be sent", example = "[1,2,3,4,5]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("recipients")
    private List<Long> recipients;

    @Schema(description = "Alternative toggle to send everyone the message", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("sendAll")
    private Boolean sendAll;

    @Schema(description = "ID of the dive event that the message relates to, used to generate a direct link in the notification email", example = "42", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("eventId")
    private Long eventId;
}
