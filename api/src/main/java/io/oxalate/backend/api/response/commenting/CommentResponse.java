package io.oxalate.backend.api.response.commenting;

import io.oxalate.backend.api.CommentStatusEnum;
import io.oxalate.backend.api.CommentTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Comment response")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommentResponse {
    private long id;

    private String title;

    private String body;

    private long userId;

    private String username;

    private String avatarUrl;

    private Instant registeredAt;

    private Long parentCommentId;

    private CommentTypeEnum commentType;

    private CommentStatusEnum commentStatus;

    private String cancelReason;

    private long childCount;

    private Instant createdAt;

    private Instant modifiedAt;

    private List<CommentResponse> childComments;

    private boolean userHasReported;
}
