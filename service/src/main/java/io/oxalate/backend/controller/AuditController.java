package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.AuditEntryResponse;
import io.oxalate.backend.api.response.PagedResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * OWASP A01/A10:2025 - the sort column, the page size and the filter column arrive straight from the client. They are
 * validated against allow-lists in {@link ApplicationAuditEventService} through {@code PagingTools}, so an unknown
 * value falls back to the default instead of reaching Spring Data and producing a 500.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("AuditController")
public class AuditController implements AuditAPI {

    private final ApplicationAuditEventService applicationAuditEventService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = AUDIT_GET_START, okMessage = AUDIT_GET_OK)
    public ResponseEntity<PagedResponse<AuditEntryResponse>> getAuditEvents(PagedRequest pagedRequest, String filterColumn) {
        log.debug("getAuditEvents: page: {}, size: {}, sortBy: {}, direction: {}, filterColumn: {}", pagedRequest.getPage(), pagedRequest.getSize(),
                pagedRequest.getSortBy(), pagedRequest.getDirection(), filterColumn);
        var auditEvents = applicationAuditEventService.getAuditEventsPaged(pagedRequest, filterColumn);

        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = AUDIT_GET_USER_START, okMessage = AUDIT_GET_USER_OK)
    public ResponseEntity<PagedResponse<AuditEntryResponse>> getAuditEventsByUserId(long userId, PagedRequest pagedRequest) {
        var auditEvents = applicationAuditEventService.getAuditEventsForUserPaged(userId, pagedRequest);

        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }
}
