package io.oxalate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.MessageRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationControllerRTC extends AbstractIntegrationTest {

    private static final String CREATE_BULK_ENDPOINT = "/api/notifications/create-bulk";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Tracks created users so they can be cleaned up after each test. */
    private final List<User> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        createdUsers.clear();
    }

    @AfterEach
    void tearDown() {
        for (var user : createdUsers) {
            // FK cleanup order: message_receivers → messages → user_roles → users
            // message_receivers has FKs to both messages(id) and users(id), neither with CASCADE.
            // Delete receivers for messages created by this user first, then receivers where this
            // user is a recipient, then the messages themselves, then roles, then the user row.
            jdbcTemplate.update(
                    "DELETE FROM message_receivers WHERE message_id IN (SELECT id FROM messages WHERE creator = ?)",
                    user.getId());
            jdbcTemplate.update("DELETE FROM message_receivers WHERE user_id = ?", user.getId());
            jdbcTemplate.update("DELETE FROM messages WHERE creator = ?", user.getId());
            roleRepository.removeUserRoles(user.getId());
            userRepository.deleteById(user.getId());
        }
        createdUsers.clear();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User createUser(String language, RoleEnum... roles) {
        var user = User.builder()
                       .username("notif.test." + System.nanoTime() + "@test.tld")
                       .password("password")
                       .firstName("Test")
                       .lastName("User")
                       .status(UserStatusEnum.ACTIVE)
                       .phoneNumber("358401234567")
                       .privacy(false)
                       .nextOfKin("Kin")
                       .registered(Instant.now().minus(30, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .language(language)
                       .lastSeen(Instant.now().minus(1, ChronoUnit.DAYS))
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();

        var saved = userRepository.save(user);

        for (var role : roles) {
            roleRepository.findByName(role)
                          .ifPresent(r -> roleRepository.addUserRole(saved.getId(), r.getId()));
        }

        createdUsers.add(saved);
        return saved;
    }

    private String jwtFor(User user, RoleEnum... roles) {
        var grantedRoles = new ArrayList<SimpleGrantedAuthority>();
        for (var role : roles) {
            grantedRoles.add(new SimpleGrantedAuthority(role.name()));
        }
        grantedRoles.add(new SimpleGrantedAuthority(RoleEnum.ROLE_ANONYMOUS.name()));

        var userDetails = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                grantedRoles,
                user.isApprovedTerms(),
                user.getHealthStatementId(),
                false,
                user.getLanguage()
        );

        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, grantedRoles);
        return jwtUtils.generateJwtToken(auth);
    }

    private String bulkRequest(List<Long> recipients, Boolean sendAll, Long eventId) throws Exception {
        var request = MessageRequest.builder()
                                    .title("Test notification")
                                    .message("This is the message body")
                                    .description("desc")
                                    .recipients(recipients)
                                    .sendAll(sendAll)
                                    .eventId(eventId)
                                    .build();
        return objectMapper.writeValueAsString(request);
    }

    // ------------------------------------------------------------------
    // Authorization tests
    // ------------------------------------------------------------------

    @Test
    void createBulkNotificationsUnauthenticatedFail() throws Exception {
        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(1L), false, null)))
               .andExpect(status().isForbidden());
    }

    @Test
    void createBulkNotificationsUserRoleForbiddenFail() throws Exception {
        var user = createUser("en", RoleEnum.ROLE_USER);
        var token = jwtFor(user, RoleEnum.ROLE_USER);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(1L), false, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isForbidden());
    }

    @Test
    void createBulkNotificationsOrganizerWithRecipientsOk() throws Exception {
        var recipient = createUser("en", RoleEnum.ROLE_USER);
        var organizer = createUser("en", RoleEnum.ROLE_ORGANIZER);
        var token = jwtFor(organizer, RoleEnum.ROLE_ORGANIZER);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(recipient.getId()), false, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("OK")));
    }

    @Test
    void createBulkNotificationsOrganizerWithSendAllForbiddenFail() throws Exception {
        var organizer = createUser("en", RoleEnum.ROLE_ORGANIZER);
        var token = jwtFor(organizer, RoleEnum.ROLE_ORGANIZER);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(null, true, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isForbidden());
    }

    @Test
    void createBulkNotificationsAdminWithRecipientsOk() throws Exception {
        var recipient = createUser("en", RoleEnum.ROLE_USER);
        var admin = createUser("en", RoleEnum.ROLE_ADMIN);
        var token = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(recipient.getId()), false, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("OK")));
    }

    @Test
    void createBulkNotificationsAdminWithSendAllOk() throws Exception {
        var admin = createUser("en", RoleEnum.ROLE_ADMIN);
        var token = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(null, true, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("OK")));
    }

    // ------------------------------------------------------------------
    // sendAll / recipients validation tests
    // ------------------------------------------------------------------

    @Test
    void createBulkNotificationsNoRecipientsAndNoSendAllFail() throws Exception {
        var admin = createUser("en", RoleEnum.ROLE_ADMIN);
        var token = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(null, false, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("FAIL")));
    }

    @Test
    void createBulkNotificationsEmptyRecipientsListAndNoSendAllFail() throws Exception {
        var admin = createUser("en", RoleEnum.ROLE_ADMIN);
        var token = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(), false, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("FAIL")));
    }

    // ------------------------------------------------------------------
    // Response localization tests
    // ------------------------------------------------------------------

    @Test
    void createBulkNotificationsEnglishResponseOk() throws Exception {
        var recipient = createUser("en", RoleEnum.ROLE_USER);
        var admin = createUser("en", RoleEnum.ROLE_ADMIN);
        var token = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(recipient.getId()), false, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("OK")))
               .andExpect(jsonPath("$.message", containsString("1 users")));
    }

    @Test
    void createBulkNotificationsFinnishResponseOk() throws Exception {
        var recipient = createUser("fi", RoleEnum.ROLE_USER);
        var admin = createUser("fi", RoleEnum.ROLE_ADMIN);
        var token = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(recipient.getId()), false, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("OK")))
               .andExpect(jsonPath("$.message", containsString("1 käyttäjälle")));
    }

    @Test
    void createBulkNotificationsOrganizerSendAllForbiddenResponseEnglishOk() throws Exception {
        var organizer = createUser("en", RoleEnum.ROLE_ORGANIZER);
        var token = jwtFor(organizer, RoleEnum.ROLE_ORGANIZER);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(null, true, null))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.message", containsString("Organizers are not allowed")));
    }

    // ------------------------------------------------------------------
    // Event link in email (eventId passed through)
    // ------------------------------------------------------------------

    @Test
    void createBulkNotificationsWithEventIdOk() throws Exception {
        var recipient = createUser("en", RoleEnum.ROLE_USER);
        var admin = createUser("en", RoleEnum.ROLE_ADMIN);
        var token = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        mockMvc.perform(post(CREATE_BULK_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(bulkRequest(List.of(recipient.getId()), false, 99L))
                       .cookie(new Cookie(JWT_TOKEN, token)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("OK")));
    }
}
