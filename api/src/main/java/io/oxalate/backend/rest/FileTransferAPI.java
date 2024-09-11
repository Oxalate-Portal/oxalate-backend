package io.oxalate.backend.rest;

import static io.oxalate.backend.api.UploadDirectoryConstants.AVATARS;
import static io.oxalate.backend.api.UploadDirectoryConstants.CERTIFICATES;
import static io.oxalate.backend.api.UploadDirectoryConstants.DIVE_PLANS;
import static io.oxalate.backend.api.UploadDirectoryConstants.DOCUMENTS;
import static io.oxalate.backend.api.UploadDirectoryConstants.PAGE_FILES;
import static io.oxalate.backend.api.UrlConstants.API;
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

@Tag(name = "FileTransferAPI", description = "File Upload and Download REST endpoints")
public interface FileTransferAPI {
    String BASE_PATH = API + "/files";

    /* Page */
    @Operation(description = "Upload a file belonging to a specific page language version, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @Parameter(name = "language", description = "Which language is this file for", example = "de", required = true)
    @Parameter(name = "pageId", description = "Which page is this upload for", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/upload/" + PAGE_FILES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadPageFile(@RequestPart("uploadFile") MultipartFile uploadFile,
            @RequestParam("language") String language,
            @RequestParam("pageId") long pageId,
            HttpServletRequest request);

    @Operation(description = "Download a page file", tags = "FileTransferAPI")
    @Parameter(name = "pageId", description = "Page ID of the page the file belongs to", required = true, example = "11")
    @Parameter(name = "language", description = "Language code is given with 2 characters as per ISO-639-1", required = true, example = "en")
    @Parameter(name = "fileName", description = "Name of the file to be downloaded. There are no path parts", required = true, example = "image.png")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/download/" + PAGE_FILES + "/{pageId}/{language}/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadPageFile(@PathVariable("pageId") long pageId,
            @PathVariable("language") String language,
            @PathVariable("fileName") String fileName, HttpServletRequest request);

    /* Certificate */
    @Operation(description = "Upload a certificate file belonging to a specific user, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @Parameter(name = "certificateId", description = "Certificate ID of the certificate the file belongs to", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/upload/" + CERTIFICATES + "/{certificateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadCertificateFile(
            @RequestPart("uploadFile") MultipartFile uploadFile,
            @PathVariable("certificateId") long certificateId,
            HttpServletRequest request);

    @Operation(description = "Download a certificate photocopy", tags = "FileTransferAPI")
    @Parameter(name = "certificateId", description = "Certificate ID of the certificate the file belongs to", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/download/" + CERTIFICATES + "/{certificateId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadCertificateFile(@PathVariable("certificateId") long certificateId, HttpServletRequest request);

    /* Document */
    @Operation(description = "Upload a document not linked to user or page, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/upload/" + DOCUMENTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadDocumentFile(@RequestPart("uploadFile") MultipartFile uploadFile, HttpServletRequest request);

    @Operation(description = "Download a document file", tags = "FileTransferAPI")
    @Parameter(name = "documentId", description = "document ID of the document to be downloaded", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/download/" + DOCUMENTS + "/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadDocumentFile(@PathVariable("documentId") long documentId, HttpServletRequest request);

    /* Dive plan */
    @Operation(description = "Upload a dive plan linked to a dive group, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @Parameter(name = "diveGroupId", description = "Which dive group is this upload for", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/upload/" + DIVE_PLANS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadDivePlanFile(@RequestPart("uploadFile") MultipartFile uploadFile, @RequestParam("diveGroupId") long pageId, HttpServletRequest request);

    @Operation(description = "Download a dive plan file", tags = "FileTransferAPI")
    @Parameter(name = "divePlanId", description = "document ID of the document to be downloaded", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/download/" + DIVE_PLANS + "/{divePlanId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadDivePlanFile(@PathVariable("divePlanId") long divePlanId, HttpServletRequest request);

    /* Avatar */
    @Operation(description = "Upload an avatar linked to a user, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH + "/upload/" + AVATARS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadAvatarFile(@RequestPart("uploadFile") MultipartFile uploadFile, HttpServletRequest request);

    @Operation(description = "Download an avatar file", tags = "FileTransferAPI")
    @Parameter(name = "avatarId", description = "Avatar ID of the avatar to be downloaded", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/download/" + AVATARS + "/{avatarId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadAvatarFile(@PathVariable("avatarId") long avatarId, HttpServletRequest request);
}
