package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.CreateTokenRequest;
import io.oxalate.backend.api.request.InvalidateTokenRequest;
import io.oxalate.backend.api.request.PagedRequest;
import static io.oxalate.backend.api.request.PagedRequest.CASE_SENSITIVE_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.DIRECTION_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.PAGE_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.SEARCH_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.SIZE_DESCRIPTION;
import io.oxalate.backend.api.request.RefreshTokenRequest;
import io.oxalate.backend.api.response.PagedResponse;
import io.oxalate.backend.api.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "TokenAPI", description = "Third-party token management endpoints")
public interface TokenAPI {
    String BASE_PATH = API + "/tokens";

    @PostMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a third-party token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<TokenResponse> createToken(@Valid @RequestBody CreateTokenRequest request);

    @PostMapping(path = BASE_PATH + "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Refresh a third-party token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request);

    @DeleteMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Invalidate a third-party token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token invalidated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<Void> invalidateToken(@Valid @RequestBody InvalidateTokenRequest request);

    @GetMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List a page of third-party tokens")
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "0")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "25")
    @Parameter(name = "sort_by", description = "Column to sort by, one of: token_id, created_at, expires_at, description. "
            + "Unknown values fall back to created_at", example = "expires_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @Parameter(name = "search", description = SEARCH_DESCRIPTION + ": description, token value", example = "calendar")
    @Parameter(name = "case_sensitive", description = CASE_SENSITIVE_DESCRIPTION, example = "false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    ResponseEntity<PagedResponse<TokenResponse>> listTokens(@Parameter(hidden = true) PagedRequest pagedRequest);
}
