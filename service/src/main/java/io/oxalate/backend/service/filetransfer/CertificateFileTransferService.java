package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UpdateStatusEnum.OK;
import io.oxalate.backend.api.UploadDirectoryConstants;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.CertificateFileResponse;
import io.oxalate.backend.model.filetransfer.CertificateFile;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.CertificateDocumentRepository;
import io.oxalate.backend.tools.FileTools;
import static io.oxalate.backend.tools.FileTools.getFileSuffix;
import static io.oxalate.backend.tools.FileTools.removeFile;
import static io.oxalate.backend.tools.FileTools.verifyUploadPath;
import java.io.FileInputStream;
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
public class CertificateFileTransferService {
    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;
    @Value("${oxalate.app.backend-url}")
    private String backendUrl;

    private final CertificateRepository certificateRepository;
    private final CertificateDocumentRepository certificateDocumentRepository;
    private final UserRepository userRepository;

    public List<CertificateFileResponse> findAllCertificateFiles() {
        var certificateFiles = certificateDocumentRepository.findAll();
        var certificateFileResponses = certificateFiles.stream()
                                                       .map(CertificateFile::toResponse)
                                                       .toList();

        certificateFileResponses.forEach(certificateFileResponse -> {
            var certificateId = certificateFileResponse.getCertificateId();
            certificateFileResponse.setUrl(getCertificateFileUrl(certificateId));
        });

        return certificateFileResponses;

    }

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
            removeFile(filePath, "Existing certificate file");
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

        if (optionalCertificateDocument.isPresent()) {
            // If there is already a document, then we only update the filename (as the file suffix may have changed), size and checksum
            var certificateDocument = optionalCertificateDocument.get();
            certificateDocument.setFileSize(fileSize);
            certificateDocument.setFileChecksum(fileChecksum);
            certificateDocument.setFileName(filePath.getFileName()
                                                    .toString());
            certificateDocumentRepository.save(certificateDocument);
        } else {
            // If there is none, then we create the entry
            var certificateDocument = CertificateFile.builder()
                                                     .certificate(certificate)
                                                     .creator(user)
                                                     .fileName(filePath.getFileName()
                                                                       .toString())
                                                     .mimeType(file.getContentType())
                                                     .fileSize(fileSize)
                                                     .fileChecksum(fileChecksum)
                                                     .createdAt(Instant.now())
                                                     .build();
            certificateDocumentRepository.save(certificateDocument);
        }

        return UploadResponse.builder()
                             .url(getCertificateFileUrl(certificateId))
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
        // Get the certificate and check that the user ID matches with the given userId
        var optionalCertificate = certificateRepository.findById(certificateId);
        if (optionalCertificate.isEmpty()) {
            log.error("Certificate to download not found: {}", certificateId);
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
        var uploadPath = Paths.get(uploadMainDirectory, UploadDirectoryConstants.CERTIFICATES, String.valueOf(certificate.getUserId()), fileName);
        var file = uploadPath.toFile();

        if (!file.exists()) {
            log.error("File to download not found on filesystem: {}", file);
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

    /**
     * Removes a certificate file from the system. The file is deleted from the upload directory and the reference in the database is removed.
     *
     * @param certificateId ID of the certificate
     * @param userId        ID of the user
     * @return response with the status of the removal
     */

    @Transactional
    public ActionResponse removeCertificateFile(long certificateId, long userId) {
        // Fetch the certificate document

        var optionalCertificateDocument = certificateDocumentRepository.findByCertificateId(certificateId);

        if (optionalCertificateDocument.isEmpty()) {
            log.error("Certificate document not found for deletion: {}", certificateId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate document not found");
        }

        var certificateDocument = optionalCertificateDocument.get();

        // Check that the user ID matches the certificate ID
        if (certificateDocument.getCertificate()
                               .getUserId() != userId) {
            log.error("User ID does not match certificate ID: {} != {}", userId, certificateDocument.getCertificate()
                                                                                                    .getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User ID does not match certificate ID");
        }

        // Delete the file from the file system
        var fileName = certificateDocument.getFileName();
        var uploadFile = Paths.get(uploadMainDirectory, UploadDirectoryConstants.CERTIFICATES, String.valueOf(userId), fileName);
        var file = uploadFile.toFile();

        if (!file.exists()) {
            log.error("File not found on filesystem: {}", file);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        removeFile(uploadFile, "Certificate file");

        // Delete the certificate document from the database
        certificateDocumentRepository.delete(certificateDocument);

        return ActionResponse.builder()
                                  .status(OK)
                                  .message("Certificate file removed")
                                  .build();
    }

    private String getCertificateFileUrl(long certificateId) {
        return backendUrl + FILES_URL + "/" + UploadDirectoryConstants.CERTIFICATES + "/" + certificateId;
    }

    @Transactional
    public void anonymize(long userId) {
        var certificateDocuments = certificateDocumentRepository.findByCreator(userId);

        for (var certificateDocument : certificateDocuments) {
            removeCertificateFile(certificateDocument.getCertificate()
                                                      .getId(), userId);
        }
    }
}
