package io.oxalate.backend.rest;

import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.PortalConfigurationRequest;
import io.oxalate.backend.api.response.FrontendConfigurationResponse;
import io.oxalate.backend.api.response.PortalConfigurationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "PortalConfigurationAPI", description = "Portal configuration REST endpoints")
public interface PortalConfigurationAPI {
    String BASE_PATH = API + "/configurations";

    @Operation(description = "Get all configurations", tags = "PortalConfigurationAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PortalConfigurationResponse>> getAllConfigurations(HttpServletRequest request);

    @Operation(description = "Update a configuration value", tags = "PortalConfigurationAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Configuration update request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
            @ApiResponse(responseCode = "404", description = "Configuration does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PortalConfigurationResponse> updateConfigurationValue(@RequestBody PortalConfigurationRequest portalConfigurationRequest,
            HttpServletRequest request);

    @Operation(description = "Get frontend configurations", tags = "PortalConfigurationAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/frontend", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<FrontendConfigurationResponse>> getFrontendConfigurations(HttpServletRequest request);

    @Operation(description = "Reload portal configurations", tags = "PortalConfigurationAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configurations reloaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/reload", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PortalConfigurationResponse>> reloadPortalConfigurations(HttpServletRequest request);

}
