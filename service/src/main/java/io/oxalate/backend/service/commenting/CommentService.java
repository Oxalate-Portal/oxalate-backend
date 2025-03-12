package io.oxalate.backend.service.commenting;

import static io.oxalate.backend.api.CommentConstants.ROOT_EVENT_COMMENT_BODY;
import static io.oxalate.backend.api.CommentConstants.ROOT_EVENT_COMMENT_ID;
import static io.oxalate.backend.api.CommentConstants.ROOT_EVENT_COMMENT_TITLE;
import io.oxalate.backend.api.CommentStatusEnum;
import static io.oxalate.backend.api.CommentStatusEnum.HELD_FOR_MODERATION;
import static io.oxalate.backend.api.CommentStatusEnum.PUBLISHED;
import static io.oxalate.backend.api.CommentTypeEnum.TOPIC;
import static io.oxalate.backend.api.CommentTypeEnum.USER_COMMENT;
import static io.oxalate.backend.api.PortalConfigEnum.COMMENTING;
import static io.oxalate.backend.api.PortalConfigEnum.CommentConfigEnum.COMMENT_REPORT_TRIGGER_LEVEL;
import static io.oxalate.backend.api.PortalConfigEnum.CommentConfigEnum.COMMENT_REQUIRE_REVIEW;
import io.oxalate.backend.api.ReportStatusEnum;
import static io.oxalate.backend.api.ReportStatusEnum.PENDING;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UpdateStatusEnum;
import static io.oxalate.backend.api.UploadDirectoryConstants.AVATARS;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.request.commenting.ReportRequest;
import io.oxalate.backend.api.response.commenting.CommentModerationResponse;
import io.oxalate.backend.api.response.commenting.CommentReportResponse;
import io.oxalate.backend.api.response.commenting.CommentResponse;
import io.oxalate.backend.api.response.commenting.ReportResponse;
import io.oxalate.backend.model.commenting.Comment;
import io.oxalate.backend.model.commenting.CommentReport;
import io.oxalate.backend.model.commenting.EventComment;
import io.oxalate.backend.repository.commenting.CommentReportRepository;
import io.oxalate.backend.repository.commenting.CommentRepository;
import io.oxalate.backend.repository.commenting.EventCommentRepository;
import io.oxalate.backend.repository.filetransfer.AvatarFileRepository;
import io.oxalate.backend.service.PortalConfigurationService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final EventCommentRepository eventCommentRepository;
    private final CommentReportRepository commentReportRepository;
    private final AvatarFileRepository avatarFileRepository;
    private final PortalConfigurationService portalConfigurationService;

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
        var initialStatus = portalConfigurationService.getBooleanConfiguration(COMMENTING.group, COMMENT_REQUIRE_REVIEW.key) ? HELD_FOR_MODERATION : PUBLISHED;

        var comment = Comment.builder()
                             .title(commentRequest.getTitle())
                             .body(commentRequest.getBody())
                             .parentCommentId(parentComment.getId())
                             .userId(userId)
                             .commentType(commentRequest.getCommentType())
                             .commentStatus(initialStatus)
                             .createdAt(Instant.now())
                             .build();
        var newComment = commentRepository.save(comment);

        var commentResponse = newComment.toResponse();
        populateUserInformation(newComment.getUserId(), commentResponse);
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
        var commentResponse = comment.toResponse();
        populateUserInformation(comment.getUserId(), commentResponse);
        var childCount = commentRepository.countChildren(comment.getId());
        commentResponse.setChildCount(childCount);

        return commentResponse;
    }

    public CommentResponse getCommentThread(long parentId, long depth, long userId) {
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

        if (!parentComment.getCommentStatus()
                          .equals(PUBLISHED)) {
            throw new IllegalStateException("Parent comment is not published");
        }

        var parentCommentResponse = parentComment.toResponse();

        // Recursively fetch the child comments and build the tree
        log.info("Fetching comments recursively for parent ID: {} to depth: {}", parentId, depth);
        parentCommentResponse.setChildComments(fetchCommentsRecursively(parentId, depth, userId));
        parentCommentResponse.setUserHasReported(hasUserReportedComment(userId, parentId));

        // Set username from userService
        populateUserInformation(parentComment.getUserId(), parentCommentResponse);

        log.info("Returning comment thread for parent ID: {}: {}", parentId, parentCommentResponse);
        return parentCommentResponse;
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

        if (!user.getId()
                 .equals(userId)) {
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
        commentResponse.setUserHasReported(hasUserReportedComment(userId, updatedComment.getId()));

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

    @Transactional
    public ReportResponse reportComment(ReportRequest reportRequest, long userId) {
        // Check first if the user has already reported the comment
        if (hasUserReportedComment(userId, reportRequest.getCommentId())) {
            log.error("User with ID: {} has already reported comment with ID: {}", userId, reportRequest.getCommentId());
            return ReportResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .errorMessage("User has already reported this comment")
                                 .errorCode(400L)
                                 .build();
        }

        // Reporting can only be done on a USER_COMMENT
        var reportedComment = commentRepository.findById(reportRequest.getCommentId())
                                               .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (reportedComment.getCommentType() != USER_COMMENT) {
            log.error("Comment with ID: {} is not a user comment", reportRequest.getCommentId());
            return ReportResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .errorMessage("Comment is not a user comment")
                                 .errorCode(400L)
                                 .build();
        }

        var commentReport = CommentReport.builder()
                                         .commentId(reportRequest.getCommentId())
                                         .reason(reportRequest.getReportReason())
                                         .userId(userId)
                                         .status(PENDING)
                                         .createdAt(Instant.now())
                                         .build();

        var reportResponse = ReportResponse.builder()
                                           .status(UpdateStatusEnum.OK)
                                           .build();

        var newReport = commentReportRepository.save(commentReport);

        if (newReport.getId() == null) {
            reportResponse.setStatus(UpdateStatusEnum.FAIL);
            reportResponse.setErrorMessage("Saving report failed");
            reportResponse.setErrorCode(500L);
        }

        // Check how many reports the comment already has
        var reportCount = commentReportRepository.countByCommentId(reportRequest.getCommentId());

        if (reportCount >= portalConfigurationService.getNumericConfiguration(COMMENTING.group, COMMENT_REPORT_TRIGGER_LEVEL.key)) {
            // Mark the comment as reported
            reportedComment.setCommentStatus(HELD_FOR_MODERATION);
            commentRepository.save(reportedComment);
        }

        return reportResponse;
    }

    @Transactional
    public ReportResponse cancelReport(long commentId, long userId) {
        if (!hasUserReportedComment(userId, commentId)) {
            log.error("User with ID: {} has not reported comment with ID: {}", userId, commentId);
            return ReportResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .errorMessage("User has not reported this comment")
                                 .errorCode(400L)
                                 .build();
        }

        var commentReport = commentReportRepository.findByUserIdAndCommentId(userId, commentId);
        commentReport.setStatus(ReportStatusEnum.CANCELLED);

        return ReportResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .build();
    }

    public List<CommentModerationResponse> getPendingReports() {
        var pendingCommentModerationResponses = new ArrayList<CommentModerationResponse>();
        var pendingReports = commentReportRepository.findAllByStatus(PENDING);

        if (!pendingReports.isEmpty()) {
            // First we get the list of unique comment IDs
            var commentIds = pendingReports.stream()
                                           .map(CommentReport::getCommentId)
                                           .toList();

            for (var commentId : commentIds) {
                var comment = commentRepository.findById(commentId)
                                               .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

                var commentModerationResponse = comment.toCommentModerationResponse();
                populateUserInformation(comment.getUserId(), commentModerationResponse);
                // Get the sub-list of reports for this comment
                var commentReports = pendingReports.stream()
                                                   .filter(report -> report.getCommentId() == commentId)
                                                   .toList();
                var reportResponses = new ArrayList<CommentReportResponse>();

                for (var report : commentReports) {
                    var reporter = userService.findUserById(report.getUserId())
                                              .orElseThrow(() -> new EntityNotFoundException("Reporter not found"));
                    var commentReportResponse = CommentReportResponse.builder()
                                                                     .id(report.getId())
                                                                     .reporterId(reporter.getId())
                                                                     .reporter(reporter.getLastName() + " " + reporter.getFirstName())
                                                                     .createdAt(report.getCreatedAt())
                                                                     .status(report.getStatus())
                                                                     .reason(report.getReason())
                                                                     .build();
                    reportResponses.add(commentReportResponse);
                }

                commentModerationResponse.setReports(reportResponses);
                var childCount = commentRepository.countChildren(comment.getId());
                commentModerationResponse.setChildCount(childCount);
                pendingCommentModerationResponses.add(commentModerationResponse);
            }
        }

        return pendingCommentModerationResponses;
    }

    @Transactional
    public ReportResponse rejectComment(long commentId) {
        rejectRecursivelyComment(commentId);
        return setStatusOfAllCommentReports(commentId, ReportStatusEnum.APPROVED);
    }

    @Transactional
    public ReportResponse rejectReports(long commentId) {
        return setStatusOfAllCommentReports(commentId, ReportStatusEnum.REJECTED);
    }

    @Transactional
    public ReportResponse acceptReport(long reportId) {
        return updateCommentReportStatus(reportId, ReportStatusEnum.APPROVED);
    }

    @Transactional
    public ReportResponse dismissReport(long reportId) {
        return updateCommentReportStatus(reportId, ReportStatusEnum.REJECTED);
    }

    private ReportResponse setStatusOfAllCommentReports(long commentId, ReportStatusEnum rejected) {
        var commentReports = commentReportRepository.findAllByCommentId(commentId);

        for (var report : commentReports) {
            report.setStatus(rejected);
            commentReportRepository.save(report);
        }

        return ReportResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .build();
    }

    private ReportResponse updateCommentReportStatus(long reportId, ReportStatusEnum reportStatusEnum) {
        var commentReport = commentReportRepository.findById(reportId)
                                                   .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        if (reportStatusEnum == ReportStatusEnum.REJECTED) {
            var commentId = commentReport.getCommentId();
            // If the comment status is currently HELD_FOR_MODERATION, then we need to update the comment status back to PUBLISHED
            var comment = commentRepository.findById(commentId)
                                           .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
            if (comment.getCommentStatus() == HELD_FOR_MODERATION) {
                comment.setCommentStatus(PUBLISHED);
                commentRepository.save(comment);
            }
        }

        commentReport.setStatus(reportStatusEnum);
        commentReportRepository.save(commentReport);

        return ReportResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .build();
    }

    private void populateUserInformation(long userId, CommentResponse commentResponse) {
        var optionalUser = userService.findUserById(userId);
        if (optionalUser.isPresent()) {
            var user = optionalUser.get();
            commentResponse.setUsername(user.getLastName() + " " + user.getFirstName());
            commentResponse.setRegisteredAt(user.getRegistered());
            commentResponse.setAvatarUrl(getUserAvatarUrl(userId));
        } else {
            log.error("User with ID: {} not found when populating comment response", userId);
        }
    }

    private List<CommentResponse> fetchCommentsRecursively(Long parentId, Long depth, long userId) {
        if (depth == 0L) {
            return new ArrayList<>();
        }

        // Order the comments by creation date

        List<Comment> childComments = commentRepository.findAllByParentCommentId(parentId)
                                                       .stream()
                                                       .filter(comment -> comment.getCommentStatus()
                                                                                 .equals(PUBLISHED))
                                                       .sorted(Comparator.comparing(Comment::getCreatedAt))
                                                       .toList();
        var childCommentResponses = new ArrayList<CommentResponse>();
        // Recursively fetch and attach child comments
        for (Comment child : childComments) {
            var childCommentResponse = child.toResponse();
            childCommentResponse.setChildComments(fetchCommentsRecursively(child.getId(), depth - 1L, userId));
            childCommentResponse.setUserHasReported(hasUserReportedComment(userId, child.getId()));
            populateUserInformation(child.getUserId(), childCommentResponse);
            childCommentResponses.add(childCommentResponse);
        }

        return childCommentResponses;
    }

    private boolean hasUserReportedComment(long userId, long commentId) {
        return commentReportRepository.existsByUserIdAndCommentId(userId, commentId);
    }

    private String getUserAvatarUrl(long userId) {
        var optionalAvatarFile = avatarFileRepository.findByUserId(userId);

        if (optionalAvatarFile.isEmpty()) {
            return null;
        }

        var avatarFile = optionalAvatarFile.get();

        return FILES_URL + "/" + AVATARS + "/" + avatarFile.getId();
    }

    private void rejectRecursivelyComment(long commentId) {
        var comment = commentRepository.findById(commentId)
                                       .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        comment.setCommentStatus(CommentStatusEnum.REJECTED);
        commentRepository.save(comment);

        var childComments = commentRepository.findAllByParentCommentId(commentId);
        for (var childComment : childComments) {
            rejectRecursivelyComment(childComment.getId());
        }
    }
}
