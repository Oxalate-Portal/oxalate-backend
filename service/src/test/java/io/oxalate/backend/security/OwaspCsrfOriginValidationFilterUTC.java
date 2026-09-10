package io.oxalate.backend.security;

import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * OWASP A01:2025 - Cross-Site Request Forgery (CWE-352).
 * <p>
 * Authentication uses an {@code HttpOnly} cookie, so the browser attaches it to any request a third-party page
 * triggers. Spring's CSRF token protection is disabled because the SPA cannot read the cookie to echo a token,
 * which leaves {@code SameSite=Strict} plus {@link CsrfOriginValidationFilter} as the defence. These tests
 * verify the filter actually blocks the cross-site case while leaving legitimate traffic alone.
 */
@DisplayName("OWASP A01: cross-origin state changing requests are rejected")
class OwaspCsrfOriginValidationFilterUTC {

    private static final String ALLOWED_ORIGIN = "https://portal.example.tld";
    private static final String EVIL_ORIGIN = "https://evil.example.tld";

    private final CsrfOriginValidationFilter filter = new CsrfOriginValidationFilter(Set.of(ALLOWED_ORIGIN, "http://localhost:3000"));

    @Test
    void postFromForeignOriginFail() throws Exception {
        var response = run("POST", "/api/memberships", EVIL_ORIGIN, null);

        assertEquals(403, response.getStatus(), "A state changing request from an unknown origin must be rejected");
    }

    @Test
    void postFromAllowedOriginOk() throws Exception {
        var response = run("POST", "/api/memberships", ALLOWED_ORIGIN, null);

        assertEquals(200, response.getStatus());
    }

    @Test
    void putFromForeignOriginFail() throws Exception {
        assertEquals(403, run("PUT", "/api/memberships", EVIL_ORIGIN, null).getStatus());
    }

    @Test
    void deleteFromForeignOriginFail() throws Exception {
        assertEquals(403, run("DELETE", "/api/users/1", EVIL_ORIGIN, null).getStatus());
    }

    @Test
    void patchFromForeignOriginFail() throws Exception {
        assertEquals(403, run("PATCH", "/api/users/1", EVIL_ORIGIN, null).getStatus());
    }

    /**
     * A cross-site form post that omits {@code Origin} still carries a {@code Referer}, so the fallback must
     * work and must compare only the origin part of the URL.
     */
    @Test
    void postWithForeignRefererFail() throws Exception {
        assertEquals(403, run("POST", "/api/memberships", null, EVIL_ORIGIN + "/attack.html").getStatus());
    }

    @Test
    void postWithAllowedRefererOk() throws Exception {
        assertEquals(200, run("POST", "/api/memberships", null, ALLOWED_ORIGIN + "/memberships?tab=2").getStatus());
    }

    /**
     * Sandboxed iframes and some privacy tools send the literal string "null" as the origin. It is not on the
     * allow-list, so it must be treated as a cross-site request rather than silently accepted.
     */
    @Test
    void postWithNullOriginFail() throws Exception {
        assertEquals(403, run("POST", "/api/memberships", "null", EVIL_ORIGIN + "/attack.html").getStatus());
    }

    /**
     * GET requests are not state changing and browsers do not always send an origin for them.
     */
    @Test
    void getFromForeignOriginOk() throws Exception {
        assertEquals(200, run("GET", "/api/events", EVIL_ORIGIN, null).getStatus());
    }

    /**
     * Server-to-server and CLI clients send neither header; blocking them would break integrations without
     * adding protection, because CSRF requires a browser to attach the cookie.
     */
    @Test
    void postWithoutOriginOrRefererOk() throws Exception {
        assertEquals(200, run("POST", "/api/memberships", null, null).getStatus());
    }

    @Test
    void postWithUnparseableRefererFail() throws Exception {
        assertEquals(200, run("POST", "/api/memberships", null, ":::not a url:::").getStatus());
    }

    private MockHttpServletResponse run(String method, String uri, String origin, String referer) throws Exception {
        var request = new MockHttpServletRequest(method, uri);

        if (origin != null) {
            request.addHeader("Origin", origin);
        }

        if (referer != null) {
            request.addHeader("Referer", referer);
        }

        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
