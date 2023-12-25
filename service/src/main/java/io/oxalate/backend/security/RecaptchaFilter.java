package io.oxalate.backend.security;

import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class RecaptchaFilter extends OncePerRequestFilter {

    private static final String AUDIT_NAME = "RecaptchaFilter";
    private final RecaptchaService recaptchaService;
    private final AppEventPublisher appEventPublisher;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        // Only run this for login attempts. The token is in the header because it is easier to fetch from there and the body will be marked as read
        // after this filter which is not what we want.
        if (request.getMethod()
                   .equals("POST") && request.getRequestURI()
                                             .equals("/api/auth/login")) {
            var auditUuid = appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_START, INFO, request, AUDIT_NAME, -1L);

            var recaptchaToken = request.getHeader("X-Captcha-Token");

            if (recaptchaToken == null || recaptchaToken.isEmpty()) {
                appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_EMPTY, WARN, request, AUDIT_NAME, -1L, auditUuid);
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
                    appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_LOW_SCORE + recaptchaResponse.getScore(), WARN, request, AUDIT_NAME, -1L, auditUuid);
                    throw new BadCredentialsException("Invalid reCaptcha token or score too low");
                } else {
                    appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_OK + recaptchaResponse.getScore(), INFO, request, AUDIT_NAME, -1L, auditUuid);
                }
            } else {
                appEventPublisher.publishAuditEvent(RECAPTCHA_FILTER_DISABLED, WARN, request, AUDIT_NAME, -1L, auditUuid);
            }
        } else {
            log.debug("RecaptchaFilter.doFilterInternal: Not a login attempt");
        }

        filterChain.doFilter(request, response);
    }
}
