package io.oxalate.backend.service;

import io.oxalate.backend.api.ParticipantTypeEnum;
import io.oxalate.backend.api.PaymentTypeEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.DiveGroupRequest;
import io.oxalate.backend.api.request.DiveGroupUpdateRequest;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.model.DiveGroup;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.EventsParticipant;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
// LENIENT is needed because several tests stub the same repository lookups with different specific
// arguments within the same test method, which Mockito strict mode flags as PotentialStubbingProblem.
@MockitoSettings(strictness = Strictness.LENIENT)
class DiveGroupServiceUTC {

    private static final long EVENT_ID = 42L;
    private static final long GROUP_ID = 7L;
    private static final long ORGANIZER_ID = 100L;
    private static final long OWNER_ID = 200L;
    private static final long MEMBER_ID = 300L;
    private static final long OUTSIDER_ID = 400L;

    @Mock
    private DiveGroupRepository diveGroupRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventParticipantsRepository eventParticipantsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageService messageService;

    @InjectMocks
    private DiveGroupService diveGroupService;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Event futureEvent() {
        return event(Instant.now()
                            .plus(2, ChronoUnit.DAYS));
    }

    private Event event(Instant startTime) {
        return Event.builder()
                    .id(EVENT_ID)
                    .title("Test event")
                    .startTime(startTime)
                    .eventDuration(4)
                    .organizerId(ORGANIZER_ID)
                    .build();
    }

    private DiveGroup diveGroup(long ownerId) {
        return DiveGroup.builder()
                        .id(GROUP_ID)
                        .eventId(EVENT_ID)
                        .name("Team Sidemount")
                        .ownerId(ownerId)
                        .createdAt(Instant.now()
                                          .minus(1, ChronoUnit.HOURS))
                        .build();
    }

    private EventsParticipant participant(long userId, Long diveGroupId, Instant joinedAt) {
        return EventsParticipant.builder()
                                .userId(userId)
                                .eventId(EVENT_ID)
                                .diveCount(0)
                                .participantType(ParticipantTypeEnum.USER)
                                .paymentType(PaymentTypeEnum.ONE_TIME)
                                .createdAt(Instant.now()
                                                  .minus(2, ChronoUnit.DAYS))
                                .eventUserType(UserTypeEnum.SCUBA_DIVER)
                                .diveGroupId(diveGroupId)
                                .diveGroupJoinedAt(joinedAt)
                                .build();
    }

    private User user(long id) {
        return User.builder()
                   .id(id)
                   .username("user" + id + "@test.tld")
                   .firstName("First" + id)
                   .lastName("Last" + id)
                   .build();
    }

    private void stubUsers(long... ids) {
        for (var id : ids) {
            when(userRepository.findById(id)).thenReturn(Optional.of(user(id)));
        }
        when(userRepository.findAllById(any())).thenReturn(List.of());
    }

    // ------------------------------------------------------------------
    // getDiveGroupsByEventId
    // ------------------------------------------------------------------

