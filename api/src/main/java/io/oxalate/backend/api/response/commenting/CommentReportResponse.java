package io.oxalate.backend.api.response.commenting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.ReportStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Report response")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReportResponse {
    @JsonProperty("id")
    private long id;
    @JsonProperty("reporter")
    private String reporter;
    @JsonProperty("reporterId")
    private long reporterId;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("createdAt")
    Instant createdAt;
    @JsonProperty("status")
    ReportStatusEnum status;
}
