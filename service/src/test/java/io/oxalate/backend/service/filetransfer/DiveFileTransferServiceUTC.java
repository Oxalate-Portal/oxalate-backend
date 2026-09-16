package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UpdateStatusEnum.OK;
import static io.oxalate.backend.api.UploadDirectoryConstants.DIVE_FILES;
import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.model.DiveGroup;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.EventsParticipant;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.DiveFile;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.DiveFileRepository;
import io.oxalate.backend.service.RoleService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for DiveFileTransferService covering the dive file upload, listing, download and removal flows.
 */
@ExtendWith(MockitoExtension.class)
class DiveFileTransferServiceUTC {

    private static final long EVENT_ID = 42L;
    private static final long GROUP_ID = 7L;
    private static final long UPLOADER_ID = 200L;
    private static final long OUTSIDER_ID = 400L;
    private static final long DIVE_FILE_ID = 11L;

    @TempDir
    Path tempDir;

    @Mock
    private DiveFileRepository diveFileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private DiveGroupRepository diveGroupRepository;
    @Mock
    private EventParticipantsRepository eventParticipantsRepository;
    @Mock
    private RoleService roleService;

    @InjectMocks
    private DiveFileTransferService diveFileTransferService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(diveFileTransferService, "backendUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(diveFileTransferService, "uploadMainDirectory", tempDir.toString());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User user(long id) {
        return User.builder()
                   .id(id)
                   .firstName("First" + id)
                   .lastName("Last" + id)
                   .build();
    }

    private Event futureEvent() {
        return Event.builder()
                    .id(EVENT_ID)
                    .title("Test event")
                    .startTime(Instant.now()
                                      .plus(2, ChronoUnit.DAYS))
                    .eventDuration(4)
                    .build();
    }

    private Event pastEvent() {
        return Event.builder()
                    .id(EVENT_ID)
                    .title("Test event")
                    .startTime(Instant.now()
                                      .minus(2, ChronoUnit.DAYS))
                    .eventDuration(4)
                    .build();
    }

    private DiveGroup diveGroup() {
        return DiveGroup.builder()
                        .id(GROUP_ID)
                        .eventId(EVENT_ID)
                        .name("Team Sidemount")
                        .ownerId(UPLOADER_ID)
                        .createdAt(Instant.now())
                        .build();
    }

    private EventsParticipant participant(long userId, Long diveGroupId) {
        return EventsParticipant.builder()
                                .userId(userId)
                                .eventId(EVENT_ID)
                                .diveGroupId(diveGroupId)
                                .build();
    }

    private DiveFile diveFile() {
        return DiveFile.builder()
                       .id(DIVE_FILE_ID)
                       .fileName(DIVE_FILE_ID + ".pdf")
                       .mimeType("application/pdf")
                       .fileSize(3L)
                       .fileChecksum("abc123")
                       .creator(user(UPLOADER_ID))
                       .createdAt(Instant.now())
                       .eventId(EVENT_ID)
                       .diveGroupId(GROUP_ID)
                       .status(UploadStatusEnum.UPLOADED)
                       .build();
    }

    private MockMultipartFile pdfUploadFile() {
        return new MockMultipartFile("uploadFile", "dive plan.pdf", "application/pdf", "PDF content".getBytes(StandardCharsets.UTF_8));
    }

