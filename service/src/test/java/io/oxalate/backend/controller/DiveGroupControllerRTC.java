package io.oxalate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oxalate.backend.AbstractIntegrationTest;
import static io.oxalate.backend.api.EventStatusEnum.PUBLISHED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.UserTypeEnum;
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
    private User firstUser;
    private User secondUser;
    private User outsider;
    private Event event;

    private String organizerToken;
    private String firstUserToken;
    private String secondUserToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                 .apply(springSecurity())
                                 .build();

        organizer = createUser(RoleEnum.ROLE_ORGANIZER);
        firstUser = createUser(RoleEnum.ROLE_USER);
        secondUser = createUser(RoleEnum.ROLE_USER);
        outsider = createUser(RoleEnum.ROLE_USER);

        organizerToken = tokenFor(organizer, RoleEnum.ROLE_ORGANIZER);
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

        for (var user : List.of(organizer, firstUser, secondUser, outsider)) {
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
}
