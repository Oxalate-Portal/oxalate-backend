package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.commenting.CommentFilterRequest;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.request.commenting.ReportRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.commenting.CommentModerationResponse;
import io.oxalate.backend.api.response.commenting.CommentResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_ACCEPT_REPORT_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_ACCEPT_REPORT_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_CANCEL_REPORT_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_CANCEL_REPORT_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_CREATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_DISMISS_REPORT_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_DISMISS_REPORT_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_FILTER_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_FILTER_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_BY_USER_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_BY_USER_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_PENDING_REPORTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_PENDING_REPORTS_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_THREAD_DEPTH_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_THREAD_DEPTH_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_THREAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_GET_THREAD_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_REJECT_COMMENT_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_REJECT_COMMENT_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_REJECT_REPORTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_REJECT_REPORTS_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_REPORT_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_REPORT_START;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_UPDATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.COMMENTS_UPDATE_START;
import io.oxalate.backend.rest.CommentAPI;
import io.oxalate.backend.service.commenting.CommentService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("CommentController")
public class CommentController implements CommentAPI {

    public final CommentService commentService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = COMMENTS_GET_THREAD_START, okMessage = COMMENTS_GET_THREAD_OK)
    public ResponseEntity<CommentResponse> getCommentThread(long parentId) {
        var userId = AuthTools.getCurrentUserId();
        var comments = commentService.getCommentThread(parentId, 0L, userId);
        return ResponseEntity.ok(comments);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = COMMENTS_GET_THREAD_DEPTH_START, okMessage = COMMENTS_GET_THREAD_DEPTH_OK)
    public ResponseEntity<CommentResponse> getCommentThreadToDepth(long parentId, long depth) {
        var userId = AuthTools.getCurrentUserId();
        var comments = commentService.getCommentThread(parentId, depth, userId);
        return ResponseEntity.ok(comments);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = COMMENTS_GET_START, okMessage = COMMENTS_GET_OK)
    public ResponseEntity<CommentResponse> getComment(long commentId) {
        var commentResponse = commentService.getComment(commentId);

        if (commentResponse != null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(commentResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = COMMENTS_CREATE_START, okMessage = COMMENTS_CREATE_OK, failMessage = COMMENTS_CREATE_FAIL)
    public ResponseEntity<CommentResponse> createComment(CommentRequest commentRequest) {
        var userId = AuthTools.getCurrentUserId();
        var commentResponse = commentService.createComment(userId, commentRequest);

        if (commentResponse == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(commentResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = COMMENTS_UPDATE_START, okMessage = COMMENTS_UPDATE_OK, failMessage = COMMENTS_UPDATE_FAIL)
    public ResponseEntity<CommentResponse> updateComment(CommentRequest commentRequest) {
        var userId = AuthTools.getCurrentUserId();
        var commentResponse = commentService.updateComment(userId, commentRequest);

        if (commentResponse == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(commentResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = COMMENTS_GET_BY_USER_START, okMessage = COMMENTS_GET_BY_USER_OK)
    public ResponseEntity<List<CommentResponse>> getCommentsByUserId(long userId) {
        var comments = commentService.getCommentsByUserId(userId);
        return ResponseEntity.ok(comments);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = COMMENTS_REPORT_START, okMessage = COMMENTS_REPORT_OK)
    public ResponseEntity<ActionResponse> report(ReportRequest reportRequest) {
        var userId = AuthTools.getCurrentUserId();
        var reportResponse = commentService.reportComment(reportRequest, userId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = COMMENTS_CANCEL_REPORT_START, okMessage = COMMENTS_CANCEL_REPORT_OK)
    public ResponseEntity<ActionResponse> cancelReport(long commentId) {
        var userId = AuthTools.getCurrentUserId();
        var reportResponse = commentService.cancelReport(commentId, userId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = COMMENTS_GET_PENDING_REPORTS_START, okMessage = COMMENTS_GET_PENDING_REPORTS_OK)
    public ResponseEntity<List<CommentModerationResponse>> getPendingReports() {
        var commentServicePendingReports = commentService.getPendingReports();
        return ResponseEntity.ok(commentServicePendingReports);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = COMMENTS_REJECT_COMMENT_START, okMessage = COMMENTS_REJECT_COMMENT_OK)
    public ResponseEntity<ActionResponse> rejectComment(long commentId) {
        var reportResponse = commentService.rejectComment(commentId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = COMMENTS_REJECT_REPORTS_START, okMessage = COMMENTS_REJECT_REPORTS_OK)
    public ResponseEntity<ActionResponse> rejectReports(long commentId) {
        var reportResponse = commentService.rejectReports(commentId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = COMMENTS_ACCEPT_REPORT_START, okMessage = COMMENTS_ACCEPT_REPORT_OK)
    public ResponseEntity<ActionResponse> acceptReport(long reportId) {
        var reportResponse = commentService.acceptReport(reportId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = COMMENTS_DISMISS_REPORT_START, okMessage = COMMENTS_DISMISS_REPORT_OK)
    public ResponseEntity<ActionResponse> dismissReport(long reportId) {
        var reportResponse = commentService.dismissReport(reportId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = COMMENTS_FILTER_START, okMessage = COMMENTS_FILTER_OK)
    public ResponseEntity<List<CommentResponse>> filterComments(CommentFilterRequest commentFilterRequest) {
        var comments = commentService.getFilteredComments(commentFilterRequest);
        return ResponseEntity.ok(comments);
    }
}
