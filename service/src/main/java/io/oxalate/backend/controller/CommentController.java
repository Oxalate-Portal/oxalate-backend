package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.response.commenting.CommentResponse;
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
        var comments = commentService.getCommentThread(parentId,0L);
        return ResponseEntity.ok(comments);
    }

    @Override
    public ResponseEntity<CommentResponse> getCommentThreadToDepth(long parentId, long depth, HttpServletRequest request) {
        var comments = commentService.getCommentThread(parentId, depth);
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
    public ResponseEntity<CommentResponse> addComment(CommentRequest commentRequest, HttpServletRequest request) {
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
}
