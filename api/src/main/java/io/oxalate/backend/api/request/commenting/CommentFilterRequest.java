package io.oxalate.backend.api.request.commenting;

import io.oxalate.backend.api.CommentClassEnum;
import io.oxalate.backend.api.CommentStatusEnum;
import io.oxalate.backend.api.CommentTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

@Data
public class CommentFilterRequest {

    @Schema(description = "ID of the comments author", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long userId;

    @Schema(description = "ID of the forum the comments should belong to", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long forumId;

    @Schema(description = "ID of the dive event the comments should belong to", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long diveEventId;

    @Schema(description = "ID of the comment the comments should belong to", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long commentId;

    @Schema(description = "ID of the parent comment the comments should belong to", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long parentId;

    @Schema(description = "Depth of the child comments", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long depth;

    @Schema(description = "Filter comments with this comment classes", example = "PAGE_COMMENTS", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private CommentClassEnum commentClass;

    @Schema(description = "Filter comments with this comment status", example = "CANCELLED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private CommentStatusEnum commentStatus;

    @Schema(description = "Filter comments with this comment type", example = "TOPIC", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private CommentTypeEnum commentType;

    @Schema(description = "Filter comments with the given search phrase in the title", example = "I have an idea", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String titleSearch;

    @Schema(description = "Filter comments with the given search phrase in the body", example = "I have an idea", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String bodySearch;

    @Schema(description = "Filter comments created before this date", example = "2021-01-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Date beforeDate;

    @Schema(description = "Filter comments created after this date", example = "2021-01-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Date afterDate;

    @Schema(description = "Filter comments with this many reports", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long reportCount;

    // Constructor to apply default values if fields are null
    public CommentFilterRequest() {
        if (this.userId == null) {
            this.userId = 0L;
        }
        if (this.forumId == null) {
            this.forumId = 0L;
        }
        if (this.diveEventId == null) {
            this.diveEventId = 0L;
        }
        if (this.commentId == null) {
            this.commentId = 0L;
        }
        if (this.parentId == null) {
            this.parentId = 0L;
        }
        if (this.depth == null) {
            this.depth = 0L;
        }
        if (this.reportCount == null) {
            this.reportCount = 0L;
        }
    }
}
