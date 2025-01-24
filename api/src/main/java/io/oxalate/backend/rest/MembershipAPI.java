package io.oxalate.backend.rest;

import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.MembershipRequest;
import io.oxalate.backend.api.response.MembershipResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "MembershipAPI", description = "Event REST endpoints")
public interface MembershipAPI {
    String BASE_PATH = API + "/memberships";

    @Operation(description = "Get a list of all active memberships", tags = "MembershipAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MembershipResponse>> getAllActiveMemberships(HttpServletRequest request);

    @Operation(description = "Get the memberships by the id", tags = "MembershipAPI")
    @Parameter(name = "id", description = "Membership ID for which the membership should be fetched", example = "123")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MembershipResponse> getMembership(@PathVariable(name = "id") long membershipId, HttpServletRequest request);

    @Operation(description = "Get a list of all memberships for a specific user", tags = "MembershipAPI")
    @Parameter(name = "userId", description = "User ID for which all memberships should be fetched", example = "123")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MembershipResponse>> getMembershipsForUser(@PathVariable(name = "userId") long userId, HttpServletRequest request);

    @Operation(description = "Create a new membership", tags = "MembershipAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New membership request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New membership created successfully"),
            @ApiResponse(responseCode = "400", description = "Creation failed, the membership may conflict with an existing one"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MembershipResponse> createMembership(@RequestBody MembershipRequest membershipRequest, HttpServletRequest request);

    @Operation(description = "Update a given membership", tags = "MembershipAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated membership request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Membership updated successfully"),
            @ApiResponse(responseCode = "404", description = "Membership does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MembershipResponse> updateMembership(@RequestBody MembershipRequest membershipRequest, HttpServletRequest request);
}
