package io.oxalate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.LoginRequest;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class AuthControllerLoginRTC extends AbstractIntegrationTest {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String RAW_PASSWORD = "Test^P4ssword";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Users created per-test — cleaned up in @AfterEach to avoid FK violations from unrelated data.
     */
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
        // Delete in FK-safe order per user: memberships → user_roles → user
        for (var user : createdUsers) {
            membershipRepository.findByUserId(user.getId())
                                .forEach(m -> membershipRepository.deleteById(m.getId()));
            roleRepository.removeUserRoles(user.getId());
            userRepository.deleteById(user.getId());
        }
        createdUsers.clear();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User createUser(UserTypeEnum primaryUserType) {
        var user = User.builder()
                       .username("login.test." + System.nanoTime() + "@test.tld")
                       .password(passwordEncoder.encode(RAW_PASSWORD))
                       .firstName("Login")
                       .lastName("Tester")
                       .status(UserStatusEnum.ACTIVE)
                       .phoneNumber("358401234567")
                       .privacy(false)
                       .nextOfKin("Kin Name")
                       .registered(Instant.now()
                                          .minus(30, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .language("en")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .primaryUserType(primaryUserType)
                       .build();

        var saved = userRepository.save(user);
        var optRole = roleRepository.findByName(RoleEnum.ROLE_USER);
        optRole.ifPresent(role -> roleRepository.addUserRole(saved.getId(), role.getId()));
        createdUsers.add(saved);
        return saved;
    }

    private void addActiveMembership(User user) {
        var membership = Membership.builder()
                                   .userId(user.getId())
                                   .type(MembershipTypeEnum.PERIODICAL)
                                   .status(MembershipStatusEnum.ACTIVE)
                                   .startDate(LocalDate.now()
                                                       .minusMonths(1))
                                   .endDate(LocalDate.now()
                                                     .plusYears(1))
                                   .created(Instant.now())
                                   .build();
        membershipRepository.save(membership);
    }

    private String loginRequest(String username) throws Exception {
        return objectMapper.writeValueAsString(
                LoginRequest.builder()
                            .username(username)
                            .password(RAW_PASSWORD)
                            .build());
    }

    /**
     * Wraps the common login POST with the required X-Captcha-Token header (captcha is disabled in test profile).
     */
    private org.springframework.test.web.servlet.ResultActions performLogin(String username) throws Exception {
        return mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Captcha-Token", "test-captcha-token")
                .content(loginRequest(username)));
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void loginReturnsPrimaryUserTypeScubaDiverOk() throws Exception {
        var user = createUser(UserTypeEnum.SCUBA_DIVER);

        performLogin(user.getUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryUserType", is(UserTypeEnum.SCUBA_DIVER.name())));
    }

    @Test
    void loginReturnsPrimaryUserTypeFreeDiverOk() throws Exception {
        var user = createUser(UserTypeEnum.FREE_DIVER);

        performLogin(user.getUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryUserType", is(UserTypeEnum.FREE_DIVER.name())));
    }

    @Test
    void loginReturnsEmptyMembershipsWhenNoneExistOk() throws Exception {
        var user = createUser(UserTypeEnum.SCUBA_DIVER);

        performLogin(user.getUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberships", notNullValue()))
                .andExpect(jsonPath("$.memberships", empty()));
    }

    @Test
    void loginReturnsMembershipsWhenActiveOneExistsOk() throws Exception {
        var user = createUser(UserTypeEnum.SCUBA_DIVER);
        addActiveMembership(user);

        performLogin(user.getUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberships", notNullValue()))
                .andExpect(jsonPath("$.memberships", hasSize(1)))
                .andExpect(jsonPath("$.memberships[0].type", is(MembershipTypeEnum.PERIODICAL.name())))
                .andExpect(jsonPath("$.memberships[0].status", is(MembershipStatusEnum.ACTIVE.name())));
    }

    @Test
    void loginReturnsBothPrimaryUserTypeAndMembershipsOk() throws Exception {
        var user = createUser(UserTypeEnum.FREE_DIVER);
        addActiveMembership(user);

        performLogin(user.getUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryUserType", is(UserTypeEnum.FREE_DIVER.name())))
                .andExpect(jsonPath("$.memberships", hasSize(1)))
                .andExpect(jsonPath("$.memberships[0].userId", is((int) user.getId()
                                                                            .longValue())));
    }

    @Test
    void loginDoesNotReturnExpiredMembershipsOk() throws Exception {
        var user = createUser(UserTypeEnum.SCUBA_DIVER);

        // Insert an expired membership directly via repository
        var expiredMembership = Membership.builder()
                                          .userId(user.getId())
                                          .type(MembershipTypeEnum.PERIODICAL)
                                          .status(MembershipStatusEnum.ACTIVE)
                                          .startDate(LocalDate.now()
                                                              .minusYears(2))
                                          .endDate(LocalDate.now()
                                                            .minusDays(1)) // past end date
                                          .created(Instant.now()
                                                          .minus(730, ChronoUnit.DAYS))
                                          .build();
        membershipRepository.save(expiredMembership);

        performLogin(user.getUsername())
                .andExpect(status().isOk())
                // populateUser uses findAllCurrentAndFutureActiveByUserId — expired memberships excluded
                .andExpect(jsonPath("$.memberships", empty()));
    }
}

