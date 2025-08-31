package io.oxalate.backend.security.jwt;

import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.security.service.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    private final AppEventPublisher appEventPublisher;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            org.springframework.security.core.AuthenticationException authException)
            throws IOException {

        log.error("Unauthorized error for {}: {}", request.getRequestURI(), authException.getMessage());
        UserDetailsImpl userPrincipal = (UserDetailsImpl) request.getUserPrincipal();
        if (userPrincipal != null) {
            appEventPublisher.publishAuditEvent("User " + userPrincipal.getUsername() + " does not have permission to access: " + request.getRequestURI(), INFO,
                    request, "AuthController",
                    userPrincipal.getId());
        } else {
            appEventPublisher.publishAuditEvent("Unauthorized access attempt on: " + request.getRequestURI(), INFO, request, "AuthController", null);
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
    }
}
