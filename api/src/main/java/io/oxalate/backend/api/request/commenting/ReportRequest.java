package io.oxalate.backend.api.request.commenting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ReportRequest {
    @Schema(description = "ID of the comment that is being reported", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(value = "commentId")
    private long commentId;

    @Size(min = 5)
    @Schema(description = "ID of the comment that is being reported, minimum length is 5", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(value = "reportReason")
    private String reportReason;
}
