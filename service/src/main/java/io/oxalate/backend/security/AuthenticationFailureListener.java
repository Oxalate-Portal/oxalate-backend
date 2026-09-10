package io.oxalate.backend.security;

import static io.oxalate.backend.tools.HttpTools.getRemoteIp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private LoginAttemptService loginAttemptService;

    /**
     * OWASP A07: the throttling key must match the key used by {@link LoginAttemptService#isBlocked()},
     * and must not be derived from a client-controlled header, otherwise the lockout is bypassed by
     * sending a different {@code X-Forwarded-For} value on every attempt.
     */
    @Override
    public void onApplicationEvent(@NonNull AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptService.loginFailed(getRemoteIp(request));
    }
}
