package io.oxalate.backend.api.response;

import io.oxalate.backend.api.AbstractMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageResponse extends AbstractMessage {
    @Schema(description = "Indicates whether the message has been read", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean read;
}
