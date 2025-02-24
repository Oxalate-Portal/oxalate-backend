package io.oxalate.backend.api.request.commenting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.CommentStatusEnum;
import io.oxalate.backend.api.CommentTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CommentRequest {
    @Schema(description = "ID of the comment, is effective only for updating comments", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("id")
    private long id;

    @Schema(description = "Title of the comment, brief text of the comment", example = "I have an idea", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("title")
    private String title;

    @Schema(description = "Content of the comment", example = "A longer thoughtful text", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("body")
    private String body;

    @Schema(description = "Parent ID to which this comment belongs to, may refer to a comment of type topic, or another user comment", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("parentCommentId")
    private long parentCommentId;

    @Schema(description = "Comment type", example = "TOPIC", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("commentType")
    private CommentTypeEnum commentType;

    @Schema(description = "Comment status", example = "CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("commentStatus")
    private CommentStatusEnum commentStatus;

    @Schema(description = "If status is cancelled, why is it cancelled", example = "Offensive", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("cancelReason")
    private String cancelReason;
}
