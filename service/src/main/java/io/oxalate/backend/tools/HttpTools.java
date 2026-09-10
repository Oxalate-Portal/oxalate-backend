package io.oxalate.backend.tools;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP request helpers.
 * <p>
 * OWASP A01/A07/A09: the client IP is used for login throttling
 * ({@code LoginAttemptService}) and is written into the audit trail. It must therefore never be taken
 * from a header the client controls. {@code X-Forwarded-For} is attacker-supplied unless a trusted
 * reverse proxy has set it, so it is deliberately not read here.
 * <p>
 * Instead we rely on {@code server.forward-headers-strategy: native}, which enables Tomcat's
 * {@code RemoteIpValve}. That valve rewrites {@code request.getRemoteAddr()} from
 * {@code X-Forwarded-For} <em>only</em> when the immediate peer is a trusted internal proxy
 * (configurable via {@code server.tomcat.remoteip.*}). Reading {@code getRemoteAddr()} therefore
 * yields the proxy-validated address when deployed behind a proxy, and the real TCP peer otherwise.
 */
public class HttpTools {

    private HttpTools() {
        // Utility class
    }

    /**
     * Returns the client IP address as established by the servlet container.
     * <p>
     * Never derive this from request headers: see the class javadoc.
     *
     * @param request current request, may be {@code null} outside a request scope
     * @return the remote address, or {@code "unknown"} when it cannot be determined
     */
    public static String getRemoteIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        var remoteAddress = request.getRemoteAddr();

        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }

        return remoteAddress;
    }
}

