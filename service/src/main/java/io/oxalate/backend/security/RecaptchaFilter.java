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
import java.util.UUID;
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

        if (request.getMethod()
                   .equals("POST") && request.getRequestURI()
                                             .equals(API + "/auth/login")) {
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
            log.debug("RecaptchaFilter.doFilterInternal: Not a login attempt");
        }

        filterChain.doFilter(request, response);
    }
}
