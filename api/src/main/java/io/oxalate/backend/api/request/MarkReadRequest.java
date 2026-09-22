package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Request to mark notifications as read")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MarkReadRequest {

    @Schema(description = "List of message IDs to mark as read", example = "[1,2,3,4,5]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> messageIds;
}
