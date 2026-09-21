package io.oxalate.backend.service;

import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.AuditEntryResponse;
import io.oxalate.backend.api.response.PagedResponse;
import io.oxalate.backend.events.AppAuditEvent;
import io.oxalate.backend.model.ApplicationAuditEvent;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.ApplicationAuditEventRepository;
import io.oxalate.backend.tools.PagingTools;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApplicationAuditEventService {

    /**
     * Value of {@code filter_column} that filters by the name of the user instead of a column of the audit entry.
     */
    public static final String USER_NAME_FILTER = "user_name";

    /**
     * OWASP A01/A10:2025 - client sort names mapped to the entity property they sort by. {@code user_name} and
     * {@code address} are the response field names the frontend table uses, so they are accepted as aliases.
     */
    private static final Map<String, String> SORTABLE_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("user_id", "userId"),
            Map.entry("user_name", "userId"),
            Map.entry("level", "level"),
            Map.entry("trace_id", "traceId"),
            Map.entry("ip_address", "ipAddress"),
            Map.entry("address", "ipAddress"),
            Map.entry("source", "source"),
            Map.entry("message", "message"),
            Map.entry("created_at", "createdAt"));
    private static final String DEFAULT_SORT_COLUMN = "createdAt";

    /**
     * Client filter column names mapped to the string property of the entity they search in.
     */
    private static final Map<String, String> SEARCHABLE_COLUMNS = Map.of(
            "trace_id", "traceId",
            "source", "source",
            "message", "message",
            "ip_address", "ipAddress",
            "address", "ipAddress");

    private final ApplicationAuditEventRepository applicationAuditEventRepository;
    private final UserService userService;

    @Value("${oxalate.audit.retention.days:30}")
    private int auditRetentionDays;

    public void save(AppAuditEvent appAuditEvent) {
        var applicationAuditEvent = ApplicationAuditEvent.builder()
                                                         .message(appAuditEvent.getMessage())
                                                         .level(appAuditEvent.getLevel())
                                                         .traceId(appAuditEvent.getTraceId())
                                                         .ipAddress(appAuditEvent.getAddress())
                                                         .userId(appAuditEvent.getUserId())
                                                         .source(appAuditEvent.getSource()
                                                                              .toString())
                                                         .createdAt(appAuditEvent.getCreatedAt())
                                                         .build();
        log.debug("Save application audit event: {}", applicationAuditEvent);

        try {
            applicationAuditEventRepository.save(applicationAuditEvent);
        } catch (Exception e) {
            log.error("Failed to store audit event: {}", appAuditEvent, e);
        }
    }

    /**
     * One page of audit entries. When both a filter column and a search term are given, only entries whose column
     * contains the term are returned; with {@value #USER_NAME_FILTER} the term is matched against user names and the
     * entries of the matching users are returned. An unknown filter column is ignored.
     *
     * @param pagedRequest paging, sorting and the search term
     * @param filterColumn column the search applies to, may be {@code null}
     * @return the requested page with user names resolved
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditEntryResponse> getAuditEventsPaged(PagedRequest pagedRequest, @Nullable String filterColumn) {
        var pageable = PagingTools.toPageable(pagedRequest, SORTABLE_COLUMNS, DEFAULT_SORT_COLUMN, Sort.Direction.DESC);
        var specification = PagingTools.allOf(filterSpecification(pagedRequest, filterColumn));

        return toPagedResponse(applicationAuditEventRepository.findAll(specification, pageable));
    }

    /**
     * One page of the audit entries of a single user.
     *
     * @param userId       user whose entries are returned
     * @param pagedRequest paging and sorting
     * @return the requested page with user names resolved
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditEntryResponse> getAuditEventsForUserPaged(long userId, PagedRequest pagedRequest) {
        var pageable = PagingTools.toPageable(pagedRequest, SORTABLE_COLUMNS, DEFAULT_SORT_COLUMN, Sort.Direction.DESC);
        Specification<ApplicationAuditEvent> specification = (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);

        return toPagedResponse(applicationAuditEventRepository.findAll(PagingTools.allOf(specification), pageable));
    }

    @Nullable
    private Specification<ApplicationAuditEvent> filterSpecification(PagedRequest pagedRequest, @Nullable String filterColumn) {
        var search = pagedRequest.getSearch();

        if (filterColumn == null || filterColumn.isBlank() || search == null || search.isBlank()) {
            return null;
        }

        var column = filterColumn.trim();

        if (USER_NAME_FILTER.equals(column)) {
            var userIds = userService.findUsersByName(search.trim())
                                     .stream()
                                     .map(User::getId)
                                     .toList();
            log.debug("Found {} users matching the audit filter", userIds.size());
            return userIdIn(userIds);
        }

        var property = SEARCHABLE_COLUMNS.get(column);

        if (property == null) {
            log.warn("Ignoring unsupported audit filter column");
            return null;
        }

        return PagingTools.searchSpecification(pagedRequest, property);
    }

    /**
     * Entries of the given users; an empty list matches nothing instead of producing an invalid {@code IN ()}.
     */
    private static Specification<ApplicationAuditEvent> userIdIn(List<Long> userIds) {
        return (root, query, criteriaBuilder) -> userIds.isEmpty() ? criteriaBuilder.disjunction() : root.get("userId")
                                                                                                          .in(userIds);
    }

    private PagedResponse<AuditEntryResponse> toPagedResponse(Page<ApplicationAuditEvent> auditEvents) {
        return PagedResponse.fromPage(auditEvents, auditEvent -> {
            var entry = auditEvent.toAuditEntryResponse();
            insertUsername(entry);
            return entry;
        });
    }

    private void insertUsername(AuditEntryResponse entry) {
        if (entry.getUserId() <= 0) {
            entry.setUserName("Unknown");
            return;
        }

        var user = userService.findUserEntityById(entry.getUserId());

        if (user == null) {
            log.warn("When fetching audit events, could not find user for ID: {}", entry.getUserId());
            entry.setUserName("Unknown");
            return;
        }

        entry.setUserName(user.getLastName() + " " + user.getFirstName());
    }

    @Transactional
    public long cleanupAuditTrail() {
        return applicationAuditEventRepository.deleteByCreatedAtBefore(Instant.now()
                                                                              .minus(auditRetentionDays, ChronoUnit.DAYS));
    }
}
