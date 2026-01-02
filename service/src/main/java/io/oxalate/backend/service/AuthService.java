package io.oxalate.backend.service;

import static io.oxalate.backend.api.AuditLevelEnum.ERROR;
import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import static io.oxalate.backend.api.AuditLevelEnum.WARN;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.UpdateStatusEnum;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import static io.oxalate.backend.api.UserStatusEnum.LOCKED;
import static io.oxalate.backend.api.UserStatusEnum.REGISTERED;
import io.oxalate.backend.api.request.EmailRequest;
import io.oxalate.backend.api.request.LoginRequest;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.api.request.TokenRequest;
import io.oxalate.backend.api.request.UserResetPasswordRequest;
import io.oxalate.backend.api.request.UserUpdatePasswordRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.RegistrationResponse;
import io.oxalate.backend.api.response.UserSessionToken;
import io.oxalate.backend.api.response.UserUpdateStatus;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_AUTHENTICATION_NO_ROLES;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_INACTIVE_STATUS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_LOST_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_TAKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_INVALID_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_REGISTRATION_VERIFY_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_EXPIRED_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_INVALID_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_INVALID_USER;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESEND_EMAIL_USED_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_EXPIRED_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_FAIL_REQUIREMENTS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_INACTIVE_STATUS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_INVALID_TOKEN;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_INVALID_USER;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_MISMATCH;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_OK;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_RESET_PASSWORD_UNKNOWN_ERROR;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_FAIL_REQUIREMENTS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_INACTIVE_STATUS;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_NEW_MISMATCH;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_OLD_MISMATCH;
import static io.oxalate.backend.events.AppAuditMessages.AUTH_UPDATE_PASSWORD_UNAUTHORIZED;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.exception.OxalateAuthenticationException;
import static io.oxalate.backend.model.TokenType.EMAIL_RESEND;
import static io.oxalate.backend.model.TokenType.PASSWORD_RESET;
import static io.oxalate.backend.model.TokenType.REGISTRATION;
import io.oxalate.backend.model.User;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private static final String AUDIT_NAME = "AuthService";
    private final AppEventPublisher appEventPublisher;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RegistrationService registrationService;
    private final EmailService emailService;
    private final JwtUtils jwtUtils;

    @Value("${oxalate.app.jwt-expiration}")
    private int expirationTime;
    @Value("${oxalate.app.jwt-secure}")
    private boolean secureCookie;
    @Value("${oxalate.app.jwt-same-site}")
    private String sameSite;
    @Value("${oxalate.token.registration-url}")
    private String registrationUrl;

    @Transactional(readOnly = true)
    public UserSessionToken authenticate(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            log.warn("User {} attempted to log in but the authentication failed: {}", loginRequest.getUsername(), e.getMessage());
            throw new OxalateAuthenticationException(ERROR, AUTH_AUTHENTICATION_FAIL + loginRequest.getUsername() + " message " + e.getMessage(), AUDIT_NAME,
                    -1L, HttpStatus.FORBIDDEN);
        }

        Optional<User> optionalUser = userService.findByUsername(loginRequest.getUsername());
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            log.debug("User {} logged in. Got: {}", user.getUsername(), user);
        } else {
            log.warn("User {} attempted to log in but the account does not exist.", loginRequest.getUsername());
            throw new OxalateAuthenticationException(ERROR, AUTH_AUTHENTICATION_FAIL + loginRequest.getUsername() + " does not exist", AUDIT_NAME, -1L,
                    HttpStatus.FORBIDDEN);
        }

        if (!user.getStatus()
                 .equals(ACTIVE)) {
            log.info("User {} attempted to log in while having non-active status {}", user.getUsername(), user.getStatus());
            throw new OxalateAuthenticationException(ERROR, AUTH_AUTHENTICATION_FAIL + loginRequest.getUsername() + " does not exist", AUDIT_NAME, user.getId(),
                    HttpStatus.FORBIDDEN);
        }

        if (user.getRoles() == null || user.getRoles()
                                           .isEmpty()) {
            log.info("User {} attempted to log in but the account has no roles.", loginRequest.getUsername());
            throw new OxalateAuthenticationException(ERROR, AUTH_AUTHENTICATION_NO_ROLES + loginRequest.getUsername(), AUDIT_NAME, user.getId(),
                    HttpStatus.FORBIDDEN);
        }

        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities()
                                        .stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .toList();

        var paymentResponses = new ArrayList<PaymentResponse>();

        for (var payment : user.getPayments()) {
            paymentResponses.add(payment.toPaymentResponse());
        }

        var jwtResponse = UserSessionToken.builder()
                                          .id(userDetails.getId())
                                          .username(userDetails.getUsername())
                                          .phoneNumber(user.getPhoneNumber())
                                          .firstName(user.getFirstName())
                                          .lastName(user.getLastName())
                                          .roles(roles)
                                          .status(user.getStatus())
                                          .registered(user.getRegistered())
                                          .type("Bearer")
                                          .accessToken(jwt)
                                          .expiresAt(Instant.now()
                                                            .plus(expirationTime, ChronoUnit.SECONDS))
                                          .approvedTerms(user.isApprovedTerms())
                                          .payments(paymentResponses)
                                          .language(user.getLanguage())
                                          .build();

        // Set JWT as a cookie
        ResponseCookie cookie = ResponseCookie.from(JWT_TOKEN, jwt)
                                              .httpOnly(true)
                                              .secure(secureCookie)
                                              .path("/")
                                              .maxAge(expirationTime)
                                              .sameSite(sameSite)
                                              .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return jwtResponse;
    }

    @Transactional
    public UserUpdateStatus updateUserPassword(long userId, UserUpdatePasswordRequest updatePasswordRequest, HttpServletRequest request) {
        var adminUserResponse = userService.findAdminUserResponseById(userId);

        if (adminUserResponse == null) {
            throw new OxalateAuthenticationException(WARN, AUTH_UPDATE_PASSWORD_UNAUTHORIZED, AUDIT_NAME, userId, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!adminUserResponse.getStatus()
                              .equals(ACTIVE)) {
            log.info("User ID {} attempted to change password while having non-active status {}", userId, adminUserResponse.getStatus());
            throw new OxalateAuthenticationException(WARN, AUTH_UPDATE_PASSWORD_INACTIVE_STATUS, AUDIT_NAME, userId, HttpStatus.UNAUTHORIZED);
        }

        // Testing that the given old password is correct
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(adminUserResponse.getUsername(), updatePasswordRequest.getOldPassword()));
        } catch (OxalateAuthenticationException e) {
            log.warn("User ID {} attempted to update password, but the old password did not match", userId);
            throw new OxalateAuthenticationException(WARN, AUTH_UPDATE_PASSWORD_OLD_MISMATCH, AUDIT_NAME, userId, HttpStatus.UNAUTHORIZED);
        }

        // Verify that the new password is not the same as the old password
        if (updatePasswordRequest.getOldPassword()
                                 .equals(updatePasswordRequest.getNewPassword())) {
            log.warn("User ID {} attempted to change password but the new password is the same as the old password.", userId);
            return UserUpdateStatus.builder()
                                   .status(UpdateStatusEnum.FAIL)
                                   .message("New password cannot be the same as the old password")
                                   .build();
        }

        // Verify that the new password fulfills the requirements
        if (passwordCheckFails(updatePasswordRequest.getNewPassword())) {
            log.warn("User ID {} attempted to change password but the new password does not meet the requirements.", userId);
            throw new OxalateAuthenticationException(WARN, AUTH_UPDATE_PASSWORD_FAIL_REQUIREMENTS, AUDIT_NAME, userId, HttpStatus.BAD_REQUEST);
        }
        // Verify that the new passwords are matching
        if (!updatePasswordRequest.getNewPassword()
                                  .equals(updatePasswordRequest.getConfirmPassword())) {
            throw new OxalateAuthenticationException(WARN, AUTH_UPDATE_PASSWORD_NEW_MISMATCH, AUDIT_NAME, userId, HttpStatus.BAD_REQUEST);
        }

        // Updating the password
        var user = userService.findUserEntityById(userId);

        if (user == null) {
            log.error("User ID {} attempted to change password, but the user does not exist", userId);
            throw new OxalateAuthenticationException(WARN, AUTH_UPDATE_PASSWORD_UNAUTHORIZED, AUDIT_NAME, userId, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        user.setPassword(generatePasswordHash(updatePasswordRequest.getNewPassword()));
        userService.updateUser(user);

        return UserUpdateStatus.builder()
                               .status(UpdateStatusEnum.OK)
                               .message("User updated successfully")
                               .build();
    }

    public ActionResponse resendConfirmationEmail(TokenRequest tokenRequest, HttpServletRequest request, UUID auditUuid) {
        var resendToken = registrationService.getValidToken(tokenRequest.getToken(), EMAIL_RESEND);

        if (resendToken == null) {
            log.warn("User attempted to resend confirmation email with invalid token: {}", tokenRequest.getToken());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_INVALID_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            return ActionResponse.builder()
                                 .message("Invalid token")
                                 .status(UpdateStatusEnum.FAIL)
                                 .build();
        }

        var user = userService.findUserEntityById(resendToken.getUserId());

        if (user == null) {
            log.error("Could not find user with token: {}", resendToken);
            registrationService.removeTokenByUserId(resendToken.getUserId());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_INVALID_USER, WARN, request, AUDIT_NAME, null, auditUuid);
            return ActionResponse.builder()
                                 .message("Invalid token")
                                 .status(UpdateStatusEnum.FAIL)
                                 .build();
        }

        var registrationToken = registrationService.findByUserIdAndTokenType(resendToken.getUserId(), REGISTRATION);

        if (registrationToken == null) {
            log.warn("Could not find registration token with token: {}", tokenRequest.getToken());
            registrationService.removeTokenByUserId(resendToken.getUserId());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_EXPIRED_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            return ActionResponse.builder()
                                 .message("Invalid token")
                                 .status(UpdateStatusEnum.FAIL)
                                 .build();
        }

        if (!user.getStatus()
                 .equals(REGISTERED)) {
            log.warn("User attempted to resend confirmation email but the user status is not REGISTERED: {}", user.getStatus());
            registrationService.removeToken(registrationToken.getToken());
            appEventPublisher.publishAuditEvent(AUTH_RESEND_EMAIL_USED_TOKEN, WARN, request, AUDIT_NAME, user.getId(), auditUuid);
            return ActionResponse.builder()
                                 .message("Invalid token")
                                 .status(UpdateStatusEnum.FAIL)
                                 .build();
        }

        registrationService.increaseTokenCounter(resendToken);
        registrationService.increaseTokenCounter(registrationToken);
        emailService.sendConfirmationEmail(user, registrationToken.getToken());
        return ActionResponse.builder()
                             .message("Confirmation email sent successfully")
                             .status(UpdateStatusEnum.OK)
                             .build();
    }

    public ActionResponse lostPassword(EmailRequest emailRequest, HttpServletRequest request, UUID auditUuid) {

        if (emailRequest.getEmail() == null || emailRequest.getEmail()
                                                           .trim()
                                                           .isEmpty()) {
            log.warn("User attempted to reset password with empty email");
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.OK)
                                 .message("")
                                 .build();
        }

        var email = emailRequest.getEmail();
        var optionalUser = userService.findByUsername(email);

        if (optionalUser.isEmpty()) {
            log.warn("User attempted to reset password with invalid email: {}", email);
            // Despite not finding the user, we return OK to avoid user enumeration
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.OK)
                                 .message("")
                                 .build();
        }

        var user = optionalUser.get();

        // The locked is the only non-active status where the email is available
        if (user.getStatus()
                .equals(LOCKED)) {
            log.warn("User attempted to reset password with locked email: {}", email);
            appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_INACTIVE_STATUS + emailRequest.getEmail(), WARN, request, AUDIT_NAME, user.getId(),
                    auditUuid);
            // Despite not finding the user, we return OK to avoid user enumeration
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.OK)
                                 .message("")
                                 .build();
        }

        var token = registrationService.generateToken(user.getId(), PASSWORD_RESET);

        if (emailService.sendForgottenPassword(user, token)) {
            appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_OK + emailRequest.getEmail(), INFO, request, AUDIT_NAME, user.getId(), auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.OK)
                                 .message("")
                                 .build();
        }

        appEventPublisher.publishAuditEvent(AUTH_LOST_PASSWORD_FAIL + emailRequest.getEmail(), ERROR, request, AUDIT_NAME, user.getId(), auditUuid);
        log.warn("Email sending failed with email: {}", email);

        return ActionResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .message("")
                             .build();
    }

    public ActionResponse resetPassword(UserResetPasswordRequest userResetPasswordRequest, HttpServletRequest request, UUID auditUuid) {
        var token = registrationService.findByTokenAndTokenType(userResetPasswordRequest.getToken(), PASSWORD_RESET);

        if (token == null) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_INVALID_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .message("")
                                 .build();
        }

        var userId = token.getUserId();

        if (token.getExpiresAt()
                 .isBefore(Instant.now())) {
            registrationService.removeToken(token.getToken());
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_EXPIRED_TOKEN, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .message("")
                                 .build();
        }

        if (passwordCheckFails(userResetPasswordRequest.getNewPassword())) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_FAIL_REQUIREMENTS, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .message("")
                                 .build();
        }

        if (!Objects.equals(userResetPasswordRequest.getNewPassword(), userResetPasswordRequest.getConfirmPassword())) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_MISMATCH, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .message("")
                                 .build();
        }

        var user = userService.findUserEntityById(userId);

        if (user == null) {
            log.warn("User ID {} found in token table does not exist in main users table", userId);
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_INVALID_USER, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .message("")
                                 .build();
        }

        if (user.getStatus() != ACTIVE) {
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_INACTIVE_STATUS + user.getStatus(), ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .message("")
                                 .build();
        }

        var newPasswordHash = generatePasswordHash(userResetPasswordRequest.getNewPassword());

        user.setPassword(newPasswordHash);
        var newUser = userService.updateUser(user);

        if (newUser == null) {
            log.error("User password update for user ID {} failed for unknown reason", userId);
            appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_UNKNOWN_ERROR, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ActionResponse.builder()
                                 .status(UpdateStatusEnum.FAIL)
                                 .message("")
                                 .build();
        }

        appEventPublisher.publishAuditEvent(AUTH_RESET_PASSWORD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ActionResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .message("")
                             .build();
    }

    public String generatePasswordHash(String password) {
        if (passwordCheckFails(password)) {
            return null;
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    private boolean passwordCheckFails(String password) {
        // TODO: These requirements should be configurable
        if (password.length() < 10) {
            return true;
        }

        return !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*") || !password.matches(
                ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }

    public RegistrationResponse registerUser(SignupRequest signupRequest, HttpServletRequest request, UUID auditUuid) {
        if (!userService.isUsernameValid(signupRequest.getUsername())) {
            log.info("User {} attempted to register but the username is invalid.", signupRequest.getUsername());
            appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_TAKEN + signupRequest.getUsername(), WARN, request, AUDIT_NAME, -1L, auditUuid);

            return RegistrationResponse.builder()
                                       .status(UpdateStatusEnum.FAIL)
                                       .message("Username is invalid or already taken")
                                       .token("")
                                       .build();
        }

        // Create new user's account
        var user = userService.createNewUser(signupRequest);

        if (user == null) {
            log.info("User {} attempted to register but the registration failed.", signupRequest.getUsername());
            return RegistrationResponse.builder()
                                       .status(UpdateStatusEnum.FAIL)
                                       .message("Registration failed")
                                       .token("")
                                       .build();
        }

        var registrationToken = registrationService.generateToken(user.getId(), REGISTRATION);
        emailService.sendConfirmationEmail(user, registrationToken);

        var resendToken = registrationService.generateToken(user.getId(), EMAIL_RESEND);

        appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_OK + signupRequest.getUsername(), INFO, request, AUDIT_NAME, user.getId(), auditUuid);

        return RegistrationResponse.builder()
                                   .status(UpdateStatusEnum.OK)
                                   .token(resendToken)
                                   .build();
    }

    public URI verifyRegistration(String token, HttpServletRequest request, UUID auditUuid) {
        var registrationToken = registrationService.getValidToken(token, REGISTRATION);
        var returnStatus = "OK";

        if (registrationToken == null) {
            log.warn("User attempted to verify registration with invalid token: {}", token);
            appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_VERIFY_INVALID_TOKEN, WARN, request, AUDIT_NAME, null, auditUuid);
            returnStatus = "INVALID";
        }

        userService.updateStatus(registrationToken.getUserId(), ACTIVE);
        registrationService.removeTokenByUserId(registrationToken.getUserId());
        appEventPublisher.publishAuditEvent(AUTH_REGISTRATION_VERIFY_OK, INFO, request, AUDIT_NAME, registrationToken.getUserId(), auditUuid);

        return UriComponentsBuilder.fromUriString(registrationUrl)
                                   .query("status={returnStatus}")
                                   .buildAndExpand(returnStatus)
                                   .toUri();
    }
}
