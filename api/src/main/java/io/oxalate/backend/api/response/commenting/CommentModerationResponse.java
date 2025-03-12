package io.oxalate.backend.api.response.commenting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Schema(description = "Comments requiring moderation and their reports")
@Data
@SuperBuilder
public class CommentModerationResponse extends CommentResponse {
    @Schema(description = "Reports related to this comment")
    @JsonProperty("reports")
    private List<CommentReportResponse> reports;
}
