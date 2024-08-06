package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.request.BlockedDateRequest;
import io.oxalate.backend.api.response.BlockedDateResponse;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_ADD_OK;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_ADD_START;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_ADD_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_REMOVE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_REMOVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_REMOVE_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.BlockedDateAPI;
import io.oxalate.backend.service.BlockedDateService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class BlockedDateController implements BlockedDateAPI {
    private final BlockedDateService blockedDateService;
    private final AppEventPublisher appEventPublisher;
    private static final String AUDIT_NAME = "BlockedDateController";

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<BlockedDateResponse>> getAllBlockedDates(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(BLOCKED_DATE_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(BLOCKED_DATE_GET_ALL_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var blockedDates = blockedDateService.getAllBlockedDates();

        appEventPublisher.publishAuditEvent(BLOCKED_DATE_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(blockedDates);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<BlockedDateResponse> addBlockedDate(BlockedDateRequest blockedDateRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(BLOCKED_DATE_ADD_START, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(BLOCKED_DATE_ADD_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var blockedDate = blockedDateService.createBlock(blockedDateRequest, userId);

        appEventPublisher.publishAuditEvent(BLOCKED_DATE_ADD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(blockedDate);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> removeBlockedDate(long blockedDateId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(BLOCKED_DATE_REMOVE_START, INFO, request, AUDIT_NAME, userId);

        var blockedDateResponse = blockedDateService.findById(blockedDateId);

        if (blockedDateResponse == null) {
            appEventPublisher.publishAuditEvent(BLOCKED_DATE_REMOVE_NOT_FOUND, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.notFound().build();
        }

        blockedDateService.removeBlock(blockedDateId);
        appEventPublisher.publishAuditEvent(BLOCKED_DATE_REMOVE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok().build();
    }
}
