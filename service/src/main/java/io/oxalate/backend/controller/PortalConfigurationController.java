package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.PortalConfigurationRequest;
import io.oxalate.backend.api.response.FrontendConfigurationResponse;
import io.oxalate.backend.api.response.PortalConfigurationResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_FRONTEND_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_GET_FRONTEND_START;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_RELOAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_RELOAD_START;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PORTAL_CONFIG_UPDATE_START;
import io.oxalate.backend.rest.PortalConfigurationAPI;
import io.oxalate.backend.service.PortalConfigurationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("PortalConfigurationController")
public class PortalConfigurationController implements PortalConfigurationAPI {
    private final PortalConfigurationService portalConfigurationService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = PORTAL_CONFIG_GET_ALL_START, okMessage = PORTAL_CONFIG_GET_ALL_OK)
    public ResponseEntity<List<PortalConfigurationResponse>> getAllConfigurations() {
        var configurations = portalConfigurationService.getAllConfigurations();
        return ResponseEntity.ok(configurations);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = PORTAL_CONFIG_UPDATE_START, okMessage = PORTAL_CONFIG_UPDATE_OK)
    public ResponseEntity<PortalConfigurationResponse> updateConfigurationValue(PortalConfigurationRequest portalConfigurationRequest) {
        portalConfigurationService.updateConfigurationValue(portalConfigurationRequest);
        return null;
    }

    @Override
    @Audited(startMessage = PORTAL_CONFIG_GET_FRONTEND_START, okMessage = PORTAL_CONFIG_GET_FRONTEND_OK)
    public ResponseEntity<List<FrontendConfigurationResponse>> getFrontendConfigurations() {
        var frontendConfigResponses = portalConfigurationService.getFrontendConfigurations();
        return ResponseEntity.ok(frontendConfigResponses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = PORTAL_CONFIG_RELOAD_START, okMessage = PORTAL_CONFIG_RELOAD_OK)
    public ResponseEntity<List<PortalConfigurationResponse>> reloadPortalConfigurations() {
        portalConfigurationService.reloadPortalConfigurations();
        var updatedConfigurations = portalConfigurationService.reloadPortalConfigurations();
        return ResponseEntity.ok(updatedConfigurations);
    }
}
