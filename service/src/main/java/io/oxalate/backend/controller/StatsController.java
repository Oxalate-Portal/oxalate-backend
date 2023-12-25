package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import static io.oxalate.backend.api.RoleEnum.ROLE_USER;
import io.oxalate.backend.api.response.stats.EventPeriodReportResponse;
import io.oxalate.backend.api.response.stats.MultiYearValue;
import io.oxalate.backend.api.response.stats.YearlyDiversListResponse;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_DIVER_LIST_OK;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_DIVER_LIST_START;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_DIVER_LIST_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_EVENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_EVENTS_START;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_EVENTS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_EVENT_REPORTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_EVENT_REPORTS_START;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_EVENT_REPORTS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_ORGANIZERS_OK;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_ORGANIZERS_START;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_ORGANIZERS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_PAYMENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_PAYMENTS_START;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_PAYMENTS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_REGISTRATION_OK;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_REGISTRATION_START;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_REGISTRATION_UNAUTHORIZED;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.StatsAPI;
import io.oxalate.backend.service.StatsService;
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
public class StatsController implements StatsAPI {
    private final StatsService statsService;
    private static final String AUDIT_NAME = "StatsController";
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<MultiYearValue>> getYearlyRegistrations(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_REGISTRATION_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_REGISTRATION_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to access yearly registration stats without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var multiYearValues = statsService.getYearlyRegistrations();
        appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_REGISTRATION_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<MultiYearValue>> getYearlyEvents(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_EVENTS_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_EVENTS_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to access yearly event stats without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var multiYearValues = statsService.getYearlyEvents();
        appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_EVENTS_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<MultiYearValue>> getYearlyOrganizers(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_ORGANIZERS_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_ORGANIZERS_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to access yearly organizer stats without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var multiYearValues = statsService.getYearlyOrganizers();
        appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_ORGANIZERS_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<MultiYearValue>> getYearlyPayments(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_PAYMENTS_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_PAYMENTS_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to access yearly payment stats without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_PAYMENTS_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        var multiYearValues = statsService.getYearlyPayments();
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EventPeriodReportResponse>> getEventReports(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_EVENT_REPORTS_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_EVENT_REPORTS_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.error("User ID {} tried to access event reports without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_EVENT_REPORTS_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        var reports = statsService.getEventReports();
        return ResponseEntity.ok().body(reports);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<YearlyDiversListResponse>> yearlyDiverList(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_DIVER_LIST_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER, ROLE_USER)) {
            appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_DIVER_LIST_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to access yearly diver list stats without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(STATS_GET_YEARLY_DIVER_LIST_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        var yearlyLists = statsService.getYearlyDiversList();
        return ResponseEntity.ok()
                             .body(yearlyLists);
    }
}
