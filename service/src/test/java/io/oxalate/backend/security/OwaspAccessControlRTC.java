package io.oxalate.backend.security;

import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * OWASP A01:2025 - Broken Access Control, exercised end to end through the real filter chain.
 * <p>
 * {@code OwaspEndpointAuthorizationUTC} proves an annotation exists; this class proves the annotation actually
 * denies the request once Spring Security, the JWT cookie filter and method security are all in play. It
 * focuses on the two failure modes that matter most for this application:
 * <ol>
 *     <li><b>Vertical escalation</b>: an ordinary member reaching an ORGANIZER or ADMIN operation. Membership
 *     records are the concrete example - they gate event participation, and every membership endpoint was
 *     unauthenticated before this audit, so any member could grant themselves one.</li>
 *     <li><b>Horizontal escalation (IDOR)</b>: a member reading another member's records by changing the user
 *     id in the URL. A role check alone cannot catch this, so the controller performs an ownership check.</li>
 * </ol>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("OWASP A01: role and ownership are enforced at runtime")
class OwaspAccessControlRTC extends AbstractIntegrationTest {

    private static final String MEMBERSHIPS = "/api/memberships";
    private static final String AUDITS = "/api/audits";
    private static final String STATS = "/api/stats";
    private static final String DATA_DOWNLOAD = "/api/data-download";
    private static final String TAG_GROUPS = "/api/tag-groups";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    private MockMvc mockMvc;

    private User member;
    private User otherMember;
    private User organizer;
    private User administrator;

    private String memberToken;
    private String organizerToken;
    private String administratorToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                 .apply(springSecurity())
                                 .build();

        member = createUser(RoleEnum.ROLE_USER);
        otherMember = createUser(RoleEnum.ROLE_USER);
        organizer = createUser(RoleEnum.ROLE_ORGANIZER);
        administrator = createUser(RoleEnum.ROLE_ADMIN);

