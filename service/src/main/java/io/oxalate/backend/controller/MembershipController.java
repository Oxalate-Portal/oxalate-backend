package io.oxalate.backend.controller;

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
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MEMBERSHIP_UPDATE_START;
import io.oxalate.backend.rest.MembershipAPI;
import io.oxalate.backend.service.MembershipService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@AuditSource("MembershipController")
public class MembershipController implements MembershipAPI {

    private final MembershipService membershipService;

    @Override
    @Audited(startMessage = MEMBERSHIP_GET_ALL_ACTIVE_START, okMessage = MEMBERSHIP_GET_ALL_ACTIVE_OK)
    public ResponseEntity<List<MembershipResponse>> getAllActiveMemberships() {
        var membershipResponses = membershipService.getAllActiveMemberships();
        return ResponseEntity.ok(membershipResponses);
    }

    @Override
    @Audited(startMessage = MEMBERSHIP_GET_START, okMessage = MEMBERSHIP_GET_OK)
    public ResponseEntity<MembershipResponse> getMembership(long membershipId) {
        var membershipResponse = membershipService.findById(membershipId);
        return ResponseEntity.ok(membershipResponse);
    }

    @Override
    @Audited(startMessage = MEMBERSHIP_GET_FOR_USER_START, okMessage = MEMBERSHIP_GET_FOR_USER_OK)
    public ResponseEntity<List<MembershipResponse>> getMembershipsForUser(long userId) {
        var membershipResponses = membershipService.getMembershipsForUser(userId);
        return ResponseEntity.ok(membershipResponses);
    }

    @Override
    @Audited(startMessage = MEMBERSHIP_CREATE_START, okMessage = MEMBERSHIP_CREATE_OK)
    public ResponseEntity<MembershipResponse> createMembership(MembershipRequest membershipRequest) {
        var membershipResponse = membershipService.createMembership(membershipRequest);
        return ResponseEntity.ok(membershipResponse);
    }

    @Override
    @Audited(startMessage = MEMBERSHIP_UPDATE_START, okMessage = MEMBERSHIP_UPDATE_OK)
    public ResponseEntity<MembershipResponse> updateMembership(MembershipRequest membershipRequest) {
        var membershipResponse = membershipService.updateMembership(membershipRequest);
        return ResponseEntity.ok(membershipResponse);
    }
}
