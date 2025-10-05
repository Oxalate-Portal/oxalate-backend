package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import io.oxalate.backend.api.TagGroupEnum;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.TagGroupRequest;
import io.oxalate.backend.api.request.TagRequest;
import io.oxalate.backend.api.response.TagGroupResponse;
import io.oxalate.backend.api.response.TagResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "TagAPI", description = "Tag management REST endpoints")
public interface TagAPI {
    String TAG_PATH = API + "/tags";
    String GROUPS_PATH = API + "/tag-groups";

    // ---- Tag Groups ----

    @Operation(description = "Get all tag groups", tags = "TagAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = GROUPS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TagGroupResponse>> getAllTagGroups(HttpServletRequest request);

    @Operation(description = "Get tag group by ID", tags = "TagAPI")
    @Parameter(name = "id", description = "Tag group ID", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag group retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Tag group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = GROUPS_PATH + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagGroupResponse> getTagGroupById(@PathVariable(name = "id") long id, HttpServletRequest request);

    @Operation(description = "Create a new tag group", tags = "TagAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Tag group to create", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag group created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = GROUPS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagGroupResponse> createTagGroup(@RequestBody TagGroupRequest tagGroupRequest, HttpServletRequest request);

    @Operation(description = "Update an existing tag group", tags = "TagAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated tag group", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag group updated successfully"),
            @ApiResponse(responseCode = "404", description = "Tag group not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PutMapping(path = GROUPS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagGroupResponse> updateTagGroup(@RequestBody TagGroupRequest tagGroupRequest,
            HttpServletRequest request);

    @Operation(description = "Delete a tag group", tags = "TagAPI")
    @Parameter(name = "id", description = "Tag group ID", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag group deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Tag group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = GROUPS_PATH + "/{id}")
    ResponseEntity<Void> deleteTagGroup(@PathVariable(name = "id") long id, HttpServletRequest request);

    @Operation(description = "Get tag groups by type", tags = "TagAPI")
    @Parameter(name = "type", description = "Tag group type", example = "USER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = GROUPS_PATH + "/type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TagGroupResponse>> getTagGroupsByType(
            @PathVariable(name = "type") TagGroupEnum type,
            HttpServletRequest request);

    // ---- Tags ----

    @Operation(description = "Get all tags (optionally filtered by tag group ID)", tags = "TagAPI")
    @Parameter(name = "groupId", description = "Optional tag group ID filter", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = TAG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TagResponse>> getAllTags(@RequestParam(value = "groupId", required = false) Long groupId, HttpServletRequest request);

    @Operation(description = "Get tag by ID", tags = "TagAPI")
    @Parameter(name = "id", description = "Tag ID", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Tag not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = TAG_PATH + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagResponse> getTagById(@PathVariable(name = "id") long id, HttpServletRequest request);

    @Operation(description = "Create a new tag", tags = "TagAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Tag to create", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = TAG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagResponse> createTag(@RequestBody TagRequest tag, HttpServletRequest request);

    @Operation(description = "Update an existing tag", tags = "TagAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated tag", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag updated successfully"),
            @ApiResponse(responseCode = "404", description = "Tag not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PutMapping(path = TAG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagResponse> updateTag(@RequestBody TagRequest tag, HttpServletRequest request);

    @Operation(description = "Delete a tag", tags = "TagAPI")
    @Parameter(name = "id", description = "Tag ID", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Tag not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = TAG_PATH + "/{id}")
    ResponseEntity<Void> deleteTag(@PathVariable(name = "id") long id, HttpServletRequest request);

    @Operation(description = "Get tags by group type", tags = "TagAPI")
    @Parameter(name = "type", description = "Tag group type", example = "USER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = TAG_PATH + "/group-type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TagResponse>> getTagsByGroupType(
            @PathVariable(name = "type") TagGroupEnum type,
            HttpServletRequest request);
}
