package io.oxalate.backend.service;

import io.oxalate.backend.AbstractIntegrationTest;
import static io.oxalate.backend.api.EventStatusEnum.PUBLISHED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.DiveGroupRequest;
import io.oxalate.backend.api.request.DiveGroupUpdateRequest;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.MessageRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiveGroupServiceITC extends AbstractIntegrationTest {

    @Autowired
    private DiveGroupService diveGroupService;
    @Autowired
    private EventService eventService;
    @Autowired
    private DiveGroupRepository diveGroupRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventParticipantsRepository eventParticipantsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User organizer;
    private User firstUser;
    private User secondUser;
    private User thirdUser;
    private User outsider;
    private Event event;

    @BeforeEach
    void setUp() {
        organizer = createUser(RoleEnum.ROLE_ORGANIZER);
        firstUser = createUser(RoleEnum.ROLE_USER);
        secondUser = createUser(RoleEnum.ROLE_USER);
        thirdUser = createUser(RoleEnum.ROLE_USER);
        outsider = createUser(RoleEnum.ROLE_USER);

        event = createEvent(Instant.now()
                                   .plus(2, ChronoUnit.DAYS));

        addParticipant(event, organizer, "ORGANIZER");
        addParticipant(event, firstUser, "USER");
        addParticipant(event, secondUser, "USER");
        addParticipant(event, thirdUser, "USER");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("UPDATE event_participants SET dive_group_id = NULL, dive_group_joined_at = NULL");
        diveGroupRepository.deleteAll();
        eventParticipantsRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM message_receivers");
        messageRepository.deleteAll();
        eventRepository.deleteAll();

        for (var user : List.of(organizer, firstUser, secondUser, thirdUser, outsider)) {
            roleRepository.deleteAllUserRolesByUserId(user.getId());
            userRepository.deleteById(user.getId());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User createUser(RoleEnum roleEnum) {
        var username = "itc-" + Instant.now()
                                       .toEpochMilli() + "-" + Math.abs((int) (Math.random() * 1_000_000)) + "@example.tld";
        var user = userRepository.save(User.builder()
                                           .username(username)
                                           .password("password")
                                           .firstName("ITC")
                                           .lastName("User")
                                           .status(ACTIVE)
                                           .approvedTerms(true)
                                           .healthStatementId(1L)
                                           .phoneNumber("123456")
                                           .privacy(false)
                                           .nextOfKin("Kin")
                                           .registered(Instant.now()
                                                              .minus(5, ChronoUnit.DAYS))
                                           .language("en")
                                           .lastSeen(Instant.now())
                                           .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                                           .build());

        var role = roleRepository.findByName(roleEnum);
        assertFalse(role.isEmpty());
        roleRepository.addUserRole(user.getId(), role.get()
                                                     .getId());
        return user;
    }

    private Event createEvent(Instant startTime) {
        return eventRepository.save(Event.builder()
                                         .title("ITC dive group event")
                                         .description("ITC dive group event description")
                                         .type(io.oxalate.backend.api.DiveTypeEnum.CAVE)
                                         .startTime(startTime)
                                         .eventDuration(4)
                                         .maxDuration(120)
                                         .maxDepth(25)
                                         .maxParticipants(10)
                                         .status(PUBLISHED)
                                         .organizerId(organizer.getId())
                                         .build());
    }

    private void addParticipant(Event targetEvent, User user, String participantType) {
        jdbcTemplate.update("""
                        INSERT INTO event_participants (user_id, event_id, dive_count, participant_type, payment_type, created_at, event_user_type)
                        VALUES (?, ?, 0, ?, 'NONE', ?, 'SCUBA_DIVER')
                        """, user.getId(), targetEvent.getId(), participantType,
                utc(Instant.now()));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Long groupIdOf(User user) {
        return jdbcTemplate.queryForObject("SELECT dive_group_id FROM event_participants WHERE user_id = ? AND event_id = ?",
                Long.class, user.getId(), event.getId());
    }

    private long createGroupFor(User user) {
        return diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                .eventId(event.getId())
                                                                .name("Group of " + user.getId())
                                                                .build(), user.getId(), false, false)
                               .getId();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void createDiveGroupAddsOwnerAsMemberOk() {
        var response = diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                        .eventId(event.getId())
                                                                        .name("First group")
                                                                        .build(), firstUser.getId(), false, false);

        assertNotNull(response.getId());
        assertEquals(firstUser.getId(), response.getOwnerId());
        assertEquals(1, response.getMembers()
                                .size());
        assertTrue(response.getMembers()
                           .getFirst()
                           .isOwner());
        assertNotNull(response.getMembers()
                              .getFirst()
                              .getJoinedAt());
        assertEquals(response.getId(), groupIdOf(firstUser));
        assertNull(response.getUpdatedAt());
    }

    @Test
    void createDiveGroupTwiceForSameUserFail() {
        createGroupFor(firstUser);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                                                              .eventId(event.getId())
                                                                                                              .name("Second group")
                                                                                                              .build(), firstUser.getId(), false, false));
    }

    @Test
    void createDiveGroupForNonParticipantFail() {
        assertThrows(OxalateValidationException.class, () -> diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                                                              .eventId(event.getId())
                                                                                                              .name("Outsider group")
                                                                                                              .build(), outsider.getId(), false, false));
    }

    @Test
    void createDiveGroupUnknownEventFail() {
        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                                                            .eventId(999_999L)
                                                                                                            .name("Ghost group")
                                                                                                            .build(), firstUser.getId(), false, false));
    }

    @Test
    void createDiveGroupByOrganizerForAnotherOwnerOk() {
        var response = diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                        .eventId(event.getId())
                                                                        .name("Organizer created")
                                                                        .ownerId(firstUser.getId())
                                                                        .build(), organizer.getId(), false, true);

        assertEquals(firstUser.getId(), response.getOwnerId());
        assertEquals(response.getId(), groupIdOf(firstUser));
        assertFalse(messageRepository.findUnreadUserMessages(firstUser.getId())
                                     .isEmpty());
    }

    @Test
    void createDiveGroupByUserForAnotherOwnerFail() {
        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                                                                .eventId(event.getId())
                                                                                                                .name("Sneaky group")
                                                                                                                .ownerId(secondUser.getId())
                                                                                                                .build(), firstUser.getId(), false, false));
    }

    @Test
    void getDiveGroupsByEventIdOk() {
        createGroupFor(firstUser);
        createGroupFor(secondUser);

        var groups = diveGroupService.getDiveGroupsByEventId(event.getId());

        assertEquals(2, groups.size());
    }

    @Test
    void getDiveGroupsByEventIdUnknownEventFail() {
        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.getDiveGroupsByEventId(999_999L));
    }

    @Test
    void getDiveGroupByIdUnknownGroupFail() {
        assertThrows(OxalateNotFoundException.class, () -> diveGroupService.getDiveGroupById(999_999L));
    }

    @Test
    void joinDiveGroupOk() {
        var groupId = createGroupFor(firstUser);

        var response = diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        assertEquals(2, response.getMembers()
                                .size());
        assertEquals(groupId, groupIdOf(secondUser));
        assertFalse(messageRepository.findUnreadUserMessages(firstUser.getId())
                                     .isEmpty());
    }

    @Test
    void joinDiveGroupWhenAlreadyInAnotherGroupFail() {
        var firstGroupId = createGroupFor(firstUser);
        createGroupFor(secondUser);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.joinDiveGroup(firstGroupId, secondUser.getId()));
    }

    @Test
    void joinDiveGroupAsNonParticipantFail() {
        var groupId = createGroupFor(firstUser);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.joinDiveGroup(groupId, outsider.getId()));
    }

    @Test
    void leaveDiveGroupAsMemberOk() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        diveGroupService.leaveDiveGroup(groupId, secondUser.getId());

        assertNull(groupIdOf(secondUser));
        assertEquals(1, diveGroupService.getDiveGroupById(groupId)
                                        .getMembers()
                                        .size());
    }

    @Test
    void leaveDiveGroupAsOwnerTransfersOwnershipOk() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());
        diveGroupService.joinDiveGroup(groupId, thirdUser.getId());

        diveGroupService.leaveDiveGroup(groupId, firstUser.getId());

        var group = diveGroupService.getDiveGroupById(groupId);
        assertEquals(secondUser.getId(), group.getOwnerId());
        assertEquals(2, group.getMembers()
                             .size());
        assertNull(groupIdOf(firstUser));
    }

    @Test
    void leaveDiveGroupAsLastMemberRemovesGroupOk() {
        var groupId = createGroupFor(firstUser);

        diveGroupService.leaveDiveGroup(groupId, firstUser.getId());

        assertTrue(diveGroupRepository.findById(groupId)
                                      .isEmpty());
        assertNull(groupIdOf(firstUser));
    }

    @Test
    void unsubscribeFromEventAsGroupOwnerRemovesLastMemberGroupOk() {
        var groupId = createGroupFor(firstUser);

        eventService.removeUserFromEvent(firstUser, event.getId());

        assertTrue(diveGroupRepository.findById(groupId)
                                      .isEmpty());
        assertNull(eventParticipantsRepository.findByEventIdAndUserId(event.getId(), firstUser.getId()));
    }

    @Test
    void unsubscribeFromEventAsGroupOwnerTransfersOwnershipToOldestMemberOk() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, thirdUser.getId());
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        eventService.removeUserFromEvent(firstUser, event.getId());

        assertEquals(secondUser.getId(), diveGroupRepository.findById(groupId)
                                                            .orElseThrow()
                                                            .getOwnerId());
        assertNull(eventParticipantsRepository.findByEventIdAndUserId(event.getId(), firstUser.getId()));
    }

    @Test
    void leaveDiveGroupWhenNotMemberFail() {
        var groupId = createGroupFor(firstUser);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.leaveDiveGroup(groupId, secondUser.getId()));
    }

    @Test
    void updateDiveGroupNameByOwnerOk() {
        var groupId = createGroupFor(firstUser);

        var response = diveGroupService.updateDiveGroup(groupId, DiveGroupUpdateRequest.builder()
                                                                                       .name("Renamed group")
                                                                                       .build(), firstUser.getId(), false, false);

        assertEquals("Renamed group", response.getName());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void updateDiveGroupByOtherUserFail() {
        var groupId = createGroupFor(firstUser);

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.updateDiveGroup(groupId, DiveGroupUpdateRequest.builder()
                                                                                                                               .name("Hijacked")
                                                                                                                               .build(), secondUser.getId(),
                false, false));
    }

    @Test
    void updateDiveGroupOwnerByOrganizerOk() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        var response = diveGroupService.updateDiveGroup(groupId, DiveGroupUpdateRequest.builder()
                                                                                       .name("Organizer managed")
                                                                                       .ownerId(secondUser.getId())
                                                                                       .build(), organizer.getId(), false, true);

        assertEquals(secondUser.getId(), response.getOwnerId());
        assertEquals(2, response.getMembers()
                                .size());
    }

    @Test
    void updateDiveGroupOwnerToNonMemberAddsMemberOk() {
        var groupId = createGroupFor(firstUser);

        var response = diveGroupService.updateDiveGroup(groupId, DiveGroupUpdateRequest.builder()
                                                                                       .name("Organizer managed")
                                                                                       .ownerId(thirdUser.getId())
                                                                                       .build(), organizer.getId(), false, true);

        assertEquals(thirdUser.getId(), response.getOwnerId());
        assertEquals(groupId, groupIdOf(thirdUser));
        assertEquals(2, response.getMembers()
                                .size());
    }

    @Test
    void deleteDiveGroupByOwnerRemovesAllMembersOk() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        diveGroupService.deleteDiveGroup(groupId, firstUser.getId(), false, false);

        assertTrue(diveGroupRepository.findById(groupId)
                                      .isEmpty());
        assertNull(groupIdOf(firstUser));
        assertNull(groupIdOf(secondUser));
        assertFalse(messageRepository.findUnreadUserMessages(secondUser.getId())
                                     .isEmpty());
    }

    @Test
    void deleteDiveGroupByOrganizerOk() {
        var groupId = createGroupFor(firstUser);

        diveGroupService.deleteDiveGroup(groupId, organizer.getId(), false, true);

        assertTrue(diveGroupRepository.findById(groupId)
                                      .isEmpty());
    }

    @Test
    void deleteDiveGroupByOtherUserFail() {
        var groupId = createGroupFor(firstUser);

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.deleteDiveGroup(groupId, secondUser.getId(), false, false));
    }

    @Test
    void addMemberToDiveGroupByOrganizerOk() {
        var groupId = createGroupFor(firstUser);

        var response = diveGroupService.addMemberToDiveGroup(groupId, secondUser.getId(), organizer.getId(), false, true);

        assertEquals(2, response.getMembers()
                                .size());
        assertEquals(groupId, groupIdOf(secondUser));
    }

    @Test
    void addMemberToDiveGroupByOwnerFail() {
        var groupId = createGroupFor(firstUser);

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.addMemberToDiveGroup(groupId, secondUser.getId(), firstUser.getId(), false,
                false));
    }

    @Test
    void addMemberToDiveGroupNonParticipantFail() {
        var groupId = createGroupFor(firstUser);

        assertThrows(OxalateValidationException.class, () -> diveGroupService.addMemberToDiveGroup(groupId, outsider.getId(), organizer.getId(), false, true));
    }

    @Test
    void removeMemberFromDiveGroupByOwnerOk() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        diveGroupService.removeMemberFromDiveGroup(groupId, secondUser.getId(), firstUser.getId(), false, false);

        assertNull(groupIdOf(secondUser));
        assertFalse(messageRepository.findUnreadUserMessages(secondUser.getId())
                                     .isEmpty());
    }

    @Test
    void removeMemberFromDiveGroupByOtherMemberFail() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        assertThrows(OxalateUnauthorizedException.class, () -> diveGroupService.removeMemberFromDiveGroup(groupId, firstUser.getId(), secondUser.getId(), false,
                false));
    }

    @Test
    void removeOwnerFromDiveGroupByOrganizerTransfersOwnershipOk() {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        diveGroupService.removeMemberFromDiveGroup(groupId, firstUser.getId(), organizer.getId(), false, true);

        assertEquals(secondUser.getId(), diveGroupService.getDiveGroupById(groupId)
                                                         .getOwnerId());
    }

    @Test
    void modifyDiveGroupAfterEventStartedAsUserFail() {
        var groupId = createGroupFor(firstUser);
        jdbcTemplate.update("UPDATE events SET start_time = ? WHERE id = ?", utc(Instant.now()
                                                                                        .minus(1, ChronoUnit.HOURS)), event.getId());

        assertThrows(OxalateValidationException.class, () -> diveGroupService.joinDiveGroup(groupId, secondUser.getId()));
        assertThrows(OxalateValidationException.class, () -> diveGroupService.leaveDiveGroup(groupId, firstUser.getId()));
    }

    @Test
    void modifyDiveGroupAfterEventEndedAsOrganizerFail() {
        var groupId = createGroupFor(firstUser);
        jdbcTemplate.update("UPDATE events SET start_time = ? WHERE id = ?", utc(Instant.now()
                                                                                        .minus(10, ChronoUnit.HOURS)), event.getId());

        assertThrows(OxalateValidationException.class, () -> diveGroupService.deleteDiveGroup(groupId, organizer.getId(), false, true));
    }

    @Test
    void modifyDiveGroupAfterEventStartedAsOrganizerOk() {
        var groupId = createGroupFor(firstUser);
        jdbcTemplate.update("UPDATE events SET start_time = ? WHERE id = ?", utc(Instant.now()
                                                                                        .minus(1, ChronoUnit.HOURS)), event.getId());

        var response = diveGroupService.addMemberToDiveGroup(groupId, secondUser.getId(), organizer.getId(), false, true);

        assertEquals(2, response.getMembers()
                                .size());
    }
}
