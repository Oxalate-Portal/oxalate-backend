package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.commenting.CommentFilterRequest;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.request.commenting.ReportRequest;
import io.oxalate.backend.api.response.commenting.CommentModerationResponse;
import io.oxalate.backend.api.response.commenting.CommentResponse;
import io.oxalate.backend.api.response.commenting.ReportResponse;
import io.oxalate.backend.rest.CommentAPI;
import io.oxalate.backend.service.commenting.CommentService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class CommentController implements CommentAPI {

    public final CommentService commentService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CommentResponse> getCommentThread(long parentId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var comments = commentService.getCommentThread(parentId,0L, userId);
        return ResponseEntity.ok(comments);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CommentResponse> getCommentThreadToDepth(long parentId, long depth, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var comments = commentService.getCommentThread(parentId, depth, userId);
        return ResponseEntity.ok(comments);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CommentResponse> getComment(long commentId, HttpServletRequest request) {
        var commentResponse = commentService.getComment(commentId);

        if (commentResponse != null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(commentResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CommentResponse> createComment(CommentRequest commentRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var commentResponse = commentService.createComment(userId, commentRequest);

        if (commentResponse == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(commentResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CommentResponse> updateComment(CommentRequest commentRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();

        var commentResponse = commentService.updateComment(userId, commentRequest);

        if (commentResponse == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(commentResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommentResponse>> getCommentsByUserId(long userId, HttpServletRequest request) {
        var comments = commentService.getCommentsByUserId(userId);
        return ResponseEntity.ok(comments);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<ReportResponse> report(ReportRequest reportRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var reportResponse = commentService.reportComment(reportRequest, userId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<ReportResponse> cancelReport(long commentId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var reportResponse = commentService.cancelReport(commentId, userId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommentModerationResponse>> getPendingReports(HttpServletRequest request) {
        var commentServicePendingReports = commentService.getPendingReports();
        return ResponseEntity.ok(commentServicePendingReports);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> rejectComment(long commentId, HttpServletRequest request) {
        var reportResponse = commentService.rejectComment(commentId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> rejectReports(long commentId, HttpServletRequest request) {
        var reportResponse = commentService.rejectReports(commentId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> acceptReport(long reportId, HttpServletRequest request) {
        var reportResponse = commentService.acceptReport(reportId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> dismissReport(long reportId, HttpServletRequest request) {
        var reportResponse = commentService.dismissReport(reportId);
        return ResponseEntity.ok(reportResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommentResponse>> filterComments(CommentFilterRequest commentFilterRequest, HttpServletRequest request) {
        var comments = commentService.getFilteredComments(commentFilterRequest);
        return ResponseEntity.ok(comments);
    }
}
