package io.oxalate.backend.model.commenting;

import io.oxalate.backend.api.CommentStatusEnum;
import io.oxalate.backend.api.CommentTypeEnum;
import io.oxalate.backend.api.response.commenting.CommentModerationResponse;
import io.oxalate.backend.api.response.commenting.CommentResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @JoinColumn(name = "parent_comment_id")
    private Long parentCommentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false)
    private CommentTypeEnum commentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_status", nullable = false)
    private CommentStatusEnum commentStatus;

    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Transient
    private List<Comment> childComments = new ArrayList<>();

    public CommentResponse toResponse() {
        return CommentResponse.builder()
                              .id(id)
                              .title(title)
                              .body(body)
                              .userId(userId)
                              .username(null)
                              .parentCommentId(parentCommentId)
                              .commentType(commentType)
                              .commentStatus(commentStatus)
                              .cancelReason(cancelReason)
                              .createdAt(createdAt)
                              .modifiedAt(modifiedAt)
                              .childCount(childComments == null ? 0 : childComments.size())
                              .childComments(childComments == null ? new ArrayList<>() : childComments.stream()
                                                                                                      .map(Comment::toResponse)
                                                                                                      .collect(Collectors.toList()))
                              .build();
    }

    public CommentModerationResponse toCommentModerationResponse() {
        return CommentModerationResponse.builder()
                                        .id(id)
                                        .title(title)
                                        .body(body)
                                        .userId(userId)
                                        .username(null)
                                        .parentCommentId(parentCommentId)
                                        .commentType(commentType)
                                        .commentStatus(commentStatus)
                                        .cancelReason(cancelReason)
                                        .createdAt(createdAt)
                                        .modifiedAt(modifiedAt)
                                        .childCount(childComments == null ? 0 : childComments.size())
                                        .build();
    }
}
