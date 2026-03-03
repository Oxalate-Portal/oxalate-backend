package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import static io.oxalate.backend.api.RoleEnum.ROLE_USER;
import io.oxalate.backend.api.response.stats.AggregateResponse;
import io.oxalate.backend.api.response.stats.EventPeriodReportResponse;
import io.oxalate.backend.api.response.stats.MultiYearValueResponse;
import io.oxalate.backend.api.response.stats.YearlyDiversListResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_AGGREGATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_AGGREGATE_START;
import static io.oxalate.backend.events.AppAuditMessages.STATS_GET_YEARLY_AGGREGATE_UNAUTHORIZED;
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
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.rest.StatsAPI;
import io.oxalate.backend.service.StatsService;
import io.oxalate.backend.tools.AuthTools;
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
@AuditSource("StatsController")
public class StatsController implements StatsAPI {
    private final StatsService statsService;

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = STATS_GET_YEARLY_REGISTRATION_START, okMessage = STATS_GET_YEARLY_REGISTRATION_OK)
    public ResponseEntity<List<MultiYearValueResponse>> getYearlyRegistrationTimeSeries() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            log.error("User ID {} tried to access yearly registration stats without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(STATS_GET_YEARLY_REGISTRATION_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var multiYearValues = statsService.getYearlyRegistrations();
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = STATS_GET_YEARLY_EVENTS_START, okMessage = STATS_GET_YEARLY_EVENTS_OK)
    public ResponseEntity<List<MultiYearValueResponse>> getYearlyEventTimeSeries() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            log.error("User ID {} tried to access yearly event stats without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(STATS_GET_YEARLY_EVENTS_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var multiYearValues = statsService.getYearlyEvents();
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = STATS_GET_YEARLY_ORGANIZERS_START, okMessage = STATS_GET_YEARLY_ORGANIZERS_OK)
    public ResponseEntity<List<MultiYearValueResponse>> getYearlyOrganizerTimeSeries() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            log.error("User ID {} tried to access yearly organizer stats without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(STATS_GET_YEARLY_ORGANIZERS_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var multiYearValues = statsService.getYearlyOrganizers();
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = STATS_GET_YEARLY_PAYMENTS_START, okMessage = STATS_GET_YEARLY_PAYMENTS_OK)
    public ResponseEntity<List<MultiYearValueResponse>> getYearlyPaymentTimeSeries() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            log.error("User ID {} tried to access yearly payment stats without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(STATS_GET_YEARLY_PAYMENTS_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var multiYearValues = statsService.getYearlyPayments();
        return ResponseEntity.ok().body(multiYearValues);
    }

    @Override
    @Audited(startMessage = STATS_GET_YEARLY_AGGREGATE_START, okMessage = STATS_GET_YEARLY_AGGREGATE_OK)
    public ResponseEntity<AggregateResponse> getAggregateStats() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            log.error("User ID {} tried to access yearly aggregate stats without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(STATS_GET_YEARLY_AGGREGATE_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var aggregateResponse = statsService.getAggregateData();
        return ResponseEntity.ok()
                             .body(aggregateResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = STATS_GET_YEARLY_EVENT_REPORTS_START, okMessage = STATS_GET_YEARLY_EVENT_REPORTS_OK)
    public ResponseEntity<List<EventPeriodReportResponse>> getEventReports() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            log.error("User ID {} tried to access event reports without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(STATS_GET_YEARLY_EVENT_REPORTS_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var reports = statsService.getEventReports();
        return ResponseEntity.ok().body(reports);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = STATS_GET_YEARLY_DIVER_LIST_START, okMessage = STATS_GET_YEARLY_DIVER_LIST_OK)
    public ResponseEntity<List<YearlyDiversListResponse>> yearlyDiverList() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER, ROLE_USER)) {
            log.error("User ID {} tried to access yearly diver list stats without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(STATS_GET_YEARLY_DIVER_LIST_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var yearlyLists = statsService.getYearlyDiversList();
        return ResponseEntity.ok()
                             .body(yearlyLists);
    }
}
