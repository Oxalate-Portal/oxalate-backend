package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevelEnum.ERROR;
import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import static io.oxalate.backend.api.AuditLevelEnum.WARN;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.EmailRequest;
import io.oxalate.backend.api.request.LoginRequest;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.api.request.TokenRequest;
import io.oxalate.backend.api.request.UserResetPasswordRequest;
import io.oxalate.backend.api.request.UserUpdatePasswordRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.RegistrationResponse;
import io.oxalate.backend.api.response.UserUpdateStatus;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOGOUT_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOGOUT_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_NEW_SAME_AS_OLD;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_UNAUTHORIZED;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.exception.OxalateAuthenticationException;
import io.oxalate.backend.rest.AuthAPI;
import io.oxalate.backend.service.AuthService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class AuthController implements AuthAPI {

    private static final String AUDIT_NAME = "AuthController";

    private final AuthService authService;
    private final UserService userService;
    private final AppEventPublisher appEventPublisher;

    @Value("${oxalate.app.jwt-secure}")
    private boolean secureCookie;

    @Override
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_AUTHENTICATION_START + loginRequest.getUsername(), INFO, request, AUDIT_NAME, -1L);

        try {
            var jwtResponse = authService.authenticate(loginRequest, request, response);
            appEventPublisher.publishAuditEvent(AUTH_AUTHENTICATION_OK + loginRequest.getUsername(), INFO, request, AUDIT_NAME, jwtResponse.getId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(jwtResponse);
        } catch (OxalateAuthenticationException e) {
            appEventPublisher.publishAuditEvent(e.getAuditMessage(), e.getAuditLevel(), request, e.getAuditSource(), e.getUserId(), auditUuid);
            return ResponseEntity.status(e.getHttpErrorStatus())
                                 .body(null);
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_LOGOUT_START, INFO, request, AUDIT_NAME, userId);
        userService.logoutUser(userId);

        var cookie = ResponseCookie.from(JWT_TOKEN, "")
                                   .httpOnly(true)
                                   .secure(secureCookie)
                                   .path("/")
                                   .maxAge(0)
                                   .sameSite("Strict")
                                   .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        SecurityContextHolder.clearContext();

        appEventPublisher.publishAuditEvent(AUTH_LOGOUT_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.noContent()
                             .build();
    }

    @Override
    public ResponseEntity<RegistrationResponse> registerUser(SignupRequest signupRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_START + signupRequest.getUsername(), INFO, request, AUDIT_NAME, -1L);
        var response = authService.registerUser(signupRequest, request, auditUuid);
        var httpStatus = response.getStatus()
                                 .equals(UpdateStatusEnum.OK) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        // Make sure the username does not have any forbidden characters
        return ResponseEntity.status(httpStatus)
                             .body(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<UserUpdateStatus> updateUserPassword(long userId, UserUpdatePasswordRequest updatePasswordRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        // Only the user can change their own password
        if (userId != AuthTools.getCurrentUserId()) {
            appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_UNAUTHORIZED + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.error("User ID {} attempted to change password for user ID {}", AuthTools.getCurrentUserId(), userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(null);
        }

        try {
            var userUpdateStatus = authService.updateUserPassword(userId, updatePasswordRequest, request);

            if (userUpdateStatus.getStatus()
                                .equals(UpdateStatusEnum.OK)) {
                appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
                return ResponseEntity.status(HttpStatus.OK)
                                     .body(userUpdateStatus);
            } else if (userUpdateStatus.getStatus()
                                       .equals(UpdateStatusEnum.FAIL)) {
                appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_NEW_SAME_AS_OLD + userId, WARN, request, AUDIT_NAME, userId, auditUuid);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                     .body(userUpdateStatus);
            } else {
                appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_UNAUTHORIZED + userId, WARN, request, AUDIT_NAME, userId, auditUuid);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .body(userUpdateStatus);
            }
        } catch (OxalateAuthenticationException e) {
            appEventPublisher.publishAuditEvent(e.getAuditMessage(), e.getAuditLevel(), request, e.getAuditSource(), e.getUserId(), auditUuid);
            return ResponseEntity.status(e.getHttpErrorStatus())
                                 .body(null);
        }
    }

    @Override
    public ResponseEntity<Void> verifyRegistration(String token, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_VERIFY_START, INFO, request, AUDIT_NAME, null);
        var uri = authService.verifyRegistration(token, request, auditUuid);

        return ResponseEntity.status(HttpStatus.FOUND)
                             .location(uri)
                             .build();
    }

    @Override
    public ResponseEntity<ActionResponse> resendConfirmationEmail(TokenRequest tokenRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_START, INFO, request, AUDIT_NAME, null);
        var actionResponse = authService.resendConfirmationEmail(tokenRequest, request, auditUuid);
        appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_OK, INFO, request, AUDIT_NAME, -1L, auditUuid);

        if (actionResponse.getStatus()
                          .equals(UpdateStatusEnum.OK)) {
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(actionResponse);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(actionResponse);
    }

    // We always return OK to avoid user enumeration
    @Override
    public ResponseEntity<ActionResponse> lostPassword(EmailRequest emailRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_START + emailRequest.getEmail(), INFO, request, AUDIT_NAME, null);
        var response = authService.lostPassword(emailRequest, request, auditUuid);

        return ResponseEntity.status(HttpStatus.OK)
                             .body(response);
    }

    @Override
    public ResponseEntity<ActionResponse> resetPassword(UserResetPasswordRequest userResetPasswordRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_START, INFO, request, AUDIT_NAME, null);
        var response = authService.resetPassword(userResetPasswordRequest, request, auditUuid);

        if (response.getStatus() == UpdateStatusEnum.OK) {
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(response);
        }
    }
}
