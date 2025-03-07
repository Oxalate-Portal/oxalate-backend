package io.oxalate.backend.service.commenting;

import static io.oxalate.backend.api.CommentConstants.ROOT_EVENT_COMMENT_BODY;
import static io.oxalate.backend.api.CommentConstants.ROOT_EVENT_COMMENT_ID;
import static io.oxalate.backend.api.CommentConstants.ROOT_EVENT_COMMENT_TITLE;
import static io.oxalate.backend.api.CommentStatusEnum.PUBLISHED;
import static io.oxalate.backend.api.CommentTypeEnum.TOPIC;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.response.commenting.CommentResponse;
import io.oxalate.backend.model.commenting.Comment;
import io.oxalate.backend.model.commenting.EventComment;
import io.oxalate.backend.repository.commenting.CommentRepository;
import io.oxalate.backend.repository.commenting.EventCommentRepository;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
    private final EventCommentRepository eventCommentRepository;

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
                             .parentCommentId(parentComment.getId())
                             .userId(userId)
                             .commentType(commentRequest.getCommentType())
                             .commentStatus(PUBLISHED)
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

    public CommentResponse getCommentThread(long parentId, long depth) {
        log.info("Fetching comment thread for parent ID: {}", parentId);

        // If the start depth is 0, then set it to max long value
        if (depth == 0L) {
            depth = Long.MAX_VALUE;
        }

        // Fetch parent comment
        var optionalParentComment = commentRepository.findById(parentId);

        if (optionalParentComment.isEmpty()) {
            throw new EntityNotFoundException("Parent comment not found");
        }

        var parentComment = optionalParentComment.get();

        if (!parentComment.getCommentStatus().equals(PUBLISHED)) {
            throw new IllegalStateException("Parent comment is not published");
        }

        // Recursively fetch the child comments and build the tree
        log.info("Fetching comments recursively for parent ID: {} to depth: {}", parentId, depth);
        parentComment.setChildComments(fetchCommentsRecursively(parentId, depth));

        // Convert the root comment to response format
        var commentResponse = parentComment.toResponse();

        // Set username from userService
        var optionalUser = userService.findUserById(parentComment.getUserId());
        if (optionalUser.isPresent()) {
            var user = optionalUser.get();
            commentResponse.setUsername(user.getLastName() + " " + user.getFirstName());
        } else {
            log.error("User with ID: {} referenced by comment ID {} not found", parentComment.getUserId(), parentComment.getId());
        }

        return commentResponse;
    }

    private List<Comment> fetchCommentsRecursively(Long parentId, Long depth) {
        if (depth == 0L) {
            return new ArrayList<>();
        }

        List<Comment> childComments = commentRepository.findAllByParentCommentId(parentId)
                                                       .stream()
                                                       .filter(comment -> comment.getCommentStatus().equals(PUBLISHED)) // Only include published comments
                                                       .collect(Collectors.toList());

        // Recursively fetch and attach child comments
        for (Comment child : childComments) {
            child.setChildComments(fetchCommentsRecursively(child.getId(), depth - 1L));
        }

        return childComments;
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

    @Transactional
    public CommentResponse createEventTopicComment(long eventId, long userId) {
        // Get id for the root of all event comments
        log.info("Creating event comment for event ID: {} linked to event root comment ID: {} by user ID: {}", eventId, ROOT_EVENT_COMMENT_ID, userId);

        var comment = Comment.builder()
                             .userId(userId)
                             .parentCommentId(ROOT_EVENT_COMMENT_ID)
                             .commentType(TOPIC)
                             .title(ROOT_EVENT_COMMENT_TITLE + eventId)
                             .body(ROOT_EVENT_COMMENT_BODY + eventId)
                             .commentStatus(PUBLISHED)
                             .createdAt(Instant.now())
                             .build();
        var newComment = commentRepository.save(comment);

        var commentResponse = newComment.toResponse();
        commentResponse.setChildCount(0);

        // Add the entry also to event_comments table
        eventCommentRepository.save(EventComment.builder()
                                                .eventId(eventId)
                                                .comment(newComment)
                                                .build());

        return commentResponse;
    }

    public long getEventCommentId(long eventId) {
        var eventComment = eventCommentRepository.findByEventId(eventId);
        return eventComment.getComment()
                           .getId();
    }
}