        memberToken = tokenFor(member, RoleEnum.ROLE_USER);
        organizerToken = tokenFor(organizer, RoleEnum.ROLE_ORGANIZER);
        administratorToken = tokenFor(administrator, RoleEnum.ROLE_ADMIN);
    }

    @AfterEach
    void tearDown() {
        for (var user : List.of(member, otherMember, organizer, administrator)) {
            roleRepository.deleteAllUserRolesByUserId(user.getId());
            userRepository.deleteById(user.getId());
        }
    }

    // ------------------------------------------------------------------
    // Memberships: the regression that motivated this suite
    // ------------------------------------------------------------------

    @Test
    void getAllActiveMembershipsAnonymouslyFail() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAllActiveMembershipsAsMemberFail() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS)
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAllActiveMembershipsAsOrganizerFail() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAllActiveMembershipsAsAdministratorOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS)
                       .cookie(new Cookie(JWT_TOKEN, administratorToken)))
               .andExpect(status().isOk());
    }

    /**
     * The core privilege escalation: a member must not be able to create a membership for themselves, which
     * would bypass the paid membership requirement for joining events.
     */
    @Test
    void createMembershipAsMemberFail() throws Exception {
        mockMvc.perform(post(MEMBERSHIPS)
                       .cookie(new Cookie(JWT_TOKEN, memberToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(membershipJson(member.getId())))
               .andExpect(status().isForbidden());
    }

    @Test
    void createMembershipAsOrganizerFail() throws Exception {
        mockMvc.perform(post(MEMBERSHIPS)
                       .cookie(new Cookie(JWT_TOKEN, organizerToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(membershipJson(organizer.getId())))
               .andExpect(status().isForbidden());
    }

    @Test
    void updateMembershipAsMemberFail() throws Exception {
        mockMvc.perform(put(MEMBERSHIPS)
                       .cookie(new Cookie(JWT_TOKEN, memberToken))
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(membershipJson(member.getId())))
               .andExpect(status().isForbidden());
    }

    @Test
    void getMembershipByIdAsMemberFail() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS + "/1")
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Horizontal escalation
    // ------------------------------------------------------------------

    @Test
    void getMembershipsForOwnUserOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS + "/user/{userId}", member.getId())
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isOk());
    }

    @Test
    void getMembershipsForAnotherUserFail() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS + "/user/{userId}", otherMember.getId())
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void getMembershipsForAnotherUserAsOrganizerFail() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS + "/user/{userId}", member.getId())
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void getMembershipsForAnotherUserAsAdministratorOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS + "/user/{userId}", member.getId())
                       .cookie(new Cookie(JWT_TOKEN, administratorToken)))
               .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Other privileged surfaces
    // ------------------------------------------------------------------

    @Test
    void getAuditEventsAsMemberFail() throws Exception {
        mockMvc.perform(get(AUDITS)
                       .param("page", "0")
                       .param("pageSize", "10")
                       .param("sorting", "createdAt,descend")
                       .param("filter", "")
                       .param("filterColumn", "")
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAuditEventsAsOrganizerFail() throws Exception {
        mockMvc.perform(get(AUDITS)
                       .param("page", "0")
                       .param("pageSize", "10")
                       .param("sorting", "createdAt,descend")
                       .param("filter", "")
                       .param("filterColumn", "")
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAggregateStatsAsMemberFail() throws Exception {
        mockMvc.perform(get(STATS + "/yearly-aggregates")
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void downloadDivesAsMemberFail() throws Exception {
        mockMvc.perform(get(DATA_DOWNLOAD + "/dives")
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void downloadDivesAsOrganizerFail() throws Exception {
        mockMvc.perform(get(DATA_DOWNLOAD + "/dives")
                       .cookie(new Cookie(JWT_TOKEN, organizerToken)))
               .andExpect(status().isForbidden());
    }

    @Test
    void getTagGroupsAsMemberFail() throws Exception {
        mockMvc.perform(get(TAG_GROUPS)
                       .cookie(new Cookie(JWT_TOKEN, memberToken)))
               .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Token handling
    // ------------------------------------------------------------------

    /**
     * A forged cookie must not be accepted, otherwise every authorization rule above is decorative.
     */
    @Test
    void privilegedEndpointWithForgedTokenFail() throws Exception {
        var forged = administratorToken.substring(0, administratorToken.lastIndexOf('.') + 1) + "Zm9yZ2VkLXNpZ25hdHVyZQ";

        mockMvc.perform(get(MEMBERSHIPS)
                       .cookie(new Cookie(JWT_TOKEN, forged)))
               .andExpect(status().isForbidden());
    }

    /**
     * The role list comes from the database at authentication time, not from the token, so a token cannot
     * carry an elevated role. This asserts the practical consequence.
     */
    @Test
    void memberTokenDoesNotCarryAdminAuthorityOk() {
        assertFalse(memberToken.isBlank());

        mockMvcExpectForbidden(MEMBERSHIPS, memberToken);
    }

    private void mockMvcExpectForbidden(String path, String token) {
        try {
            mockMvc.perform(get(path)
                           .cookie(new Cookie(JWT_TOKEN, token)))
                   .andExpect(status().isForbidden());
        } catch (Exception e) {
            throw new AssertionError("Request to " + path + " failed unexpectedly", e);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * A fully populated request body. Message conversion runs before method security, so an incomplete body
     * would produce a 400 and the authorization rule would never be exercised.
     */
    private String membershipJson(long userId) {
        return """
                {"id": 0, "userId": %d, "status": "ACTIVE", "type": "PERIODICAL", "startDate": "2024-01-01", "endDate": "2024-12-31"}
                """.formatted(userId);
    }

    private User createUser(RoleEnum roleEnum) {
        var username = "owasp-" + Instant.now()
                                         .toEpochMilli() + "-" + Math.abs((int) (Math.random() * 1_000_000)) + "@example.tld";
        var user = userRepository.save(User.builder()
                                           .username(username)
                                           .password("password")
                                           .firstName("Owasp")
                                           .lastName("Tester")
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
        assertFalse(role.isEmpty(), "Role " + roleEnum + " must exist");
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
}
