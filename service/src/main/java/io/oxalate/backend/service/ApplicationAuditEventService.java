package io.oxalate.backend.service;

import io.oxalate.backend.api.response.AuditEntryResponse;
import io.oxalate.backend.events.AppAuditEvent;
import io.oxalate.backend.model.ApplicationAuditEvent;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.ApplicationAuditEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApplicationAuditEventService {
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

    public Page<AuditEntryResponse> getAllAuditEvents(int page, int pageSize, Sort sort) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, sort);

        var auditEvents = applicationAuditEventRepository.findAll(pageRequest);
        var auditEntryResponses = auditEvents.map(ApplicationAuditEvent::toAuditEntryResponse);
        insertUsernames(auditEntryResponses);

        return auditEntryResponses;
    }

    public Page<AuditEntryResponse> getAllAuditEventsFiltered(int page, int pageSize, Sort sort, String filter, String filterColumn) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, sort);

        log.debug("getAllAuditEventsFiltered: filter: {}, filterColumn: {}", filter, filterColumn);

        var userIdList = new ArrayList<Long>();
        if (filterColumn.equals("userId")) {
            var users = userService.findUsersByName(filter);
            // Get the user ID from the list of users
            userIdList.addAll(users.stream()
                                   .map(User::getId)
                                   .toList());
            log.debug("Found {} users with name {}: {}", userIdList.size(), filter, userIdList);
        }

        Page<ApplicationAuditEvent> auditEvents = switch (filterColumn) {
            case "traceId" -> applicationAuditEventRepository.findByTraceIdContainingIgnoreCase(filter, pageRequest);
            case "message" -> applicationAuditEventRepository.findByMessageContainingIgnoreCase(filter, pageRequest);
            case "ipAddress" -> applicationAuditEventRepository.findByIpAddressContainingIgnoreCase(filter, pageRequest);
            case "source" -> applicationAuditEventRepository.findBySourceContainingIgnoreCase(filter, pageRequest);
            case "userId" -> applicationAuditEventRepository.findAllByUserIdIn(userIdList, pageRequest);
            default -> applicationAuditEventRepository.findAll(pageRequest);
        };

        var auditEntryResponses = auditEvents.map(ApplicationAuditEvent::toAuditEntryResponse);
        insertUsernames(auditEntryResponses);

        return auditEntryResponses;
    }

    public Page<AuditEntryResponse> getAllAuditEventsForUser(long userId, int page, int pageSize, Sort sort) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, sort);
        var auditEvents = applicationAuditEventRepository.findByUserId(userId, pageRequest);

        var auditEntryResponses = auditEvents.map(ApplicationAuditEvent::toAuditEntryResponse);
        insertUsernames(auditEntryResponses);

        return auditEntryResponses;
    }

    private void insertUsernames(Page<AuditEntryResponse> auditEntryResponses) {
        for (var entry : auditEntryResponses) {
            if (entry.getUserId() > 0) {
                var user = userService.findUserEntityById(entry.getUserId());

                if (user != null) {
                    entry.setUserName(user.getLastName() + " " + user.getFirstName());
                } else {
                    if (entry.getUserId() > 0) {
                        log.warn("When fetching audit events, could not find user for ID: {}", entry.getUserId());
                    }

                    entry.setUserName("Unknown");
                }
            } else {
                entry.setUserName("Unknown");
            }
        }
    }

    @Transactional
    public long cleanupAuditTrail() {
        return applicationAuditEventRepository.deleteByCreatedAtBefore(Instant.now()
                                                                              .minus(auditRetentionDays, ChronoUnit.DAYS));
    }
}
