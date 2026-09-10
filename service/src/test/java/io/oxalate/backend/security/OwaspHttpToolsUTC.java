package io.oxalate.backend.security;

import io.oxalate.backend.tools.HttpTools;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * OWASP A07:2025 (brute force protection) and A09:2025 (log integrity).
 * <p>
 * {@code LoginAttemptService} locks an account source out after ten failed logins, and every audit record
 * stores the caller's IP. Both use {@link HttpTools#getRemoteIp}. The original implementation returned the
 * first entry of the {@code X-Forwarded-For} header whenever the header was present, which the client fully
 * controls: sending a new value on each attempt reset the lockout counter every time, and any audit entry
 * could be attributed to an arbitrary address.
 * <p>
 * These tests pin the fix: the address always comes from the servlet container. When Oxalate runs behind a
 * reverse proxy, {@code server.forward-headers-strategy: native} lets Tomcat's RemoteIpValve rewrite
 * {@code getRemoteAddr()} from the header, but only for requests arriving from a trusted internal proxy.
 */
@DisplayName("OWASP A07/A09: the client IP cannot be spoofed by a header")
class OwaspHttpToolsUTC {

    @Test
    void getRemoteIpIgnoresForwardedHeaderOk() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertEquals("10.1.2.3", HttpTools.getRemoteIp(request),
                "The X-Forwarded-For header is attacker controlled and must not determine the client IP");
    }

    @Test
    void getRemoteIpIsStableAcrossRotatingForwardedHeadersOk() {
        var first = new MockHttpServletRequest();
        first.setRemoteAddr("10.1.2.3");
        first.addHeader("X-Forwarded-For", "1.1.1.1");

        var second = new MockHttpServletRequest();
        second.setRemoteAddr("10.1.2.3");
        second.addHeader("X-Forwarded-For", "2.2.2.2, 3.3.3.3");

        assertEquals(HttpTools.getRemoteIp(first), HttpTools.getRemoteIp(second),
                "Rotating the forwarded header must not produce a new throttling key");
    }

    @Test
    void getRemoteIpIgnoresOtherForwardingHeadersOk() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Real-IP", "203.0.113.9");
        request.addHeader("Forwarded", "for=203.0.113.9");
        request.addHeader("X-Client-IP", "203.0.113.9");

        assertNotEquals("203.0.113.9", HttpTools.getRemoteIp(request));
        assertEquals("10.1.2.3", HttpTools.getRemoteIp(request));
    }

    @Test
    void getRemoteIpWithoutRequestOk() {
        assertEquals("unknown", HttpTools.getRemoteIp(null), "A missing request must not throw");
    }

    @Test
    void getRemoteIpWithoutAddressOk() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        assertEquals("unknown", HttpTools.getRemoteIp(request));
    }
}
