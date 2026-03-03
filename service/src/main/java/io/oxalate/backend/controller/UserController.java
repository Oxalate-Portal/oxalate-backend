package io.oxalate.backend.controller;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import static io.oxalate.backend.api.UserStatusEnum.ANONYMIZED;
import io.oxalate.backend.api.request.AdminUserRequest;
import io.oxalate.backend.api.request.TermRequest;
import io.oxalate.backend.api.request.UserStatusRequest;
import io.oxalate.backend.api.response.AdminUserResponse;
import io.oxalate.backend.api.response.ListUserResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_DETAILS_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_DETAILS_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_DETAILS_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_DETAILS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_WITH_ROLE_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_WITH_ROLE_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_GET_WITH_ROLE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_HEALTHCHECK_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_HEALTHCHECK_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_HEALTHCHECK_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_TERM_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_TERM_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_TERM_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_SET_TERM_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_SET_TERM_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_SET_TERM_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_USERNAME_CHANGED;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.model.User;
import io.oxalate.backend.rest.UserAPI;
import io.oxalate.backend.service.AnonymizeService;
import io.oxalate.backend.service.PaymentService;
import io.oxalate.backend.service.RoleService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("UserController")
public class UserController implements UserAPI {

    private final UserService userService;
    private final RoleService roleService;
    private final AnonymizeService anonymizeService;
    private final PaymentService paymentService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = USERS_GET_DETAILS_START, okMessage = USERS_GET_DETAILS_OK)
    public ResponseEntity<AdminUserResponse> getUserDetails(long userId) {
        // Only organizers and admins can view other users info
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) && AuthTools.getCurrentUserId() != userId) {
            log.error("User {} tried to get user {} info", AuthTools.getCurrentUserId(), userId);
            throw new OxalateUnauthorizedException(AuditLevelEnum.WARN, USERS_GET_DETAILS_UNAUTHORIZED + userId, HttpStatus.NOT_FOUND);
        }

        var adminUserResponse = userService.findAdminUserResponseById(userId);

        if (adminUserResponse == null) {
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, USERS_GET_DETAILS_NOT_FOUND + userId, HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(adminUserResponse);
    }

    /**
     * Update user info
     *
     * @param updateRequest Update request
     * @return HTTP response
     */
    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = USERS_UPDATE_START, okMessage = USERS_UPDATE_OK, failMessage = USERS_UPDATE_FAIL)
    public ResponseEntity<AdminUserResponse> updateUser(AdminUserRequest updateRequest) {
        // Only admins can update other users
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN) && AuthTools.getCurrentUserId() != updateRequest.getId()) {
            log.error("User {} tried to update user {} info", AuthTools.getCurrentUserId(), updateRequest.getId());
            throw new OxalateUnauthorizedException(AuditLevelEnum.ERROR, USERS_UPDATE_UNAUTHORIZED + updateRequest.getId(), HttpStatus.NOT_FOUND);
        }

        var user = userService.findUserEntityById(updateRequest.getId());

        if (user == null) {
            log.warn("Non-existing user {} tried to update their data with {}", updateRequest.getId(), updateRequest);
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, USERS_UPDATE_NOT_FOUND + updateRequest.getId(), HttpStatus.NOT_FOUND);
        }

        // Check first whether the new user status is ANONYMIZED
        if (updateRequest.getStatus() == ANONYMIZED) {
            anonymizeService.anonymize(updateRequest.getId());
            log.info("User {} anonymized", updateRequest.getId());
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(null);
        }

        // Currently we do not allow changing the username/email
        if (!user.getUsername()
                 .equals(updateRequest.getUsername())) {
            log.error("User {} tried to change username from {} to {}", updateRequest.getId(), user.getUsername(), updateRequest.getUsername());
            throw new OxalateValidationException(AuditLevelEnum.ERROR, USERS_UPDATE_USERNAME_CHANGED + updateRequest.getId(), HttpStatus.NOT_FOUND);
        }

        // Update user info from request
        user.setFirstName(updateRequest.getFirstName());
        user.setLastName(updateRequest.getLastName());
        user.setPhoneNumber(updateRequest.getPhoneNumber());
        user.setPrivacy(updateRequest.isPrivacy());
        user.setNextOfKin(updateRequest.getNextOfKin());
        user.setStatus(updateRequest.getStatus());
        user.setLanguage(updateRequest.getLanguage());
        user.setPrimaryUserType(updateRequest.getPrimaryUserType());

        // Only admins can change the user roles
        if (AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            var roles = roleService.findAllByNames(updateRequest.getRoles());

            // If the updated list of user roles contain organizer role, then the privacy flag gets turned off
            if (roles.contains(roleService.findByName(ROLE_ORGANIZER.name()))) {
                user.setPrivacy(false);
            }

            user.setRoles(roles);
        }

        var newUser = userService.updateUser(user);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(newUser.toAdminUserResponse());
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = USERS_UPDATE_STATUS_START, okMessage = USERS_UPDATE_STATUS_OK, failMessage = USERS_UPDATE_STATUS_FAIL)
    public ResponseEntity<Void> updateUserStatus(long userId, UserStatusRequest userStatusRequest) {
        // Either the call is made by the user itself or by an admin
        if (!AuthTools.isUserIdCurrentUser(userId) && !AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.ERROR, USERS_UPDATE_STATUS_UNAUTHORIZED + userId, HttpStatus.NOT_FOUND);
        }

        var user = userService.findUserEntityById(userId);

        if (user == null) {
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, USERS_UPDATE_STATUS_NOT_FOUND + userId, HttpStatus.NOT_FOUND);
        }

        user.setStatus(userStatusRequest.getStatus());
        userService.updateUser(user);

        if (userStatusRequest.getStatus() == ANONYMIZED) {
            anonymizeService.anonymize(userId);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(null);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = USERS_GET_START, okMessage = USERS_GET_OK)
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        if (!AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.ERROR, USERS_GET_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = USERS_GET_WITH_ROLE_START, okMessage = USERS_GET_WITH_ROLE_OK)
    public ResponseEntity<List<ListUserResponse>> getUserIdNameListWithRole(RoleEnum roleEnum) {
        var users = userService.findAllByRole(roleEnum);
        var userIdNameResponses = new ArrayList<ListUserResponse>();

        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.ERROR, USERS_GET_WITH_ROLE_UNAUTHORIZED + roleEnum, HttpStatus.NOT_FOUND);
        }

        for (User user : users) {
            var userResponse = user.toEventUserResponse();
            var paymentResponses = paymentService.getActivePaymentResponsesByUser(user.getId());
            userResponse.setPayments(paymentResponses);
            userIdNameResponses.add(userResponse);
        }

        var sortedList = userIdNameResponses.stream()
                                            .sorted(Comparator.comparing(ListUserResponse::getName))
                                            .collect(Collectors.toCollection(ArrayList::new));
        return ResponseEntity.status(HttpStatus.OK)
                             .body(sortedList);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = USERS_SET_TERM_START, okMessage = USERS_SET_TERM_OK)
    public ResponseEntity<Void> recordTermAnswer(TermRequest termRequest) {
        var userId = AuthTools.getCurrentUserId();

        if (userId < 0) {
            log.error("Someone unauthorized tried to set term and conditions answer");
            throw new OxalateUnauthorizedException(AuditLevelEnum.ERROR, USERS_SET_TERM_UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        userService.setTermAnswer(userId, termRequest.getTermAnswer()
                                                     .equalsIgnoreCase("yes"));
        return ResponseEntity.status(HttpStatus.OK)
                             .body(null);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = USERS_RESET_TERM_START, okMessage = USERS_RESET_TERM_OK)
    public ResponseEntity<Void> resetTermAnswer() {
        if (!AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.ERROR, USERS_RESET_TERM_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        userService.resetTermAnswer();
        return ResponseEntity.status(HttpStatus.OK)
                             .body(null);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = USERS_RESET_HEALTHCHECK_START, okMessage = USERS_RESET_HEALTHCHECK_OK)
    public ResponseEntity<Void> resetHealthCheckAnswer() {
        if (!AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.ERROR, USERS_RESET_HEALTHCHECK_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        userService.resetHealthCheck();
        return ResponseEntity.status(HttpStatus.OK)
                             .body(null);
    }
}
