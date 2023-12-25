package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.ERROR;
import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import static io.oxalate.backend.api.UserStatus.ANONYMIZED;
import io.oxalate.backend.api.request.TermRequest;
import io.oxalate.backend.api.request.UserStatusRequest;
import io.oxalate.backend.api.request.UserUpdateRequest;
import io.oxalate.backend.api.response.AdminUserResponse;
import io.oxalate.backend.api.response.EventUserResponse;
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
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_TERM_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_TERM_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_RESET_TERM_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_SET_TERM_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_SET_TERM_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_SET_TERM_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_ANONYMIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_ANONYMIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_OK;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_START;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_STATUS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.USERS_UPDATE_USERNAME_CHANGED;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.model.User;
import io.oxalate.backend.rest.UserAPI;
import io.oxalate.backend.service.AnonymizeService;
import io.oxalate.backend.service.RoleService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
public class UserController implements UserAPI {

    private final UserService userService;
    private final RoleService roleService;
    private final AnonymizeService anonymizeService;

    private static final String AUDIT_NAME = "UserController";
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ORGANIZER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdminUserResponse> getUserDetails(long userId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(USERS_GET_DETAILS_START + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        // Only organizers and admins can view other users info
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) && AuthTools.getCurrentUserId() != userId) {
            appEventPublisher.publishAuditEvent(USERS_GET_DETAILS_UNAUTHORIZED + userId, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User {} tried to get user {} info", AuthTools.getCurrentUserId(), userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .build();
        }

        var optionalUser = userService.findUserById(userId);

        if (optionalUser.isEmpty()) {
            appEventPublisher.publishAuditEvent(USERS_GET_DETAILS_NOT_FOUND + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        var user = optionalUser.get();

        appEventPublisher.publishAuditEvent(USERS_GET_DETAILS_OK + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.ok(user.toAdminUserResponse());
    }

    /**
     * Update user info
     *
        * @param updateRequest Update request
     * @return HTTP response
     */
    @Override
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ORGANIZER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdminUserResponse> updateUser(UserUpdateRequest updateRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(USERS_UPDATE_START + updateRequest.getUserId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        // Only admins can update other users
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN) && AuthTools.getCurrentUserId() != updateRequest.getUserId()) {
            appEventPublisher.publishAuditEvent(USERS_UPDATE_UNAUTHORIZED + updateRequest.getUserId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User {} tried to update user {} info", AuthTools.getCurrentUserId(), updateRequest.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        try {
            Optional<User> optionalUser = userService.findUserById(updateRequest.getUserId());

            if (optionalUser.isEmpty()) {
                appEventPublisher.publishAuditEvent(USERS_UPDATE_NOT_FOUND + updateRequest.getUserId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
                log.warn("Non-existing user {} tried to update their data with {}", updateRequest.getUserId(), updateRequest);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Check first whether the new user status is ANONYMIZED, in which case we start an alternative process
            if (updateRequest.getStatus() == ANONYMIZED) {
                anonymizeService.anonymize(updateRequest.getUserId());
                appEventPublisher.publishAuditEvent(USERS_UPDATE_ANONYMIZED + updateRequest.getUserId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
                log.info("User {} anonymized", updateRequest.getUserId());
                return ResponseEntity.status(HttpStatus.OK).body(null);
            }

            var user = optionalUser.get();

            // Currently we do not allow changing the username/email
            if (!user.getUsername().equals(updateRequest.getUsername())) {
                appEventPublisher.publishAuditEvent(USERS_UPDATE_USERNAME_CHANGED + updateRequest.getUserId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                        auditUuid);
                log.error("User {} tried to change username from {} to {}", updateRequest.getUserId(), user.getUsername(), updateRequest.getUsername());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Update user info from request
            user.setFirstName(updateRequest.getFirstName());
            user.setLastName(updateRequest.getLastName());
            user.setPhoneNumber(updateRequest.getPhoneNumber());
            user.setPrivacy(updateRequest.isPrivacy());
            user.setNextOfKin(updateRequest.getNextOfKin());
            user.setStatus(updateRequest.getStatus());
            user.setLanguage(updateRequest.getLanguage());

            var roles = roleService.findAllByNames(updateRequest.getRoles());

            // If the user has organizer role, then the privacy flag gets turned off
            if (roles.contains(roleService.findByName(ROLE_ORGANIZER.name()))) {
                user.setPrivacy(false);
            }

            user.setRoles(roles);
            var newUser = userService.updateUser(user);

            appEventPublisher.publishAuditEvent(USERS_UPDATE_OK + updateRequest.getUserId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.OK).body(newUser.toAdminUserResponse());
        } catch (Exception e) {
            appEventPublisher.publishAuditEvent(USERS_UPDATE_FAIL + updateRequest.getUserId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Failed to update user ID {}: {}", updateRequest.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> updateUserStatus(long userId, UserStatusRequest userStatusRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(USERS_UPDATE_STATUS_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        // Either the call is made by the user itself or by an admin
        if (AuthTools.isUserIdCurrentUser(userId) || AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            try {
                Optional<User> optionalUser = userService.findUserById(userId);

                if (optionalUser.isEmpty()) {
                    appEventPublisher.publishAuditEvent(USERS_UPDATE_STATUS_NOT_FOUND + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                            auditUuid);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                         .body(null);
                }

                User user = optionalUser.get();
                user.setStatus(userStatusRequest.getStatus());
                userService.updateUser(user);

                if (userStatusRequest.getStatus() == ANONYMIZED) {
                    appEventPublisher.publishAuditEvent(USERS_UPDATE_STATUS_ANONYMIZED + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                            auditUuid);
                    anonymizeService.anonymize(userId);
                }

                appEventPublisher.publishAuditEvent(USERS_UPDATE_STATUS_OK + userId, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
                return ResponseEntity.status(HttpStatus.OK)
                                     .body(null);
            } catch (Exception e) {
                appEventPublisher.publishAuditEvent(USERS_UPDATE_STATUS_FAIL + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
                log.error("Failed to update user ID {} status: {}", userId, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(null);
            }
        }

        appEventPublisher.publishAuditEvent(USERS_UPDATE_STATUS_UNAUTHORIZED + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);

        return null;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserResponse>> getUsers(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(USERS_GET_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(USERS_GET_UNAUTHORIZED, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var users = userService.findAll();
        appEventPublisher.publishAuditEvent(USERS_GET_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return getAdminUserResponseList(users);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EventUserResponse>> getUserIdNameListWithRole(RoleEnum roleEnum, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(USERS_GET_WITH_ROLE_START + roleEnum, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        var users = userService.findAllByRole(roleEnum);
        var userIdNameResponses = new ArrayList<EventUserResponse>();

        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(USERS_GET_WITH_ROLE_UNAUTHORIZED + roleEnum, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        for (User user : users) {
            userIdNameResponses.add(EventUserResponse.builder()
                                                     .id(user.getId())
                                                     .name(user.getLastName() + " " + user.getFirstName())
                                                     .eventDiveCount(user.getDiveCount())
                                                     .build());
        }

        var sortedList = userIdNameResponses.stream()
                .sorted(Comparator.comparing(EventUserResponse::getName)).collect(Collectors.toCollection(ArrayList::new));
        appEventPublisher.publishAuditEvent(USERS_GET_WITH_ROLE_OK + roleEnum, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(sortedList);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ORGANIZER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> recordTermAnswer(TermRequest termRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(USERS_SET_TERM_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());
        var userId = AuthTools.getCurrentUserId();

        if (userId < 0) {
            appEventPublisher.publishAuditEvent(USERS_SET_TERM_UNAUTHORIZED, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("Someone unauthorized tried to set term and conditions answer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }

        userService.setTermAnswer(userId, termRequest.getTermAnswer()
                                                     .equalsIgnoreCase("yes"));

        appEventPublisher.publishAuditEvent(USERS_SET_TERM_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetTermAnswer(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(USERS_RESET_TERM_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(USERS_RESET_TERM_UNAUTHORIZED, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        userService.resetTermAnswer();
        appEventPublisher.publishAuditEvent(USERS_RESET_TERM_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(null);
    }

    private static ResponseEntity<List<AdminUserResponse>> getAdminUserResponseList(List<User> users) {
        var adminUserResponses = new ArrayList<AdminUserResponse>();

        for (User user : users) {
            var adminUserResponse = user.toAdminUserResponse();
            adminUserResponse.setStatus(user.getStatus().name());
            adminUserResponses.add(adminUserResponse);
        }

        return ResponseEntity.status(HttpStatus.OK).body(adminUserResponses);
    }
}
