package io.oxalate.backend.rest;

import io.oxalate.backend.api.response.AuditEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AuditAPI", description = "Audit REST endpoints")
public interface AuditAPI {

    String BASE_PATH = "/api/audits";

    @Operation(description = "Get all audit entries", tags = "AuditAPI")
    @Parameter(name = "page", description = "Index of page to be retrieved", example = "3")
    @Parameter(name = "pageSize", description = "Size of the page to be retrieved", example = "10")
    @Parameter(name = "sorting", description = "What field the page should be sorted by, and which direction. The two values should be comma separated",
            example = "createdAt,desc")
    @Parameter(name = "filter", description = "Filter the column by the string", example = "Find me")
    @Parameter(name = "filterColumn", description = "By which column should the filtering be done", example = "message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of audit entries retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Page<AuditEntryResponse>> getAuditEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sorting", defaultValue = "createdAt,desc") String sorting,
            @RequestParam(name = "filter", defaultValue = "") String filter,
            @RequestParam(name = "filterColumn", defaultValue = "") String filterColumn,
            HttpServletRequest request);

    @Operation(description = "Get all audit entries of a user", tags = "AuditAPI")
    @Parameter(name = "userId", description = "User ID who's certificates should be retrieved", example = "123", required = true)
    @Parameter(name = "page", description = "Index of page to be retrieved", example = "3")
    @Parameter(name = "pageSize", description = "Size of the page to be retrieved", example = "10")
    @Parameter(name = "sorting", description = "What field the page should be sorted by, and which direction. The two values should be comma separated",
            example = "createdAt,desc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of audit entries for a user retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Page<AuditEntryResponse>> getAuditEventsByUserId(
            @PathVariable("userId") long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sorting", defaultValue = "createdAt,desc") String sorting,
            HttpServletRequest request
    );
}
