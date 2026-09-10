package io.oxalate.backend.security;

import io.oxalate.backend.AbstractIntegrationTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * OWASP A02:2025 (Security Misconfiguration), A01:2025 (CSRF) and A04:2025 (Cryptographic Failures).
 * <p>
 * Verifies the hardening that lives in {@code WebSecurityConfig} against real responses from the running
 * application, rather than by reading the configuration back. Header and CSRF regressions are easy to
 * introduce while refactoring the filter chain and are invisible until exploited.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("OWASP A02: responses are hardened and cross-site writes are blocked")
class OwaspSecurityHeadersRTC extends AbstractIntegrationTest {

    private static final String PUBLIC_ENDPOINT = "/api/configurations/frontend";

    private static final String CROSS_ORIGIN_REJECTION = "Cross-origin request rejected";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                 .apply(springSecurity())
                                 .build();
    }

    @Test
    void responseCarriesContentSecurityPolicyOk() throws Exception {
        mockMvc.perform(get(PUBLIC_ENDPOINT))
               .andExpect(status().isOk())
               .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'none'")))
               .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
    }

    @Test
    void responseDeniesFramingOk() throws Exception {
        mockMvc.perform(get(PUBLIC_ENDPOINT))
               .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void responseDisablesContentTypeSniffingOk() throws Exception {
        mockMvc.perform(get(PUBLIC_ENDPOINT))
               .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void responseSetsReferrerPolicyOk() throws Exception {
        mockMvc.perform(get(PUBLIC_ENDPOINT))
               .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void responseSetsPermissionsPolicyOk() throws Exception {
        var permissionsPolicy = mockMvc.perform(get(PUBLIC_ENDPOINT))
                                       .andReturn()
                                       .getResponse()
                                       .getHeader("Permissions-Policy");

        assertNotNull(permissionsPolicy, "A Permissions-Policy header must be present");
        assertTrue(permissionsPolicy.contains("camera=()"), "Device access must be denied by default");
    }

    /**
     * HSTS is only meaningful, and only emitted, over TLS.
     */
    @Test
    void secureResponseSetsStrictTransportSecurityOk() throws Exception {
        mockMvc.perform(get(PUBLIC_ENDPOINT)
                       .secure(true))
               .andExpect(header().string("Strict-Transport-Security", org.hamcrest.Matchers.containsString("max-age=31536000")));
    }

    @Test
    void responseDoesNotLeakServerDetailsOk() throws Exception {
        var response = mockMvc.perform(get(PUBLIC_ENDPOINT))
                              .andReturn()
                              .getResponse();

        assertEquals(null, response.getHeader("X-Powered-By"), "The stack must not be advertised");
    }

    // ------------------------------------------------------------------
    // CSRF
    // ------------------------------------------------------------------

    /**
     * A cross-site write is stopped twice over: Spring's CORS filter rejects the unknown {@code Origin} first,
     * and {@link CsrfOriginValidationFilter} would reject it otherwise. Only the outcome is asserted here; the
     * filter's own behaviour is pinned in {@code OwaspCsrfOriginValidationFilterUTC}.
     */
    @Test
    void stateChangingRequestFromForeignOriginFail() throws Exception {
        mockMvc.perform(post("/api/memberships")
                       .header("Origin", "https://evil.example.tld")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("{}"))
               .andExpect(status().isForbidden());
    }

    /**
     * A classic cross-site form post carries no {@code Origin} that CORS would reject, so this case is caught
     * only by the referer fallback in {@link CsrfOriginValidationFilter}.
     */
    @Test
    void stateChangingRequestFromForeignRefererFail() throws Exception {
        var response = mockMvc.perform(post("/api/memberships")
                                      .header("Referer", "https://evil.example.tld/csrf.html")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content("{}"))
                              .andExpect(status().isForbidden())
                              .andReturn()
                              .getResponse();

        assertEquals(CROSS_ORIGIN_REJECTION, response.getErrorMessage());
    }

    /**
     * The configured frontend origin must still be able to write, otherwise the defence has broken the
     * application. The request is still denied because it is unauthenticated, but it must be denied by
     * authorization rather than by the origin filter.
     */
    @Test
    void stateChangingRequestFromAllowedOriginReachesAuthorizationOk() throws Exception {
        var response = mockMvc.perform(post("/api/memberships")
                                      .header("Origin", "http://localhost:3000")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content("{}"))
                              .andReturn()
                              .getResponse();

        assertTrue(!CROSS_ORIGIN_REJECTION.equals(response.getErrorMessage()),
                "A request from the configured frontend origin must not be blocked as cross-origin");
    }

    // ------------------------------------------------------------------
    // Management endpoints
    // ------------------------------------------------------------------

    /**
     * {@code /actuator/**} used to be entirely {@code permitAll}. Enabling a new management endpoint would then
     * have exposed it without anyone noticing.
     */
    @Test
    void actuatorEnvironmentEndpointIsNotPublicFail() throws Exception {
        mockMvc.perform(get("/actuator/env"))
               .andExpect(status().isForbidden());
    }

    @Test
    void actuatorHeapDumpIsNotPublicFail() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
               .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Password hashing
    // ------------------------------------------------------------------

    @Test
    void passwordEncoderUsesConfiguredBcryptStrengthOk() {
        var hash = passwordEncoder.encode("a-password-for-hashing");

        assertTrue(hash.startsWith("$2a$" + WebSecurityConfig.BCRYPT_STRENGTH + "$"),
                "Passwords must be hashed with the configured bcrypt work factor, got: " + hash.substring(0, 7));
        assertTrue(passwordEncoder.matches("a-password-for-hashing", hash));
    }

    /**
     * Raising the work factor must not lock out accounts whose hashes were written with the old cost, because
     * bcrypt encodes the cost in the hash itself.
     */
    @Test
    void passwordEncoderStillVerifiesLegacyHashesOk() {
        var legacyHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(10).encode("password");

        assertTrue(legacyHash.startsWith("$2a$10$"), "The fixture must represent the previous work factor");
        assertTrue(passwordEncoder.matches("password", legacyHash), "Existing cost 10 hashes must keep verifying");
    }
}
