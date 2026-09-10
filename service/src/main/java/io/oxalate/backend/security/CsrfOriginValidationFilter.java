package io.oxalate.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * OWASP A01:2025 (CWE-352 Cross-Site Request Forgery).
 * <p>
 * This service authenticates with a JWT carried in a cookie, which the browser attaches automatically.
 * Token-based CSRF protection is disabled because the SPA cannot read the {@code HttpOnly} cookie to
 * echo a token, so the remaining defences are:
 * <ol>
 *     <li>{@code SameSite=Strict} on the {@code JWT_TOKEN} cookie (enforced at startup by
 *     {@code SecurityPreflight}), and</li>
 *     <li>this filter, which implements the OWASP "verifying origin with standard headers" pattern.</li>
 * </ol>
 * For every state-changing request the {@code Origin} header (or {@code Referer} as a fallback) must
 * resolve to one of the configured allowed origins. Browsers always send {@code Origin} on cross-site
 * POST/PUT/PATCH/DELETE, so a forged cross-site request is rejected before it reaches a controller.
 * Requests carrying neither header are not browser-initiated cross-site requests and are allowed
 * through, so server-to-server and CLI clients keep working.
 */
@Slf4j
public class CsrfOriginValidationFilter extends OncePerRequestFilter {

    private static final Set<String> STATE_CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final Set<String> allowedOrigins;

    public CsrfOriginValidationFilter(Set<String> allowedOrigins) {
        this.allowedOrigins = Set.copyOf(allowedOrigins);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!STATE_CHANGING_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        var origin = request.getHeader(HttpHeaders.ORIGIN);

        if (origin == null || origin.isBlank() || "null".equals(origin)) {
            // No Origin: fall back to Referer, which browsers send for same-site navigations and form posts
            origin = toOrigin(request.getHeader(HttpHeaders.REFERER));
        }

        if (origin == null) {
            // Not a browser-initiated cross-site request
            filterChain.doFilter(request, response);
            return;
        }

        if (!allowedOrigins.contains(origin)) {
            log.warn("Rejected {} {} because origin '{}' is not an allowed origin", request.getMethod(), request.getRequestURI(), origin);
            response.sendError(HttpStatus.FORBIDDEN.value(), "Cross-origin request rejected");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Reduces a full URL to its {@code scheme://host[:port]} origin form so it can be compared against
     * the allowed origin list.
     *
     * @param url a referer URL, may be {@code null}
     * @return the origin, or {@code null} when the URL is absent or unparseable
     */
    private String toOrigin(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            var uri = new URI(url);

            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }

            var origin = new StringBuilder(uri.getScheme()).append("://")
                                                           .append(uri.getHost());

            if (uri.getPort() != -1) {
                origin.append(':')
                      .append(uri.getPort());
            }

            return origin.toString();
        } catch (URISyntaxException e) {
            log.warn("Could not parse referer into an origin");
            return null;
        }
    }
}
