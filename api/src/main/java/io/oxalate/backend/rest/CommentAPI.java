package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.response.commenting.CommentResponse;
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

@Tag(name = "CommentAPI", description = "Comment (thread) retrieval endpoints")
public interface CommentAPI {
    String BASE_PATH = API + "/comments";

    @Operation(description = "Get all comments belonging to the given parent comment", tags = "CommentAPI")
    @Parameter(name = "parentId", description = "Parent ID of the comment from which all children should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment thread retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/{parentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CommentResponse> getCommentThread(@PathVariable("parentId") long parentId, HttpServletRequest request);

    @Operation(description = "Get all comments belonging to the given parent comment to the given depth", tags = "CommentAPI")
    @Parameter(name = "parentId", description = "Parent ID of the comment from which all children should be retrieved", example = "123", required = true)
    @Parameter(name = "depth", description = "To what depth the children should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment thread retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/{parentId}/{depth}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CommentResponse> getCommentThreadToDepth(@PathVariable("parentId") long parentId, @PathVariable("depth") long depth,
            HttpServletRequest request);

    @Operation(description = "Get specific comment", tags = "CommentAPI")
    @Parameter(name = "commentId", description = "Comment ID that should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/comment/{commentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CommentResponse> getComment(@PathVariable("commentId") long commentId, HttpServletRequest request);

    @Operation(description = "Add a comment", tags = "CommentAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CommentRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment added successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CommentResponse> addComment(@RequestBody CommentRequest commentRequest, HttpServletRequest request);

    @Operation(description = "Update a comment", tags = "CommentAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CommentRequest", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment updated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PutMapping(value = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CommentResponse> updateComment(@RequestBody CommentRequest commentRequest, HttpServletRequest request);

    @Operation(description = "Get all comments by user ID", tags = "CommentAPI")
    @Parameter(name = "userId", description = "User ID of the comment that should be retrieved", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<CommentResponse>> getCommentsByUserId(@PathVariable("userId") long userId, HttpServletRequest request);
}
