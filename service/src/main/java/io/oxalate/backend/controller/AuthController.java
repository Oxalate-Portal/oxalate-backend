package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.ERROR;
import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import io.oxalate.backend.api.UpdateStatusEnum;
import static io.oxalate.backend.api.UserStatus.ACTIVE;
import static io.oxalate.backend.api.UserStatus.LOCKED;
import static io.oxalate.backend.api.UserStatus.REGISTERED;
import io.oxalate.backend.api.request.EmailRequest;
import io.oxalate.backend.api.request.LoginRequest;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.api.request.TokenRequest;
import io.oxalate.backend.api.request.UserResetPasswordRequest;
import io.oxalate.backend.api.request.UserUpdatePasswordRequest;
import io.oxalate.backend.api.response.JwtResponse;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.RegistrationResponse;
import io.oxalate.backend.api.response.UserUpdateStatus;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_NON_ACTIVE;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_NO_ROLES;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_INACTIVE_STATUS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_TAKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_INVALID_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_EXPIRED_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_INVALID_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_INVALID_USER;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_USED_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_EXPIRED_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_FAIL_REQUIREMENTS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_INACTIVE_STATUS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_INVALID_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_INVALID_USER;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_MISMATCH;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_UNKNOWN_ERROR;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_FAIL_REQUIREMENTS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_INACTIVE_STATUS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_NEW_MISMATCH;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_NEW_SAME_AS_OLD;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_OLD_MISMATCH;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_START;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_UNAUTHORIZED;
import io.oxalate.backend.events.AppEventPublisher;
import static io.oxalate.backend.model.TokenType.EMAIL_RESEND;
import static io.oxalate.backend.model.TokenType.PASSWORD_RESET;
import static io.oxalate.backend.model.TokenType.REGISTRATION;
import io.oxalate.backend.model.User;
import io.oxalate.backend.rest.AuthAPI;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import io.oxalate.backend.service.AuthService;
import io.oxalate.backend.service.EmailService;
import io.oxalate.backend.service.RegistrationService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@RestController
public class AuthController implements AuthAPI {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final EmailService emailService;
    private final UserService userService;
    private final RegistrationService registrationService;
    private final JwtUtils jwtUtils;
    private final AppEventPublisher appEventPublisher;

    private static final String AUDIT_NAME = "AuthController";

    @Value("${oxalate.token.registration-url}")
    private String registrationUrl;

    @Value("${oxalate.app.jwt-expiration-ms}")
    private long expirationTime;

    private static final String JSON_MESSAGE_OK = "{\"message\": \"OK\"}";
    private static final String JSON_MESSAGE_ERROR = "{\"message\": \"ERROR\"}";

