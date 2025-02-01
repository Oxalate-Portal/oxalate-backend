package io.oxalate.backend.rest;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.TermRequest;
import io.oxalate.backend.api.request.UserStatusRequest;
import io.oxalate.backend.api.request.UserUpdateRequest;
import io.oxalate.backend.api.response.AdminUserResponse;
import io.oxalate.backend.api.response.ListUserResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "UserAPI", description = "User REST endpoints")
public interface UserAPI {
    String BASE_PATH = API + "/users";

    @Operation(description = "Get a list of all users", tags = "UserAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<AdminUserResponse>> getUsers(HttpServletRequest request);

    @Operation(description = "Get a list of all users with given role", tags = "UserAPI")
    @Parameter(name = "role", description = "Role by which the users should be filtered", example = "ROLE_USER", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/role/{role}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<ListUserResponse>> getUserIdNameListWithRole(@PathVariable("role") RoleEnum roleEnum, HttpServletRequest request);

    @Operation(description = "Show specific user details", tags = "UserAPI")
    @Parameter(name = "userId", description = "User ID to be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AdminUserResponse> getUserDetails(@PathVariable("userId") long userId, HttpServletRequest request);

    @Operation(description = "Update user information, except password", tags = "UserAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user information", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AdminUserResponse> updateUser(@RequestBody UserUpdateRequest updateRequest, HttpServletRequest request);

    @Operation(description = "Update the status of the given user", tags = "UserAPI")
    @Parameter(name = "userId", description = "User ID whose status should be updated", example = "123", required = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "UserStatusRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "500", description = "Given user not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = BASE_PATH + "/{userId}/status")
    ResponseEntity<Void> updateUserStatus(@PathVariable("userId") long userId, @RequestBody UserStatusRequest userStatusRequest, HttpServletRequest request);

    @Operation(description = "Receive answer to whether the user accepts or rejects the terms", tags = "UserAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "TermRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Term answer registered successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = BASE_PATH + "/accept-terms")
    ResponseEntity<Void> recordTermAnswer(@RequestBody TermRequest termRequest, HttpServletRequest request);

    @Operation(description = "Reset term and conditions answer for all users, forcing them to re-approve", tags = "UserAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Term answer reset successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = BASE_PATH + "/reset-terms", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> resetTermAnswer(HttpServletRequest request);

}
