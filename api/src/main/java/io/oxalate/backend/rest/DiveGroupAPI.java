package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.DiveGroupRequest;
import io.oxalate.backend.api.request.DiveGroupUpdateRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.DiveGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST contract for managing dive groups. A dive group always belongs to a single dive event and its members are
 * participants of that dive event. All endpoints require an authenticated user.
 */
@Tag(name = "DiveGroupAPI", description = "Dive group REST endpoints")
public interface DiveGroupAPI {
    String BASE_PATH = API + "/dive-groups";

    @Operation(description = "Get all dive groups of a specific dive event", tags = "DiveGroupAPI")
    @Parameter(name = "eventId", description = "ID of the dive event for which the dive groups are fetched", example = "42", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dive groups retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/events/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DiveGroupResponse>> getDiveGroupsByEventId(@NotNull @PathVariable("eventId") long eventId);

    @Operation(description = "Get a single dive group", tags = "DiveGroupAPI")
    @Parameter(name = "diveGroupId", description = "ID of the dive group", example = "1", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dive group retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/{diveGroupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DiveGroupResponse> getDiveGroupById(@NotNull @PathVariable("diveGroupId") long diveGroupId);

    @Operation(description = "Create a new dive group. The owner is automatically added as a member of the group. A user may only own a single dive group "
            + "per dive event. Only organizers and administrators may assign another user as the owner.", tags = "DiveGroupAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New dive group request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dive group created successfully"),
            @ApiResponse(responseCode = "400", description = "Creation failed due to invalid or conflicting data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DiveGroupResponse> createDiveGroup(@RequestBody DiveGroupRequest diveGroupRequest);

    @Operation(description = "Update an existing dive group. Only the owner of the group, or the organizer of the dive event, may update the group.",
            tags = "DiveGroupAPI")
    @Parameter(name = "diveGroupId", description = "ID of the dive group to update", example = "1", required = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated dive group request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dive group updated successfully"),
            @ApiResponse(responseCode = "400", description = "Update failed due to invalid or conflicting data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PutMapping(path = BASE_PATH + "/{diveGroupId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DiveGroupResponse> updateDiveGroup(@NotNull @PathVariable("diveGroupId") long diveGroupId,
            @RequestBody DiveGroupUpdateRequest diveGroupUpdateRequest);

    @Operation(description = "Delete a dive group. All members are removed from the group. Only the owner of the group, or the organizer of the dive event, "
            + "may delete the group.", tags = "DiveGroupAPI")
    @Parameter(name = "diveGroupId", description = "ID of the dive group to delete", example = "1", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dive group deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Deletion failed because the dive event can no longer be modified"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/{diveGroupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> deleteDiveGroup(@NotNull @PathVariable("diveGroupId") long diveGroupId);

    @Operation(description = "Join a dive group as the currently authenticated user. The user must be a participant of the dive event.", tags = "DiveGroupAPI")
    @Parameter(name = "diveGroupId", description = "ID of the dive group to join", example = "1", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dive group joined successfully"),
            @ApiResponse(responseCode = "400", description = "Joining failed, the user may already belong to a group of the same dive event"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/{diveGroupId}/members", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DiveGroupResponse> joinDiveGroup(@NotNull @PathVariable("diveGroupId") long diveGroupId);

    @Operation(description = "Leave a dive group as the currently authenticated user. If the user is the owner, the ownership is transferred to the next "
            + "user that joined the group. If no members remain, the group is removed.", tags = "DiveGroupAPI")
    @Parameter(name = "diveGroupId", description = "ID of the dive group to leave", example = "1", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dive group left successfully"),
            @ApiResponse(responseCode = "400", description = "Leaving failed, the user may not be a member of the group"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/{diveGroupId}/members", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> leaveDiveGroup(@NotNull @PathVariable("diveGroupId") long diveGroupId);

    @Operation(description = "Add another user to a dive group. Only organizers of the dive event and administrators may add other users. The user must be a "
            + "participant of the dive event.", tags = "DiveGroupAPI")
    @Parameter(name = "diveGroupId", description = "ID of the dive group", example = "1", required = true)
    @Parameter(name = "userId", description = "ID of the user to add to the dive group", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User added to the dive group successfully"),
            @ApiResponse(responseCode = "400", description = "Adding failed, the user may not be a participant or may already belong to a group"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/{diveGroupId}/members/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DiveGroupResponse> addMemberToDiveGroup(@NotNull @PathVariable("diveGroupId") long diveGroupId,
            @NotNull @PathVariable("userId") long userId);

    @Operation(description = "Remove another user from a dive group. Only the owner of the group, the organizer of the dive event, or an administrator may "
            + "remove other users.", tags = "DiveGroupAPI")
    @Parameter(name = "diveGroupId", description = "ID of the dive group", example = "1", required = true)
    @Parameter(name = "userId", description = "ID of the user to remove from the dive group", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User removed from the dive group successfully"),
            @ApiResponse(responseCode = "400", description = "Removal failed, the user may not be a member of the group"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Dive group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/{diveGroupId}/members/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> removeMemberFromDiveGroup(@NotNull @PathVariable("diveGroupId") long diveGroupId,
            @NotNull @PathVariable("userId") long userId);
}
