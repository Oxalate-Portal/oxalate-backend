package io.oxalate.backend.rest;

import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "PageAPI", description = "Page REST endpoints")
public interface PageAPI {
    String BASE_PATH = "/api/pages";

    @Operation(description = "Get list of all paths", tags = "PageAPI")
    @Parameter(name = "language", description = "Language", example = "fi")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/navigation-elements", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PageGroupResponse>> getNavigationElements(@RequestParam(name = "language") String language, HttpServletRequest request);

    @Operation(description = "Get all pages for a given path and language", tags = "PageAPI")
    @Parameter(name = "path", description = "Path of the pages that should be fetched", example = "/info", required = true)
    @Parameter(name = "language", description = "Language", example = "fi")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/by-path", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PageGroupResponse>> getPagesByPath(@RequestParam(name = "path") String path,
            @RequestParam(name = "language") String language,
            HttpServletRequest request);

    @Operation(description = "Get page by the given page ID", tags = "PageAPI")
    @Parameter(name = "pageId", description = "Page ID to be retrieved", example = "1", required = true)
    @Parameter(name = "language", description = "Language, optional. If not given then will default to fi", example = "en")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User has not permission to view page"),
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageResponse> getPageById(@PathVariable(name = "pageId") long pageId,
            @RequestParam(name = "language", required = false) String language,
            HttpServletRequest request);
}
