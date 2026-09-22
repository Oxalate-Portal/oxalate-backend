package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UploadDirectoryConstants.AVATARS;
import static io.oxalate.backend.api.UploadDirectoryConstants.CERTIFICATES;
import static io.oxalate.backend.api.UploadDirectoryConstants.DIVE_FILES;
import static io.oxalate.backend.api.UploadDirectoryConstants.DOCUMENTS;
import static io.oxalate.backend.api.UploadDirectoryConstants.PAGE_FILES;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.request.PagedRequest;
import static io.oxalate.backend.api.request.PagedRequest.CASE_SENSITIVE_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.DIRECTION_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.PAGE_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.SEARCH_DESCRIPTION;
import static io.oxalate.backend.api.request.PagedRequest.SIZE_DESCRIPTION;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.PagedResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "FileTransferAPI", description = "File Upload and Download REST endpoints")
public interface FileTransferAPI {
    String BASE_PATH = API + "/files";

    /* ==== Avatar ==== */
    /* Find all */
    @Operation(description = "Get a page of all avatar files", tags = "FileTransferAPI")
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "0")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "25")
    @Parameter(name = "sort_by", description = "Column to sort by, one of: id, filename, filesize, mimetype, creator, created_at. "
            + "Unknown values fall back to created_at", example = "created_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @Parameter(name = "search", description = SEARCH_DESCRIPTION + ": file name, mime type, creator first name, creator last name", example = "jpeg")
    @Parameter(name = "case_sensitive", description = CASE_SENSITIVE_DESCRIPTION, example = "false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + AVATARS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<AvatarFileResponse>> findAllAvatarFiles(@RequestBody PagedRequest pagedRequest);

    /* Upload */
    @Operation(description = "Upload an avatar linked to a user, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "upload_file", description = "File to be uploaded", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + AVATARS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadAvatarFile(@RequestPart("upload_file") MultipartFile uploadFile);

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
    @Operation(description = "Get a page of all certificate files", tags = "FileTransferAPI")
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "0")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "25")
    @Parameter(name = "sort_by", description = "Column to sort by, one of: id, filename, filesize, mimetype, creator, created_at, certificate_id. "
            + "Unknown values fall back to created_at", example = "created_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @Parameter(name = "search", description = SEARCH_DESCRIPTION + ": file name, mime type, creator first name, creator last name", example = "jpeg")
    @Parameter(name = "case_sensitive", description = CASE_SENSITIVE_DESCRIPTION, example = "false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + CERTIFICATES, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<CertificateFileResponse>> findAllCertificateFiles(@RequestBody PagedRequest pagedRequest);

    /* Upload */
    @Operation(description = "Upload a certificate file belonging to a specific user, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "upload_file", description = "File to be uploaded", required = true)
    @Parameter(name = "certificateId", description = "Certificate ID of the certificate the file belongs to", required = true, example = "11")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + CERTIFICATES
            + "/{certificateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadCertificateFile(
            @RequestPart("upload_file") MultipartFile uploadFile,
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
    @Operation(description = "Get a page of all dive files, optionally limited to one event. The page is empty when dive files are disabled",
            tags = "FileTransferAPI")
    @Parameter(name = "event_id", description = "Only return the dive files of this event", example = "11")
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "0")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "25")
    @Parameter(name = "sort_by", description = "Column to sort by, one of: id, filename, filesize, mimetype, creator, created_at, event_id, "
            + "dive_group_id, status. Unknown values fall back to created_at", example = "created_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @Parameter(name = "search", description = SEARCH_DESCRIPTION + ": file name, mime type, creator first name, creator last name", example = "jpeg")
    @Parameter(name = "case_sensitive", description = CASE_SENSITIVE_DESCRIPTION, example = "false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + DIVE_FILES, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<DiveFileResponse>> findAllDiveFiles(@RequestBody PagedRequest pagedRequest,
            @RequestParam(value = "event_id", required = false) Long eventId);

    /* Upload */
    @Operation(description = "Upload a dive plan linked to a dive group, returns the external URL to access the file. Only members of the dive group may upload files", tags = "FileTransferAPI")
    @Parameter(name = "upload_file", description = "File to be uploaded", required = true)
    @Parameter(name = "event_id", description = "Event ID to which the dive file belongs to", required = true, example = "11")
    @Parameter(name = "dive_group_id", description = "Which dive group is this upload for", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + DIVE_FILES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadDiveFile(
            @RequestPart("upload_file") MultipartFile uploadFile,
            @RequestParam("event_id") long eventId,
            @RequestParam("dive_group_id") long diveGroupId);

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
    @Operation(description = "Get a page of document files. Administrators see every document or the documents of the given creator, other users only"
            + " see their own. The page is empty when documents are disabled", tags = "FileTransferAPI")
    @Parameter(name = "creator_id", description = "Only return the documents uploaded by this user", example = "11")
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "0")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "25")
    @Parameter(name = "sort_by", description = "Column to sort by, one of: id, filename, filesize, mimetype, creator, created_at, status. "
            + "Unknown values fall back to created_at", example = "created_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @Parameter(name = "search", description = SEARCH_DESCRIPTION + ": file name, mime type, creator first name, creator last name", example = "jpeg")
    @Parameter(name = "case_sensitive", description = CASE_SENSITIVE_DESCRIPTION, example = "false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + DOCUMENTS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<DocumentFileResponse>> findAllDocumentFiles(@RequestBody PagedRequest pagedRequest,
            @RequestParam(value = "creator_id", required = false) Long creatorId);

    /* Upload */
    @Operation(description = "Upload a document not linked to user or page, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "upload_file", description = "File to be uploaded", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + DOCUMENTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadDocumentFile(@RequestPart("upload_file") MultipartFile uploadFile, HttpServletRequest request);

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
    @Operation(description = "Get a page of all page files", tags = "FileTransferAPI")
    @Parameter(name = "page", description = PAGE_DESCRIPTION, example = "0")
    @Parameter(name = "size", description = SIZE_DESCRIPTION, example = "25")
    @Parameter(name = "sort_by", description = "Column to sort by, one of: id, filename, filesize, mimetype, creator, created_at, page_id, language, status. "
            + "Unknown values fall back to created_at", example = "created_at")
    @Parameter(name = "direction", description = DIRECTION_DESCRIPTION + " (DESC)", example = "DESC")
    @Parameter(name = "search", description = SEARCH_DESCRIPTION + ": file name, mime type, creator first name, creator last name", example = "jpeg")
    @Parameter(name = "case_sensitive", description = CASE_SENSITIVE_DESCRIPTION, example = "false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + PAGE_FILES, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<PageFileResponse>> findAllPageFiles(@RequestBody PagedRequest pagedRequest);

    /* Upload */
    @Operation(description = "Upload a file belonging to a specific page language version, returns the external URL to access the file", tags = "FileTransferAPI")
    @Parameter(name = "upload_file", description = "File to be uploaded", required = true)
    @Parameter(name = "language", description = "Which language is this file for", example = "de", required = true)
    @Parameter(name = "page_id", description = "Which page is this upload for", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @PostMapping(path = BASE_PATH + "/" + PAGE_FILES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> uploadPageFile(@RequestPart("upload_file") MultipartFile uploadFile,
            @RequestParam("language") String language,
            @RequestParam("page_id") long pageId);

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
