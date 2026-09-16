package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.request.ConfirmationRequest;
import io.oxalate.backend.api.response.AdminUserResponse;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.service.AnonymizeService;
import io.oxalate.backend.service.PaymentService;
import io.oxalate.backend.service.RoleService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerUTC {

    private final UserService userService = Mockito.mock(UserService.class);
    private final RoleService roleService = Mockito.mock(RoleService.class);
    private final AnonymizeService anonymizeService = Mockito.mock(AnonymizeService.class);
    private final PaymentService paymentService = Mockito.mock(PaymentService.class);
    private final UserController controller = new UserController(userService, roleService, anonymizeService, paymentService);

    @Test
    void getUserDetailsRejectsOtherUser() {
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN, io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER))
                .thenReturn(false);
            auth.when(AuthTools::getCurrentUserId)
                .thenReturn(1L);

            assertThrows(OxalateUnauthorizedException.class, () -> controller.getUserDetails(2L));
        }
    }

    @Test
    void getUserDetailsReturnsNotFoundForMissingUser() {
        when(userService.findAdminUserResponseById(2L)).thenReturn(null);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER, ROLE_ADMIN))
                .thenReturn(true);

            assertThrows(OxalateNotFoundException.class, () -> controller.getUserDetails(2L));
        }
    }

    @Test
    void getUserDetailsReturnsResponseForAuthorizedUser() {
        var expected = AdminUserResponse.builder()
                                        .id(2L)
                                        .build();
        when(userService.findAdminUserResponseById(2L)).thenReturn(expected);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER, ROLE_ADMIN))
                .thenReturn(true);

            assertEquals(expected, controller.getUserDetails(2L)
                                             .getBody());
        }
    }

    @Test
    void recordTermAnswerRejectsAnonymousAndRecordsAnswer() {
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(AuthTools::getCurrentUserId)
                .thenReturn(-1L);
            assertThrows(OxalateUnauthorizedException.class,
                    () -> controller.recordTermAnswer(ConfirmationRequest.builder()
                                                                         .confirmationAnswer(true)
                                                                         .build()));
        }

        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(AuthTools::getCurrentUserId)
                .thenReturn(4L);
            assertEquals(200, controller.recordTermAnswer(ConfirmationRequest.builder()
                                                                             .confirmationAnswer(true)
                                                                             .build())
                                        .getStatusCode()
                                        .value());
            verify(userService).setTermAnswer(4L, true);
        }
    }

    @Test
    void resetAnswersRequireAdmin() {
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                .thenReturn(false);
            assertThrows(OxalateUnauthorizedException.class, controller::resetTermAnswer);
        }

        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasRole(ROLE_ADMIN))
                .thenReturn(true);
            controller.resetTermAnswer();
            controller.resetHealthCheckAnswer();
            verify(userService).resetTermAnswer();
            verify(userService).resetHealthCheck();
        }
    }
}
