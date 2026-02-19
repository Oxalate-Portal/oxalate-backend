package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.BlockedDateRequest;
import io.oxalate.backend.api.response.BlockedDateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "BlockedDateAPI", description = "Date blocking REST endpoints")
public interface BlockedDateAPI {
    String BASE_PATH = API + "/blocked-dates";

    @Operation(description = "Get all blocked dates", tags = "BlockedDateAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of blocked dates retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<BlockedDateResponse>> getAllBlockedDates();

    @Operation(description = "Get all blocked dates", tags = "BlockedDateAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "BlockedDateRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blocked date added successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BlockedDateResponse> addBlockedDate(@RequestBody BlockedDateRequest blockedDateRequest);

    @Operation(description = "Remove a blocked date", tags = "BlockedDateAPI")
    @Parameter(name = "blockedDateId", description = "Blocked date ID that should be removed", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blocked date removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "404", description = "Given blocked date ID not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(value = BASE_PATH + "/{blockedDateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> removeBlockedDate(@PathVariable("blockedDateId") long blockedDateId);
}
