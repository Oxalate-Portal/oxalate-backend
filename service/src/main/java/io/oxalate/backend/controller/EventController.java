package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevelEnum.ERROR;
import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import static io.oxalate.backend.api.AuditLevelEnum.WARN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.EventDiveListRequest;
import io.oxalate.backend.api.request.EventDiveRequest;
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.api.request.EventSubscribeRequest;
import io.oxalate.backend.api.response.EventDiveListResponse;
import io.oxalate.backend.api.response.EventListResponse;
import io.oxalate.backend.api.response.EventResponse;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CANCEL_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CANCEL_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CANCEL_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CREATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CREATE_INVALID_DATETIME;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CREATE_INVALID_ORGANIZER;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CREATE_INVALID_PARTICIPANTS_COUNT;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_CURRENT_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_CURRENT_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_CURRENT_TERMS_NOT_ACCEPTED;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_DIVES_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_DIVES_NO_DIVES;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_DIVES_NO_PARTICIPANTS;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_DIVES_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_DIVES_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_DIVES_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_FUTURE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_FUTURE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_FUTURE_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_FUTURE_TERMS_NOT_ACCEPTED;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_PAST_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_PAST_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_PAST_TERMS_NOT_ACCEPTED;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_SINGLE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_SINGLE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_SINGLE_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_USER_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_USER_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_GET_USER_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_SUBSCRIBE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_SUBSCRIBE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_SUBSCRIBE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_SUBSCRIBE_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_SUBSCRIBE_TERMS_NOT_ACCEPTED;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_SUBSCRIBE_UNKNOWN_USER;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UNSUBSCRIBE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UNSUBSCRIBE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UNSUBSCRIBE_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UNSUBSCRIBE_UNKNOWN_USER;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_DIVES_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_DIVES_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_DIVES_NO_DIVES;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_DIVES_NO_PARTICIPANTS;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_DIVES_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_DIVES_START;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_DIVES_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_INVALID_DATETIME;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_INVALID_PARTICIPANTS_COUNT;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_ORGANIZER_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.EVENTS_UPDATE_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.EventAPI;
import io.oxalate.backend.service.EventService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class EventController implements EventAPI {

    private final EventService eventService;
    private final UserService userService;
    private static final String AUDIT_NAME = "EventController";
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EventResponse>> getFutureEvents(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_GET_FUTURE_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAcceptedTerms()) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_FUTURE_TERMS_NOT_ACCEPTED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                                 .body(null);
        }

        try {
            var events = eventService.findAllEventsAfter(Instant.now(), AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN));

            if (events.isEmpty()) {
                log.debug("No future events found");
            }

            appEventPublisher.publishAuditEvent(EVENTS_GET_FUTURE_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK).body(events);
        } catch (Exception e) {
            log.error("Failed to get events: {}", e.getMessage(), e);
            appEventPublisher.publishAuditEvent(EVENTS_GET_FUTURE_FAIL, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EventResponse>> getOngoingEvents(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_GET_CURRENT_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAcceptedTerms()) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_CURRENT_TERMS_NOT_ACCEPTED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                                 .body(null);
        }

        var eventResponses = eventService.findAllCurrentEvents();

        if (eventResponses.isEmpty()) {
            log.debug("No current events found");
        }

        appEventPublisher.publishAuditEvent(EVENTS_GET_CURRENT_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(eventResponses);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EventResponse>> getPastEvents(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_GET_PAST_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAcceptedTerms()) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_PAST_TERMS_NOT_ACCEPTED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                                 .body(null);
        }

        var eventResponses = eventService.findAllEventsBefore(Instant.now());

        appEventPublisher.publishAuditEvent(EVENTS_GET_PAST_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok().body(eventResponses);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EventListResponse>> getEventsForUser(long userId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_GET_USER_START + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        // Check if user is allowed to see this user's events. ADMIN and ORGANIZER can see any, USER can see only their own
        if (AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) || AuthTools.isUserIdCurrentUser(userId)) {
            var events = eventService.findEventsForUser(userId);
            appEventPublisher.publishAuditEvent(EVENTS_GET_USER_OK + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(events);
        }

        appEventPublisher.publishAuditEvent(EVENTS_GET_USER_UNAUTHORIZED + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        log.error("User {} is not allowed to see user {}'s event list", AuthTools.getCurrentUserId(), userId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> getEventById(long eventId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_GET_SINGLE_START + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        var eventResponse = eventService.findById(eventId);

        if (eventResponse == null) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_SINGLE_NOT_FOUND + eventId, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.warn("Event ID {} not found", eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(EVENTS_GET_SINGLE_OK + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(eventResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> createEvent(EventRequest eventRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_CREATE_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        var userId = eventRequest.getOrganizerId();

        if (userId < 1L) {
            log.error("Failed to create event from request, invalid organizer: {}", eventRequest);
            appEventPublisher.publishAuditEvent(EVENTS_CREATE_INVALID_ORGANIZER + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        if (eventRequest.getStartTime().isBefore(Instant.now())) {
            appEventPublisher.publishAuditEvent(EVENTS_CREATE_INVALID_DATETIME + eventRequest.getStartTime(), ERROR, request, AUDIT_NAME,
                    AuthTools.getCurrentUserId(), auditUuid);
            log.warn("Can not create an event {} with start time is set before present", eventRequest.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // Make sure the number of current participants is not higher than the maximum number of participants
        if (eventRequest.getParticipants() != null && eventRequest.getMaxParticipants() < eventRequest.getParticipants().size()) {
            appEventPublisher.publishAuditEvent(EVENTS_CREATE_INVALID_PARTICIPANTS_COUNT + eventRequest.getMaxParticipants(), ERROR, request, AUDIT_NAME,
                    AuthTools.getCurrentUserId(), auditUuid);
            log.error("Event has more current participants than maximum number of participants: {}", eventRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var eventResponse = eventService.createEvent(eventRequest, userId);

        if (eventResponse == null) {
            appEventPublisher.publishAuditEvent(EVENTS_CREATE_FAIL, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Failed to create event from request: {}", eventRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        appEventPublisher.publishAuditEvent(EVENTS_CREATE_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(EventRequest eventRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_UPDATE_START + eventRequest.getId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        var checkEvent = eventService.findById(eventRequest.getId());

        if (checkEvent == null) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_NOT_FOUND + eventRequest.getId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Attempted to update non-existing event: {}", eventRequest);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var organizer = userService.findUserEntityById(eventRequest.getOrganizerId());

        if (organizer == null) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_ORGANIZER_NOT_FOUND + eventRequest.getOrganizerId(), ERROR, request, AUDIT_NAME,
                    AuthTools.getCurrentUserId(), auditUuid);
            log.error("Event has an non-existing organizer: {}", eventRequest.getOrganizerId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // Make sure the number of current participants is not higher than the maximum number of participants
        if (eventRequest.getMaxParticipants() < eventRequest.getParticipants().size()) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_INVALID_PARTICIPANTS_COUNT + eventRequest.getMaxParticipants(), ERROR, request, AUDIT_NAME,
                    AuthTools.getCurrentUserId(), auditUuid);
            log.error("Event has more current participants than maximum number of participants: {}", eventRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var calculatedEndTime = eventRequest.getStartTime().plus(eventRequest.getEventDuration(), ChronoUnit.HOURS);
        // We allow edit to an ongoing event, but not to an event that has already ended
        if (Instant.now().isAfter(calculatedEndTime)) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_INVALID_DATETIME + calculatedEndTime, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.warn("Can't update an event {} which calculated end time {} has passed", eventRequest.getId(), calculatedEndTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var eventResponse = eventService.updateEvent(eventRequest);

        appEventPublisher.publishAuditEvent(EVENTS_UPDATE_OK + eventRequest.getId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok().body(eventResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventDiveListResponse> getEventDives(long eventId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_GET_DIVES_START + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_DIVES_UNAUTHORIZED + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User {} is not allowed to retrieve event ID {} dive count", AuthTools.getCurrentUserId(), eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        var eventResponse = eventService.findById(eventId);

        if (eventResponse == null) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_DIVES_NOT_FOUND + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Event {} not found for dive retrieval", eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        if (eventResponse.getParticipants()
                         .isEmpty()) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_DIVES_NO_PARTICIPANTS + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.error("Event {} does not have any participants", eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var eventDiveListResponse = eventService.getEventDives(eventId);

        if (eventDiveListResponse == null || eventDiveListResponse.getDives().isEmpty()) {
            appEventPublisher.publishAuditEvent(EVENTS_GET_DIVES_NO_DIVES + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Event {} does not have any dives", eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        appEventPublisher.publishAuditEvent(EVENTS_GET_DIVES_OK + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(eventDiveListResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventDiveListResponse> updateEventDives(long eventId, EventDiveListRequest eventDiveListRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_UPDATE_DIVES_START + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_DIVES_UNAUTHORIZED + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.error("User {} is not allowed to update event ID {} dive count", AuthTools.getCurrentUserId(), eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        if (eventDiveListRequest.getDives()
                                .isEmpty()) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_DIVES_NO_DIVES + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Event dive list in request is empty for dive ID: {}", eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        var event = eventService.findById(eventId);

        if (event == null) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_DIVES_NOT_FOUND + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Event {} not found for dive updating", eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (event.getParticipants().isEmpty()) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_DIVES_NO_PARTICIPANTS + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.error("Event {} does not have any participants", eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        for (EventDiveRequest eventDiveRequest : eventDiveListRequest.getDives()) {
            eventService.updateUserDiveCount(eventId, eventDiveRequest.getUserId(), eventDiveRequest.getDiveCount());
        }

        var eventDiveListResponse = eventService.getEventDives(eventId);

        if (eventDiveListResponse == null) {
            appEventPublisher.publishAuditEvent(EVENTS_UPDATE_DIVES_FAIL + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Something went wrong while updating diver counts for event ID: {}", eventId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        appEventPublisher.publishAuditEvent(EVENTS_UPDATE_DIVES_OK + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(eventDiveListResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<HttpStatus> cancelEvent(long eventId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_CANCEL_START + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        try {
            eventService.cancel(eventId);
            appEventPublisher.publishAuditEvent(EVENTS_CANCEL_OK + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                                 .body(null);
        } catch (Exception e) {
            appEventPublisher.publishAuditEvent(EVENTS_CANCEL_FAIL + eventId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<EventResponse> subscribe(Authentication auth, EventSubscribeRequest eventSubscribeRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_SUBSCRIBE_START + eventSubscribeRequest.getDiveEventId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        var eventId = eventSubscribeRequest.getDiveEventId();

        if (!AuthTools.currentUserHasAcceptedTerms()) {
            appEventPublisher.publishAuditEvent(EVENTS_SUBSCRIBE_TERMS_NOT_ACCEPTED, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                                 .body(null);
        }

        var optionalUser = userService.findByUsername(auth.getName());

        if (optionalUser.isEmpty()) {
            appEventPublisher.publishAuditEvent(EVENTS_SUBSCRIBE_UNKNOWN_USER + auth.getName(), WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.error("Could not add non-existing user to event: {}", auth.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        var eventResponse = eventService.findById(eventId);

        if (eventResponse == null) {
            appEventPublisher.publishAuditEvent(EVENTS_SUBSCRIBE_NOT_FOUND + eventId, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Could not add user to non-existing event: {}", eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        log.info("Adding user ID {} to event {}", optionalUser.get().getId(), eventId);

        var newEventResponse = eventService.addUserToEvent(optionalUser.get(), eventSubscribeRequest);

        if (newEventResponse == null) {
            appEventPublisher.publishAuditEvent(EVENTS_SUBSCRIBE_FAIL + eventId, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Can not subscribe user ID {} to event: {}", optionalUser.get()
                                                                               .getId(), eventId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        appEventPublisher.publishAuditEvent(EVENTS_SUBSCRIBE_OK + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok().body(newEventResponse);
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<EventResponse> unSubscribe(Authentication auth, long eventId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(EVENTS_UNSUBSCRIBE_START + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        var optionalUser = userService.findByUsername(auth.getName());

        if (optionalUser.isEmpty()) {
            appEventPublisher.publishAuditEvent(EVENTS_UNSUBSCRIBE_UNKNOWN_USER + auth.getName(), WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.warn("Unable to find user {} when unsubscribing from event {}", auth.getName(), eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        log.info("Removing user ID {} from event {}", optionalUser.get()
                                                                  .getId(), eventId);

        var eventResponse = eventService.removeUserFromEvent(optionalUser.get(), eventId);

        if (eventResponse == null) {
            appEventPublisher.publishAuditEvent(EVENTS_UNSUBSCRIBE_FAIL + eventId, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.warn("Can not unsubscribe user {} from event {}", optionalUser.get()
                                                                              .getId(), eventId);
            return ResponseEntity.status(HttpStatus.LOCKED).body(null);
        }

        appEventPublisher.publishAuditEvent(EVENTS_UNSUBSCRIBE_OK + eventId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok().body(eventResponse);
    }
}
