package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.MembershipRequest;
import io.oxalate.backend.api.response.MembershipResponse;
import io.oxalate.backend.rest.MembershipAPI;
import io.oxalate.backend.service.MembershipService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class MembershipController implements MembershipAPI {

    private final MembershipService membershipService;

    @Override
    public ResponseEntity<List<MembershipResponse>> getAllActiveMemberships(HttpServletRequest request) {
        var membershipResponses = membershipService.getAllActiveMemberships();
        return ResponseEntity.ok(membershipResponses);
    }

    @Override
    public ResponseEntity<List<MembershipResponse>> getMembershipsForUser(long userId, HttpServletRequest request) {
        var membershipResponse = membershipService.getMembershipsForUser(userId);
        return ResponseEntity.ok(membershipResponse);
    }

    @Override
    public ResponseEntity<MembershipResponse> createMembership(MembershipRequest membershipRequest, HttpServletRequest request) {
        var membershipResponse = membershipService.createMembership(membershipRequest);
        return ResponseEntity.ok(membershipResponse);
    }

    @Override
    public ResponseEntity<MembershipResponse> updateMembership(MembershipRequest membershipRequest, HttpServletRequest request) {
        var membershipResponse = membershipService.updateMembership(membershipRequest);
        return ResponseEntity.ok(membershipResponse);
    }
}
