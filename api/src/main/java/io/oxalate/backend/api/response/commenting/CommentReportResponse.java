package io.oxalate.backend.api.response.commenting;

import io.oxalate.backend.api.ReportStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Report response")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommentReportResponse {
    private long id;
    private String reporter;
    private long reporterId;
    private String reason;
    Instant createdAt;
    ReportStatusEnum status;
}
