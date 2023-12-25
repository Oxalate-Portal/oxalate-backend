package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import io.oxalate.backend.api.response.AuditEntryResponse;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_USER_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_USER_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.AuditAPI;
import io.oxalate.backend.service.ApplicationAuditEventService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class AuditController implements AuditAPI {
    private static final String AUDIT_NAME = "AuditController";
    private final ApplicationAuditEventService applicationAuditEventService;
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Page<AuditEntryResponse>> getAuditEvents(int page, int pageSize, String sorting, String filter, String filterColumn,
            HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUDIT_GET_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        log.debug("getAuditEvents: page: {}, pageSize: {}, sorting: {}, filter: {}, filterColumn: {}", page, pageSize, sorting, filter, filterColumn);
        // Parse sorting string to separate strings for field and direction
        var column = sorting.split(",")[0];
        var direction = sorting.split(",")[1];

        // The frontend sends the direction as "ascend" or "descend", but Spring expects "ASC" or "DESC", so we convert
        if (direction.equals("ascend")) {
            direction = "ASC";
        } else {
            direction = "DESC";
        }

        if (column.equals("userName")) {
            column = "userId";
        }

        if (filterColumn.equals("userName")) {
            filterColumn = "userId";
        }

        var useFiltering = false;

        if (!filter.trim()
                   .isEmpty()) {
            if (!filterColumn.trim()
                             .isEmpty()) {
                useFiltering = true;
            }
        }

        // Then create the Sort object
        var sort = Sort.by(Sort.Direction.fromString(direction), column);

        Page<AuditEntryResponse> auditEvents = null;

        if (useFiltering) {
            auditEvents = applicationAuditEventService.getAllAuditEventsFiltered(page, pageSize, sort, filter, filterColumn);
        } else {
            auditEvents = applicationAuditEventService.getAllAuditEvents(page, pageSize, sort);
        }

        appEventPublisher.publishAuditEvent(AUDIT_GET_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Page<AuditEntryResponse>> getAuditEvents(long userId, int page, int pageSize, String sorting, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUDIT_GET_USER_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        var column = sorting.split(",")[0];
        var direction = sorting.split(",")[1];

        // The frontend sends the direction as "ascend" or "descend", but Spring expects "ASC" or "DESC", so we convert
        if (direction.equals("ascend")) {
            direction = "ASC";
        } else {
            direction = "DESC";
        }

        // Then create the Sort object
        var sort = Sort.by(Sort.Direction.fromString(direction), "userId");

        var auditEvents = applicationAuditEventService.getAllAuditEventsForUser(userId, page, pageSize, sort);

        appEventPublisher.publishAuditEvent(AUDIT_GET_USER_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }
}
