package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import io.oxalate.backend.api.request.PortalConfigurationRequest;
import io.oxalate.backend.api.response.FrontendConfigurationResponse;
import io.oxalate.backend.api.response.PortalConfigurationResponse;
import io.oxalate.backend.api.response.PortalConfigurationStatusResponse;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_FRONTEND_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_FRONTEND_START;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_RELOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_RELOAD_START;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_UPDATE_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.PortalConfigurationAPI;
import io.oxalate.backend.service.PortalConfigurationService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PortalConfigurationController implements PortalConfigurationAPI {
    private final PortalConfigurationService portalConfigurationService;
    private static final String AUDIT_NAME = "PortalConfigurationController";
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PortalConfigurationResponse>> getAllConfigurations(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(PORTAL_CONFIG_GET_ALL_START, INFO, request, AUDIT_NAME, userId);
        var configurations = portalConfigurationService.getAllConfigurations();
        appEventPublisher.publishAuditEvent(PORTAL_CONFIG_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);

        return ResponseEntity.ok(configurations);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PortalConfigurationResponse> updateConfigurationValue(PortalConfigurationRequest portalConfigurationRequest,
            HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(PORTAL_CONFIG_UPDATE_START, INFO, request, AUDIT_NAME, userId);

        portalConfigurationService.updateConfigurationValue(portalConfigurationRequest);

        appEventPublisher.publishAuditEvent(PORTAL_CONFIG_UPDATE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return null;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'USER')")
    public ResponseEntity<List<FrontendConfigurationResponse>> getFrontendConfigurations(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(PORTAL_CONFIG_GET_FRONTEND_START, INFO, request, AUDIT_NAME, userId);

        var frontendConfigResponses = portalConfigurationService.getFrontendConfigurations();

        appEventPublisher.publishAuditEvent(PORTAL_CONFIG_GET_FRONTEND_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(frontendConfigResponses);
    }

    @Override
    public ResponseEntity<PortalConfigurationStatusResponse> reloadPortalConfigurations(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(PORTAL_CONFIG_RELOAD_START, INFO, request, AUDIT_NAME, userId);

        portalConfigurationService.reloadPortalConfigurations();

        var status = PortalConfigurationStatusResponse.builder()
                                                      .success(true)
                                                      .message("Portal configurations reloaded successfully")
                                                      .build();

        appEventPublisher.publishAuditEvent(PORTAL_CONFIG_RELOAD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(status);
    }
}
