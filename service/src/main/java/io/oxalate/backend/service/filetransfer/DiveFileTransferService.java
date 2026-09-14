package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UpdateStatusEnum.OK;
import static io.oxalate.backend.api.UploadDirectoryConstants.DIVE_FILES;
import io.oxalate.backend.api.UploadStatusEnum;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.DiveFile;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.DiveFileRepository;
import io.oxalate.backend.service.RoleService;
import io.oxalate.backend.tools.FileTools;
import static io.oxalate.backend.tools.FileTools.getFileSuffix;
import static io.oxalate.backend.tools.FileTools.getSha1OfFile;
import static io.oxalate.backend.tools.FileTools.readFileToResponseEntity;
import static io.oxalate.backend.tools.FileTools.removeFile;
import static io.oxalate.backend.tools.FileTools.verifyUploadPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class DiveFileTransferService {
    @Value("${oxalate.upload.directory}")
    private String uploadMainDirectory;
    @Value("${oxalate.app.backend-url}")
    private String backendUrl;

    private final DiveFileRepository diveFileRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final DiveGroupRepository diveGroupRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final RoleService roleService;

    public List<DiveFileResponse> findAllDiveFiles() {
        var diveFiles = diveFileRepository.findAll();
        return mapDiveFilesToResponses(diveFiles);
    }

    /**
     * Finds all dive files uploaded for a specific dive group. The generated download URL is populated for each file.
     *
     * @param diveGroupId ID of the dive group
     * @return List of dive file responses of the dive group
     */
    public List<DiveFileResponse> findDiveFilesByDiveGroupId(long diveGroupId) {
        var diveFiles = diveFileRepository.findByDiveGroupId(diveGroupId);
        return mapDiveFilesToResponses(diveFiles);
    }

    /**
     * Upload a dive file
     *
     * @param uploadFile  The file to upload
     * @param eventId     The event ID
     * @param diveGroupId The dive group ID
     * @param userId      The user ID
     * @return The upload response
     */
    @Transactional
    public UploadResponse uploadDiveFile(MultipartFile uploadFile, long eventId, long diveGroupId, long userId) {
        log.debug("Uploading dive file: {} for event: {} in dive group: {} by user: {}", uploadFile.getOriginalFilename(), eventId, diveGroupId, userId);
        // Does the user exist?
        var optionalUser = userRepository.findById(userId);
        User uploader;

        if (optionalUser.isEmpty()) {
            log.error("User does not exist: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist");
        }

        uploader = optionalUser.get();

        // Check that the event exists and is in the future
        var optionalEvent = eventRepository.findById(eventId);

        if (optionalEvent.isEmpty()) {
            log.error("Event does not exist: {}", eventId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event does not exist");
        }

        var event = optionalEvent.get();

        // If the event start time is in the past, then fail
        if (event.getStartTime()
                 .isBefore(Instant.now())) {
            log.error("Event is in the past: {}", eventId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is in the past");
        }

        // Check that the dive group exists and belongs to the given event
        var optionalDiveGroup = diveGroupRepository.findById(diveGroupId);

        if (optionalDiveGroup.isEmpty()) {
            log.error("Dive group does not exist: {}", diveGroupId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dive group does not exist");
        }

        var diveGroup = optionalDiveGroup.get();

        if (diveGroup.getEventId() != eventId) {
            log.error("Dive group {} does not belong to event {}", diveGroupId, eventId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dive group does not belong to the event");
        }

        // Check that the user is a member of the dive group
        if (!isMemberOfDiveGroup(eventId, diveGroupId, userId)) {
            log.error("User {} is not a member of dive group {}", userId, diveGroupId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of the dive group");
        }

        // The sanitization also rejects invalid file names
        var sanitizedFilename = FileTools.sanitizeFileName(uploadFile.getOriginalFilename());
        var fileSuffix = getFileSuffix(uploadFile);

        if (fileSuffix == null) {
            log.error("Unsupported dive file type: {}", uploadFile.getContentType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported dive file type");
        }

        var diveFile = DiveFile.builder()
                               .fileName(sanitizedFilename)
                               .mimeType(uploadFile.getContentType())
                               .fileSize(uploadFile.getSize())
                               .fileChecksum("PLACEHOLDER")
                               .eventId(eventId)
                               .diveGroupId(diveGroupId)
                               .creator(uploader)
                               .createdAt(Instant.now())
                               .status(UploadStatusEnum.UPLOADED)
                               .build();

        var newDiveFile = diveFileRepository.save(diveFile);

        // Base structure of the upload directory is: eventId/diveGroupId/diveFileId.[suffix]
        var uploadPath = generateUploadPath(eventId, diveGroupId);
        log.debug("Upload path for dive file: {}", uploadPath);
        verifyUploadPath(uploadPath);

        var uploadFilename = newDiveFile.getId() + fileSuffix;
        var resolvedFilename = uploadPath.resolve(uploadFilename);

        // Move the file to the upload directory
        try {
            uploadFile.transferTo(resolvedFilename);
        } catch (Exception e) {
            log.error("Error uploading dive file to: {}", uploadPath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dive file could not be stored");
        }

        var checksum = getSha1OfFile(resolvedFilename.toFile());
        newDiveFile.setFileName(uploadFilename);
        newDiveFile.setFileChecksum(checksum);
        diveFileRepository.save(newDiveFile);

        return UploadResponse.builder()
                             .url(generateDiveFileUrl(newDiveFile.getId()))
                             .build();
    }

    /**
     * Downloads a dive file.
     *
     * @param diveFileId ID of the dive file to download
     * @return The dive file content
     */
    public ResponseEntity<byte[]> downloadDiveFile(long diveFileId) {
        var diveFile = getDiveFile(diveFileId);

        var uploadPath = generateUploadPath(diveFile.getEventId(), diveFile.getDiveGroupId());
        var file = uploadPath.resolve(diveFile.getFileName())
                             .toFile();

        if (!file.exists()) {
            log.error("Dive file not found on filesystem: {}", file);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dive file not found");
        }

        try {
            return readFileToResponseEntity(file, diveFile.getMimeType(), diveFile.getFileName());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not download file", ex);
        }
    }

    /**
     * Removes a dive file. Only the creator of the dive file, or an administrator, may remove the file.
     *
     * @param diveFileId ID of the dive file to remove
     * @param userId     ID of the user requesting the removal
     * @return The action response
     */
    @Transactional
    public ActionResponse removeDiveFile(long diveFileId, long userId) {
        var diveFile = getDiveFile(diveFileId);

        if (diveFile.getCreator()
                    .getId() != userId && !roleService.userHasRole(userId, RoleEnum.ROLE_ADMIN)) {
            log.error("User {} is not allowed to remove dive file {}", userId, diveFileId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to remove the dive file");
        }

        var uploadPath = generateUploadPath(diveFile.getEventId(), diveFile.getDiveGroupId());
        var filePath = uploadPath.resolve(diveFile.getFileName());

        if (Files.exists(filePath)) {
            removeFile(filePath, "Dive file");
        }

        diveFileRepository.delete(diveFile);

        return ActionResponse.builder()
                             .status(OK)
                             .message("Dive file removed")
                             .build();
    }

    private boolean isMemberOfDiveGroup(long eventId, long diveGroupId, long userId) {
        var participant = eventParticipantsRepository.findByEventIdAndUserId(eventId, userId);
        return participant != null && participant.getDiveGroupId() != null && participant.getDiveGroupId() == diveGroupId;
    }

    private DiveFile getDiveFile(long diveFileId) {
        var optionalDiveFile = diveFileRepository.findById(diveFileId);

        if (optionalDiveFile.isEmpty()) {
            log.error("Dive file not found: {}", diveFileId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dive file not found");
        }

        return optionalDiveFile.get();
    }

    private List<DiveFileResponse> mapDiveFilesToResponses(List<DiveFile> diveFiles) {
        var diveFileResponses = diveFiles.stream()
                                         .map(DiveFile::toResponse)
                                         .toList();

        diveFileResponses.forEach(diveFileResponse -> diveFileResponse.setUrl(generateDiveFileUrl(diveFileResponse.getId())));

        return diveFileResponses;
    }

    private String generateDiveFileUrl(long diveFileId) {
        return backendUrl.replaceAll("/+$", "") + FILES_URL + "/" + DIVE_FILES + "/" + diveFileId;
    }

    private Path generateUploadPath(long eventId, long diveGroupId) {
        return Paths.get(uploadMainDirectory, DIVE_FILES, String.valueOf(eventId), String.valueOf(diveGroupId));
    }
}