    @Test
    void getDiveGroupsByEventIdOk() {
        when(eventRepository.existsById(EVENT_ID)).thenReturn(true);
        when(diveGroupRepository.findAllByEventIdOrderByCreatedAtAsc(EVENT_ID)).thenReturn(List.of(diveGroup(OWNER_ID)));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(OWNER_ID, GROUP_ID, Instant.now())));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(OWNER_ID)));

        var responses = diveGroupService.getDiveGroupsByEventId(EVENT_ID);

        assertEquals(1, responses.size());
        assertEquals(GROUP_ID, responses.getFirst()
                                        .getId());
        assertEquals(1, responses.getFirst()
                                 .getMembers()
                                 .size());
        assertTrue(responses.getFirst()
                            .getMembers()
                            .getFirst()
                            .isOwner());
        assertEquals("First" + OWNER_ID + " Last" + OWNER_ID, responses.getFirst()
                                                                       .getOwnerName());
    }

    @Test
    void getDiveGroupsByEventIdUnknownEventFail() {
        when(eventRepository.existsById(EVENT_ID)).thenReturn(false);

        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.getDiveGroupsByEventId(EVENT_ID));
    }

    @Test
    void getDiveGroupByIdOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());

        var response = diveGroupService.getDiveGroupById(GROUP_ID);

        assertEquals(GROUP_ID, response.getId());
        assertEquals("Unknown user", response.getOwnerName());
        assertTrue(response.getMembers()
                           .isEmpty());
    }

    @Test
    void getDiveGroupByIdUnknownGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.getDiveGroupById(GROUP_ID));
    }

    // ------------------------------------------------------------------
    // createDiveGroup
    // ------------------------------------------------------------------

    private DiveGroupRequest createRequest(Long ownerId) {
        return DiveGroupRequest.builder()
                               .eventId(EVENT_ID)
                               .name("Team Sidemount")
                               .ownerId(ownerId)
                               .build();
    }

    @Test
    void createDiveGroupAsUserOk() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, null, null));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(diveGroupRepository.save(any(DiveGroup.class))).thenReturn(diveGroup(OWNER_ID));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(OWNER_ID, GROUP_ID, Instant.now())));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(OWNER_ID)));

        var response = diveGroupService.createDiveGroup(createRequest(null), OWNER_ID, false, false);

        assertEquals(GROUP_ID, response.getId());
        assertEquals(OWNER_ID, response.getOwnerId());
        verify(eventParticipantsRepository).assignDiveGroup(eq(EVENT_ID), eq(OWNER_ID), eq(GROUP_ID), any(Instant.class));
        verify(messageService, never()).createSimpleNotification(anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void createDiveGroupTrimsNameOk() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, null, null));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(diveGroupRepository.save(any(DiveGroup.class))).thenReturn(diveGroup(OWNER_ID));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                         .eventId(EVENT_ID)
                                                         .name("  Padded name  ")
                                                         .build(), OWNER_ID, false, false);

        var captor = ArgumentCaptor.forClass(DiveGroup.class);
        verify(diveGroupRepository).save(captor.capture());
        assertEquals("Padded name", captor.getValue()
                                          .getName());
        assertNotNull(captor.getValue()
                            .getCreatedAt());
    }

    @Test
    void createDiveGroupByOrganizerForAnotherOwnerOk() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, null, null));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(diveGroupRepository.save(any(DiveGroup.class))).thenReturn(diveGroup(OWNER_ID));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.createDiveGroup(createRequest(OWNER_ID), ORGANIZER_ID, false, true);

        assertEquals(OWNER_ID, response.getOwnerId());
        verify(messageService).createSimpleNotification(eq(OWNER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void createDiveGroupByUserForAnotherOwnerFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        assertThrows(OxalateUnauthorizedException.class,
                () -> diveGroupService.createDiveGroup(createRequest(OWNER_ID), MEMBER_ID, false, false));
        verify(diveGroupRepository, never()).save(any(DiveGroup.class));
    }

    @Test
    void createDiveGroupByOrganizerOfAnotherEventFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        // An organizer who is not the organizer of this event is not privileged
        assertThrows(OxalateUnauthorizedException.class,
                () -> diveGroupService.createDiveGroup(createRequest(OWNER_ID), OUTSIDER_ID, false, true));
    }

    @Test
    void createDiveGroupByAdminForAnotherOwnerOk() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, null, null));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(diveGroupRepository.save(any(DiveGroup.class))).thenReturn(diveGroup(OWNER_ID));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.createDiveGroup(createRequest(OWNER_ID), OUTSIDER_ID, true, false);

        assertEquals(OWNER_ID, response.getOwnerId());
    }

    @Test
    void createDiveGroupUnknownEventFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.createDiveGroup(createRequest(null), OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupNullRequestFail() {
        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(null, OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupNullEventIdFail() {
        var request = DiveGroupRequest.builder()
                                      .name("Team")
                                      .build();

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(request, OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupBlankNameFail() {
        var request = DiveGroupRequest.builder()
                                      .eventId(EVENT_ID)
                                      .name("   ")
                                      .build();

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(request, OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupNullNameFail() {
        var request = DiveGroupRequest.builder()
                                      .eventId(EVENT_ID)
                                      .build();

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(request, OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupTooLongNameFail() {
        var request = DiveGroupRequest.builder()
                                      .eventId(EVENT_ID)
                                      .name("x".repeat(256))
                                      .build();

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(request, OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupNotParticipantFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(null);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(createRequest(null), OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupSecondGroupForSameUserFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, null, null));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(createRequest(null), OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupWhenAlreadyMemberFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, 99L, Instant.now()));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(createRequest(null), OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupAfterEventStartedAsUserFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(1, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(createRequest(null), OWNER_ID, false, false));
    }

    @Test
    void createDiveGroupAfterEventEndedAsOrganizerFail() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(10, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(createRequest(OWNER_ID), ORGANIZER_ID, false, true));
    }

    @Test
    void createDiveGroupAfterEventStartedAsOrganizerOk() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(1, ChronoUnit.HOURS))));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, null, null));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(diveGroupRepository.save(any(DiveGroup.class))).thenReturn(diveGroup(OWNER_ID));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        assertNotNull(diveGroupService.createDiveGroup(createRequest(OWNER_ID), ORGANIZER_ID, false, true));
    }

    // ------------------------------------------------------------------
    // updateDiveGroup
    // ------------------------------------------------------------------

    @Test
    void updateDiveGroupByOwnerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                        .name("Renamed")
                                                                                        .build(), OWNER_ID, false, false);

        assertEquals("Renamed", response.getName());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void updateDiveGroupByNonOwnerFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                                                                .name("Renamed")
                                                                                                                                .build(), MEMBER_ID, false,
                false));
    }

    @Test
    void updateDiveGroupByEventOrganizerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                        .name("Organizer rename")
                                                                                        .build(), ORGANIZER_ID, false, true);

        assertEquals("Organizer rename", response.getName());
    }

    @Test
    void updateDiveGroupUnknownGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                                                            .name("Renamed")
                                                                                                                            .build(), OWNER_ID, false, false));
    }

    @Test
    void updateDiveGroupNullRequestFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.updateDiveGroup(GROUP_ID, null, OWNER_ID, false, false));
    }

    @Test
    void updateDiveGroupAfterEventEndedFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(10, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                                                              .name("Renamed")
                                                                                                                              .build(), ORGANIZER_ID, false,
                true));
    }

    @Test
    void updateDiveGroupOwnerTransferByOwnerFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                                                                .name("Renamed")
                                                                                                                                .ownerId(MEMBER_ID)
                                                                                                                                .build(), OWNER_ID, false,
                false));
    }

    @Test
    void updateDiveGroupOwnerTransferToExistingMemberOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, GROUP_ID, Instant.now()));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                        .name("Renamed")
                                                                                        .ownerId(MEMBER_ID)
                                                                                        .build(), ORGANIZER_ID, false, true);

        assertEquals(MEMBER_ID, response.getOwnerId());
        verify(eventParticipantsRepository, never()).assignDiveGroup(anyLong(), eq(MEMBER_ID), anyLong(), any(Instant.class));
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
        verify(messageService).createSimpleNotification(eq(OWNER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void updateDiveGroupOwnerTransferToNonMemberOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, null, null));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                        .name("Renamed")
                                                                                        .ownerId(MEMBER_ID)
                                                                                        .build(), ORGANIZER_ID, false, true);

        assertEquals(MEMBER_ID, response.getOwnerId());
        verify(eventParticipantsRepository).assignDiveGroup(eq(EVENT_ID), eq(MEMBER_ID), eq(GROUP_ID), any(Instant.class));
    }

    @Test
    void updateDiveGroupOwnerTransferToMemberOfAnotherGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, 999L, Instant.now()));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                                                              .name("Renamed")
                                                                                                                              .ownerId(MEMBER_ID)
                                                                                                                              .build(), ORGANIZER_ID, false,
                true));
    }

    @Test
    void updateDiveGroupOwnerTransferToNonParticipantFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OUTSIDER_ID)).thenReturn(null);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                                                              .name("Renamed")
                                                                                                                              .ownerId(OUTSIDER_ID)
                                                                                                                              .build(), ORGANIZER_ID, false,
                true));
    }

    // ------------------------------------------------------------------
    // deleteDiveGroup
    // ------------------------------------------------------------------

    @Test
    void deleteDiveGroupByOwnerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(OWNER_ID, GROUP_ID, Instant.now()),
                participant(MEMBER_ID, GROUP_ID, Instant.now())));
        stubUsers(OWNER_ID, MEMBER_ID);

        var response = diveGroupService.deleteDiveGroup(GROUP_ID, OWNER_ID, false, false);

        assertNotNull(response);
        verify(eventParticipantsRepository).clearDiveGroupMembers(GROUP_ID);
        verify(diveGroupRepository).delete(any(DiveGroup.class));
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(OWNER_ID), anyString(), anyString(), anyString());
        verify(messageService, never()).createSimpleNotification(eq(OWNER_ID), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteDiveGroupByNonOwnerFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.deleteDiveGroup(GROUP_ID, MEMBER_ID, false, false));
        verify(diveGroupRepository, never()).delete(any(DiveGroup.class));
    }

    @Test
    void deleteDiveGroupByEventOrganizerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        assertNotNull(diveGroupService.deleteDiveGroup(GROUP_ID, ORGANIZER_ID, false, true));
        verify(diveGroupRepository).delete(any(DiveGroup.class));
    }

    @Test
    void deleteDiveGroupAfterEventEndedFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(10, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.deleteDiveGroup(GROUP_ID, ORGANIZER_ID, false, true));
    }

    @Test
    void deleteDiveGroupUnknownGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.deleteDiveGroup(GROUP_ID, OWNER_ID, false, false));
    }

    // ------------------------------------------------------------------
    // joinDiveGroup
    // ------------------------------------------------------------------

    @Test
    void joinDiveGroupOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, null, null));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        stubUsers(MEMBER_ID);

        var response = diveGroupService.joinDiveGroup(GROUP_ID, MEMBER_ID);

        assertEquals(GROUP_ID, response.getId());
        verify(eventParticipantsRepository).assignDiveGroup(eq(EVENT_ID), eq(MEMBER_ID), eq(GROUP_ID), any(Instant.class));
        verify(messageService).createSimpleNotification(eq(OWNER_ID), eq(MEMBER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void joinDiveGroupUnknownGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.joinDiveGroup(GROUP_ID, MEMBER_ID));
    }

    @Test
    void joinDiveGroupNotParticipantFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OUTSIDER_ID)).thenReturn(null);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.joinDiveGroup(GROUP_ID, OUTSIDER_ID));
    }

    @Test
    void joinDiveGroupAlreadyInGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, 999L, Instant.now()));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.joinDiveGroup(GROUP_ID, MEMBER_ID));
    }

    @Test
    void joinDiveGroupAfterEventStartedFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(1, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.joinDiveGroup(GROUP_ID, MEMBER_ID));
    }

    // ------------------------------------------------------------------
    // leaveDiveGroup
    // ------------------------------------------------------------------

    @Test
    void leaveDiveGroupAsMemberOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, GROUP_ID, Instant.now()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(OWNER_ID, GROUP_ID, Instant.now())));
        stubUsers(MEMBER_ID);

        assertNotNull(diveGroupService.leaveDiveGroup(GROUP_ID, MEMBER_ID));

        verify(eventParticipantsRepository).clearDiveGroupForUser(EVENT_ID, MEMBER_ID);
        verify(messageService).createSimpleNotification(eq(OWNER_ID), eq(MEMBER_ID), anyString(), anyString(), anyString());
        verify(diveGroupRepository, never()).delete(any(DiveGroup.class));
    }

    @Test
    void leaveDiveGroupAsOwnerTransfersOwnershipOk() {
        var earlyJoiner = participant(MEMBER_ID, GROUP_ID, Instant.now()
                                                                  .minus(2, ChronoUnit.HOURS));
        var lateJoiner = participant(OUTSIDER_ID, GROUP_ID, Instant.now()
                                                                   .minus(1, ChronoUnit.HOURS));

        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, GROUP_ID, Instant.now()
                                                                                                                                       .minus(3,
                                                                                                                                               ChronoUnit.HOURS)));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(lateJoiner, earlyJoiner));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubUsers(OWNER_ID);

        assertNotNull(diveGroupService.leaveDiveGroup(GROUP_ID, OWNER_ID));

        var captor = ArgumentCaptor.forClass(DiveGroup.class);
        verify(diveGroupRepository).save(captor.capture());
        assertEquals(MEMBER_ID, captor.getValue()
                                      .getOwnerId());
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(OWNER_ID), anyString(), anyString(), anyString());
        verify(diveGroupRepository, never()).delete(any(DiveGroup.class));
    }

    @Test
    void leaveDiveGroupAsLastOwnerRemovesGroupOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, GROUP_ID, Instant.now()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        stubUsers(OWNER_ID);

        assertNotNull(diveGroupService.leaveDiveGroup(GROUP_ID, OWNER_ID));

        verify(diveGroupRepository).delete(any(DiveGroup.class));
        verify(messageService, never()).createSimpleNotification(anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void leaveDiveGroupNotMemberFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, null, null));
        stubUsers(MEMBER_ID);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.leaveDiveGroup(GROUP_ID, MEMBER_ID));
    }

    @Test
    void leaveDiveGroupMemberOfAnotherGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, 999L, Instant.now()));
        stubUsers(MEMBER_ID);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.leaveDiveGroup(GROUP_ID, MEMBER_ID));
    }

    @Test
    void leaveDiveGroupAfterEventStartedFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(1, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.leaveDiveGroup(GROUP_ID, MEMBER_ID));
    }

    // ------------------------------------------------------------------
    // addMemberToDiveGroup
    // ------------------------------------------------------------------

    @Test
    void addMemberToDiveGroupByOrganizerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, null, null));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        stubUsers(MEMBER_ID);

        assertNotNull(diveGroupService.addMemberToDiveGroup(GROUP_ID, MEMBER_ID, ORGANIZER_ID, false, true));

        verify(eventParticipantsRepository).assignDiveGroup(eq(EVENT_ID), eq(MEMBER_ID), eq(GROUP_ID), any(Instant.class));
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
        verify(messageService).createSimpleNotification(eq(OWNER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void addMemberToDiveGroupByOwnerFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.addMemberToDiveGroup(GROUP_ID, MEMBER_ID, OWNER_ID, false, false));
        verify(eventParticipantsRepository, never()).assignDiveGroup(anyLong(), anyLong(), anyLong(), any(Instant.class));
    }

    @Test
    void addMemberToDiveGroupNotParticipantFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OUTSIDER_ID)).thenReturn(null);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.addMemberToDiveGroup(GROUP_ID, OUTSIDER_ID, ORGANIZER_ID, false, true));
    }

    @Test
    void addMemberToDiveGroupAlreadyInGroupFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, 999L, Instant.now()));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.addMemberToDiveGroup(GROUP_ID, MEMBER_ID, ORGANIZER_ID, false, true));
    }

    @Test
    void addMemberToDiveGroupAfterEventEndedFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(10, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.addMemberToDiveGroup(GROUP_ID, MEMBER_ID, ORGANIZER_ID, false, true));
    }

    @Test
    void addMemberToDiveGroupAfterEventStartedOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(1, ChronoUnit.HOURS))));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, null, null));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        stubUsers(MEMBER_ID);

        assertNotNull(diveGroupService.addMemberToDiveGroup(GROUP_ID, MEMBER_ID, ORGANIZER_ID, false, true));
    }

    // ------------------------------------------------------------------
    // removeMemberFromDiveGroup
    // ------------------------------------------------------------------

    @Test
    void removeMemberFromDiveGroupByOwnerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, GROUP_ID, Instant.now()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(OWNER_ID, GROUP_ID, Instant.now())));
        stubUsers(MEMBER_ID);

        assertNotNull(diveGroupService.removeMemberFromDiveGroup(GROUP_ID, MEMBER_ID, OWNER_ID, false, false));

        verify(eventParticipantsRepository).clearDiveGroupForUser(EVENT_ID, MEMBER_ID);
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(OWNER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void removeMemberFromDiveGroupByOtherMemberFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.removeMemberFromDiveGroup(GROUP_ID, OWNER_ID, MEMBER_ID, false, false));
    }

    @Test
    void removeMemberFromDiveGroupByOrganizerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, GROUP_ID, Instant.now()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(OWNER_ID, GROUP_ID, Instant.now())));
        stubUsers(MEMBER_ID);

        assertNotNull(diveGroupService.removeMemberFromDiveGroup(GROUP_ID, MEMBER_ID, ORGANIZER_ID, false, true));

        verify(messageService, times(2)).createSimpleNotification(anyLong(), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void removeMemberFromDiveGroupOwnerTransfersOwnershipOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, GROUP_ID, Instant.now()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(MEMBER_ID, GROUP_ID, Instant.now())));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubUsers(OWNER_ID);

        assertNotNull(diveGroupService.removeMemberFromDiveGroup(GROUP_ID, OWNER_ID, ORGANIZER_ID, false, true));

        var captor = ArgumentCaptor.forClass(DiveGroup.class);
        verify(diveGroupRepository).save(captor.capture());
        assertEquals(MEMBER_ID, captor.getValue()
                                      .getOwnerId());
    }

    @Test
    void removeMemberFromDiveGroupNotMemberFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, null, null));
        stubUsers(MEMBER_ID);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.removeMemberFromDiveGroup(GROUP_ID, MEMBER_ID, OWNER_ID, false, false));
    }

    @Test
    void removeMemberFromDiveGroupAfterEventStartedAsOwnerFail() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(Instant.now()
                                                                                     .minus(1, ChronoUnit.HOURS))));

        assertThrows(OxalateValidationException.class, () -> diveGroupService.removeMemberFromDiveGroup(GROUP_ID, MEMBER_ID, OWNER_ID, false, false));
    }

    @Test
    void removeMemberFromDiveGroupUnknownUserNameOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, GROUP_ID, Instant.now()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertNotNull(diveGroupService.removeMemberFromDiveGroup(GROUP_ID, MEMBER_ID, OWNER_ID, false, false));
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(OWNER_ID), anyString(), anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    void createDiveGroupWithOwnIdAsOwnerOk() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, null, null));
        when(diveGroupRepository.findByEventIdAndOwnerId(EVENT_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(diveGroupRepository.save(any(DiveGroup.class))).thenReturn(diveGroup(OWNER_ID));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.createDiveGroup(createRequest(OWNER_ID), OWNER_ID, false, false);

        assertEquals(OWNER_ID, response.getOwnerId());
        verify(messageService, never()).createSimpleNotification(anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void updateDiveGroupWithSameOwnerIdOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                        .name("Renamed")
                                                                                        .ownerId(OWNER_ID)
                                                                                        .build(), OWNER_ID, false, false);

        assertEquals(OWNER_ID, response.getOwnerId());
        verify(messageService, never()).createSimpleNotification(anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void updateDiveGroupOwnedByOrganizerTransfersWithoutSelfNotificationOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(ORGANIZER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, GROUP_ID, Instant.now()));
        when(diveGroupRepository.save(any(DiveGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var response = diveGroupService.updateDiveGroup(GROUP_ID, DiveGroupUpdateRequest.builder()
                                                                                        .name("Renamed")
                                                                                        .ownerId(MEMBER_ID)
                                                                                        .build(), ORGANIZER_ID, false, true);

        assertEquals(MEMBER_ID, response.getOwnerId());
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
        verify(messageService, never()).createSimpleNotification(eq(ORGANIZER_ID), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void addMemberToGroupOwnedByTheOrganizerOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(ORGANIZER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, MEMBER_ID)).thenReturn(participant(MEMBER_ID, null, null));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        stubUsers(MEMBER_ID);

        assertNotNull(diveGroupService.addMemberToDiveGroup(GROUP_ID, MEMBER_ID, ORGANIZER_ID, false, true));

        verify(messageService, times(1)).createSimpleNotification(anyLong(), anyLong(), anyString(), anyString(), anyString());
        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void removeLastMemberWhoIsOwnerByOrganizerDeletesGroupOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, GROUP_ID, Instant.now()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of());
        stubUsers(OWNER_ID);

        assertNotNull(diveGroupService.removeMemberFromDiveGroup(GROUP_ID, OWNER_ID, ORGANIZER_ID, false, true));

        verify(diveGroupRepository).delete(any(DiveGroup.class));
        verify(messageService).createSimpleNotification(eq(OWNER_ID), eq(ORGANIZER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void removeMemberWithStaleGroupRowIsIgnoredOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(participant(OWNER_ID, GROUP_ID, Instant.now()));
        // The repository still reports the removed user, which must be filtered out before the ownership transfer
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(OWNER_ID, GROUP_ID, Instant.now())));
        stubUsers(OWNER_ID);

        assertNotNull(diveGroupService.leaveDiveGroup(GROUP_ID, OWNER_ID));

        verify(diveGroupRepository).delete(any(DiveGroup.class));
    }

    @Test
    void notificationFallsBackToSystemUserWhenActorIsUnknownOk() {
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup(OWNER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(eventParticipantsRepository.findAllByDiveGroupId(GROUP_ID)).thenReturn(List.of(participant(MEMBER_ID, GROUP_ID, Instant.now())));
        stubUsers(MEMBER_ID);

        assertNotNull(diveGroupService.deleteDiveGroup(GROUP_ID, -1L, true, false));

        verify(messageService).createSimpleNotification(eq(MEMBER_ID), eq(1L), anyString(), anyString(), anyString());
    }
}
