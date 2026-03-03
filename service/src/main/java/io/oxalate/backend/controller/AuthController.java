package io.oxalate.backend.controller;

import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.request.EmailRequest;
import io.oxalate.backend.api.request.LoginRequest;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.api.request.TokenRequest;
import io.oxalate.backend.api.request.UserResetPasswordRequest;
import io.oxalate.backend.api.request.UserUpdatePasswordRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.RegistrationResponse;
import io.oxalate.backend.api.response.UserSessionToken;
import io.oxalate.backend.api.response.UserUpdateStatus;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOGOUT_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOGOUT_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_START;
import io.oxalate.backend.rest.AuthAPI;
import io.oxalate.backend.service.AuthService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("AuthController")
public class AuthController implements AuthAPI {

    private final AuthService authService;
    private final UserService userService;

    @Override
    @Audited(startMessage = AUTH_AUTHENTICATION_START, okMessage = AUTH_AUTHENTICATION_OK)
    public ResponseEntity<UserSessionToken> authenticateUser(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        // OxalateAuthenticationException is caught by the AuditAspect, which publishes the audit event and returns the error response
        var jwtResponse = authService.authenticate(loginRequest, request, response);
        return ResponseEntity.ok(jwtResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = AUTH_UPDATE_PASSWORD_START, okMessage = AUTH_UPDATE_PASSWORD_OK)
    public ResponseEntity<UserUpdateStatus> updateUserPassword(long userId, UserUpdatePasswordRequest updatePasswordRequest, HttpServletRequest request) {
        // OxalateAuthenticationException is caught by the AuditAspect
        var status = authService.updateUserPassword(userId, updatePasswordRequest, request);
        return ResponseEntity.ok(status);
    }

    @Override
    @Audited(startMessage = AUTH_RESEND_EMAIL_START, okMessage = AUTH_RESEND_EMAIL_OK)
    public ResponseEntity<ActionResponse> resendConfirmationEmail(TokenRequest tokenRequest, HttpServletRequest request) {
        var response = authService.resendConfirmationEmail(tokenRequest, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @Audited(startMessage = AUTH_LOST_PASSWORD_START, okMessage = AUTH_LOST_PASSWORD_START)
    public ResponseEntity<ActionResponse> lostPassword(EmailRequest emailRequest, HttpServletRequest request) {
        var response = authService.lostPassword(emailRequest, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @Audited(startMessage = AUTH_RESET_PASSWORD_START, okMessage = AUTH_RESET_PASSWORD_OK)
    public ResponseEntity<ActionResponse> resetPassword(UserResetPasswordRequest userResetPasswordRequest, HttpServletRequest request) {
        var response = authService.resetPassword(userResetPasswordRequest, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @Audited(startMessage = AUTH_REGISTRATION_START, okMessage = AUTH_REGISTRATION_OK)
    public ResponseEntity<RegistrationResponse> registerUser(SignupRequest signupRequest, HttpServletRequest request) {
        var response = authService.registerUser(signupRequest, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @Audited(startMessage = AUTH_REGISTRATION_VERIFY_START, okMessage = AUTH_REGISTRATION_VERIFY_OK)
    public ResponseEntity<Void> verifyRegistration(String token, HttpServletRequest request) {
        var uri = authService.verifyRegistration(token, request);
        return ResponseEntity.status(301)
                             .location(uri)
                             .build();
    }

    @Override
    @Audited(startMessage = AUTH_LOGOUT_START, okMessage = AUTH_LOGOUT_OK)
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        var userId = AuthTools.getCurrentUserId();
        userService.logoutUser(userId);
        // Clear the JWT cookie
        ResponseCookie cookie = ResponseCookie.from(JWT_TOKEN, "")
                                              .httpOnly(true)
                                              .path("/")
                                              .maxAge(0)
                                              .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok()
                             .build();
    }
}
