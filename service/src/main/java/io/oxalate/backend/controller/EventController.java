package io.oxalate.backend.controller;

import io.oxalate.backend.api.AuditLevelEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.EventDiveListRequest;
import io.oxalate.backend.api.request.EventDiveRequest;
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.api.request.EventSubscribeRequest;
import io.oxalate.backend.api.response.EventDiveListResponse;
import io.oxalate.backend.api.response.EventListResponse;
import io.oxalate.backend.api.response.EventResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
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
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.rest.EventAPI;
import io.oxalate.backend.service.EventService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
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
@AuditSource("EventController")
public class EventController implements EventAPI {

    private final EventService eventService;
    private final UserService userService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_GET_FUTURE_START, okMessage = EVENTS_GET_FUTURE_OK, failMessage = EVENTS_GET_FUTURE_FAIL)
    public ResponseEntity<List<EventResponse>> getFutureEvents() {
        if (!AuthTools.currentUserHasAcceptedTerms()) {
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            throw new OxalateValidationException(AuditLevelEnum.WARN, EVENTS_GET_FUTURE_TERMS_NOT_ACCEPTED, HttpStatus.NO_CONTENT);
        }

        var events = eventService.findAllEventsAfter(Instant.now(), AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN));

        if (events.isEmpty()) {
            log.debug("No future events found");
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(events);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_GET_CURRENT_START, okMessage = EVENTS_GET_CURRENT_OK)
    public ResponseEntity<List<EventResponse>> getOngoingEvents() {
        if (!AuthTools.currentUserHasAcceptedTerms()) {
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            throw new OxalateValidationException(AuditLevelEnum.WARN, EVENTS_GET_CURRENT_TERMS_NOT_ACCEPTED, HttpStatus.NO_CONTENT);
        }

        var eventResponses = eventService.findAllCurrentEvents();

        if (eventResponses.isEmpty()) {
            log.debug("No current events found");
        }

        return ResponseEntity.status(HttpStatus.OK).body(eventResponses);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_GET_PAST_START, okMessage = EVENTS_GET_PAST_OK)
    public ResponseEntity<List<EventResponse>> getPastEvents() {
        if (!AuthTools.currentUserHasAcceptedTerms()) {
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            throw new OxalateValidationException(AuditLevelEnum.WARN, EVENTS_GET_PAST_TERMS_NOT_ACCEPTED, HttpStatus.NO_CONTENT);
        }

        var eventResponses = eventService.findAllEventsBefore(Instant.now());
        return ResponseEntity.ok().body(eventResponses);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_GET_USER_START, okMessage = EVENTS_GET_USER_OK)
    public ResponseEntity<List<EventListResponse>> getEventsForUser(long userId) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) && !AuthTools.isUserIdCurrentUser(userId)) {
            log.error("User {} is not allowed to see user {}'s event list", AuthTools.getCurrentUserId(), userId);
            throw new OxalateUnauthorizedException(EVENTS_GET_USER_UNAUTHORIZED + userId, HttpStatus.BAD_REQUEST);
        }

        var events = eventService.findEventsForUser(userId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(events);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_GET_SINGLE_START, okMessage = EVENTS_GET_SINGLE_OK)
    public ResponseEntity<EventResponse> getEventById(long eventId) {
        var eventResponse = eventService.findById(eventId);

        if (eventResponse == null) {
            log.warn("Event ID {} not found", eventId);
            throw new OxalateNotFoundException(EVENTS_GET_SINGLE_NOT_FOUND + eventId);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(eventResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_CREATE_START, okMessage = EVENTS_CREATE_OK)
    public ResponseEntity<EventResponse> createEvent(EventRequest eventRequest) {
        var userId = eventRequest.getOrganizerId();

        if (userId < 1L) {
            log.error("Failed to create event from request, invalid organizer: {}", eventRequest);
            throw new OxalateValidationException(EVENTS_CREATE_INVALID_ORGANIZER + userId, HttpStatus.BAD_REQUEST);
        }

        if (eventRequest.getStartTime().isBefore(Instant.now())) {
            log.warn("Can not create an event {} with start time is set before present", eventRequest.getId());
            throw new OxalateValidationException(EVENTS_CREATE_INVALID_DATETIME + eventRequest.getStartTime(), HttpStatus.BAD_REQUEST);
        }

        if (eventRequest.getParticipants() != null && eventRequest.getMaxParticipants() < eventRequest.getParticipants().size()) {
            log.error("Event has more current participants than maximum number of participants: {}", eventRequest);
            throw new OxalateValidationException(EVENTS_CREATE_INVALID_PARTICIPANTS_COUNT + eventRequest.getMaxParticipants(), HttpStatus.BAD_REQUEST);
        }

        var eventResponse = eventService.createEvent(eventRequest, userId);

        if (eventResponse == null) {
            log.error("Failed to create event from request: {}", eventRequest);
            throw new OxalateValidationException(EVENTS_CREATE_FAIL, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(eventResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_UPDATE_START, okMessage = EVENTS_UPDATE_OK)
    public ResponseEntity<EventResponse> updateEvent(EventRequest eventRequest) {
        var checkEvent = eventService.findById(eventRequest.getId());

        if (checkEvent == null) {
            log.error("Attempted to update non-existing event: {}", eventRequest);
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, EVENTS_UPDATE_NOT_FOUND + eventRequest.getId(), HttpStatus.NOT_FOUND);
        }

        var organizer = userService.findUserEntityById(eventRequest.getOrganizerId());

        if (organizer == null) {
            log.error("Event has an non-existing organizer: {}", eventRequest.getOrganizerId());
            throw new OxalateValidationException(EVENTS_UPDATE_ORGANIZER_NOT_FOUND + eventRequest.getOrganizerId(), HttpStatus.BAD_REQUEST);
        }

        if (eventRequest.getMaxParticipants() < eventRequest.getParticipants().size()) {
            log.error("Event has more current participants than maximum number of participants: {}", eventRequest);
            throw new OxalateValidationException(EVENTS_UPDATE_INVALID_PARTICIPANTS_COUNT + eventRequest.getMaxParticipants(), HttpStatus.BAD_REQUEST);
        }

        var calculatedEndTime = eventRequest.getStartTime().plus(eventRequest.getEventDuration(), ChronoUnit.HOURS);
        if (Instant.now().isAfter(calculatedEndTime)) {
            log.warn("Can't update an event {} which calculated end time {} has passed", eventRequest.getId(), calculatedEndTime);
            throw new OxalateValidationException(AuditLevelEnum.WARN, EVENTS_UPDATE_INVALID_DATETIME + calculatedEndTime, HttpStatus.BAD_REQUEST);
        }

        var eventResponse = eventService.updateEvent(eventRequest);
        return ResponseEntity.ok().body(eventResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_GET_DIVES_START, okMessage = EVENTS_GET_DIVES_OK)
    public ResponseEntity<EventDiveListResponse> getEventDives(long eventId) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN)) {
            log.error("User {} is not allowed to retrieve event ID {} dive count", AuthTools.getCurrentUserId(), eventId);
            throw new OxalateUnauthorizedException(EVENTS_GET_DIVES_UNAUTHORIZED + eventId, HttpStatus.BAD_REQUEST);
        }

        var eventResponse = eventService.findById(eventId);

        if (eventResponse == null) {
            log.error("Event {} not found for dive retrieval", eventId);
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, EVENTS_GET_DIVES_NOT_FOUND + eventId, HttpStatus.BAD_REQUEST);
        }

        if (eventResponse.getParticipants()
                         .isEmpty()) {
            log.error("Event {} does not have any participants", eventId);
            throw new OxalateValidationException(EVENTS_GET_DIVES_NO_PARTICIPANTS + eventId, HttpStatus.BAD_REQUEST);
        }

        var eventDiveListResponse = eventService.getEventDives(eventId);

        if (eventDiveListResponse == null || eventDiveListResponse.getDives().isEmpty()) {
            log.error("Event {} does not have any dives", eventId);
            throw new OxalateValidationException(EVENTS_GET_DIVES_NO_DIVES + eventId, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK).body(eventDiveListResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_UPDATE_DIVES_START, okMessage = EVENTS_UPDATE_DIVES_OK)
    public ResponseEntity<EventDiveListResponse> updateEventDives(long eventId, EventDiveListRequest eventDiveListRequest) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN)) {
            log.error("User {} is not allowed to update event ID {} dive count", AuthTools.getCurrentUserId(), eventId);
            throw new OxalateUnauthorizedException(EVENTS_UPDATE_DIVES_UNAUTHORIZED + eventId, HttpStatus.BAD_REQUEST);
        }

        if (eventDiveListRequest.getDives()
                                .isEmpty()) {
            log.error("Event dive list in request is empty for dive ID: {}", eventId);
            throw new OxalateValidationException(EVENTS_UPDATE_DIVES_NO_DIVES + eventId, HttpStatus.BAD_REQUEST);
        }

        var event = eventService.findById(eventId);

        if (event == null) {
            log.error("Event {} not found for dive updating", eventId);
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, EVENTS_UPDATE_DIVES_NOT_FOUND + eventId, HttpStatus.BAD_REQUEST);
        }

        if (event.getParticipants().isEmpty()) {
            log.error("Event {} does not have any participants", eventId);
            throw new OxalateValidationException(EVENTS_UPDATE_DIVES_NO_PARTICIPANTS + eventId, HttpStatus.BAD_REQUEST);
        }

        for (EventDiveRequest eventDiveRequest : eventDiveListRequest.getDives()) {
            eventService.updateUserDiveCount(eventId, eventDiveRequest.getUserId(), eventDiveRequest.getDiveCount());
        }

        var eventDiveListResponse = eventService.getEventDives(eventId);

        if (eventDiveListResponse == null) {
            log.error("Something went wrong while updating diver counts for event ID: {}", eventId);
            throw new OxalateValidationException(EVENTS_UPDATE_DIVES_FAIL + eventId, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK).body(eventDiveListResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EVENTS_CANCEL_START, okMessage = EVENTS_CANCEL_OK, failMessage = EVENTS_CANCEL_FAIL)
    public ResponseEntity<HttpStatus> cancelEvent(long eventId) {
        eventService.cancel(eventId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .body(null);
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    @Audited(startMessage = EVENTS_SUBSCRIBE_START, okMessage = EVENTS_SUBSCRIBE_OK)
    public ResponseEntity<EventResponse> subscribe(Authentication auth, EventSubscribeRequest eventSubscribeRequest) {
        var eventId = eventSubscribeRequest.getDiveEventId();

        if (!AuthTools.currentUserHasAcceptedTerms()) {
            log.error("User ID {} has not accepted terms and conditions", AuthTools.getCurrentUserId());
            throw new OxalateValidationException(AuditLevelEnum.INFO, EVENTS_SUBSCRIBE_TERMS_NOT_ACCEPTED, HttpStatus.NO_CONTENT);
        }

        var optionalUser = userService.findByUsername(auth.getName());

        if (optionalUser.isEmpty()) {
            log.error("Could not add non-existing user to event: {}", auth.getName());
            throw new OxalateNotFoundException(AuditLevelEnum.WARN, EVENTS_SUBSCRIBE_UNKNOWN_USER + auth.getName(), HttpStatus.NOT_FOUND);
        }

        var eventResponse = eventService.findById(eventId);

        if (eventResponse == null) {
            log.error("Could not add user to non-existing event: {}", eventId);
            throw new OxalateNotFoundException(AuditLevelEnum.WARN, EVENTS_SUBSCRIBE_NOT_FOUND + eventId, HttpStatus.NOT_FOUND);
        }

        log.info("Adding user ID {} to event {}", optionalUser.get().getId(), eventId);

        var newEventResponse = eventService.addUserToEvent(optionalUser.get(), eventSubscribeRequest);

        if (newEventResponse == null) {
            log.error("Can not subscribe user ID {} to event: {}", optionalUser.get()
                                                                               .getId(), eventId);
            throw new OxalateValidationException(AuditLevelEnum.WARN, EVENTS_SUBSCRIBE_FAIL + eventId, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok().body(newEventResponse);
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    @Audited(startMessage = EVENTS_UNSUBSCRIBE_START, okMessage = EVENTS_UNSUBSCRIBE_OK)
    public ResponseEntity<EventResponse> unSubscribe(Authentication auth, long eventId) {
        var optionalUser = userService.findByUsername(auth.getName());

        if (optionalUser.isEmpty()) {
            log.warn("Unable to find user {} when unsubscribing from event {}", auth.getName(), eventId);
            throw new OxalateNotFoundException(AuditLevelEnum.WARN, EVENTS_UNSUBSCRIBE_UNKNOWN_USER + auth.getName(), HttpStatus.NOT_FOUND);
        }

        log.info("Removing user ID {} from event {}", optionalUser.get()
                                                                  .getId(), eventId);

        var eventResponse = eventService.removeUserFromEvent(optionalUser.get(), eventId);

        if (eventResponse == null) {
            log.warn("Can not unsubscribe user {} from event {}", optionalUser.get()
                                                                              .getId(), eventId);
            throw new OxalateValidationException(AuditLevelEnum.WARN, EVENTS_UNSUBSCRIBE_FAIL + eventId, HttpStatus.LOCKED);
        }

        return ResponseEntity.ok().body(eventResponse);
    }
}

