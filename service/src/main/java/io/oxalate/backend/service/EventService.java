package io.oxalate.backend.service;

import io.oxalate.backend.api.EmailNotificationDetailEnum;
import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.api.ParticipantTypeEnum;
import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.EMAIL;
import static io.oxalate.backend.api.PortalConfigEnum.EmailConfigEnum.EMAIL_NOTIFICATIONS;
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.api.response.EventDiveListResponse;
import io.oxalate.backend.api.response.EventListResponse;
import io.oxalate.backend.api.response.EventResponse;
import io.oxalate.backend.api.response.EventUserResponse;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.EventsParticipant;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventService {
    private final EventRepository eventRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final UserService userService;
    private final PaymentService paymentService;
    private final EmailQueueService emailQueueService;
    private final PortalConfigurationService portalConfigurationService;

    public EventResponse findById(Long eventId) {
        var event = eventRepository.findById(eventId)
                                   .orElse(null);

        if (event == null) {
            log.warn("Event {} not found", eventId);
            return null;
        }

        var optionalEventResponse = getPopulatedEventResponse(event);
        return optionalEventResponse.orElse(null);
    }

    @Transactional
    public EventResponse updateEvent(EventRequest eventRequest) {
        var event = eventRepository.findById(eventRequest.getId())
                                   .orElse(null);

        if (event == null) {
            log.error("Event {} not found for updating", eventRequest.getId());
            return null;
        }

        // We need to recreate the list of participants as there may have been changes to the list, this means that we first need to store the current list
        // as it contains the create time of the existing registrations
        var currentParticipants = eventParticipantsRepository.findAllByEventId(eventRequest.getId());

        // Go through the new list, and if it contains a participant that is in the current list, then we skip over it
        for (Long participantId : eventRequest.getParticipants()) {
            var found = false;
            for (EventsParticipant currentParticipant : currentParticipants) {
                if (currentParticipant.getUserId() == participantId) {
                    found = true;
                    break;
                }
            }

            if (found) {
                continue;
            }

            eventRepository.addParticipantToEvent(participantId, eventRequest.getId(), ParticipantTypeEnum.USER.name(),
                    paymentService.getBestAvailablePaymentType(participantId)
                                  .name(), Instant.now());
        }

        // Next we go through the current list, and if a participant is not in the new list, then we remove it
        for (EventsParticipant currentParticipant : currentParticipants) {
            var found = false;
            for (Long participantId : eventRequest.getParticipants()) {
                if (currentParticipant.getUserId() == participantId) {
                    found = true;
                    break;
                }
            }

            if (found) {
                continue;
            }

            eventRepository.removeParticipantFromEvent(currentParticipant.getUserId(), eventRequest.getId());
        }

        // Finally we remove the organizer from the list, as we will add it back later
        eventRepository.removeAllParticipantsFromEvent(eventRequest.getId(), ParticipantTypeEnum.ORGANIZER.name());

        // We need to store the old status to determine if we need to send a notification
        var oldStatus = event.getStatus();
        // Finally add the event organizer, whether it is the current or a new one
        eventRepository.addParticipantToEvent(eventRequest.getOrganizerId(), eventRequest.getId(), ParticipantTypeEnum.ORGANIZER.name(),
                PaymentTypeEnum.NONE.name(), Instant.now());

        event.setType(eventRequest.getType());
        event.setTitle(eventRequest.getTitle());
        event.setStartTime(eventRequest.getStartTime());
        event.setEventDuration(eventRequest.getEventDuration());
        event.setMaxDuration(eventRequest.getMaxDuration());
        event.setMaxDepth(eventRequest.getMaxDepth());
        event.setMaxParticipants(eventRequest.getMaxParticipants());
        event.setDescription(eventRequest.getDescription());
        event.setOrganizerId(eventRequest.getOrganizerId());
        event.setStatus(eventRequest.getStatus());
        var updatedEvent = eventRepository.save(event);
        var newStatus = updatedEvent.getStatus();

        // DRAFTED -> PUBLISHED = Send notification for new event
        // PUBLISHED -> PUBLISHED = Send notification for updated event
        // PUBLISHED -> DRAFTED = Send notification for cancelled event
        // PUBLISHED -> CANCELLED = Send notification for cancelled event
        if (oldStatus.equals(EventStatusEnum.DRAFTED)
                && newStatus.equals(EventStatusEnum.PUBLISHED)
                && portalConfigurationService.isEnabled(EMAIL, EMAIL_NOTIFICATIONS.key, "event-new")) {
            emailQueueService.addNotification(EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.NEW, updatedEvent.getId());
        } else if (oldStatus.equals(EventStatusEnum.PUBLISHED)
                && newStatus.equals(EventStatusEnum.PUBLISHED)
                && portalConfigurationService.isEnabled(EMAIL, EMAIL_NOTIFICATIONS.key, "event-updated")) {
            emailQueueService.addNotification(EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.UPDATED, updatedEvent.getId());
        } else if ((oldStatus.equals(EventStatusEnum.PUBLISHED) && newStatus.equals(EventStatusEnum.CANCELLED)
                || oldStatus.equals(EventStatusEnum.PUBLISHED) && newStatus.equals(EventStatusEnum.DRAFTED))
                && portalConfigurationService.isEnabled(EMAIL, EMAIL_NOTIFICATIONS.key, "event-removed")) {
            emailQueueService.addNotification(EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.DELETED, updatedEvent.getId());
        }

        return getPopulatedEventResponse(updatedEvent).orElse(null);
    }

    @Transactional
    public EventResponse addUserToEvent(User user, long eventId) {
        var eventResponse = findById(eventId);

        if (eventResponse == null) {
            log.warn("Can not add user to non-existing event: {}", eventId);
            return null;
        }

        if (eventResponse.getParticipants()
                         .size() >= eventResponse.getMaxParticipants()) {
            log.warn("Event {} is full", eventResponse.getTitle());
            return null;
        }

        if (isUserInList(user.getId(), eventResponse.getParticipants())) {
            log.warn("User {} already in event {}", user.getId(), eventId);
            return null;
        }

        eventRepository.addParticipantToEvent(user.getId(), eventId, ParticipantTypeEnum.USER.name(), paymentService.getBestAvailablePaymentType(user.getId())
                                                                                                                    .name(), Instant.now());

        return getRefreshedEventResponse(eventId).orElse(null);
    }

    private boolean isUserInList(long userId, List<EventUserResponse> eventUserResponseList) {
        for (EventUserResponse eventUserResponse : eventUserResponseList) {
            if (eventUserResponse.getId() == userId) {
                return true;
            }
        }

        return false;
    }

    @Transactional
    public void updateUserDiveCount(long eventId, long userId, long diveCount) {
        eventRepository.updateEventUserDiveCount(eventId, userId, diveCount);
    }

    @Transactional
    public EventResponse removeUserFromEvent(User user, long eventId) {
        var eventResponse = findById(eventId);

        if (eventResponse == null) {
            log.warn("Can not remove user from non-existing event: {}", eventId);
            return null;
        }

        // Get the list of participant user IDs
        var participantIds = new HashSet<Long>();
        for (EventUserResponse eventUserResponse : eventResponse.getParticipants()) {
            participantIds.add(eventUserResponse.getId());
        }

        if (!participantIds.contains(user.getId())) {
            log.warn("User {} has not been subscribed to event {}", user.getId(), eventId);
            return null;
        }

        eventRepository.removeParticipantFromEvent(user.getId(), eventId);
        log.info("Removed user {} from event {}", user.getId(), eventId);

        return getRefreshedEventResponse(eventId).orElse(null);
    }

    /**
     * Returns all events that have status PUBLISHED and have a start time after the given timestamp
     *
     * @param instant Instant The instant after which the events should have started
     * @param allEvents boolean Whether to return all events or only published ones
     * @return List<EventResponse>
     */
    public List<EventResponse> findAllEventsAfter(Instant instant, boolean allEvents) {
        List<Event> events;

        if (allEvents) {
            events = eventRepository.findByStartTimeAfterOrderByStartTimeAsc(instant);
        } else {
            events = eventRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(EventStatusEnum.PUBLISHED, instant);
        }

        var eventResponses = new ArrayList<EventResponse>();

        for (Event event : events) {
            // The allEvents flag is only set to true when the requester is either ORGANIZER or ADMIN for whom we can show private fields
            var optionalEventResponse = getPopulatedEventResponse(event);
            optionalEventResponse.ifPresent(eventResponses::add);
        }

        return eventResponses;
    }

    public List<EventResponse> findAllEventsBefore(Instant until) {
        var events = eventRepository.findAllEventsBefore(until);
        var eventList = new ArrayList<EventResponse>();

        for (Event event : events) {
            var eventResponse = getPopulatedEventResponse(event);

            if (eventResponse.isPresent()) {
                eventList.add(eventResponse.get());
            } else {
                log.error("Event {} can not be populated to a EventResponse, the event may be in an incoherent state", event);
            }
        }

        return eventList;
    }

    public List<EventResponse> findAllCurrentEvents() {
        var events = eventRepository.findAllCurrentEvents();
        var eventList = new ArrayList<EventResponse>();

        for (Event event : events) {
            var eventResponse = getPopulatedEventResponse(event);

            if (eventResponse.isPresent()) {
                eventList.add(eventResponse.get());
            } else {
                log.error("Event {} can not be populated to a EventResponse, the event may be in an incoherent state", event);
            }
        }

        return eventList;
    }

    @Transactional
    public void cancel(long eventId) {
        // Get the dive event data
        var event = eventRepository.findById(eventId)
                                   .orElse(null);

        if (event == null) {
            log.error("Event ID {} not found for cancelling", eventId);
            throw new IllegalArgumentException("Event not found");
        }

        if (event.getStatus()
                 .equals(EventStatusEnum.CANCELLED)) {
            log.error("Event ID {} is already cancelled", eventId);
            throw new IllegalArgumentException("Event already cancelled");
        }

        if (event.getStatus()
                 .equals(EventStatusEnum.PUBLISHED)) {
            emailQueueService.addNotification(EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.DELETED, eventId);
        }

        eventRepository.updateEventStatus(eventId, EventStatusEnum.CANCELLED);
    }

    private Optional<EventResponse> getRefreshedEventResponse(long eventId) {
        var event = eventRepository.findById(eventId)
                                   .orElse(null);

        if (event == null) {
            log.error("Something went wrong when updating event participant list, the event can not be found anymore: {}", eventId);
            return Optional.empty();
        }

        var optionalEventResponse = getPopulatedEventResponse(event);

        if (optionalEventResponse.isEmpty()) {
            log.error("Something went wrong when fetching event response, it can not be populated anymore: {}", eventId);
            return Optional.empty();
        }

        return optionalEventResponse;
    }

    private Optional<EventResponse> getPopulatedEventResponse(Event event) {
        var organizer = userService.findUserById(event.getOrganizerId());

        if (organizer.isEmpty()) {
            log.error("Event has an non-existing organizer: {}", event.getOrganizerId());
            return Optional.empty();
        }

        var participants = userService.findEventParticipants(event.getId());
        var participantList = new ArrayList<EventUserResponse>();

        for (User participant : participants) {
            var eventUserResponse = participant.toEventUserResponse();
            var eventParticipant = eventParticipantsRepository.findByEventIdAndUserId(event.getId(), participant.getId());
            eventUserResponse.setCreatedAt(eventParticipant.getCreatedAt());
            eventUserResponse.setEventDiveCount(countDivesByUserAndEvent(participant.getId(), event.getId()));
            participantList.add(eventUserResponse);
        }

        var eventResponse = event.toEventResponse();
        eventResponse.setOrganizer(organizer.get()
                                            .toUserResponse());
        eventResponse.setParticipants(participantList);

        return Optional.of(eventResponse);
    }

    private long countDivesByUserAndEvent(Long userId, long eventId) {
        return eventParticipantsRepository.countDivesByUserIdAndEventId(userId, eventId);
    }

    private Optional<EventListResponse> getPopulatedEventListResponse(Event event) {
        var optionalOrganizer = userService.findUserById(event.getOrganizerId());

        if (optionalOrganizer.isEmpty()) {
            log.error("Event has an non-existing organizer: {}", event.getOrganizerId());
            return Optional.empty();
        }
        var organizer = optionalOrganizer.get();
        var participants = userService.findEventParticipants(event.getId());

        var eventListResponse = event.toEventListResponse();
        eventListResponse.setOrganizerName(organizer.getFirstName() + " " + organizer.getLastName());
        eventListResponse.setParticipantCount(participants.size());

        return Optional.of(eventListResponse);
    }

    @Transactional
    public EventResponse createEvent(EventRequest eventRequest, Long userId) {
        if (!verifyEventRequest(eventRequest)) {
            return null;
        }

        var event = Event.builder()
                         .title(eventRequest.getTitle())
                         .description(eventRequest.getDescription())
                         .startTime(eventRequest.getStartTime())
                         .eventDuration(eventRequest.getEventDuration())
                         .maxDuration(eventRequest.getMaxDuration())
                         .maxDepth(eventRequest.getMaxDepth())
                         .maxParticipants(eventRequest.getMaxParticipants())
                         .status(eventRequest.getStatus())
                         .organizerId(userId)
                         .type(eventRequest.getType())
                         .build();

        var newEvent = eventRepository.save(event);

        // Add organizer as event participant with ORGANIZER type
        eventRepository.addParticipantToEvent(userId, newEvent.getId(), ParticipantTypeEnum.ORGANIZER.name(), PaymentTypeEnum.NONE.name(), Instant.now());

        // Add participants
        if (eventRequest.getParticipants() != null) {
            for (Long participantId : eventRequest.getParticipants()) {
                eventRepository.addParticipantToEvent(participantId, newEvent.getId(), ParticipantTypeEnum.USER.name(),
                        paymentService.getBestAvailablePaymentType(participantId)
                                      .name(), Instant.now());
            }
        }

        // Now we can get the repopulated response
        var optionalEventResponse = getPopulatedEventResponse(newEvent);

        if (optionalEventResponse.isEmpty()) {
            log.error("Failed to populate the event ID {} response", newEvent.getId());
            return null;
        }

        var eventResponse = optionalEventResponse.get();

        log.debug("Created new event: title '{}', ID '{}'", eventResponse.getTitle(), eventResponse.getId());

        // Send notification only if the event is in a published state
        if (newEvent.getStatus()
                    .equals(EventStatusEnum.PUBLISHED)) {
            emailQueueService.addNotification(EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.NEW, eventResponse.getId());
        }

        return eventResponse;
    }

    public List<Event> findEventsForUserAsParticipant(Long userId) {
        return eventRepository.findByUserId(userId);
    }

    public List<Event> findEventsForUserAsOrganizer(Long userId) {
        return eventRepository.findByOrganizer(userId);
    }

    public List<EventListResponse> findEventsForUser(long userId) {
        var events = findEventsForUserAsOrganizer(userId);
        events.addAll(findEventsForUserAsParticipant(userId));

        var eventSet = new HashSet<EventListResponse>();

        for (Event event : events) {
            var eventListResponse = getPopulatedEventListResponse(event);

            if (eventListResponse.isPresent()) {
                eventSet.add(eventListResponse.get());
            } else {
                log.error("Event {} can not be populated to a EventListResponse, the event may be in an incoherent state", event);
            }
        }

        return eventSet.stream()
                       .sorted(Comparator.comparing(EventListResponse::getStartTime))
                       .toList();
    }

    private boolean verifyEventRequest(EventRequest eventRequest) {
        var result = true;

        if (eventRequest.getTitle() == null || eventRequest.getTitle()
                                                           .isEmpty()) {
            log.error("Event verification: Title can not be empty");
            result = false;
        }

        if (eventRequest.getDescription() == null || eventRequest.getDescription()
                                                                 .isEmpty()) {
            log.error("Event verification: Description can not be empty");
            result = false;
        }

        if (eventRequest.getStartTime() == null) {
            log.error("Event verification: Start time can not be empty");
            result = false;
        }

        if (eventRequest.getEventDuration() < 0) {
            log.error("Event verification: Event duration can not be empty");
            result = false;
        }

        // The event can not be longer than 1 day
        if (eventRequest.getEventDuration() > 24) {
            log.error("Event verification: Max duration can not be empty");
            result = false;
        }

        // Maximum dive duration can not be longer than 4 hours
        if (eventRequest.getMaxDuration() > 4 * 60) {
            log.error("Event verification: Max dive duration can not be more than 4 hours (240min)");
            result = false;
        }

        // The max depth can not be beyond 180 meters
        if (eventRequest.getMaxDepth() > 180) {
            log.error("Event verification: Max depth can not be empty");
            result = false;
        }

        if (eventRequest.getType() == null) {
            log.error("Event verification: Type can not be empty");
            result = false;
        }

        return result;
    }

    /**
     * This actually removes the member participation from all future events. The method is nonetheless called "anonymize" because it is used in anonymization
     * process.
     *
     * @param userId The user ID to anonymize
     */
    @Transactional
    public void anonymize(long userId) {
        var events = eventRepository.findFutureEventsByUserId(userId);

        if (events.isEmpty()) {
            log.info("No events to anonymize for user {}", userId);
            return;
        }

        for (Event event : events) {
            eventRepository.removeParticipantFromEvent(userId, event.getId());
        }
    }

    /**
     * DO NOT USE THIS method! This save-method should _only_ be used by the test controller and is used as an alternative to createEvent.
     */
    @Transactional
    public Event save(Event event) {
        var newEvent = eventRepository.save(event);
        eventRepository.addParticipantToEvent(newEvent.getOrganizerId(), newEvent.getId(), ParticipantTypeEnum.ORGANIZER.name(), PaymentTypeEnum.NONE.name(), Instant.now());
        return newEvent;
    }

    public EventDiveListResponse getEventDives(long eventId) {
        var eventDives = eventParticipantsRepository.findEventDives(eventId);

        var eventDiveListResponse = new EventDiveListResponse();
        eventDiveListResponse.setDives(new HashSet<>());

        for (EventsParticipant eventsParticipant : eventDives) {
            var optionalUser = userService.findUserById(eventsParticipant.getUserId());

            if (optionalUser.isEmpty()) {
                log.error("User {} does not exist", eventsParticipant.getUserId());
                return null;
            }

            var eventDiveResponse = eventsParticipant.toEventDiveResponse(optionalUser.get()
                                                                                      .getLastName() + " " + optionalUser.get()
                                                                                                                         .getFirstName());
            eventDiveListResponse.getDives()
                                 .add(eventDiveResponse);
        }

        return eventDiveListResponse;
    }
}
