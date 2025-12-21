package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UpdateStatusEnum.OK;
import static io.oxalate.backend.api.UploadDirectoryConstants.PAGE_FILES;
import io.oxalate.backend.api.UploadStatusEnum;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.PageFileResponse;
import io.oxalate.backend.model.filetransfer.PageFile;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.PageFileRepository;
import io.oxalate.backend.tools.FileTools;
import static io.oxalate.backend.tools.FileTools.readFileToResponseEntity;
import static io.oxalate.backend.tools.FileTools.removeFile;
import static io.oxalate.backend.tools.FileTools.sanitizeFileName;
import static io.oxalate.backend.tools.FileTools.verifyUploadPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class PageFileTransferService {
    private final PageFileRepository pageFileRepository;
    private final PageRoleAccessRepository pageRoleAccessRepository;
    private final UserRepository userRepository;

    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;
    @Value("${oxalate.app.backend-url}")
    private String backendUrl;

    public List<PageFileResponse> findAllPageFiles() {
        var pageFiles = pageFileRepository.findAll();
        var pageFileResponses = pageFiles.stream()
                                         .map(PageFile::toResponse)
                                         .toList();

        pageFileResponses.forEach(pageFileResponse -> pageFileResponse.setUrl(generatePageFileUrl(pageFileResponse.getPageId(),
                pageFileResponse.getLanguage(),
                pageFileResponse.getFilename())));

        return pageFileResponses;

    }

    public UploadResponse uploadPageFile(MultipartFile file, String language, long pageId, long userId) {
        log.debug("Uploading page file: {} for page: {} in language: {}", file.getOriginalFilename(), pageId, language);
        // Does the user exist
        var optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            log.error("User not found: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        var user = optionalUser.get();

        // Base structure of the upload directory is: BaseDir/pageId/language under which the filename is then placed
        var uploadPath = Paths.get(uploadMainDirectory, PAGE_FILES, String.valueOf(pageId), language);
        log.debug("Upload path for page file: {}", uploadPath);

        verifyUploadPath(uploadPath);

        String fileName;
        // Saving file to the specified directory
        try {
            // Save the file to the specified directory
            fileName = sanitizeFileName(file.getOriginalFilename());
            log.debug("Sanitized file name: {} from {}", fileName, file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            log.error("Invalid file name: {}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Filename could not be sanitized");
        }

        log.debug("Resolving file path: {}", fileName);
        Path filePath = uploadPath.resolve(fileName);

        try {
            Files.copy(file.getInputStream(), filePath);
            log.info("Page file saved: {}", filePath);
        } catch (IOException e) {
            log.error("Could not save page file: {}", filePath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Page file could not be stored");
        }

        // Get the sha256 checksum of the file
        var fileChecksum = FileTools.getSha1OfFile(filePath.toFile());

        // At this point the file has been uploaded, but we're not yet aware of the page versions of the page actually being created or updated, so we set the
        // status to UPLOADED. When the page versions are later saved, we can update the status to PUBLISHED.
        var pageFile = PageFile.builder()
                               .fileName(fileName)
                               .language(language)
                               .pageId(pageId)
                               .mimeType(file.getContentType())
                               .fileSize(file.getSize())
                               .fileChecksum(fileChecksum)
                               .status(UploadStatusEnum.UPLOADED)
                               .creator(user)
                               .createdAt(Instant.now())
                               .build();

        try {
            pageFileRepository.save(pageFile);
        } catch (Exception e) {
            // Remove the file from the file system
            removeFile(filePath, "Page file");

            log.error("Could not save file to database: {}", pageFile);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save file to database");
        }

        var responseUrl = generatePageFileUrl(pageId, language, fileName);

        log.debug("Response URL: {}", responseUrl);

        return UploadResponse.builder()
                             .url(responseUrl)
                             .build();
    }

    /**
     * This returns the whole response entity with the file content which is an exception. The reason for this is that we also need to set the headers
     * which is more convenient than to return the file content and set the headers in the controller.
     *
     * @param pageId   ID of the page
     * @param language Language of the file
     * @param fileName Path of the file
     * @param roles    List of roles the user has
     * @return Response entity with the file content
     */
    public ResponseEntity<byte[]> downloadPageFile(long pageId, String language, String fileName, Set<RoleEnum> roles) {
        var pageFile = getExistingPageFile(pageId, language, fileName);

        // Does the role have access to the given page?
        var pageRoles = pageRoleAccessRepository.findByPageIdAndRoleIn(pageId, roles);
        log.debug("Roles when fetching a page file: {}", pageRoles);
        if (pageRoles.isEmpty()) {
            log.error("User does not have access to download the image '{}' related to page: {}", fileName, pageId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to page image");
        }

        var sanitizedFileName = sanitizeFileName(pageFile.getFileName());

        try {
            var filePath = Paths.get(uploadMainDirectory, PAGE_FILES, String.valueOf(pageId), language, sanitizedFileName);
            var file = filePath.toFile();

            if (!file.exists()) {
                log.error("Page file not found on filesystem: {}", filePath.toFile());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            return readFileToResponseEntity(file, pageFile.getMimeType(), sanitizedFileName);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not download file", ex);
        }
    }

    public ActionResponse removePageFile(long pageId, String language, String fileName) {
        // First make sure the file exists
        var pageFile = getExistingPageFile(pageId, language, fileName);
        // Then delete the file from the file system
        var filePath = Paths.get(uploadMainDirectory, PAGE_FILES, String.valueOf(pageId), language, fileName);
        var file = filePath.toFile();

        if (!file.exists()) {
            log.error("File not found on filesystem: {}", file);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        removeFile(filePath, "Page file");
        pageFileRepository.delete(pageFile);

        return ActionResponse.builder()
                                  .status(OK)
                                  .message("Page file removed")
                                  .build();
    }

    private PageFile getExistingPageFile(long pageId, String language, String fileName) {
        var optionalPageFile = pageFileRepository.findByPageIdAndLanguageAndFileName(pageId, language, fileName);

        if (optionalPageFile.isEmpty()) {
            log.error("File not found: {} {} {}", pageId, language, fileName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return optionalPageFile.get();
    }

    private String generatePageFileUrl(long pageId, String language, String fileName) {
        return backendUrl + FILES_URL + "/" + PAGE_FILES + "/" + pageId + "/" + language + "/" + fileName;
    }
}
