package io.oxalate.backend.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "UploadAPI", description = "File Upload and Download REST endpoints")
public interface UploadAPI {
    String BASE_PATH = "/api/files";

    @Operation(description = "Upload a file belonging to a specific page language version", tags = "UploadAPI")
    @Parameter(name = "upload", description = "File to be uploaded", required = true)
    @Parameter(name = "language", description = "Which language is this file for", example = "de", required = true)
    @Parameter(name = "pageId", description = "Which page is this upload for", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadFile(@RequestPart("upload") MultipartFile file,
            @RequestParam("language") String language,
            @RequestParam("pageId") long pageId,
            HttpServletRequest request);

    @Operation(description = "Download a page file", tags = "UploadAPI")
    @Parameter(name = "pageId", description = "Page ID of the page the file belongs to", required = true, example = "11")
    @Parameter(name = "language", description = "Language code is given with 2 characters as per ISO-639-1", required = true, example = "en")
    @Parameter(name = "fileName", description = "Name of the file to be downloaded. There are no path parts", required = true, example = "image.png")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/download/{pageId}/{language}/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadFile(@PathVariable("pageId") long pageId,
            @PathVariable("language") String language,
            @PathVariable("fileName") String fileName, HttpServletRequest request);
}
