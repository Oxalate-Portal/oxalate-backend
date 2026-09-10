package io.oxalate.backend.controller;

import io.oxalate.backend.api.AuditLevelEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.request.MembershipRequest;
import io.oxalate.backend.api.response.MembershipResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_ALL_ACTIVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_ALL_ACTIVE_START;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_FOR_USER_OK;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_FOR_USER_START;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_FOR_USER_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_UPDATE_START;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.rest.MembershipAPI;
import io.oxalate.backend.service.MembershipService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("MembershipController")
public class MembershipController implements MembershipAPI {

    private final MembershipService membershipService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = MEMBERSHIP_GET_ALL_ACTIVE_START, okMessage = MEMBERSHIP_GET_ALL_ACTIVE_OK)
    public ResponseEntity<List<MembershipResponse>> getAllActiveMemberships() {
        var membershipResponses = membershipService.getAllActiveMemberships();
        return ResponseEntity.ok(membershipResponses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = MEMBERSHIP_GET_START, okMessage = MEMBERSHIP_GET_OK)
    public ResponseEntity<MembershipResponse> getMembership(long membershipId) {
        var membershipResponse = membershipService.findById(membershipId);
        return ResponseEntity.ok(membershipResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MEMBERSHIP_GET_FOR_USER_START, okMessage = MEMBERSHIP_GET_FOR_USER_OK)
    public ResponseEntity<List<MembershipResponse>> getMembershipsForUser(long userId) {
        // A member may only read their own memberships, admins may read anyone's
        if (!AuthTools.currentUserHasRole(ROLE_ADMIN) && !AuthTools.isUserIdCurrentUser(userId)) {
            log.warn("User {} tried to read memberships of user {}", AuthTools.getCurrentUserId(), userId);
            throw new OxalateUnauthorizedException(AuditLevelEnum.WARN, MEMBERSHIP_GET_FOR_USER_UNAUTHORIZED + userId, HttpStatus.FORBIDDEN);
        }

        var membershipResponses = membershipService.getMembershipsForUser(userId);
        return ResponseEntity.ok(membershipResponses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = MEMBERSHIP_CREATE_START, okMessage = MEMBERSHIP_CREATE_OK)
    public ResponseEntity<MembershipResponse> createMembership(MembershipRequest membershipRequest) {
        var membershipResponse = membershipService.createMembership(membershipRequest);
        return ResponseEntity.ok(membershipResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = MEMBERSHIP_UPDATE_START, okMessage = MEMBERSHIP_UPDATE_OK)
    public ResponseEntity<MembershipResponse> updateMembership(MembershipRequest membershipRequest) {
        var membershipResponse = membershipService.updateMembership(membershipRequest);
        return ResponseEntity.ok(membershipResponse);
    }
}
