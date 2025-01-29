package io.oxalate.backend.service.commenting;

import io.oxalate.backend.api.CommentStatusEnum;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.response.commenting.CommentResponse;
import io.oxalate.backend.model.commenting.Comment;
import io.oxalate.backend.repository.commenting.CommentRepository;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserService userService;

    @Transactional
    public CommentResponse createComment(long userId, CommentRequest commentRequest) {
        log.info("Creating comment: {}", commentRequest);

        var optionalUser = userService.findUserById(userId);

        if (optionalUser.isEmpty()) {
            log.error("Attempting to create a comment with a nonexistent user ID: {}", userId);
            return null;
        }

        var user = optionalUser.get();

        var optionalParentComment = commentRepository.findById(commentRequest.getParentCommentId());

        if (optionalParentComment.isEmpty()) {
            log.error("Parent comment with ID: {} not found", commentRequest.getParentCommentId());
            return null;
        }

        var parentComment = optionalParentComment.get();

        var comment = Comment.builder()
                             .title(commentRequest.getTitle())
                             .body(commentRequest.getBody())
                             .parentComment(parentComment)
                             .userId(userId)
                             .commentType(commentRequest.getCommentType())
                             .commentStatus(CommentStatusEnum.PUBLISHED)
                             .createdAt(Instant.now())
                             .build();
        var newComment = commentRepository.save(comment);

        var commentResponse = newComment.toResponse();
        commentResponse.setUsername(user.getLastName() + " " + user.getFirstName());
        var childCount = commentRepository.countChildren(comment.getId());
        commentResponse.setChildCount(childCount);

        return commentResponse;
    }

    public CommentResponse getComment(Long commentId) {
        log.info("Fetching comment with ID: {}", commentId);

        var optionalComment = commentRepository.findById(commentId);

        if (optionalComment.isEmpty()) {
            log.error("Comment with ID: {} not found", commentId);
            return null;
        }

        var comment = optionalComment.get();
        var optionalUser = userService.findUserById(comment.getUserId());

        if (optionalUser.isEmpty()) {
            log.error("User with ID: {} referenced by comment ID {} not found", comment.getUserId(), commentId);
            return null;
        }

        var user = optionalUser.get();
        var commentResponse = comment.toResponse();
        commentResponse.setUsername(user.getLastName() + " " + user.getFirstName());
        var childCount = commentRepository.countChildren(comment.getId());
        commentResponse.setChildCount(childCount);

        return commentResponse;
    }

    public List<CommentResponse> getCommentThread(long parentId, long depth) {
        log.info("Fetching comment thread for parent ID: {}", parentId);
        var thread = new ArrayList<Comment>();
        fetchCommentsRecursively(parentId, thread, (depth == 0) ? Long.MAX_VALUE : depth);

        var commentResponseList = new ArrayList<CommentResponse>();

        for (Comment comment : thread) {
            var optionalUser = userService.findUserById(comment.getUserId());
            if (optionalUser.isEmpty()) {
                log.error("User with ID: {} referenced by comment ID {} not found", comment.getUserId(), comment.getId());
                continue;
            }

            var user = optionalUser.get();
            var commentResponse = comment.toResponse();
            commentResponse.setUsername(user.getLastName() + " " + user.getFirstName());
            var childCount = commentRepository.countChildren(comment.getId());
            commentResponse.setChildCount(childCount);
            commentResponseList.add(commentResponse);
        }

        return commentResponseList;
    }

    private void fetchCommentsRecursively(Long parentId, List<Comment> thread, Long depth) {
        var optionalParentComment = commentRepository.findById(parentId);

        if (optionalParentComment.isEmpty() || depth == 0L) {
            return;
        }

        var parentComment = optionalParentComment.get();

        if (!parentComment.getCommentStatus()
                          .equals(CommentStatusEnum.PUBLISHED)) {
            return;
        }

        thread.add(parentComment);

        List<Comment> childComments = commentRepository.findAllByParentComment(parentComment);

        for (Comment child : childComments) {
            fetchCommentsRecursively(child.getId(), thread, (depth - 1L));
        }
    }

    @Transactional
    public CommentResponse updateComment(Long userId, CommentRequest commentRequest) {
        log.info("Updating comment with ID: {}", commentRequest.getId());

        var originalOptionalComment = commentRepository.findById(commentRequest.getId());

        if (originalOptionalComment.isEmpty()) {
            log.error("Comment with ID: {} not found for editing", commentRequest.getId());
            return null;
        }

        var originalComment = originalOptionalComment.get();
        var optionalUser = userService.findUserById(originalComment.getUserId());

        if (optionalUser.isEmpty()) {
            log.error("Attempting to update a comment with a nonexistent user ID: {}", userId);
            return null;
        }

        var user = optionalUser.get();
        var isAdmin = false;

        if (user.getId() != userId) {
            var userRoles = AuthTools.getUserRoles();
            // Only admin may update comments created by other users
            if (!userRoles.contains(RoleEnum.ROLE_ADMIN)) {
                log.error("User with ID: {} does not have permission to update comment with ID: {}", userId, commentRequest.getId());
                return null;
            }

            isAdmin = true;
        }

        var originalOptionalUser = userService.findUserById(originalComment.getUserId());
        if (originalOptionalUser.isEmpty()) {
            log.error("Original user with ID: {} referenced by comment ID {} not found", originalComment.getUserId(), commentRequest.getId());
            return null;
        }

        var originalUser = originalOptionalUser.get();

        // Admin only updates the status of the comment
        if (isAdmin) {
            originalComment.setCommentStatus(commentRequest.getCommentStatus());
            originalComment.setModifiedAt(Instant.now());
        } else {
            originalComment.setTitle(commentRequest.getTitle());
            originalComment.setBody(commentRequest.getBody());
            originalComment.setModifiedAt(Instant.now());
        }

        var updatedComment = commentRepository.save(originalComment);
        var commentResponse = updatedComment.toResponse();
        commentResponse.setUsername(originalUser.getLastName() + " " + originalUser.getFirstName());

        return commentResponse;
    }

    public List<CommentResponse> getCommentsByUserId(long userId) {
        log.info("Fetching comments for user ID: {}", userId);
        var comments = commentRepository.findAllByUserId(userId);
        var commentResponseList = new ArrayList<CommentResponse>();

        for (Comment comment : comments) {
            var commentResponse = comment.toResponse();
            var childCount = commentRepository.countChildren(comment.getId());
            commentResponse.setChildCount(childCount);
            commentResponseList.add(comment.toResponse());
        }

        return commentResponseList;
    }
}
