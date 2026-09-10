package io.oxalate.backend.security;

import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import static io.oxalate.backend.api.AuditLevelEnum.WARN;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.audit.AuditContext;
import io.oxalate.backend.client.api.response.RecaptchaResponse;
import static io.oxalate.backend.events.AppAuditMessages.RECAPTCHA_FILTER_DISABLED;
import static io.oxalate.backend.events.AppAuditMessages.RECAPTCHA_FILTER_EMPTY;
import static io.oxalate.backend.events.AppAuditMessages.RECAPTCHA_FILTER_LOW_SCORE;
import static io.oxalate.backend.events.AppAuditMessages.RECAPTCHA_FILTER_OK;
import static io.oxalate.backend.events.AppAuditMessages.RECAPTCHA_FILTER_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.service.RecaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class RecaptchaFilter extends OncePerRequestFilter {

    private static final String AUDIT_NAME = "RecaptchaFilter";

    /**
     * OWASP A07:2025 - every unauthenticated POST endpoint that can be abused for credential stuffing, account
     * enumeration or mail bombing must be captcha protected, not just the login endpoint.
     */
    static final Set<String> PROTECTED_PATHS = Set.of(
            API + "/auth/login",
            API + "/auth/register",
            API + "/auth/lost-password",
            API + "/auth/reset-password",
            API + "/auth/registrations/resend-confirmation"
    );

    private final RecaptchaService recaptchaService;
    private final AppEventPublisher appEventPublisher;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        if (requiresCaptcha(request)) {
            var traceId = UUID.randomUUID();
            AuditContext.setTraceId(traceId);

            try {
                appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_START, INFO, AUDIT_NAME, -1L);

                var recaptchaToken = request.getHeader("X-Captcha-Token");

                if (recaptchaToken == null || recaptchaToken.isEmpty()) {
                    appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_EMPTY, WARN, AUDIT_NAME, -1L);
                    throw new BadCredentialsException("No reCaptcha token found");
                }

                RecaptchaResponse recaptchaResponse;

                try {
                    recaptchaResponse = recaptchaService.validateToken(recaptchaToken);
                } catch (RuntimeException e) {
                    log.error("RecaptchaFilter.doFilterInternal: Failed to validate captcha token", e);
                    throw new BadCredentialsException("Failed to validate captcha token towards server");
                }

                if (recaptchaResponse != null) {
                    if (!recaptchaResponse.isSuccess() || recaptchaResponse.getScore() < recaptchaService.getCaptchaThreshold()) {
                        appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_LOW_SCORE + recaptchaResponse.getScore(), WARN, AUDIT_NAME, -1L);
                        throw new BadCredentialsException("Invalid reCaptcha token or score too low");
                    } else {
                        appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_OK + recaptchaResponse.getScore(), INFO, AUDIT_NAME, -1L);
                    }
                } else {
                    appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_DISABLED, WARN, AUDIT_NAME, -1L);
                }
            } finally {
                AuditContext.clear();
            }
        } else {
            log.debug("RecaptchaFilter.doFilterInternal: Not a captcha protected endpoint");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * @param request the incoming request
     * @return {@code true} when the request targets an unauthenticated endpoint that requires a captcha
     */
    private boolean requiresCaptcha(HttpServletRequest request) {
        if (!recaptchaService.isCaptchaEnabled()) {
            // Captcha is switched off for this deployment, so demanding a token would only break the flow
            return false;
        }

        return "POST".equals(request.getMethod()) && PROTECTED_PATHS.contains(request.getRequestURI());
    }
}
