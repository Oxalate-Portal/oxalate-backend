package io.oxalate.backend.service;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.model.PageFile;
import io.oxalate.backend.repository.PageFileRepository;
import io.oxalate.backend.tools.FileTools;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class UploadService {

    private final PageFileRepository pageFileRepository;
    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;
    private String pageDirectory;
    @Value("${oxalate.app.backend-url}")
    private String backendUrl;

    public UploadService(PageFileRepository pageFileRepository) {
        this.pageFileRepository = pageFileRepository;
        this.pageDirectory = uploadMainDirectory + "/page-files";
    }

    public UploadResponse uploadFile(MultipartFile file, String language, long pageId, long userId) {
        // Base structure of the upload directory is: BaseDir/pageId/language under which the filename is then placed
        var uploadPath = Paths.get(pageDirectory, String.valueOf(pageId), language);

        // If the upload path does not exist, create it
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException ex) {
                log.error("Could not create upload directory: {}", uploadPath, ex);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create upload directory", ex);
            }
        }

        // Make sure we have write access to the upload path
        if (!Files.isWritable(uploadPath)) {
            log.error("Can not create files in directory: {}", uploadPath);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No write access to upload directory");
        }

        String fileName;
        // Saving file to the specified directory
        try {
            // Save the file to the specified directory
            fileName = FileTools.sanitizeFileName(file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            log.error("Invalid file name: {}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Filename could not be sanitized");
        }

        Path filePath = uploadPath.resolve(fileName);

        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            log.error("Could not save file: {}", filePath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File could not be stored");
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
                               .creator(userId)
                               .createdAt(Instant.now())
                               .build();

        try {
            pageFileRepository.save(pageFile);
        } catch (Exception e) {
            // Remove the file from the file system
            try {
                Files.delete(filePath);
            } catch (IOException ex) {
                log.error("Could not delete file: {}", filePath, ex);
            }

            log.error("Could not save file to database: {}", pageFile);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save file to database");
        }

        var responseUrl = backendUrl + "/api/files/download/" + pageId + "/" + language + "/" + filePath.getFileName();

        log.debug("Response URL: {}", responseUrl);

        return UploadResponse.builder()
                             .url(responseUrl)
                             .build();
    }

    /**
     * This returns the whole response entity with the file content which is an exception. The reason for this is that we also need to set the headers
     * which is more convenient than to return the file content and set the headers in the controller.
     *
     * @param fileName Path of the file
     * @param roles
     * @return Response entity with the file content
     */
    public ResponseEntity<byte[]> downloadFile(long pageId, String language, String fileName, Set<RoleEnum> roles) {
        var optionalPageFile = pageFileRepository.findByPageIdAndLanguageAndFileName(pageId, language, fileName);

        if (optionalPageFile.isEmpty()) {
            log.error("File not found: {} {} {}", pageId, language, fileName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        var pageFile = optionalPageFile.get();
        var sanitizedFileName = FileTools.sanitizeFileName(pageFile.getFileName());

        try {
            var filePath = Paths.get(pageDirectory, String.valueOf(pageId), language, sanitizedFileName);
            var file = filePath.toFile();

            if (!file.exists()) {
                log.error("File not found on filesystem: {}", filePath.toFile());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            var fileContent = FileCopyUtils.copyToByteArray(new FileInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");

            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(fileContent);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not download file", ex);
        }
    }
}
