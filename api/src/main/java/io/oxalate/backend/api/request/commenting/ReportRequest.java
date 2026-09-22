package io.oxalate.backend.api.request.commenting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReportRequest {
    @Schema(description = "ID of the comment that is being reported", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private long commentId;

    @Size(min = 5)
    @Schema(description = "ID of the comment that is being reported, minimum length is 5", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reportReason;
}
