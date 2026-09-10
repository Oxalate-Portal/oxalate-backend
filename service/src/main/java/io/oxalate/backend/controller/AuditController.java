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
import java.util.Set;
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

    /**
     * OWASP A01/A10:2025 - the sort column arrives straight from the client and is handed to Spring Data,
     * which resolves it against the entity. An unknown value produces a PropertyReferenceException and a 500,
     * so only known-safe properties are accepted.
     */
    private static final Set<String> SORTABLE_COLUMNS = Set.of("id", "userId", "level", "traceId", "ipAddress", "source", "message", "createdAt");

    private static final String DEFAULT_SORT_COLUMN = "createdAt";

    /**
     * OWASP A10:2025 - an uncapped page size lets a single request pull the whole audit table into memory.
     */
    private static final int MAX_PAGE_SIZE = 200;

    private final ApplicationAuditEventService applicationAuditEventService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = AUDIT_GET_START, okMessage = AUDIT_GET_OK)
    public ResponseEntity<Page<AuditEntryResponse>> getAuditEvents(int page, int pageSize, String sorting, String filter, String filterColumn) {
        log.debug("getAuditEvents: page: {}, pageSize: {}, sorting: {}, filter: {}, filterColumn: {}", page, pageSize, sorting, filter, filterColumn);
        var sort = parseSorting(sorting);

        if (filterColumn != null && filterColumn.equals("userName")) {
            filterColumn = "userId";
        }

        var useFiltering = filter != null
                && !filter.trim()
                          .isEmpty()
                && filterColumn != null
                && !filterColumn.trim()
                                .isEmpty();

        Page<AuditEntryResponse> auditEvents;

        if (useFiltering) {
            auditEvents = applicationAuditEventService.getAllAuditEventsFiltered(sanitizePage(page), sanitizePageSize(pageSize), sort, filter, filterColumn);
        } else {
            auditEvents = applicationAuditEventService.getAllAuditEvents(sanitizePage(page), sanitizePageSize(pageSize), sort);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = AUDIT_GET_USER_START, okMessage = AUDIT_GET_USER_OK)
    public ResponseEntity<Page<AuditEntryResponse>> getAuditEventsByUserId(long userId, int page, int pageSize, String sorting) {
        var direction = parseSorting(sorting).stream()
                                             .findFirst()
                                             .map(Sort.Order::getDirection)
                                             .orElse(Sort.Direction.DESC);
        var sort = Sort.by(direction, "userId");
        var auditEvents = applicationAuditEventService.getAllAuditEventsForUser(userId, sanitizePage(page), sanitizePageSize(pageSize), sort);

        return ResponseEntity.status(HttpStatus.OK)
                             .body(auditEvents);
    }

    /**
     * Turns the client supplied {@code column,direction} string into a validated {@link Sort}.
     * <p>
     * Anything malformed, unknown or missing falls back to the default ordering rather than throwing, so a
     * crafted query parameter cannot produce a 500 or reach Spring Data with an arbitrary property name.
     *
     * @param sorting raw sorting parameter, may be {@code null}
     * @return a safe sort specification
     */
    private Sort parseSorting(String sorting) {
        var column = DEFAULT_SORT_COLUMN;
        var direction = Sort.Direction.DESC;

        if (sorting != null && !sorting.isBlank()) {
            var parts = sorting.split(",");

            if (parts.length == 0) {
                // A parameter consisting only of commas splits into an empty array
                return Sort.by(direction, column);
            }

            var requestedColumn = parts[0].trim();

            if ("userName".equals(requestedColumn)) {
                requestedColumn = "userId";
            }

            if (SORTABLE_COLUMNS.contains(requestedColumn)) {
                column = requestedColumn;
            } else {
                log.warn("Ignoring unsupported audit sort column");
            }

            if (parts.length > 1 && ("ascend".equalsIgnoreCase(parts[1].trim()) || "asc".equalsIgnoreCase(parts[1].trim()))) {
                direction = Sort.Direction.ASC;
            }
        }

        return Sort.by(direction, column);
    }

    private int sanitizePage(int page) {
        return Math.max(page, 0);
    }

    private int sanitizePageSize(int pageSize) {
        if (pageSize < 1) {
            return 1;
        }

        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
