package io.oxalate.backend.api.response;

import io.oxalate.backend.api.AbstractMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@ToString(callSuper = true)
public class MessageResponse extends AbstractMessage {
    @Schema(description = "Indicates whether the message has been read", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean read;
}
