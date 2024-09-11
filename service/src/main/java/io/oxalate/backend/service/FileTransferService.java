package io.oxalate.backend.service;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UploadDirectoryConstants;
import io.oxalate.backend.api.UploadStatusEnum;
import static io.oxalate.backend.api.UrlConstants.DOWNLOAD_URL;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.model.CertificateFile;
import io.oxalate.backend.model.PageFile;
import io.oxalate.backend.repository.CertificateDocumentRepository;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.PageFileRepository;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.tools.FileTools;
import static io.oxalate.backend.tools.FileTools.getFileSuffix;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileTransferService {

    private final PageFileRepository pageFileRepository;
    private final PageRoleAccessRepository pageRoleAccessRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateDocumentRepository certificateDocumentRepository;
    private final UserRepository userRepository;

    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;
    @Value("${oxalate.app.backend-url}")
    private String backendUrl;

    /* Page */
    public UploadResponse uploadPageFile(MultipartFile file, String language, long pageId, long userId) {
        log.debug("Uploading page file: {} for page: {} in language: {}", file.getOriginalFilename(), pageId, language);
        // Base structure of the upload directory is: BaseDir/pageId/language under which the filename is then placed
        var uploadPath = Paths.get(uploadMainDirectory, UploadDirectoryConstants.PAGE_FILES, String.valueOf(pageId), language);
        log.debug("Upload path for page file: {}", uploadPath);

        verifyUploadPath(uploadPath);

        String fileName;
        // Saving file to the specified directory
        try {
            // Save the file to the specified directory
            fileName = FileTools.sanitizeFileName(file.getOriginalFilename());
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

        var responseUrl =
                backendUrl + DOWNLOAD_URL + "/" + UploadDirectoryConstants.PAGE_FILES + "/" + pageId + "/" + language + "/" + filePath.getFileName();

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
        var optionalPageFile = pageFileRepository.findByPageIdAndLanguageAndFileName(pageId, language, fileName);

        if (optionalPageFile.isEmpty()) {
            log.error("File not found: {} {} {}", pageId, language, fileName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        // Does the role have access to the given page?
        var pageRoles = pageRoleAccessRepository.findByPageIdAndRoleIn(pageId, roles);
        log.debug("Roles when fetching a page file: {}", pageRoles);
        if (pageRoles.isEmpty()) {
            log.error("User does not have access to download the image '{}' related to page: {}", fileName, pageId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to page image");
        }

        var pageFile = optionalPageFile.get();
        var sanitizedFileName = FileTools.sanitizeFileName(pageFile.getFileName());

        try {
            var filePath = Paths.get(uploadMainDirectory, UploadDirectoryConstants.PAGE_FILES, String.valueOf(pageId), language, sanitizedFileName);
            var file = filePath.toFile();

            if (!file.exists()) {
                log.error("File not found on filesystem: {}", filePath.toFile());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            var fileContent = FileCopyUtils.copyToByteArray(new FileInputStream(file));
            var headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, pageFile.getMimeType());

            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(fileContent);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not download file", ex);
        }
    }

    /* Certificate */

    /**
     * Uploads a certificate file to the system. The file is stored in the upload directory and a reference to the file is stored in the database.
     *
     * @param file          file to upload
     * @param userId        ID of the user
     * @param certificateId ID of the certificate
     * @return response with the URL of the uploaded file
     */
    @Transactional
    public UploadResponse uploadCertificateFile(MultipartFile file, long userId, long certificateId) {
        // First make sure the certificate ID is valid
        var optionalCertificate = certificateRepository.findById(certificateId);

        if (optionalCertificate.isEmpty()) {
            log.error("Certificate not found: {}", certificateId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found");
        }

        var certificate = optionalCertificate.get();

        // Match the user from the user ID
        if (certificate.getUserId() != userId) {
            log.error("User ID does not match certificate ID: {} != {}", userId, certificate.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User ID does not match certificate ID");
        }

        var optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            log.error("User not found: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        var user = optionalUser.get();

        log.debug("Uploading certificate file: {} for certificate ID: {} for user ID: {}", file.getOriginalFilename(), certificateId, userId);
        // Base structure of the upload directory is: BaseDir/userId/ under which the filename is then placed with the certificateId as filename
        var uploadPath = Paths.get(uploadMainDirectory, UploadDirectoryConstants.CERTIFICATES, String.valueOf(userId));
        log.debug("Upload path for certificate: {}", uploadPath);
        verifyUploadPath(uploadPath);

        var fileSuffix = getFileSuffix(file);
        var acceptedFileTypes = Set.of(".jpg", ".png");

        if (fileSuffix == null || !acceptedFileTypes.contains(fileSuffix)) {
            log.error("Invalid file type: {}", file.getContentType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type");
        }

        Path filePath = uploadPath.resolve(certificateId + fileSuffix);

        // If the file already exists, delete it as it is then getting replaced
        if (Files.exists(filePath)) {
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                log.error("Could not delete existing certificate file: {}", filePath, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete existing certificate file");
            }
        }

        try {
            Files.copy(file.getInputStream(), filePath);
            log.info("Certificate file saved: {}", filePath);
        } catch (IOException e) {
            log.error("Could not save certificate file: {}", filePath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Certificate file could not be stored");
        }

        // Get the file size and checksum
        var fileSize = file.getSize();
        var fileChecksum = FileTools.getSha1OfFile(filePath.toFile());

        // Check if there is already a document for this certificate in the database
        var optionalCertificateDocument = certificateDocumentRepository.findByCertificateId(certificateId);

        CertificateFile cert;

        if (optionalCertificateDocument.isPresent()) {
            // If there is already a document, then we only update the filename (as the file suffix may have changed), size and checksum
            var certificateDocument = optionalCertificateDocument.get();
            certificateDocument.setFileSize(fileSize);
            certificateDocument.setFileChecksum(fileChecksum);
            certificateDocument.setFileName(filePath.getFileName()
                                                    .toString());
            cert = certificateDocumentRepository.save(certificateDocument);
        } else {
            // If there is none, then we create the entry
            var certificateDocument = CertificateFile.builder()
                                                     .certificate(certificate)
                                                     .user(user)
                                                     .fileName(filePath.getFileName()
                                                                           .toString())
                                                        .mimeType(file.getContentType())
                                                        .fileSize(fileSize)
                                                        .fileChecksum(fileChecksum)
                                                     .createdAt(Instant.now())
                                                     .build();
            cert = certificateDocumentRepository.save(certificateDocument);
        }

        var urlPath = backendUrl + DOWNLOAD_URL + "/" + UploadDirectoryConstants.CERTIFICATES + "/" + certificateId;
        log.debug("Certificate file URL: {}", urlPath);

        return UploadResponse.builder()
                             .url(urlPath)
                             .build();
    }

    /**
     * Downloads a certificate file from the system. The file is retrieved from the upload directory and returned as a byte array.
     * Note that this returns HttpStatus.FORBIDDEN on any error or missing entry/file cases in order to prevent information leakage.
     *
     * @param certificateId ID of the certificate
     * @param userId        ID of the user
     * @param roles         List of roles the user has
     * @return response with the file content
     */
    public ResponseEntity<byte[]> downloadCertificateFile(long certificateId, long userId, Set<RoleEnum> roles) {
        log.info("XXXXX Downloading certificate file: {} for user ID: {} with roles: {}", certificateId, userId, roles);
        // Get the certificate and check that the user ID matches with the given userId
        var optionalCertificate = certificateRepository.findById(certificateId);
        if (optionalCertificate.isEmpty()) {
            log.error("Certificate not found: {}", certificateId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Certificate not found");
        }

        var certificate = optionalCertificate.get();

        if (certificate.getUserId() != userId) {
            // Then check whether the user roles allows to see the certificates, it requires at least ORGANIZER or ADMIN roles
            if (!roles.contains(RoleEnum.ROLE_ORGANIZER) && !roles.contains(RoleEnum.ROLE_ADMIN)) {
                log.warn("User ID {} attempted to view certificate ID: {}", userId, certificateId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User ID does not match certificate ID or does not have required roles");
            }
        }

        var optionalCertificateDocument = certificateDocumentRepository.findByCertificateId(certificateId);

        if (optionalCertificateDocument.isEmpty()) {
            log.error("Certificate document not found: {}", certificateId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Certificate document not found");
        }

        var certificateDocument = optionalCertificateDocument.get();
        var fileName = certificateDocument.getFileName();
        var uploadPath = Paths.get(uploadMainDirectory, UploadDirectoryConstants.CERTIFICATES, String.valueOf(userId), fileName);
        log.info("XXXXX Upload path: {}", uploadPath);
        var file = uploadPath.toFile();

        if (!file.exists()) {
            log.error("File not found on filesystem: {}", file);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File not found");
        }

        try {
            var fileContent = FileCopyUtils.copyToByteArray(new FileInputStream(file));
            var headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, certificateDocument.getMimeType());
            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(fileContent);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Could not download file", ex);
        }
    }

    /* Document */

    /* Dive plan */

    /* Avatar */

    private static void verifyUploadPath(Path uploadPath) {
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
    }
}
