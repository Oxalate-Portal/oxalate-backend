package io.oxalate.backend.service.filetransfer;

import static io.oxalate.backend.api.UpdateStatusEnum.OK;
import static io.oxalate.backend.api.UploadDirectoryConstants.AVATARS;
import io.oxalate.backend.api.response.FileRemovalResponse;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.AvatarFileResponse;
import io.oxalate.backend.model.filetransfer.AvatarFile;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.AvatarFileRepository;
import io.oxalate.backend.tools.FileTools;
import static io.oxalate.backend.tools.FileTools.getFileSuffix;
import static io.oxalate.backend.tools.FileTools.readFileToResponseEntity;
import static io.oxalate.backend.tools.FileTools.removeFile;
import static io.oxalate.backend.tools.FileTools.verifyUploadPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class AvatarFileTransferService {
    private final AvatarFileRepository avatarFileRepository;
    private final UserRepository userRepository;

    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;
    @Value("${oxalate.app.backend-url}")
    private String backendUrl;

    /**
     * Find all avatar files
     * @return List of all available avatar files as a response entity
     */
    public List<AvatarFileResponse> findAllAvatarFiles() {
        var avatarFiles = avatarFileRepository.findAll();
        var avatarFileResponses = avatarFiles.stream()
                .map(AvatarFile::toResponse).toList();

        log.info("Found {} avatar files", avatarFileResponses.size());
        avatarFileResponses.forEach(avatarFileResponse -> avatarFileResponse.setUrl(getAvatarFileUrl(avatarFileResponse.getFilename())));

        return avatarFileResponses;
    }

    /**
     * Upload the avatar file to the filesystem and save the metadata to the database
     *
     * @param uploadFile File to upload
     * @param userId     ID of the user
     * @return Response with the URL of the uploaded file
     */

    @Transactional
    public UploadResponse uploadAvatarFile(MultipartFile uploadFile, long userId) {
        log.debug("Uploading avatar file: {} for user ID: {}", uploadFile.getOriginalFilename(), userId);
        var user = userRepository.findById(userId)
                                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // Base structure of the upload directory is: BaseDir/userId.[suffix]
        var uploadPath = Paths.get(uploadMainDirectory, AVATARS);
        log.debug("Upload path for page file: {}", uploadPath);

        verifyUploadPath(uploadPath);
        var fileSuffix = getFileSuffix(uploadFile);
        var fileName = userId + fileSuffix;
        var destinationFile = Paths.get(uploadPath.toString(), fileName);

        // If the file already exists, remove it
        var optionalAvatarFile = avatarFileRepository.findByCreator(user);
        optionalAvatarFile.ifPresent(avatarFile -> removeAvatarFile(avatarFile.getId(), userId));

        // Also remove any physical file that might exist
        if (Files.exists(destinationFile)) {
            log.warn("Rogue avatar file already exists: {}", destinationFile);
            removeFile(destinationFile, "Avatar file");
        }

        try {
            Files.copy(uploadFile.getInputStream(), destinationFile);
            log.info("Page file saved: {}", destinationFile);
        } catch (IOException e) {
            log.error("Could not save avatar file: {}", destinationFile, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar file could not be stored");
        }

        var fileChecksum = FileTools.getSha1OfFile(destinationFile.toFile());
        var fileSize = uploadFile.getSize();

        if (optionalAvatarFile.isEmpty()) {
            var avatarFile = AvatarFile.builder()
                                       .creator(user)
                                       .fileName(fileName)
                                       .fileChecksum(fileChecksum)
                                       .fileSize(fileSize)
                                       .mimeType(uploadFile.getContentType())
                                       .createdAt(Instant.now())
                                       .build();
            avatarFileRepository.save(avatarFile);
        } else {
            var avatarFile = optionalAvatarFile.get();
            avatarFile.setFileName(fileName);
            avatarFile.setFileChecksum(fileChecksum);
            avatarFile.setFileSize(fileSize);
            avatarFile.setMimeType(uploadFile.getContentType());
            avatarFile.setCreatedAt(Instant.now());
            avatarFileRepository.save(avatarFile);
        }

        return UploadResponse.builder()
                             .url(getAvatarFileUrl(fileName))
                             .build();
    }

    /**
     * This returns the whole response entity with the file content which is an exception. The reason for this is that we also need to set the headers
     * which is more convenient than to return the file content and set the headers in the controller.
     *
     * @param avatarId ID of the avatar
     * @return Response entity with the file content
     */
    public ResponseEntity<byte[]> downloadAvatarFile(long avatarId) {
        var avatarFile = avatarFileRepository.findById(avatarId)
                                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar file not found"));

        // Check if the file exists
        var file = Paths.get(uploadMainDirectory, AVATARS, avatarFile.getFileName()).toFile();

        if (!file.exists()) {
            log.error("Avatar file not found on filesystem: {}", file);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        try {
            return readFileToResponseEntity(file, avatarFile.getMimeType(), avatarFile.getFileName());
        } catch (IOException e) {
            log.error("Could not read file: {}", file, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
        }
    }

    /**
     * Remove the avatar file from the filesystem and the database
     *
     * @param avatarId ID of the avatar
     * @param userId   ID of the requesting user
     * @return Response with status and message
     */

    @Transactional
    public FileRemovalResponse removeAvatarFile(long avatarId, long userId) {
        // Only the user who owns the avatar file can remove it
        var avatarFile = avatarFileRepository.findById(avatarId)
                                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar file not found"));

        if (avatarFile.getCreator().getId() != userId) {
            log.error("User does not have access to remove the avatar file: {}", avatarId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to remove avatar file");
        }

        var file = Paths.get(uploadMainDirectory, AVATARS, avatarFile.getFileName()).toFile();

        if (!file.exists()) {
            log.error("File not found on filesystem: {}", file);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        removeFile(file.toPath(), "Avatar file");
        avatarFileRepository.delete(avatarFile);

        return FileRemovalResponse.builder()
                                  .status(OK)
                                  .message("Avatar file removed")
                                  .build();
    }

    private String getAvatarFileUrl(String fileName) {
        return backendUrl + AVATARS + "/" + fileName;
    }
}
