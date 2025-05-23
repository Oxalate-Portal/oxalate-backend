package io.oxalate.backend.api.response.commenting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.CommentStatusEnum;
import io.oxalate.backend.api.CommentTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Schema(description = "Comment response")
@SuperBuilder
@Data
public class CommentResponse {
    @JsonProperty("id")
    private long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    @JsonProperty("userId")
    private long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("avatarUrl")
    private String avatarUrl;

    @JsonProperty("registeredAt")
    private Instant registeredAt;

    @JsonProperty("parentCommentId")
    private Long parentCommentId;

    @JsonProperty("commentType")
    private CommentTypeEnum commentType;

    @JsonProperty("commentStatus")
    private CommentStatusEnum commentStatus;

    @JsonProperty("cancelReason")
    private String cancelReason;

    @JsonProperty("childCount")
    private long childCount;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("modifiedAt")
    private Instant modifiedAt;

    @JsonProperty("childComments")
    private List<CommentResponse> childComments;

    @JsonProperty("userHasReported")
    private boolean userHasReported;
}
