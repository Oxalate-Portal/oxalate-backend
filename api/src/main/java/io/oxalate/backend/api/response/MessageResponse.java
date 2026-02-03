package io.oxalate.backend.api.response;

import io.oxalate.backend.api.AbstractMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class MessageResponse extends AbstractMessage {
    @Schema(description = "Indicates whether the message has been read", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean read;
}
