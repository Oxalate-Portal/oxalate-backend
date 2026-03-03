package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.BlockedDateRequest;
import io.oxalate.backend.api.response.BlockedDateResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_ADD_OK;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_ADD_START;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_ADD_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_REMOVE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_REMOVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.BLOCKED_DATE_REMOVE_START;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.rest.BlockedDateAPI;
import io.oxalate.backend.service.BlockedDateService;
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
@AuditSource("BlockedDateController")
public class BlockedDateController implements BlockedDateAPI {
    private final BlockedDateService blockedDateService;

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = BLOCKED_DATE_GET_ALL_START, okMessage = BLOCKED_DATE_GET_ALL_OK)
    public ResponseEntity<List<BlockedDateResponse>> getAllBlockedDates() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            throw new OxalateUnauthorizedException(BLOCKED_DATE_GET_ALL_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var blockedDates = blockedDateService.getAllBlockedDates();
        return ResponseEntity.ok(blockedDates);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = BLOCKED_DATE_ADD_START, okMessage = BLOCKED_DATE_ADD_OK)
    public ResponseEntity<BlockedDateResponse> addBlockedDate(BlockedDateRequest blockedDateRequest) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(BLOCKED_DATE_ADD_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var userId = AuthTools.getCurrentUserId();
        var blockedDate = blockedDateService.createBlock(blockedDateRequest, userId);
        return ResponseEntity.ok(blockedDate);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = BLOCKED_DATE_REMOVE_START, okMessage = BLOCKED_DATE_REMOVE_OK)
    public ResponseEntity<Void> removeBlockedDate(long blockedDateId) {
        var blockedDateResponse = blockedDateService.findById(blockedDateId);

        if (blockedDateResponse == null) {
            throw new OxalateNotFoundException(BLOCKED_DATE_REMOVE_NOT_FOUND);
        }

        blockedDateService.removeBlock(blockedDateId);
        return ResponseEntity.ok().build();
    }
}
