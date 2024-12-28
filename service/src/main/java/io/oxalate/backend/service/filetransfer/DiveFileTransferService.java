package io.oxalate.backend.service.filetransfer;

import static io.oxalate.backend.api.UploadDirectoryConstants.DIVE_FILES;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.DiveFile;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.DiveFileRepository;
import io.oxalate.backend.service.EventService;
import static io.oxalate.backend.tools.FileTools.getFileSuffix;
import static io.oxalate.backend.tools.FileTools.verifyUploadPath;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    private final EventService eventService;


    public List<DiveFileResponse> findAllDiveFiles() {
        var diveFiles = diveFileRepository.findAll();
        var diveFileResponses = diveFiles.stream()
                                         .map(DiveFile::toResponse)
                                         .toList();

        diveFileResponses.forEach(diveFileResponse -> diveFileResponse.setUrl(generateDiveFileUrl(diveFileResponse.getId())));

        return diveFileResponses;
    }

    /**
     * Upload a dive file
     * @param uploadFile The file to upload
     * @param eventId The event ID
     * @param diveGroupId The dive group ID
     * @param userId The user ID
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

        // Check that the event is in the future
        var event = eventService.findById(eventId);
        // If the event start time is in the past, then fail
        if (event.getStartTime().isBefore(Instant.now())) {
            log.error("Event is in the past: {}", eventId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is in the past");
        }

        // TODO Check that the user is a member of the dive group

        var diveFile = DiveFile.builder()
                               .fileName("PLACEHOLDER")
                               .eventId(eventId)
                               .diveGroupId(diveGroupId)
                               .creator(uploader)
                               .build();

        var newDiveFile = diveFileRepository.save(diveFile);

        // Base structure of the upload directory is: eventId/diveGroupId/diveFileId.[suffix]
        var fileSuffix = getFileSuffix(uploadFile);
        var uploadPath = generateUploadPath(eventId, diveGroupId);
        log.debug("Upload path for page file: {}", uploadPath);
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

        newDiveFile.setFileName(uploadFilename);
        diveFileRepository.save(newDiveFile);

        return UploadResponse.builder()
                             .url(generateDiveFileUrl(newDiveFile.getId()))
                             .build();
    }

    private String generateDiveFileUrl(long diveFileId) {
        return backendUrl + FILES_URL + File.pathSeparator + DIVE_FILES + File.pathSeparator + diveFileId;
    }

    private Path generateUploadPath(long eventId, long diveGroupId) {
        return Paths.get(uploadMainDirectory, DIVE_FILES, String.valueOf(eventId), String.valueOf(diveGroupId));
    }
}
