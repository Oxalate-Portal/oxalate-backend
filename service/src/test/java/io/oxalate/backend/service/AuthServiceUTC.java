package io.oxalate.backend.service;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import static io.oxalate.backend.api.UserStatusEnum.LOCKED;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.EmailChangeRequest;
import io.oxalate.backend.api.request.LoginRequest;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.Role;
import io.oxalate.backend.model.Token;
import static io.oxalate.backend.model.TokenType.EMAIL_CHANGE;
import io.oxalate.backend.model.User;
import io.oxalate.backend.security.LoginAttemptService;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceUTC {

    @Mock
    private AppEventPublisher appEventPublisher;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;
    @Mock
    private RegistrationService registrationService;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "secureCookie", true);
        ReflectionTestUtils.setField(authService, "sameSite", "Strict");
        ReflectionTestUtils.setField(authService, "emailChangeUrl", "http://localhost:3000/auth/email-change");
    }

    @Test
    void requestEmailChangeValidRequestOk() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var user = createActiveUser(100L, "current.user@example.com");
        var emailChangeRequest = EmailChangeRequest.builder()
                                                   .newEmail("new.user@example.com")
                                                   .password("ValidPassword1!")
                                                   .build();

        when(userService.findUserEntityById(100L)).thenReturn(user);
        when(loginAttemptService.isBlocked("email-change:100")).thenReturn(false);
        when(userService.isUsernameAvailableForUser("new.user@example.com", 100L)).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(registrationService.generateToken(eq(100L), eq(EMAIL_CHANGE), anyString())).thenReturn("token-value");

        var result = authService.requestEmailChange(100L, emailChangeRequest, request, response);

        assertTrue(result);
        verify(registrationService).removeTokenByUserIdAndType(100L, EMAIL_CHANGE);
        verify(loginAttemptService).resetAttempts("email-change:100");
        verify(emailService).sendEmailChangeConfirmationEmail(user, "new.user@example.com", "token-value");
    }

    @Test
    void requestEmailChangeInvalidPasswordFail() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var user = createActiveUser(100L, "current.user@example.com");
        var emailChangeRequest = EmailChangeRequest.builder()
                                                   .newEmail("new.user@example.com")
                                                   .password("WrongPassword1!")
                                                   .build();

        when(userService.findUserEntityById(100L)).thenReturn(user);
        when(loginAttemptService.isBlocked("email-change:100")).thenReturn(false);
        when(userService.isUsernameAvailableForUser("new.user@example.com", 100L)).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("bad password"));
        when(loginAttemptService.getAttempts("email-change:100")).thenReturn(1);

        var result = authService.requestEmailChange(100L, emailChangeRequest, request, response);

        assertFalse(result);
        verify(loginAttemptService).loginFailed("email-change:100");
        verify(emailService, never()).sendEmailChangeConfirmationEmail(any(User.class), anyString(), anyString());
    }

    @Test
    void requestEmailChangeMaxAttemptsLocksUser() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var user = createActiveUser(100L, "current.user@example.com");
        var emailChangeRequest = EmailChangeRequest.builder()
                                                   .newEmail("new.user@example.com")
                                                   .password("WrongPassword1!")
                                                   .build();

        when(userService.findUserEntityById(100L)).thenReturn(user);
        when(loginAttemptService.isBlocked("email-change:100")).thenReturn(false);
        when(userService.isUsernameAvailableForUser("new.user@example.com", 100L)).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("bad password"));
        when(loginAttemptService.getAttempts("email-change:100")).thenReturn(LoginAttemptService.MAX_ATTEMPT);

        var result = authService.requestEmailChange(100L, emailChangeRequest, request, response);

        assertFalse(result);
        verify(userService).updateStatus(100L, LOCKED);
        assertNotNull(response.getHeader("Set-Cookie"));
        assertTrue(response.getHeader("Set-Cookie")
                           .contains(JWT_TOKEN + "="));
    }

    @Test
    void verifyEmailChangeValidTokenOk() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var token = Token.builder()
                         .token("token-value")
                         .userId(100L)
                         .tokenType(EMAIL_CHANGE)
                         .data("{\"newEmail\":\"new.user@example.com\"}")
                         .build();

        when(registrationService.getValidToken("token-value", EMAIL_CHANGE)).thenReturn(token);
        when(userService.isUsernameAvailableForUser("new.user@example.com", 100L)).thenReturn(true);
        when(userService.updateUsername(100L, "new.user@example.com")).thenReturn(true);

        URI uri = authService.verifyEmailChange("token-value", request, response);

        assertEquals("http://localhost:3000/auth/email-change?status=OK", uri.toString());
        verify(registrationService).removeTokenByUserIdAndType(100L, EMAIL_CHANGE);
        assertNotNull(response.getHeader("Set-Cookie"));
    }

    @Test
    void verifyEmailChangeInvalidTokenFail() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        when(registrationService.getValidToken("missing-token", EMAIL_CHANGE)).thenReturn(null);

        URI uri = authService.verifyEmailChange("missing-token", request, response);

        assertEquals("http://localhost:3000/auth/email-change?status=INVALID", uri.toString());
        verify(userService, never()).updateUsername(anyLong(), anyString());
    }

    @Test
    void verifyEmailChangeInvalidPayloadFail() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var token = Token.builder()
                         .token("token-value")
                         .userId(100L)
                         .tokenType(EMAIL_CHANGE)
                         .data("{}")
                         .build();

        when(registrationService.getValidToken("token-value", EMAIL_CHANGE)).thenReturn(token);

        URI uri = authService.verifyEmailChange("token-value", request, response);

        assertEquals("http://localhost:3000/auth/email-change?status=INVALID", uri.toString());
        verify(registrationService).removeTokenByUserIdAndType(100L, EMAIL_CHANGE);
        verify(userService, never()).updateUsername(anyLong(), anyString());
    }

    private User createActiveUser(long id, String username) {
        return User.builder()
                   .id(id)
                   .username(username)
                   .firstName("Current")
                   .lastName("User")
                   .status(ACTIVE)
                   .phoneNumber("358401234567")
                   .privacy(false)
                   .password("$2a$10$hash")
                   .approvedTerms(true)
                   .language("en")
                   .build();
    }

    // -------------------------------------------------------------------------
    // authenticate() tests
    // -------------------------------------------------------------------------

    @Test
    void authenticatePrimaryUserTypePopulatedOk() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var loginRequest = LoginRequest.builder()
                                       .username("test@example.com")
                                       .password("TestPassword1!")
                                       .build();

        var user = buildActiveUserWithTypeAndMemberships(100L, "test@example.com",
                UserTypeEnum.SCUBA_DIVER, List.of());

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var userDetails = new UserDetailsImpl(100L, "test@example.com", "hash",
                authorities, true, null, false, "en");
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        ReflectionTestUtils.setField(authService, "expirationTime", 3600);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        var result = authService.authenticate(loginRequest, request, response);

        assertNotNull(result);
        assertEquals(UserTypeEnum.SCUBA_DIVER, result.getPrimaryUserType());
    }

    @Test
    void authenticateWithMembershipsPopulatedOk() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var loginRequest = LoginRequest.builder()
                                       .username("test@example.com")
                                       .password("TestPassword1!")
                                       .build();

        var user = buildActiveUserWithTypeAndMemberships(100L, "test@example.com",
                UserTypeEnum.FREE_DIVER, buildSingleMembership(100L, "test@example.com"));

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var userDetails = new UserDetailsImpl(100L, "test@example.com", "hash",
                authorities, true, null, false, "en");
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        ReflectionTestUtils.setField(authService, "expirationTime", 3600);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        var result = authService.authenticate(loginRequest, request, response);

        assertNotNull(result);
        assertNotNull(result.getMemberships());
        assertEquals(1, result.getMemberships()
                              .size());
        assertEquals(MembershipTypeEnum.PERIODICAL, result.getMemberships()
                                                          .get(0)
                                                          .getType());
        assertEquals(MembershipStatusEnum.ACTIVE, result.getMemberships()
                                                        .get(0)
                                                        .getStatus());
    }

    @Test
    void authenticateWithNoMembershipsReturnsEmptyListOk() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var loginRequest = LoginRequest.builder()
                                       .username("test@example.com")
                                       .password("TestPassword1!")
                                       .build();

        var user = buildActiveUserWithTypeAndMemberships(100L, "test@example.com",
                UserTypeEnum.SCUBA_DIVER, List.of());

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var userDetails = new UserDetailsImpl(100L, "test@example.com", "hash",
                authorities, true, null, false, "en");
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        ReflectionTestUtils.setField(authService, "expirationTime", 3600);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        var result = authService.authenticate(loginRequest, request, response);

        assertNotNull(result);
        assertNotNull(result.getMemberships());
        assertTrue(result.getMemberships()
                         .isEmpty());
    }

    @Test
    void authenticateWithNullPrimaryUserTypeOk() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var loginRequest = LoginRequest.builder()
                                       .username("test@example.com")
                                       .password("TestPassword1!")
                                       .build();

        var user = buildActiveUserWithTypeAndMemberships(100L, "test@example.com",
                null, List.of());

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var userDetails = new UserDetailsImpl(100L, "test@example.com", "hash",
                authorities, true, null, false, "en");
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        ReflectionTestUtils.setField(authService, "expirationTime", 3600);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        var result = authService.authenticate(loginRequest, request, response);

        assertNotNull(result);
        assertNull(result.getPrimaryUserType());
    }

    private User buildActiveUserWithTypeAndMemberships(long id, String username,
            UserTypeEnum primaryUserType, List<Membership> memberships) {
        var role = new Role();
        role.setName(io.oxalate.backend.api.RoleEnum.ROLE_USER);

        return User.builder()
                   .id(id)
                   .username(username)
                   .firstName("Test")
                   .lastName("User")
                   .status(ACTIVE)
                   .phoneNumber("358401234567")
                   .privacy(false)
                   .password("$2a$10$hash")
                   .approvedTerms(true)
                   .language("en")
                   .primaryUserType(primaryUserType)
                   .roles(Set.of(role))
                   .membership(memberships)
                   .payments(List.of())
                   .build();
    }

    private List<Membership> buildSingleMembership(long userId, String username) {
        var innerUser = User.builder()
                            .id(userId)
                            .username(username)
                            .firstName("Test")
                            .lastName("User")
                            .status(ACTIVE)
                            .password("$2a$10$hash")
                            .phoneNumber("358401234567")
                            .privacy(false)
                            .approvedTerms(true)
                            .language("en")
                            .build();

        var membership = Membership.builder()
                                   .id(1L)
                                   .userId(userId)
                                   .type(MembershipTypeEnum.PERIODICAL)
                                   .status(MembershipStatusEnum.ACTIVE)
                                   .startDate(LocalDate.now())
                                   .endDate(LocalDate.now()
                                                     .plusYears(1))
                                   .created(Instant.now())
                                   .user(innerUser)
                                   .build();
        return List.of(membership);
    }
}