    @Override
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest, HttpServletRequest request) {
        Authentication authentication;
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_AUTHENTICATION_START + loginRequest.getUsername(), INFO, request, AUDIT_NAME,
                -1L);

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            log.warn("User {} attempted to log in but the authentication failed: {}", loginRequest.getUsername(), e.getMessage());
            appEventPublisher.publishAuditEvent(AUTH_AUTHENTICATION_FAIL + loginRequest.getUsername() + " message " + e.getMessage(), ERROR, request,
                    AUDIT_NAME, -1L, auditUuid);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(null);
        }

        Optional<User> optionalUser = userService.findByUsername(loginRequest.getUsername());
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            log.debug("User {} logged in. Got: {}", user.getUsername(), user);
        } else {
            log.warn("User {} attempted to log in but the account does not exist.", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(null);
        }

        if (!user.getStatus()
                 .equals(ACTIVE)) {
            log.info("User {} attempted to log in while having non-active status {}", user.getUsername(), user.getStatus());
            appEventPublisher.publishAuditEvent(AUTH_AUTHENTICATION_NON_ACTIVE + loginRequest.getUsername(), ERROR, request, AUDIT_NAME, user.getId(),
                    auditUuid);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(null);
        }

        if (user.getRoles() == null || user.getRoles()
                                           .isEmpty()) {
            log.info("User {} attempted to log in but the account has no roles.", loginRequest.getUsername());
            appEventPublisher.publishAuditEvent(AUTH_AUTHENTICATION_NO_ROLES + loginRequest.getUsername(), ERROR, request, AUDIT_NAME, user.getId(),
                    auditUuid);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(null);
        }

        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities()
                                        .stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .toList();

        var paymentResponses = new HashSet<PaymentResponse>();

        for (var payment : user.getPayments()) {
            paymentResponses.add(payment.toPaymentResponse());
        }

        var jwtResponse = JwtResponse.builder()
                                     .id(userDetails.getId())
                                     .username(userDetails.getUsername())
                                     .phoneNumber(user.getPhoneNumber())
                                     .firstName(user.getFirstName())
                                     .lastName(user.getLastName())
                                     .roles(roles)
                                     .status(user.getStatus()
                                                 .toString())
                                     .registered(user.getRegistered())
                                     .type("Bearer")
                                     .accessToken(jwt)
                                     .expiresAt(Instant.now()
                                                       .plus(expirationTime, ChronoUnit.MILLIS))
                                     .approvedTerms(user.isApprovedTerms())
                                     .payments(paymentResponses)
                                     .language(user.getLanguage())
                                     .build();
        appEventPublisher.publishAuditEvent(AUTH_AUTHENTICATION_OK + loginRequest.getUsername(), INFO, request, AUDIT_NAME, user.getId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(jwtResponse);
    }

    @Override
    public ResponseEntity<RegistrationResponse> registerUser(SignupRequest signupRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_START + signupRequest.getUsername(), INFO, request, AUDIT_NAME, -1L);
        // Make sure the username does not have any forbidden characters
        if (!userService.isUsernameValid(signupRequest.getUsername())) {
            log.info("User {} attempted to register but the username is invalid.", signupRequest.getUsername());
            appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_TAKEN + signupRequest.getUsername(), WARN, request, AUDIT_NAME, -1L,
                    auditUuid);

            var response = RegistrationResponse.builder()
                                               .status(UpdateStatusEnum.FAIL)
                                               .token("")
                                               .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(response);
        }

        // Create new user's account
        var user = userService.createNewUser(signupRequest);

        if (user == null) {
            log.info("User {} attempted to register but the registration failed.", signupRequest.getUsername());
            var response = RegistrationResponse.builder()
                                               .status(UpdateStatusEnum.FAIL)
                                               .token("")
                                               .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(response);
        }

        var registrationToken = registrationService.generateToken(user.getId(), REGISTRATION);
        emailService.sendConfirmationEmail(user, registrationToken);

        var resendToken = registrationService.generateToken(user.getId(), EMAIL_RESEND);

        var response = RegistrationResponse.builder()
                                           .status(UpdateStatusEnum.OK)
                                           .token(resendToken)
                                           .build();
        appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_OK + signupRequest.getUsername(), INFO, request, AUDIT_NAME, user.getId(),
                auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(response);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ORGANIZER') or hasRole('ROLE_ADMIN')")
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

        Optional<User> optionalUser = userService.findUserById(userId);

        if (optionalUser.isEmpty()) {
            var response = UserUpdateStatus.builder()
                                           .status(UpdateStatusEnum.FAIL)
                                           .message("Something went wrong, please contact support")
                                           .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(response);
        }

        var user = optionalUser.get();

        if (!user.getStatus()
                 .equals(ACTIVE)) {
            log.info("User ID {} attempted to change password while having non-active status {}", userId, user.getStatus());
            appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_INACTIVE_STATUS, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(null);
        }

        // Testing that the given old password is correct
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), updatePasswordRequest.getOldPassword()));
        } catch (AuthenticationException e) {
            log.warn("User ID {} attempted to update password, but the old password did not match", userId);
            appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_OLD_MISMATCH, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(null);
        }

        // Verify that the new password is not the same as the old password
        if (updatePasswordRequest.getOldPassword()
                                 .equals(updatePasswordRequest.getNewPassword())) {
            log.warn("User ID {} attempted to change password but the new password is the same as the old password.", userId);
            appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_NEW_SAME_AS_OLD, WARN, request, AUDIT_NAME, userId, auditUuid);
            var response = UserUpdateStatus.builder()
                                           .status(UpdateStatusEnum.FAIL)
                                           .message("New password cannot be the same as the old password")
                                           .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(response);
        }

        // Verify that the new password fulfills the requirements
        if (!authService.passwordCheck(updatePasswordRequest.getNewPassword())) {
            log.warn("User ID {} attempted to change password but the new password does not meet the requirements.", userId);
            appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_FAIL_REQUIREMENTS, WARN, request, AUDIT_NAME, userId, auditUuid);
            var response = UserUpdateStatus.builder()
                                           .status(UpdateStatusEnum.FAIL)
                                           .message("New password does not meet the requirements")
                                           .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(response);
        }
        // Verify that the new passwords are matching
        if (!updatePasswordRequest.getNewPassword()
                                  .equals(updatePasswordRequest.getConfirmPassword())) {
            var response = UserUpdateStatus.builder()
                                           .status(UpdateStatusEnum.FAIL)
                                           .message("New passwords do not match")
                                           .build();
            appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_NEW_MISMATCH, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(response);
        }

        // Updating the password
        user.setPassword(authService.generatePasswordHash(updatePasswordRequest.getNewPassword()));
        userService.updateUser(user);

        var response = UserUpdateStatus.builder()
                                       .status(UpdateStatusEnum.OK)
                                       .message("User updated successfully")
                                       .build();
        appEventPublisher.publishAuditEvent(AUTH_UPDATE_PASSWORD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(response);
    }

    @Override
    public ResponseEntity<Void> verifyRegistration(String token, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_VERIFY_START, INFO, request, AUDIT_NAME, null);
        var returnStatus = "OK";

        var registrationToken = registrationService.getValidToken(token, REGISTRATION);

        if (registrationToken == null) {
            log.warn("User attempted to verify registration with invalid token: {}", token);
            appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_VERIFY_INVALID_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            returnStatus = "INVALID";
        } else {
            userService.updateStatus(registrationToken.getUserId(), ACTIVE);
            registrationService.removeTokenByUserId(registrationToken.getUserId());
            appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_VERIFY_OK, INFO, request, AUDIT_NAME, registrationToken.getUserId(), auditUuid);
        }

        var uri = UriComponentsBuilder.fromHttpUrl(registrationUrl)
                                      .query("status={returnStatus}")
                                      .buildAndExpand(returnStatus)
                                      .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                             .location(uri)
                             .build();
    }

    @Override
    public ResponseEntity<?> resendConfirmationEmail(TokenRequest tokenRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_START, INFO, request, AUDIT_NAME, null);
        var resendToken = registrationService.getValidToken(tokenRequest.getToken(), EMAIL_RESEND);

        if (resendToken == null) {
            log.warn("User attempted to resend confirmation email with invalid token: {}", tokenRequest.getToken());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_INVALID_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        var optionalUser = userService.findUserById(resendToken.getUserId());

        if (optionalUser.isEmpty()) {
            log.error("Could not find user with token: {}", resendToken);
            registrationService.removeTokenByUserId(resendToken.getUserId());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_INVALID_USER, WARN, request, AUDIT_NAME, null, auditUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        var registrationToken = registrationService.findByUserIdAndTokenType(resendToken.getUserId(), REGISTRATION);

        if (registrationToken == null) {
            log.warn("Could not find registration token with token: {}", tokenRequest.getToken());
            registrationService.removeTokenByUserId(resendToken.getUserId());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_EXPIRED_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        var user = optionalUser.get();

        if (!user.getStatus()
                 .equals(REGISTERED)) {
            log.warn("User attempted to resend confirmation email but the user status is not REGISTERED: {}", user.getStatus());
            registrationService.removeToken(registrationToken.getToken());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_USED_TOKEN, WARN, request, AUDIT_NAME, user.getId(), auditUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }

        registrationService.increaseTokenCounter(resendToken);
        registrationService.increaseTokenCounter(registrationToken);
        emailService.sendConfirmationEmail(user, registrationToken.getToken());
        appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_OK, INFO, request, AUDIT_NAME, user.getId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(JSON_MESSAGE_OK);
    }

    // We always return OK to avoid user enumeration
    @Override
    public ResponseEntity<?> lostPassword(EmailRequest emailRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_START + emailRequest.getEmail(), INFO, request, AUDIT_NAME, null);

        if (emailRequest.getEmail() == null || emailRequest.getEmail()
                                                           .trim()
                                                           .isEmpty()) {
            log.warn("User attempted to reset password with empty email");
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(JSON_MESSAGE_ERROR);
        }

        var email = emailRequest.getEmail();
        var optionalUser = userService.findByUsername(email);

        if (optionalUser.isEmpty()) {
            log.warn("User attempted to reset password with invalid email: {}", email);
            // Despite not finding the user, we return OK to avoid user enumeration
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(JSON_MESSAGE_OK);
        }

        var user = optionalUser.get();

        // The locked is the only non-active status where the email is available
        if (user.getStatus()
                .equals(LOCKED)) {
            log.warn("User attempted to reset password with locked email: {}", email);
            appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_INACTIVE_STATUS + emailRequest.getEmail(), WARN, request, AUDIT_NAME, user.getId(),
                    auditUuid);
            // Despite not finding the user, we return OK to avoid user enumeration
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(JSON_MESSAGE_OK);
        }

        var token = registrationService.generateToken(user.getId(), PASSWORD_RESET);

        if (emailService.sendForgottenPassword(user, token)) {
            appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_OK + emailRequest.getEmail(), INFO, request, AUDIT_NAME, user.getId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(JSON_MESSAGE_OK);
        }

        appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_FAIL + emailRequest.getEmail(), ERROR, request, AUDIT_NAME, user.getId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(JSON_MESSAGE_ERROR);
    }

    @Override
    public ResponseEntity<?> resetPassword(UserResetPasswordRequest userResetPasswordRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_START, INFO, request, AUDIT_NAME, null);

        var token = registrationService.findByTokenAndTokenType(userResetPasswordRequest.getToken(), PASSWORD_RESET);

        if (token == null) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_INVALID_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var userId = token.getUserId();

        if (token.getExpiresAt()
                 .isBefore(Instant.now())) {
            registrationService.removeToken(token.getToken());
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_EXPIRED_TOKEN, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        if (!authService.passwordCheck(userResetPasswordRequest.getNewPassword())) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_FAIL_REQUIREMENTS, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        if (!Objects.equals(userResetPasswordRequest.getNewPassword(), userResetPasswordRequest.getConfirmPassword())) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_MISMATCH, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var optionalUser = userService.findUserById(userId);

        if (optionalUser.isEmpty()) {
            log.warn("User ID {} found in token table does not exist in main users table", userId);
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_INVALID_USER, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var user = optionalUser.get();

        if (user.getStatus() != ACTIVE) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_INACTIVE_STATUS + user.getStatus(), ERROR, request, AUDIT_NAME,
                    userId, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var newPasswordHash = authService.generatePasswordHash(userResetPasswordRequest.getNewPassword());

        user.setPassword(newPasswordHash);
        var newUser = userService.updateUser(user);

        if (newUser == null) {
            log.error("User password update for user ID {} failed for unknown reason", userId);
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_UNKNOWN_ERROR, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(JSON_MESSAGE_OK);
    }
}
