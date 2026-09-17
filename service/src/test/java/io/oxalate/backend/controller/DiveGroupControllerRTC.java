package io.oxalate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oxalate.backend.AbstractIntegrationTest;
import static io.oxalate.backend.api.EventStatusEnum.PUBLISHED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.DiveGroupDetailsRequest;
import io.oxalate.backend.api.request.DiveGroupOrderRequest;
import io.oxalate.backend.api.request.DiveGroupRequest;
import io.oxalate.backend.api.request.DiveGroupUpdateRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.MessageRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import io.oxalate.backend.service.DiveGroupService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiveGroupControllerRTC extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/api/dive-groups";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventParticipantsRepository eventParticipantsRepository;
    @Autowired
    private DiveGroupRepository diveGroupRepository;
    @Autowired
    private DiveGroupService diveGroupService;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private User organizer;
    private User otherOrganizer;
    private User firstUser;
    private User secondUser;
    private User outsider;
    private Event event;

    private String organizerToken;
    private String otherOrganizerToken;
    private String firstUserToken;
    private String secondUserToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                 .apply(springSecurity())
                                 .build();

        organizer = createUser(RoleEnum.ROLE_ORGANIZER);
        otherOrganizer = createUser(RoleEnum.ROLE_ORGANIZER);
        firstUser = createUser(RoleEnum.ROLE_USER);
        secondUser = createUser(RoleEnum.ROLE_USER);
        outsider = createUser(RoleEnum.ROLE_USER);

        organizerToken = tokenFor(organizer, RoleEnum.ROLE_ORGANIZER);
        otherOrganizerToken = tokenFor(otherOrganizer, RoleEnum.ROLE_ORGANIZER);
        firstUserToken = tokenFor(firstUser, RoleEnum.ROLE_USER);
        secondUserToken = tokenFor(secondUser, RoleEnum.ROLE_USER);
        outsiderToken = tokenFor(outsider, RoleEnum.ROLE_USER);

        event = eventRepository.save(Event.builder()
                                          .title("RTC dive group event")
                                          .description("RTC dive group event description")
                                          .type(io.oxalate.backend.api.DiveTypeEnum.CAVE)
                                          .startTime(Instant.now()
                                                            .plus(2, ChronoUnit.DAYS))
                                          .eventDuration(4)
                                          .maxDuration(120)
                                          .maxDepth(25)
                                          .maxParticipants(10)
                                          .status(PUBLISHED)
                                          .organizerId(organizer.getId())
                                          .build());

        addParticipant(organizer, "ORGANIZER");
        addParticipant(firstUser, "USER");
        addParticipant(secondUser, "USER");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("UPDATE event_participants SET dive_group_id = NULL, dive_group_joined_at = NULL");
        diveGroupRepository.deleteAll();
        eventParticipantsRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM message_receivers");
        messageRepository.deleteAll();
        eventRepository.deleteAll();

        for (var user : List.of(organizer, otherOrganizer, firstUser, secondUser, outsider)) {
            roleRepository.deleteAllUserRolesByUserId(user.getId());
            userRepository.deleteById(user.getId());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User createUser(RoleEnum roleEnum) {
        var username = "rtc-dg-" + Instant.now()
                                          .toEpochMilli() + "-" + Math.abs((int) (Math.random() * 1_000_000)) + "@example.tld";
        var user = userRepository.save(User.builder()
                                           .username(username)
                                           .password("password")
                                           .firstName("RTC")
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

    private String tokenFor(User user, RoleEnum roleEnum) {
        var authorities = List.of(new SimpleGrantedAuthority(roleEnum.name()));
        var userDetails = new UserDetailsImpl(user.getId(), user.getUsername(), user.getPassword(), authorities, user.isApprovedTerms(),
                user.getHealthStatementId(), false, user.getLanguage());
        return jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));
    }

    private void addParticipant(User user, String participantType) {
        jdbcTemplate.update("""
                        INSERT INTO event_participants (user_id, event_id, dive_count, participant_type, payment_type, created_at, event_user_type)
                        VALUES (?, ?, 0, ?, 'NONE', ?, 'SCUBA_DIVER')
                        """, user.getId(), event.getId(), participantType,
                LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
    }

    private long createGroupFor(User user) {
        return diveGroupService.createDiveGroup(DiveGroupRequest.builder()
                                                                .eventId(event.getId())
                                                                .name("Group of " + user.getId())
                                                                .build(), user.getId(), false, false)
                               .getId();
    }

    private String orderJson(Long... diveGroupIds) throws Exception {
        return json(DiveGroupOrderRequest.builder()
                                         .diveGroupIds(List.of(diveGroupIds))
                                         .build());
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    @Test
    void getDiveGroupsByEventIdWithoutAuthenticationFail() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/events/{eventId}", event.getId()))
               .andExpect(status().isForbidden());
    }

    @Test
    void createDiveGroupWithoutAuthenticationFail() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Anonymous group")
                                                     .build())))
               .andExpect(status().isForbidden());
    }

    @Test
    void joinDiveGroupWithoutAuthenticationFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId))
               .andExpect(status().isForbidden());
    }

    @Test
    void addMemberToDiveGroupAsUserForbidden() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members/{userId}", groupId, secondUser.getId())
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Happy paths
    // ------------------------------------------------------------------

    @Test
    void createDiveGroupOk() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("First group")
                                                     .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("First group"))
               .andExpect(jsonPath("$.ownerId").value(firstUser.getId()))
               .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void createDiveGroupTwiceFail() throws Exception {
        createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Second group")
                                                     .build())))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createDiveGroupAsNonParticipantFail() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, outsiderToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Outsider group")
                                                     .build())))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createDiveGroupForUnknownEventFail() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(999_999L)
                                                     .name("Ghost group")
                                                     .build())))
               .andExpect(status().isNotFound());
    }

    @Test
    void createDiveGroupWithAssignedOwnerAsUserFail() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Sneaky group")
                                                     .ownerId(secondUser.getId())
                                                     .build())))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void createDiveGroupWithAssignedOwnerAsOrganizerOk() throws Exception {
        jdbcTemplate.update("DELETE FROM event_participants WHERE event_id = ? AND user_id = ?", event.getId(), organizer.getId());

        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Organizer group")
                                                     .ownerId(firstUser.getId())
                                                     .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ownerId").value(firstUser.getId()));
    }

    @Test
    void createDiveGroupWithOrganizerAsOwnerOk() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Organizer owned group")
                                                     .ownerId(organizer.getId())
                                                     .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ownerId").value(organizer.getId()));
    }

    @Test
    void createDiveGroupWithMembersAsUserOk() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Group with members")
                                                     .memberIds(List.of(secondUser.getId()))
                                                     .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ownerId").value(firstUser.getId()))
               .andExpect(jsonPath("$.members.length()").value(2))
               .andExpect(jsonPath("$.members[?(@.userId == " + secondUser.getId() + ")].owner").value(false));
    }

    @Test
    void createDiveGroupWithMemberAlreadyInAnotherGroupFail() throws Exception {
        createGroupFor(secondUser);

        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Poaching group")
                                                     .memberIds(List.of(secondUser.getId()))
                                                     .build())))
               .andExpect(status().isBadRequest());

        // The whole creation is rolled back, so the group must not exist
        mockMvc.perform(get(BASE_PATH + "/events/{eventId}", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createDiveGroupWithNonParticipantMemberFail() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Group with outsider")
                                                     .memberIds(List.of(outsider.getId()))
                                                     .build())))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createDiveGroupWithMembersAsOrganizerOk() throws Exception {
        jdbcTemplate.update("DELETE FROM event_participants WHERE event_id = ? AND user_id = ?", event.getId(), organizer.getId());

        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Organizer built group")
                                                     .ownerId(firstUser.getId())
                                                     .memberIds(List.of(secondUser.getId(), firstUser.getId()))
                                                     .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ownerId").value(firstUser.getId()))
               .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void getDiveGroupsByEventIdOk() throws Exception {
        createGroupFor(firstUser);
        createGroupFor(secondUser);

        mockMvc.perform(get(BASE_PATH + "/events/{eventId}", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getDiveGroupsByUnknownEventIdFail() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/events/{eventId}", 999_999L)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isNotFound());
    }

    @Test
    void getDiveGroupByIdOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(get(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(groupId));
    }

    @Test
    void getDiveGroupByUnknownIdFail() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/{diveGroupId}", 999_999L)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isNotFound());
    }

    @Test
    void joinAndLeaveDiveGroupOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.members.length()").value(2));

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void joinDiveGroupTwiceFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk());

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void leaveDiveGroupWhenNotMemberFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void leaveDiveGroupAsOwnerTransfersOwnershipOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk());

        mockMvc.perform(get(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ownerId").value(secondUser.getId()));
    }

    @Test
    void updateDiveGroupAsOwnerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupUpdateRequest.builder()
                                                           .name("Renamed group")
                                                           .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Renamed group"));
    }

    @Test
    void updateDiveGroupAsOtherUserFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupUpdateRequest.builder()
                                                           .name("Hijacked")
                                                           .build())))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void updateDiveGroupWithBlankNameFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupUpdateRequest.builder()
                                                           .name("   ")
                                                           .build())))
               .andExpect(status().isBadRequest());
    }

    @Test
    void updateDiveGroupOwnerAsOrganizerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupUpdateRequest.builder()
                                                           .name("Organizer managed")
                                                           .ownerId(secondUser.getId())
                                                           .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ownerId").value(secondUser.getId()));
    }

    @Test
    void deleteDiveGroupAsOwnerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("OK"));

        mockMvc.perform(get(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isNotFound());
    }

    @Test
    void deleteDiveGroupAsOtherUserFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteDiveGroupAsOrganizerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isOk());
    }

    @Test
    void addMemberToDiveGroupAsOrganizerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members/{userId}", groupId, secondUser.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void addNonParticipantToDiveGroupAsOrganizerFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members/{userId}", groupId, outsider.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void removeMemberFromDiveGroupAsOwnerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}/members/{userId}", groupId, secondUser.getId())
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void removeMemberFromDiveGroupAsOtherMemberFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}/members/{userId}", groupId, firstUser.getId())
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void removeMemberFromDiveGroupAsOrganizerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}/members/{userId}", groupId, secondUser.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isOk());
    }

    @Test
    void modifyDiveGroupAfterEventEndedFail() throws Exception {
        var groupId = createGroupFor(firstUser);
        jdbcTemplate.update("UPDATE events SET start_time = ? WHERE id = ?",
                LocalDateTime.ofInstant(Instant.now()
                                               .minus(10, ChronoUnit.HOURS), ZoneOffset.UTC), event.getId());

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void joinDiveGroupAfterEventStartedFail() throws Exception {
        var groupId = createGroupFor(firstUser);
        jdbcTemplate.update("UPDATE events SET start_time = ? WHERE id = ?",
                LocalDateTime.ofInstant(Instant.now()
                                               .minus(1, ChronoUnit.HOURS), ZoneOffset.UTC), event.getId());

        mockMvc.perform(post(BASE_PATH + "/{diveGroupId}/members", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Dive group order
    // ------------------------------------------------------------------

    @Test
    void createdDiveGroupsGetTheCreationOrderOk() throws Exception {
        createGroupFor(firstUser);
        createGroupFor(secondUser);
        createGroupFor(organizer);

        mockMvc.perform(get(BASE_PATH + "/events/{eventId}", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].groupOrder").value(1))
               .andExpect(jsonPath("$[1].groupOrder").value(2))
               .andExpect(jsonPath("$[2].groupOrder").value(3))
               .andExpect(jsonPath("$[0].ownerId").value(firstUser.getId()))
               .andExpect(jsonPath("$[2].ownerId").value(organizer.getId()));
    }

    @Test
    void reorderDiveGroupsAsEventOrganizerOk() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        var secondGroupId = createGroupFor(secondUser);
        var thirdGroupId = createGroupFor(organizer);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(thirdGroupId, firstGroupId, secondGroupId)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(3))
               .andExpect(jsonPath("$[0].id").value(thirdGroupId))
               .andExpect(jsonPath("$[0].groupOrder").value(1))
               .andExpect(jsonPath("$[1].id").value(firstGroupId))
               .andExpect(jsonPath("$[1].groupOrder").value(2))
               .andExpect(jsonPath("$[2].id").value(secondGroupId))
               .andExpect(jsonPath("$[2].groupOrder").value(3));

        mockMvc.perform(get(BASE_PATH + "/events/{eventId}", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value(thirdGroupId))
               .andExpect(jsonPath("$[1].id").value(firstGroupId))
               .andExpect(jsonPath("$[2].id").value(secondGroupId));
    }

    @Test
    void reorderDiveGroupsWithoutAuthenticationFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(firstGroupId)))
               .andExpect(status().isForbidden());
    }

    @Test
    void reorderDiveGroupsAsUserFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        var secondGroupId = createGroupFor(secondUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(secondGroupId, firstGroupId)))
               .andExpect(status().isForbidden());
    }

    /**
     * The organizer role alone is not enough: only the organizer of this particular dive event may set the order.
     */
    @Test
    void reorderDiveGroupsAsOrganizerOfAnotherEventFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        var secondGroupId = createGroupFor(secondUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, otherOrganizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(secondGroupId, firstGroupId)))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void reorderDiveGroupsOfUnknownEventFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", 999_999L)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(firstGroupId)))
               .andExpect(status().isNotFound());
    }

    @Test
    void reorderDiveGroupsWithIncompleteListFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        createGroupFor(secondUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(firstGroupId)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void reorderDiveGroupsWithUnknownGroupIdFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        createGroupFor(secondUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(firstGroupId, 999_999L)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void reorderDiveGroupsWithDuplicateGroupIdFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        createGroupFor(secondUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(firstGroupId, firstGroupId)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void reorderDiveGroupsWithEmptyListFail() throws Exception {
        createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson()))
               .andExpect(status().isBadRequest());
    }

    @Test
    void reorderDiveGroupsAfterEventEndedFail() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        var secondGroupId = createGroupFor(secondUser);
        jdbcTemplate.update("UPDATE events SET start_time = ? WHERE id = ?",
                LocalDateTime.ofInstant(Instant.now()
                                               .minus(10, ChronoUnit.HOURS), ZoneOffset.UTC), event.getId());

        mockMvc.perform(put(BASE_PATH + "/events/{eventId}/order", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(orderJson(secondGroupId, firstGroupId)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDiveGroupResequencesTheRemainingGroupsOk() throws Exception {
        var firstGroupId = createGroupFor(firstUser);
        var secondGroupId = createGroupFor(secondUser);
        var thirdGroupId = createGroupFor(organizer);

        mockMvc.perform(delete(BASE_PATH + "/{diveGroupId}", firstGroupId)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isOk());

        mockMvc.perform(get(BASE_PATH + "/events/{eventId}", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2))
               .andExpect(jsonPath("$[0].id").value(secondGroupId))
               .andExpect(jsonPath("$[0].groupOrder").value(1))
               .andExpect(jsonPath("$[1].id").value(thirdGroupId))
               .andExpect(jsonPath("$[1].groupOrder").value(2));
    }

    // ------------------------------------------------------------------
    // Description and member-editable details
    // ------------------------------------------------------------------

    @Test
    void createDiveGroupWithDescriptionOk() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Described group")
                                                     .description("  We dive the wreck first  ")
                                                     .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.description").value("We dive the wreck first"));
    }

    @Test
    void createDiveGroupWithTooLongDescriptionFail() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupRequest.builder()
                                                     .eventId(event.getId())
                                                     .name("Described group")
                                                     .description("d".repeat(8001))
                                                     .build())))
               .andExpect(status().isBadRequest());
    }

    @Test
    void updateDiveGroupDescriptionAsOwnerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupUpdateRequest.builder()
                                                           .name("Renamed group")
                                                           .description("Owner plan")
                                                           .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.description").value("Owner plan"));
    }

    @Test
    void updateDiveGroupDetailsAsMemberOk() throws Exception {
        var groupId = createGroupFor(firstUser);
        diveGroupService.joinDiveGroup(groupId, secondUser.getId());

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Member renamed")
                                                            .description("Member plan")
                                                            .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Member renamed"))
               .andExpect(jsonPath("$.description").value("Member plan"))
               .andExpect(jsonPath("$.ownerId").value(firstUser.getId()));
    }

    @Test
    void updateDiveGroupDetailsAsOwnerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Owner renamed")
                                                            .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Owner renamed"))
               .andExpect(jsonPath("$.description").isEmpty());
    }

    @Test
    void updateDiveGroupDetailsAsOrganizerOk() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Organizer renamed")
                                                            .description("Organizer plan")
                                                            .build())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.description").value("Organizer plan"));
    }

    /**
     * Horizontal access control: a participant of the same event who is not a member of the group may not touch it.
     */
    @Test
    void updateDiveGroupDetailsAsNonMemberFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, secondUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Hijacked")
                                                            .build())))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_PATH + "/{diveGroupId}", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Group of " + firstUser.getId()));
    }

    @Test
    void updateDiveGroupDetailsAsOrganizerOfAnotherEventFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, otherOrganizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Hijacked")
                                                            .build())))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void updateDiveGroupDetailsWithoutAuthenticationFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Anonymous")
                                                            .build())))
               .andExpect(status().isForbidden());
    }

    @Test
    void updateDiveGroupDetailsUnknownGroupFail() throws Exception {
        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", 999_999L)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Missing")
                                                            .build())))
               .andExpect(status().isNotFound());
    }

    @Test
    void updateDiveGroupDetailsWithBlankNameFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("   ")
                                                            .build())))
               .andExpect(status().isBadRequest());
    }

    @Test
    void updateDiveGroupDetailsWithTooLongDescriptionFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json(DiveGroupDetailsRequest.builder()
                                                            .name("Renamed")
                                                            .description("d".repeat(8001))
                                                            .build())))
               .andExpect(status().isBadRequest());
    }

    @Test
    void updateDiveGroupDetailsWithMalformedBodyFail() throws Exception {
        var groupId = createGroupFor(firstUser);

        mockMvc.perform(put(BASE_PATH + "/{diveGroupId}/details", groupId)
                       .cookie(new Cookie(JWT_TOKEN, firstUserToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("{not json"))
               .andExpect(status().isBadRequest());
    }
}
