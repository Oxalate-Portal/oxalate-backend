package io.oxalate.backend.controller;

import io.oxalate.backend.api.response.AuditEntryResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_USER_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUDIT_GET_USER_START;
import io.oxalate.backend.rest.AuditAPI;
import io.oxalate.backend.service.ApplicationAuditEventService;
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
@AuditSource("AuditController")
public class AuditController implements AuditAPI {
    private final ApplicationAuditEventService applicationAuditEventService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = AUDIT_GET_START, okMessage = AUDIT_GET_OK)
    public ResponseEntity<Page<AuditEntryResponse>> getAuditEvents(int page, int pageSize, String sorting, String filter, String filterColumn) {
        log.debug("getAuditEvents: page: {}, pageSize: {}, sorting: {}, filter: {}, filterColumn: {}", page, pageSize, sorting, filter, filterColumn);
        var column = sorting.split(",")[0];
        var direction = sorting.split(",")[1];

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

        var sort = Sort.by(Sort.Direction.fromString(direction), column);

        Page<AuditEntryResponse> auditEvents;

        if (useFiltering) {
            auditEvents = applicationAuditEventService.getAllAuditEventsFiltered(page, pageSize, sort, filter, filterColumn);
        } else {
            auditEvents = applicationAuditEventService.getAllAuditEvents(page, pageSize, sort);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = AUDIT_GET_USER_START, okMessage = AUDIT_GET_USER_OK)
    public ResponseEntity<Page<AuditEntryResponse>> getAuditEventsByUserId(long userId, int page, int pageSize, String sorting) {
        var column = sorting.split(",")[0];
        var direction = sorting.split(",")[1];

        if (direction.equals("ascend")) {
            direction = "ASC";
        } else {
            direction = "DESC";
        }

        var sort = Sort.by(Sort.Direction.fromString(direction), "userId");
        var auditEvents = applicationAuditEventService.getAllAuditEventsForUser(userId, page, pageSize, sort);

        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }
}
