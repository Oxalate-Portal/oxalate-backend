package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.PagedRequest;
import static io.oxalate.backend.api.request.PagedRequest.CASE_SENSITIVE_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.DIRECTION_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.PAGE_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.SIZE_DESCRIPTION;
import io.oxalate.backend.api.response.AuditEntryResponse;
import io.oxalate.backend.api.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AuditAPI", description = "Audit REST endpoints")
public interface AuditAPI {

    String BASE_PATH = API + "/audits";

    String SORTABLE_COLUMNS_DESCRIPTION = "Column to sort by, one of: id, user_id, user_name, level, trace_id, ip_address, address, source, message, "
            + "created_at. Unknown values fall back to created_at";

    @Operation(description = "Get a page of audit entries, optionally filtered by one column", tags = "AuditAPI")
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "3")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "10")
    @Parameter(name = "sort_by", description = SORTABLE_COLUMNS_DESCRIPTION, example = "created_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @Parameter(name = "search", description = "Text the column given in filter_column must contain. Ignored without a filter_column", example = "Find me")
    @Parameter(name = "case_sensitive", description = CASE_SENSITIVE_DESCRIPTION, example = "false")
    @Parameter(name = "filter_column", description = "Which column the search applies to, one of: user_name, trace_id, source, address, ip_address, message. "
            + "With user_name the search matches user first or last names and returns the entries of the matching users", example = "message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of audit entries retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<AuditEntryResponse>> getAuditEvents(@RequestBody PagedRequest pagedRequest);

    @Operation(description = "Get a page of the audit entries of a user", tags = "AuditAPI")
    @Parameter(name = "userId", description = "User ID whose audit entries should be retrieved", example = "123", required = true)
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "3")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "10")
    @Parameter(name = "sort_by", description = SORTABLE_COLUMNS_DESCRIPTION, example = "created_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of audit entries for a user retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH + "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<AuditEntryResponse>> getAuditEventsByUserId(@PathVariable("userId") long userId,
            @RequestBody PagedRequest pagedRequest);
}