    private void stubValidUpload() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, UPLOADER_ID)).thenReturn(participant(UPLOADER_ID, GROUP_ID));
        when(diveFileRepository.save(any(DiveFile.class))).thenAnswer(invocation -> {
            DiveFile diveFile = invocation.getArgument(0);
            diveFile.setId(DIVE_FILE_ID);
            return diveFile;
        });
    }

    private Path storedFilePath() {
        return tempDir.resolve(Path.of(DIVE_FILES, String.valueOf(EVENT_ID), String.valueOf(GROUP_ID), DIVE_FILE_ID + ".pdf"));
    }

    private void writeStoredFile() throws IOException {
        Files.createDirectories(storedFilePath().getParent());
        Files.write(storedFilePath(), "PDF content".getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // findAllDiveFiles / findDiveFilesByDiveGroupId
    // ------------------------------------------------------------------

    @Test
    void findAllDiveFilesOk() {
        when(diveFileRepository.findAll()).thenReturn(List.of(diveFile()));

        var responses = diveFileTransferService.findAllDiveFiles();

        assertEquals(1, responses.size());
        assertEquals("http://localhost:8080/api/files/dive-files/" + DIVE_FILE_ID, responses.getFirst()
                                                                                            .getUrl());
    }

    @Test
    void findDiveFilesByDiveGroupIdOk() {
        when(diveFileRepository.findByDiveGroupId(GROUP_ID)).thenReturn(List.of(diveFile()));

        var responses = diveFileTransferService.findDiveFilesByDiveGroupId(GROUP_ID);

        assertEquals(1, responses.size());
        var response = responses.getFirst();
        assertEquals(DIVE_FILE_ID + ".pdf", response.getFilename());
        assertEquals(GROUP_ID, response.getDiveGroupId());
        assertEquals(EVENT_ID, response.getEventId());
        assertEquals("http://localhost:8080/api/files/dive-files/" + DIVE_FILE_ID, response.getUrl());
    }

    @Test
    void findDiveFilesByDiveGroupIdEmptyOk() {
        when(diveFileRepository.findByDiveGroupId(GROUP_ID)).thenReturn(List.of());

        var responses = diveFileTransferService.findDiveFilesByDiveGroupId(GROUP_ID);

        assertTrue(responses.isEmpty());
    }

    @Test
    void findDiveFilesByDiveGroupIdUrlIsWellFormedWhenBackendUrlHasTrailingSlashOk() {
        ReflectionTestUtils.setField(diveFileTransferService, "backendUrl", "http://localhost:8080/");
        when(diveFileRepository.findByDiveGroupId(GROUP_ID)).thenReturn(List.of(diveFile()));

        var responses = diveFileTransferService.findDiveFilesByDiveGroupId(GROUP_ID);

        assertEquals("http://localhost:8080/api/files/dive-files/" + DIVE_FILE_ID, responses.getFirst()
                                                                                            .getUrl());
    }

    // ------------------------------------------------------------------
    // uploadDiveFile
    // ------------------------------------------------------------------

    @Test
    void uploadDiveFileOk() {
        stubValidUpload();

        var response = diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID);

        assertNotNull(response);
        assertEquals("http://localhost:8080/api/files/dive-files/" + DIVE_FILE_ID, response.getUrl());
        assertTrue(Files.exists(storedFilePath()), "Uploaded file must be stored as diveFileId.suffix");

        var diveFileCaptor = ArgumentCaptor.forClass(DiveFile.class);
        verify(diveFileRepository, org.mockito.Mockito.times(2)).save(diveFileCaptor.capture());
        var savedDiveFile = diveFileCaptor.getValue();
        assertEquals(DIVE_FILE_ID + ".pdf", savedDiveFile.getFileName());
        assertEquals("application/pdf", savedDiveFile.getMimeType());
        assertEquals(UploadStatusEnum.UPLOADED, savedDiveFile.getStatus());
        assertNotNull(savedDiveFile.getFileChecksum());
        assertFalse(savedDiveFile.getFileChecksum()
                                 .isBlank());
        assertEquals(EVENT_ID, savedDiveFile.getEventId());
        assertEquals(GROUP_ID, savedDiveFile.getDiveGroupId());
        assertNotNull(savedDiveFile.getCreatedAt());
    }

    @Test
    void uploadDiveFileUnknownUserFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void uploadDiveFileUnknownEventFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void uploadDiveFilePastEventFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(pastEvent()));

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void uploadDiveFileUnknownDiveGroupFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void uploadDiveFileDiveGroupOfAnotherEventFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        var otherEventGroup = diveGroup();
        otherEventGroup.setEventId(EVENT_ID + 1);
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(otherEventGroup));

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void uploadDiveFileNonMemberFail() {
        when(userRepository.findById(OUTSIDER_ID)).thenReturn(Optional.of(user(OUTSIDER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, OUTSIDER_ID)).thenReturn(null);

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, OUTSIDER_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(diveFileRepository, never()).save(any());
    }

    @Test
    void uploadDiveFileMemberOfAnotherGroupFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, UPLOADER_ID)).thenReturn(participant(UPLOADER_ID, GROUP_ID + 1));

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(diveFileRepository, never()).save(any());
    }

    @Test
    void uploadDiveFileParticipantWithoutGroupFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, UPLOADER_ID)).thenReturn(participant(UPLOADER_ID, null));

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(pdfUploadFile(), EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void uploadDiveFileUnsupportedMimeTypeFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, UPLOADER_ID)).thenReturn(participant(UPLOADER_ID, GROUP_ID));

        var uploadFile = new MockMultipartFile("uploadFile", "malware.exe", "application/octet-stream", new byte[] { 1, 2, 3 });

        var exception = assertThrows(ResponseStatusException.class,
                () -> diveFileTransferService.uploadDiveFile(uploadFile, EVENT_ID, GROUP_ID, UPLOADER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(diveFileRepository, never()).save(any());
    }

    @Test
    void uploadDiveFileInvalidFileNameFail() {
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user(UPLOADER_ID)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(futureEvent()));
        when(diveGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(diveGroup()));
        when(eventParticipantsRepository.findByEventIdAndUserId(EVENT_ID, UPLOADER_ID)).thenReturn(participant(UPLOADER_ID, GROUP_ID));

        var uploadFile = new MockMultipartFile("uploadFile", "../..", "application/pdf", new byte[] { 1, 2, 3 });

        assertThrows(IllegalArgumentException.class,
                () -> diveFileTransferService.uploadDiveFile(uploadFile, EVENT_ID, GROUP_ID, UPLOADER_ID));
        verify(diveFileRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // downloadDiveFile
    // ------------------------------------------------------------------

    @Test
    void downloadDiveFileOk() throws IOException {
        writeStoredFile();
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.of(diveFile()));

        var response = diveFileTransferService.downloadDiveFile(DIVE_FILE_ID);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PDF content", new String(response.getBody(), StandardCharsets.UTF_8));
        assertEquals("application/pdf", String.valueOf(response.getHeaders()
                                                               .getContentType()));
    }

    @Test
    void downloadDiveFileUnknownIdFail() {
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class, () -> diveFileTransferService.downloadDiveFile(DIVE_FILE_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void downloadDiveFileMissingOnFilesystemFail() {
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.of(diveFile()));

        var exception = assertThrows(ResponseStatusException.class, () -> diveFileTransferService.downloadDiveFile(DIVE_FILE_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ------------------------------------------------------------------
    // removeDiveFile
    // ------------------------------------------------------------------

    @Test
    void removeDiveFileByCreatorOk() throws IOException {
        writeStoredFile();
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.of(diveFile()));

        var response = diveFileTransferService.removeDiveFile(DIVE_FILE_ID, UPLOADER_ID);

        assertEquals(OK, response.getStatus());
        assertFalse(Files.exists(storedFilePath()), "Stored file must be removed from the filesystem");
        verify(diveFileRepository).delete(any(DiveFile.class));
    }

    @Test
    void removeDiveFileByAdminOk() throws IOException {
        writeStoredFile();
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.of(diveFile()));
        when(roleService.userHasRole(OUTSIDER_ID, RoleEnum.ROLE_ADMIN)).thenReturn(true);

        var response = diveFileTransferService.removeDiveFile(DIVE_FILE_ID, OUTSIDER_ID);

        assertEquals(OK, response.getStatus());
        verify(diveFileRepository).delete(any(DiveFile.class));
    }

    @Test
    void removeDiveFileMissingOnFilesystemStillRemovesMetadataOk() {
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.of(diveFile()));

        var response = diveFileTransferService.removeDiveFile(DIVE_FILE_ID, UPLOADER_ID);

        assertEquals(OK, response.getStatus());
        verify(diveFileRepository).delete(any(DiveFile.class));
    }

    @Test
    void removeDiveFileByNonCreatorFail() {
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.of(diveFile()));
        when(roleService.userHasRole(OUTSIDER_ID, RoleEnum.ROLE_ADMIN)).thenReturn(false);

        var exception = assertThrows(ResponseStatusException.class, () -> diveFileTransferService.removeDiveFile(DIVE_FILE_ID, OUTSIDER_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(diveFileRepository, never()).delete(any(DiveFile.class));
    }

    @Test
    void removeDiveFileUnknownIdFail() {
        when(diveFileRepository.findById(DIVE_FILE_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class, () -> diveFileTransferService.removeDiveFile(DIVE_FILE_ID, UPLOADER_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
