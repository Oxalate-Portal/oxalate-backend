package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UploadDirectoryConstants.AVATARS;
import static io.oxalate.backend.api.UploadDirectoryConstants.CERTIFICATES;
import static io.oxalate.backend.api.UploadDirectoryConstants.DIVE_FILES;
import static io.oxalate.backend.api.UploadDirectoryConstants.DOCUMENTS;
import static io.oxalate.backend.api.UploadDirectoryConstants.PAGE_FILES;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.filetransfer.AvatarFileResponse;
import io.oxalate.backend.api.response.filetransfer.CertificateFileResponse;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
import io.oxalate.backend.api.response.filetransfer.DocumentFileResponse;
import io.oxalate.backend.api.response.filetransfer.PageFileResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "FileTransferAPI", description = "File Upload and Download REST endpoints")
public interface FileTransferAPI {
    String BASE_PATH = API + "/files";

    /* ==== Avatar ==== */
    /* Find all */
    @Operation(description = "Get list of all avatar files", tags = "FileTransferAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + AVATARS, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<AvatarFileResponse>> findAllAvatarFiles();

    /* Upload */
    @Operation(description = "Upload an avatar linked to a user, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + AVATARS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadAvatarFile(@RequestPart("uploadFile") MultipartFile uploadFile);

    /* Download */
    @Operation(description = "Download an avatar file", tags = "FileTransferAPI")
    @Parameter(name = "avatarId", description = "Avatar ID of the avatar to be downloaded", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + AVATARS + "/{avatarId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadAvatarFile(@PathVariable("avatarId") long avatarId);

    /* Remove */
    @Operation(description = "Remove a avatar file", tags = "FileTransferAPI")
    @Parameter(name = "avatarId", description = "Avatar ID of the avatar to be removed", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File removed successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/" + AVATARS + "/{avatarId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> removeAvatarFile(@PathVariable("avatarId") long avatarId);

    /* ==== Certificate ==== */
    /* Find all */
    @Operation(description = "Get list of all certificate files", tags = "FileTransferAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + CERTIFICATES, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<CertificateFileResponse>> findAllCertificateFiles();

    /* Upload */
    @Operation(description = "Upload a certificate file belonging to a specific user, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @Parameter(name = "certificateId", description = "Certificate ID of the certificate the file belongs to", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + CERTIFICATES
            + "/{certificateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadCertificateFile(
            @RequestPart("uploadFile") MultipartFile uploadFile,
            @PathVariable("certificateId") long certificateId);

    /* Download */
    @Operation(description = "Download a certificate photocopy", tags = "FileTransferAPI")
    @Parameter(name = "certificateId", description = "Certificate ID of the certificate the file belongs to", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + CERTIFICATES + "/{certificateId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadCertificateFile(@PathVariable("certificateId") long certificateId);

    /* Remove */
    @Operation(description = "Remove a certificate photocopy", tags = "FileTransferAPI")
    @Parameter(name = "certificateId", description = "Certificate ID of the certificate of which the picture should be removed", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File removed successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/" + CERTIFICATES + "/{certificateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> removeCertificateFile(@PathVariable("certificateId") long certificateId);

    /* ==== Dive files ==== */
    /* Find all */
    @Operation(description = "Get list of all certificate files", tags = "FileTransferAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + DIVE_FILES, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DiveFileResponse>> findAllDiveFiles();

    /* Upload */
    @Operation(description = "Upload a dive plan linked to a dive group, returns the external URL to access the file. Currently only stubs", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @Parameter(name = "eventId", description = "Event ID to which the dive file belongs to", required = true, example = "11")
    @Parameter(name = "diveGroupId", description = "Which dive group is this upload for", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + DIVE_FILES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadDiveFile(
            @RequestPart("uploadFile") MultipartFile uploadFile,
            @RequestParam("eventId") long eventId,
            @RequestParam("diveGroupId") long diveGroupId);

    /* Download */
    @Operation(description = "Download a dive-related file", tags = "FileTransferAPI")
    @Parameter(name = "diveFileId", description = "Dive file ID to be downloaded", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + DIVE_FILES + "/{diveFileId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadDiveFile(@PathVariable("diveFileId") long diveFileId);

    /* Remove */
    @Operation(description = "Remove a dive file", tags = "FileTransferAPI")
    @Parameter(name = "diveFileId", description = "Dive file ID to be removed", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File removed successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/" + DIVE_FILES + "/{diveFileId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> removeDiveFile(@PathVariable("diveFileId") long diveFileId);

    /* ==== Document ==== */
    /* Find all */
    @Operation(description = "Get list of all document files", tags = "FileTransferAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + DOCUMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DocumentFileResponse>> findAllDocumentFiles();

    /* Upload */
    @Operation(description = "Upload a document not linked to user or page, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + DOCUMENTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadDocumentFile(@RequestPart("uploadFile") MultipartFile uploadFile, HttpServletRequest request);

    /* Download */
    @Operation(description = "Download a document file", tags = "FileTransferAPI")
    @Parameter(name = "documentId", description = "document ID of the document to be downloaded", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + DOCUMENTS + "/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadDocumentFile(@PathVariable("documentId") long documentId);

    /* Remove */
    @Operation(description = "Remove a document file", tags = "FileTransferAPI")
    @Parameter(name = "documentId", description = "document ID of the document to be removed", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File removed successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/" + DOCUMENTS + "/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> removeDocumentFile(@PathVariable("documentId") long documentId);

    /* ==== Page ==== */
    /* Find all */
    @Operation(description = "Get list of all page files", tags = "FileTransferAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(path = BASE_PATH + "/" + PAGE_FILES, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PageFileResponse>> findAllPageFiles();

    /* Upload */
    @Operation(description = "Upload a file belonging to a specific page language version, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "uploadFile", description = "File to be uploaded", required = true)
    @Parameter(name = "language", description = "Which language is this file for", example = "de", required = true)
    @Parameter(name = "pageId", description = "Which page is this upload for", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + PAGE_FILES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadPageFile(@RequestPart("uploadFile") MultipartFile uploadFile,
            @RequestParam("language") String language,
            @RequestParam("pageId") long pageId);

    /* Download */
    @Operation(description = "Download a page file", tags = "FileTransferAPI")
    @Parameter(name = "pageId", description = "Page ID of the page the file belongs to", required = true, example = "11")
    @Parameter(name = "language", description = "Language code is given with 2 characters as per ISO-639-1", required = true, example = "en")
    @Parameter(name = "fileName", description = "Name of the file to be downloaded. There are no path parts", required = true, example = "image.png")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = BASE_PATH + "/" + PAGE_FILES + "/{pageId}/{language}/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<byte[]> downloadPageFile(@PathVariable("pageId") long pageId,
            @PathVariable("language") String language,
            @PathVariable("fileName") String fileName);

    /* Remove */
    @Operation(description = "Remove a page file", tags = "FileTransferAPI")
    @Parameter(name = "pageId", description = "Page ID of the page the file belongs to", required = true, example = "11")
    @Parameter(name = "language", description = "Language code is given with 2 characters as per ISO-639-1", required = true, example = "en")
    @Parameter(name = "fileName", description = "Name of the file to be downloaded. There are no path parts", required = true, example = "image.png")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File removed successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @DeleteMapping(path = BASE_PATH + "/" + PAGE_FILES + "/{pageId}/{language}/{fileName}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActionResponse> removePageFile(@PathVariable("pageId") long pageId,
            @PathVariable("language") String language,
            @PathVariable("fileName") String fileName);
}
