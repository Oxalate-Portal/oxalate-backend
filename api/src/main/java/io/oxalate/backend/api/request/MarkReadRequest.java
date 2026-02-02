package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request to mark notifications as read")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkReadRequest {

    @Schema(description = "List of message IDs to mark as read", example = "[1,2,3,4,5]", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("messageIds")
    private List<Long> messageIds;
}
