package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UpdateStatusEnum.OK;
import static io.oxalate.backend.api.UploadDirectoryConstants.DOCUMENTS;
import io.oxalate.backend.api.UploadStatusEnum;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.PagedResponse;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.DocumentFileResponse;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.DocumentFile;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.DocumentFileRepository;
import io.oxalate.backend.service.RoleService;
import io.oxalate.backend.tools.FileTools;
import static io.oxalate.backend.tools.FileTools.getSha1OfFile;
import static io.oxalate.backend.tools.FileTools.readFileToResponseEntity;
import static io.oxalate.backend.tools.FileTools.removeFile;
import io.oxalate.backend.tools.PagingTools;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentFileTransferService {
    private static final Map<String, String> SORTABLE_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("filename", "fileName"),
            Map.entry("filesize", "fileSize"),
            Map.entry("mimetype", "mimeType"),
            Map.entry("creator", "creator.lastName"),
            Map.entry("created_at", "createdAt"),
            Map.entry("status", "status"));
    private static final String DEFAULT_SORT_COLUMN = "createdAt";
    private static final String[] SEARCHABLE_COLUMNS = {"fileName", "mimeType", "creator.firstName", "creator.lastName"};

    private final DocumentFileRepository documentFileRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;

    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;
    @Value("${oxalate.app.backend-url}")
    private String backendUrl;

    /**
     * One page of document files, optionally limited to one creator, with the download URL populated. The caller is
     * responsible for deciding whether the current user may see documents of other creators.
     *
     * @param pagedRequest paging, sorting and search parameters
     * @param creatorId    only documents uploaded by this user when given
     * @return the requested page
     */
    public PagedResponse<DocumentFileResponse> findAllDocumentFilesPaged(PagedRequest pagedRequest, @Nullable Long creatorId) {
        var pageable = PagingTools.toPageable(pagedRequest, SORTABLE_COLUMNS, DEFAULT_SORT_COLUMN, Sort.Direction.DESC);
        Specification<DocumentFile> creatorFilter = creatorId == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("creator")
                                                                              .get("id"), creatorId);
        var specification = PagingTools.allOf(creatorFilter,
                PagingTools.searchSpecification(pagedRequest, SEARCHABLE_COLUMNS),
                PagingTools.enumSearchSpecification(pagedRequest, UploadStatusEnum.class, "status"));

        return PagedResponse.fromPage(documentFileRepository.findAll(specification, pageable), documentFile -> {
            var documentFileResponse = documentFile.toResponse();
            documentFileResponse.setUrl(getDocumentUrl(documentFileResponse.getId()));
            return documentFileResponse;
        });
    }

    /**
     * Upload the document file to the filesystem and save the metadata to the database. Note that the URL generated uses the document ID.
     *
     * @param uploadFile File to upload
     * @param userId     ID of the user
     * @return Response with the URL of the uploaded file
     */
    @Transactional
    public UploadResponse uploadDocumentFile(MultipartFile uploadFile, long userId) {
        // Does the user exist?
        var optionalUser = userRepository.findById(userId);
        User uploader;

        if (optionalUser.isEmpty()) {
            log.error("User does not exist: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist");
        }

        uploader = optionalUser.get();

        // First make sure the file does not already exist. The only identifier we have is the filename
        var sanitizedFilename = FileTools.sanitizeFileName(uploadFile.getOriginalFilename());
        var optionalCertificateDocument = documentFileRepository.findByFileName(sanitizedFilename);

        if (optionalCertificateDocument.isPresent()) {
            log.error("Document file already exists: {}", sanitizedFilename);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document file already exists");
        }

        var uploadPath = getGetUploadPath(sanitizedFilename);

        // Check if it already exists in the filesystem
        if (Files.exists(uploadPath)) {
            // Since it did not exist in the database, we can safely remove it

            log.info("Removing rogue document file in the filesystem: {}", uploadPath);
            removeFile(uploadPath, "Document file");
        }

        // Move the file to the upload directory
        try {
            uploadFile.transferTo(uploadPath);
        } catch (Exception e) {
            log.error("Error uploading document file to: {}", uploadPath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading document file");
        }

        // Get the checksum
        var checksum = getSha1OfFile(uploadPath.toFile());
        var fileSize = uploadFile.getSize();
        var mimetype = uploadFile.getContentType();
        // Add upload file to database
        var documentFile = DocumentFile.builder()
                                       .fileName(sanitizedFilename)
                                       .fileChecksum(checksum)
                                       .fileSize(fileSize)
                                       .creator(uploader)
                                       .createdAt(Instant.now())
                                       .mimeType(mimetype)
                                       .status(UploadStatusEnum.UPLOADED)
                                       .build();

        var newDocumentFile = documentFileRepository.save(documentFile);

        var responseUrl = getDocumentUrl(newDocumentFile.getId());

        log.debug("Response URL: {}", responseUrl);

        return UploadResponse.builder()
                             .url(responseUrl)
                             .build();
    }

    public ResponseEntity<byte[]> downloadDocumentFile(long documentId) {
        var documentFile = getDocumentFile(documentId);

        // Check if the file exists
        var uploadPath = getGetUploadPath(documentFile.getFileName());
        var file = uploadPath.toFile();

        if (!file.exists()) {
            log.error("Document file not found on filesystem: {}", file);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
        }

        // Return the file content
        try {
            return readFileToResponseEntity(file, documentFile.getMimeType(), documentFile.getFileName());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not download file", ex);
        }
    }

    @Transactional
    public ActionResponse removeDocumentFile(long documentId, long userId) {
        // Make sure the user ID exists and has ADMIN role

        if (!roleService.userHasRole(userId, RoleEnum.ROLE_ADMIN)) {
            log.error("User does not have ADMIN role: {}", userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have ADMIN role");
        }

        var documentFile = getDocumentFile(documentId);

        // Remove the file from the filesystem
        var uploadPath = getGetUploadPath(documentFile.getFileName());
        removeFile(uploadPath, "Document file");

        // Remove the document file from the database
        documentFileRepository.delete(documentFile);

        return ActionResponse.builder()
                                  .status(OK)
                                  .message("Document file removed")
                                  .build();
    }

    private DocumentFile getDocumentFile(long documentId) {
        // Fetch the document information from the database
        var optionalDocumentFile = documentFileRepository.findById(documentId);

        if (optionalDocumentFile.isEmpty()) {
            log.error("Document file not found: {}", documentId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
        }

        return optionalDocumentFile.get();
    }

    private String getDocumentUrl(long documentId) {
        return backendUrl.replaceAll("/+$", "") + FILES_URL + "/" + DOCUMENTS + "/" + documentId;
    }

    private Path getGetUploadPath(String sanitizedFilename) {
        log.debug("Called with arguments: Main dir: {}, sanitizedFilename: {}", uploadMainDirectory, sanitizedFilename);
        var uploadPath = Paths.get(uploadMainDirectory, DOCUMENTS);
        log.debug("Upload path for document file: {}", uploadPath);

        return uploadPath.resolve(sanitizedFilename);
    }
}
