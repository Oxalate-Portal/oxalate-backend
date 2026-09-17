package io.oxalate.backend.service;

import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.commenting.EventCommentRepository;
import io.oxalate.backend.service.commenting.CommentService;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

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
