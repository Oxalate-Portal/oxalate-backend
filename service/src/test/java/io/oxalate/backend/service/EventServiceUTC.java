package io.oxalate.backend.service;

import io.oxalate.backend.api.DiveTypeEnum;
import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.commenting.EventCommentRepository;
import io.oxalate.backend.service.commenting.CommentService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class EventServiceUTC {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventParticipantsRepository eventParticipantsRepository;
    @Mock
    private UserService userService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private MembershipService membershipService;
    @Mock
    private EmailService emailService;
    @Mock
    private EmailQueueService emailQueueService;
    @Mock
    private PortalConfigurationService portalConfigurationService;
    @Mock
    private CommentService commentService;
    @Mock
    private EventCommentRepository eventCommentRepository;
    @Mock
    private MessageService messageService;
    @Mock
    private DiveGroupService diveGroupService;
    @Mock
    private DiveGroupRepository diveGroupRepository;
    @Mock
    private NotificationLocalizationService notificationLocalizationService;

    @InjectMocks
    private EventService eventService;

    @Test
    void findPastEventsPagedKeepsEventWithMissingOrganizerOk() {
        var withOrganizer = pastEvent(1L, 10L);
        var orphan = pastEvent(2L, 99L);
        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withOrganizer, orphan)));
        when(userService.findUserEntityById(10L)).thenReturn(User.builder()
                                                                .id(10L)
                                                                .firstName("Olga")
                                                                .lastName("Organizer")
                                                                .build());
        when(userService.findUserEntityById(99L)).thenReturn(null);

        var page = eventService.findPastEventsPaged(PagedRequest.builder()
                                                                .sortBy("title")
                                                                .direction(Sort.Direction.ASC)
                                                                .build(), Instant.now());

        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent()
                            .size());
        assertNotNull(page.getContent()
                          .getFirst()
                          .getOrganizer());
        assertNull(page.getContent()
                       .get(1)
                       .getOrganizer());
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(ArgumentMatchers.<Specification<Event>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("title");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void findPastEventsPagedUnknownSortFallsBackToStartTimeDescOk() {
        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        eventService.findPastEventsPaged(PagedRequest.builder()
                                                     .sortBy("organizer_id")
                                                     .build(), Instant.now());

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(ArgumentMatchers.<Specification<Event>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("startTime");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    private static Event pastEvent(long id, long organizerId) {
        return Event.builder()
                    .id(id)
                    .title("Past event " + id)
                    .description("A description that is long enough for validation")
                    .type(DiveTypeEnum.OPEN_WATER)
                    .startTime(Instant.now()
                                      .minus(id, ChronoUnit.DAYS))
                    .eventDuration(4)
                    .maxDuration(60)
                    .maxDepth(30)
                    .maxParticipants(8)
                    .organizerId(organizerId)
                    .status(EventStatusEnum.HELD)
                    .build();
    }

    @Test
    void createEventRejectsInvalidRequestsBeforePersistence() {
        var request = EventRequest.builder()
                                  .title("")
                                  .description("")
                                  .eventDuration(-1)
                                  .maxDuration(241)
                                  .maxDepth(181)
                                  .build();

        assertNull(eventService.createEvent(request, 4L));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEventRejectsMissingTypeAndStartTime() {
        var request = EventRequest.builder()
                                  .title("Event")
                                  .description("Description")
                                  .eventDuration(25)
                                  .maxDuration(0)
                                  .maxDepth(0)
                                  .build();

        assertNull(eventService.createEvent(request, 4L));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void cancelMissingEventFails() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> eventService.cancel(99L));
        verify(eventRepository, never()).updateEventStatus(any(Long.class), any(EventStatusEnum.class));
    }

    @Test
    void cancelAlreadyCancelledEventFails() {
        when(eventRepository.findById(99L)).thenReturn(Optional.of(Event.builder()
                                                                        .id(99L)
                                                                        .status(EventStatusEnum.CANCELLED)
                                                                        .build()));

        assertThrows(IllegalArgumentException.class, () -> eventService.cancel(99L));
        verify(eventRepository, never()).updateEventStatus(any(Long.class), any(EventStatusEnum.class));
    }

    @Test
    void cancelPublishedEventQueuesNotificationAndUpdatesStatus() {
        when(eventRepository.findById(99L)).thenReturn(Optional.of(Event.builder()
                                                                        .id(99L)
                                                                        .status(EventStatusEnum.PUBLISHED)
                                                                        .build()));

        eventService.cancel(99L);

        verify(emailQueueService).addNotification(any(), any(), org.mockito.ArgumentMatchers.eq(99L));
        verify(eventRepository).updateEventStatus(99L, EventStatusEnum.CANCELLED);
    }

    @Test
    void cancelDraftEventUpdatesStatusWithoutNotification() {
        when(eventRepository.findById(99L)).thenReturn(Optional.of(Event.builder()
                                                                        .id(99L)
                                                                        .status(EventStatusEnum.DRAFTED)
                                                                        .build()));

        eventService.cancel(99L);

        verify(emailQueueService, never()).addNotification(any(), any(), any(Long.class));
        verify(eventRepository).updateEventStatus(99L, EventStatusEnum.CANCELLED);
    }
}
