package io.oxalate.backend.rest;

import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.PageGroupRequest;
import io.oxalate.backend.api.request.PageRequest;
import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "PageManagementAPI", description = "Page management REST endpoints")
public interface PageManagementAPI {
    String BASE_PATH = API + "/page-management";

    // Page groups
    @Operation(description = "Get list of all paths", tags = "PageManagementAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/page-groups", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PageGroupResponse>> getPageGroups(HttpServletRequest request);

    @Operation(description = "Get the details of a given path", tags = "PageManagementAPI")
    @Parameter(name = "pageGroupId", description = "Page group ID of the page that should be fetched", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page group retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Page group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/page-groups/{pageGroupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageGroupResponse> getPageGroup(@PathVariable(name = "pageGroupId") long pageGroupId, HttpServletRequest request);

    @Operation(description = "Add a new path, the request contains several path requests, one for each language", tags = "PageManagementAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New path request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Path created successfully"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to create path"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/page-groups", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageGroupResponse> createPageGroup(@RequestBody PageGroupRequest pathRequests, HttpServletRequest request);

    @Operation(description = "Update a path, the request contains several path requests, one for each language", tags = "PageManagementAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New page request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Path updated successfully"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to update path"),
            @ApiResponse(responseCode = "404", description = "Path does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH + "/page-groups", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageGroupResponse> updatePageGroup(@RequestBody PageGroupRequest pathRequests, HttpServletRequest request);

    @Operation(description = "Close a path, this will close all language versions of the path as well as the pages", tags = "PageManagementAPI")
    @Parameter(name = "pageGroupId", description = "Page group ID to be closed", example = "/info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Path closed successfully"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to close path"),
            @ApiResponse(responseCode = "404", description = "Path does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(path = BASE_PATH + "/page-groups/{pageGroupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HttpStatus> closePageGroup(@PathVariable(name = "pageGroupId") long pageGroupId, HttpServletRequest request);

    // Pages
    @Operation(description = "Get list of all pages for a specific path ID", tags = "PageManagementAPI")
    @Parameter(name = "pageGroupId", description = "Page group ID of the pages that should be fetched", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PageResponse>> getPagesByPageGroupId(@RequestParam(name = "pageGroupId") long pageGroupId, HttpServletRequest request);

    @Operation(description = "Get page by the given page ID", tags = "PageAPI")
    @Parameter(name = "pageId", description = "Page ID to be retrieved", example = "/info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User has not permission to view page"),
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/pages/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageResponse> getPageById(@PathVariable(name = "pageId") long pageId, HttpServletRequest request);

    @Operation(description = "Add a new page, the request contains several page requests, one for each language", tags = "PageManagementAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New page request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page created successfully"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to create page"),
            @ApiResponse(responseCode = "404", description = "Page group does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/pages", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageResponse> createPage(@RequestBody PageRequest pageRequests, HttpServletRequest request);

    @Operation(description = "Update an existing page, the request contains several page requests, one for each language", tags = "PageManagementAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New page request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page updated successfully"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to update page"),
            @ApiResponse(responseCode = "404", description = "Page or page group not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH + "/pages", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageResponse> updatePage(@RequestBody PageRequest pageRequests, HttpServletRequest request);

    @Operation(description = "Close a page, this will close all language versions of the page", tags = "PageManagementAPI")
    @Parameter(name = "pageId", description = "Page ID to be closed", example = "/info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page closed successfully"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to close page"),
            @ApiResponse(responseCode = "404", description = "Page or page group not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(path = BASE_PATH + "/pages/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HttpStatus> closePage(@PathVariable(name = "pageId") long pageId, HttpServletRequest request);
}
