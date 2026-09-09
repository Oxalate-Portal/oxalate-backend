package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.DiveGroupRequest;
import io.oxalate.backend.api.request.DiveGroupUpdateRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.DiveGroupResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ADD_MEMBER_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ADD_MEMBER_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ADD_MEMBER_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_CREATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_DELETE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_DELETE_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_DELETE_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_GET_BY_EVENT_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_GET_BY_EVENT_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_GET_BY_EVENT_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_GET_SINGLE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_GET_SINGLE_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_GET_SINGLE_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_JOIN_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_JOIN_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_JOIN_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_LEAVE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_LEAVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_LEAVE_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_REMOVE_MEMBER_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_REMOVE_MEMBER_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_REMOVE_MEMBER_START;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_UPDATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_UPDATE_START;
import io.oxalate.backend.rest.DiveGroupAPI;
import io.oxalate.backend.service.DiveGroupService;
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
@AuditSource("DiveGroupController")
public class DiveGroupController implements DiveGroupAPI {

    private final DiveGroupService diveGroupService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_GET_BY_EVENT_START, okMessage = DIVE_GROUPS_GET_BY_EVENT_OK, failMessage = DIVE_GROUPS_GET_BY_EVENT_FAIL)
    public ResponseEntity<List<DiveGroupResponse>> getDiveGroupsByEventId(long eventId) {
        var diveGroups = diveGroupService.getDiveGroupsByEventId(eventId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(diveGroups);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_GET_SINGLE_START, okMessage = DIVE_GROUPS_GET_SINGLE_OK, failMessage = DIVE_GROUPS_GET_SINGLE_FAIL)
    public ResponseEntity<DiveGroupResponse> getDiveGroupById(long diveGroupId) {
        var diveGroup = diveGroupService.getDiveGroupById(diveGroupId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(diveGroup);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_CREATE_START, okMessage = DIVE_GROUPS_CREATE_OK, failMessage = DIVE_GROUPS_CREATE_FAIL)
    public ResponseEntity<DiveGroupResponse> createDiveGroup(DiveGroupRequest diveGroupRequest) {
        var diveGroup = diveGroupService.createDiveGroup(diveGroupRequest, AuthTools.getCurrentUserId(), AuthTools.currentUserHasRole(ROLE_ADMIN),
                AuthTools.currentUserHasRole(ROLE_ORGANIZER));
        return ResponseEntity.status(HttpStatus.OK)
                             .body(diveGroup);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_UPDATE_START, okMessage = DIVE_GROUPS_UPDATE_OK, failMessage = DIVE_GROUPS_UPDATE_FAIL)
    public ResponseEntity<DiveGroupResponse> updateDiveGroup(long diveGroupId, DiveGroupUpdateRequest diveGroupUpdateRequest) {
        var diveGroup = diveGroupService.updateDiveGroup(diveGroupId, diveGroupUpdateRequest, AuthTools.getCurrentUserId(),
                AuthTools.currentUserHasRole(ROLE_ADMIN), AuthTools.currentUserHasRole(ROLE_ORGANIZER));
        return ResponseEntity.status(HttpStatus.OK)
                             .body(diveGroup);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_DELETE_START, okMessage = DIVE_GROUPS_DELETE_OK, failMessage = DIVE_GROUPS_DELETE_FAIL)
    public ResponseEntity<ActionResponse> deleteDiveGroup(long diveGroupId) {
        var actionResponse = diveGroupService.deleteDiveGroup(diveGroupId, AuthTools.getCurrentUserId(), AuthTools.currentUserHasRole(ROLE_ADMIN),
                AuthTools.currentUserHasRole(ROLE_ORGANIZER));
        return ResponseEntity.status(HttpStatus.OK)
                             .body(actionResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_JOIN_START, okMessage = DIVE_GROUPS_JOIN_OK, failMessage = DIVE_GROUPS_JOIN_FAIL)
    public ResponseEntity<DiveGroupResponse> joinDiveGroup(long diveGroupId) {
        var diveGroup = diveGroupService.joinDiveGroup(diveGroupId, AuthTools.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.OK)
                             .body(diveGroup);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_LEAVE_START, okMessage = DIVE_GROUPS_LEAVE_OK, failMessage = DIVE_GROUPS_LEAVE_FAIL)
    public ResponseEntity<ActionResponse> leaveDiveGroup(long diveGroupId) {
        var actionResponse = diveGroupService.leaveDiveGroup(diveGroupId, AuthTools.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.OK)
                             .body(actionResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_ADD_MEMBER_START, okMessage = DIVE_GROUPS_ADD_MEMBER_OK, failMessage = DIVE_GROUPS_ADD_MEMBER_FAIL)
    public ResponseEntity<DiveGroupResponse> addMemberToDiveGroup(long diveGroupId, long userId) {
        var diveGroup = diveGroupService.addMemberToDiveGroup(diveGroupId, userId, AuthTools.getCurrentUserId(), AuthTools.currentUserHasRole(ROLE_ADMIN),
                AuthTools.currentUserHasRole(ROLE_ORGANIZER));
        return ResponseEntity.status(HttpStatus.OK)
                             .body(diveGroup);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = DIVE_GROUPS_REMOVE_MEMBER_START, okMessage = DIVE_GROUPS_REMOVE_MEMBER_OK, failMessage = DIVE_GROUPS_REMOVE_MEMBER_FAIL)
    public ResponseEntity<ActionResponse> removeMemberFromDiveGroup(long diveGroupId, long userId) {
        var actionResponse = diveGroupService.removeMemberFromDiveGroup(diveGroupId, userId, AuthTools.getCurrentUserId(),
                AuthTools.currentUserHasRole(ROLE_ADMIN), AuthTools.currentUserHasRole(ROLE_ORGANIZER));
        return ResponseEntity.status(HttpStatus.OK)
                             .body(actionResponse);
    }
}
