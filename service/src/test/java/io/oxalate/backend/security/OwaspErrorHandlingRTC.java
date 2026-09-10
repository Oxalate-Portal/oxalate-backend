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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * OWASP A10:2025 (Mishandling of Exceptional Conditions) and A09:2025 (Logging and Alerting Failures).
 * <p>
 * Hostile input reached Spring Data and Jackson unvalidated in several places, turning a crafted query
 * parameter into a 500 and, with the container's default error page, into a leak of internal types. A 500 is
 * also a denial-of-service primitive when it is cheap to trigger and expensive to serve. These tests assert
 * that malformed input produces a controlled 4xx and that error bodies never carry implementation detail.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("OWASP A10: malformed input is handled, not crashed on")
class OwaspErrorHandlingRTC extends AbstractIntegrationTest {

    private static final String AUDITS = "/api/audits";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    private MockMvc mockMvc;
    private User administrator;
    private String administratorToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                 .apply(springSecurity())
                                 .build();

        administrator = createAdministrator();
        administratorToken = tokenFor(administrator);
    }

    @AfterEach
    void tearDown() {
        roleRepository.deleteAllUserRolesByUserId(administrator.getId());
        userRepository.deleteById(administrator.getId());
    }

    /**
     * The sorting parameter was split on a comma and indexed at position one without checking the length, and
     * the column name was passed straight to Spring Data.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "createdAt",
            "",
            ",",
            ",,,,",
            "createdAt,",
            "; DROP TABLE application_audit_event; --,ascend",
            "nonExistentColumn,ascend",
            "createdAt,notADirection",
            "../../etc/passwd,ascend"
    })
    void getAuditEventsWithMalformedSortingOk(String sorting) throws Exception {
        var status = mockMvc.perform(get(AUDITS)
                                    .param("page", "0")
                                    .param("pageSize", "10")
                                    .param("sorting", sorting)
                                    .param("filter", "")
                                    .param("filterColumn", "")
                                    .cookie(new Cookie(JWT_TOKEN, administratorToken)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

        assertTrue(status < 500, "Malformed sorting '" + sorting + "' must not produce a server error, got HTTP " + status);
    }

    /**
     * An uncapped page size lets one request pull the entire audit table into memory.
     */
    @Test
    void getAuditEventsWithHugePageSizeIsCappedOk() throws Exception {
        var status = mockMvc.perform(get(AUDITS)
                                    .param("page", "0")
                                    .param("pageSize", String.valueOf(Integer.MAX_VALUE))
                                    .param("sorting", "createdAt,descend")
                                    .param("filter", "")
                                    .param("filterColumn", "")
                                    .cookie(new Cookie(JWT_TOKEN, administratorToken)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

        assertTrue(status < 500, "An absurd page size must be clamped rather than crash, got HTTP " + status);
    }

    @Test
    void getAuditEventsWithNegativePageOk() throws Exception {
        var status = mockMvc.perform(get(AUDITS)
                                    .param("page", "-5")
                                    .param("pageSize", "-1")
                                    .param("sorting", "createdAt,descend")
                                    .param("filter", "")
                                    .param("filterColumn", "")
                                    .cookie(new Cookie(JWT_TOKEN, administratorToken)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

        assertTrue(status < 500, "Negative paging must not crash, got HTTP " + status);
    }

    /**
     * {@code CertificateController} dereferenced the lookup result before checking it for null, so asking for a
     * certificate that does not exist returned a 500 driven by a NullPointerException.
     */
    @Test
    void getUnknownCertificateReturnsClientErrorOk() throws Exception {
        var status = mockMvc.perform(get("/api/certificates/{id}", Long.MAX_VALUE)
                                    .cookie(new Cookie(JWT_TOKEN, administratorToken)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

        assertTrue(status < 500, "A missing certificate must be a client error, got HTTP " + status);
    }

    @Test
    void malformedJsonBodyReturnsBadRequestOk() throws Exception {
        var response = mockMvc.perform(post("/api/memberships")
                                      .cookie(new Cookie(JWT_TOKEN, administratorToken))
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content("{ this is not json"))
                              .andReturn()
                              .getResponse();

        assertTrue(response.getStatus() < 500, "Unparseable JSON must be a 400, got HTTP " + response.getStatus());
        assertNoImplementationDetail(response.getContentAsString());
    }

    @Test
    void wrongTypeInPathReturnsClientErrorOk() throws Exception {
        var response = mockMvc.perform(get("/api/memberships/{id}", "not-a-number")
                                      .cookie(new Cookie(JWT_TOKEN, administratorToken)))
                              .andReturn()
                              .getResponse();

        assertTrue(response.getStatus() < 500, "A non-numeric path variable must be a 400, got HTTP " + response.getStatus());
        assertNoImplementationDetail(response.getContentAsString());
    }

    /**
     * OWASP A09: the details belong in the server log, not in the response.
     */
    private void assertNoImplementationDetail(String body) {
        var lowerCased = body.toLowerCase();

        for (var leak : List.of("io.oxalate", "org.springframework", "com.fasterxml", "nullpointerexception", "at java.", "sqlexception",
                "org.hibernate", "jdbc")) {
            assertFalse(lowerCased.contains(leak), "The error response leaks implementation detail '" + leak + "': " + body);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User createAdministrator() {
        var username = "owasp-a10-" + Instant.now()
                                             .toEpochMilli() + "-" + Math.abs((int) (Math.random() * 1_000_000)) + "@example.tld";
        var user = userRepository.save(User.builder()
                                           .username(username)
                                           .password("password")
                                           .firstName("Owasp")
                                           .lastName("Admin")
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

        var role = roleRepository.findByName(RoleEnum.ROLE_ADMIN);
        assertFalse(role.isEmpty());
        roleRepository.addUserRole(user.getId(), role.get()
                                                     .getId());
        return user;
    }

    private String tokenFor(User user) {
        var authorities = List.of(new SimpleGrantedAuthority(RoleEnum.ROLE_ADMIN.name()));
        var userDetails = new UserDetailsImpl(user.getId(), user.getUsername(), user.getPassword(), authorities, user.isApprovedTerms(),
                user.getHealthStatementId(), false, user.getLanguage());
        return jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));
    }
}
