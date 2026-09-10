package io.oxalate.backend.service.filetransfer;

import static io.oxalate.backend.api.UploadDirectoryConstants.AVATARS;
import io.oxalate.backend.api.response.UploadResponse;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.AvatarFile;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.AvatarFileRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit tests for AvatarFileTransferService, focused on regression-testing
 * the correct URL construction returned after avatar upload/listing.
 * <p>
 * Previously, getAvatarFileUrl() was missing the "/" separator between the
 * backend host and the path, and was using the physical filename instead of
 * the DB entity id, producing malformed URLs such as:
 * {@code http://localhost:8080avatars/100.jpg}
 * The correct form is: {@code http://localhost:8080/api/files/avatars/42}
 */
@ExtendWith(MockitoExtension.class)
class AvatarFileTransferServiceUTC {

    @TempDir
    Path tempDir;

    @Mock
    private AvatarFileRepository avatarFileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AvatarFileTransferService avatarFileTransferService;

    private AvatarFile testAvatarFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(avatarFileTransferService, "backendUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(avatarFileTransferService, "uploadMainDirectory", "/tmp");

        var user = User.builder()
                       .firstName("John")
                       .lastName("Doe")
                       .build();

        testAvatarFile = AvatarFile.builder()
                                   .id(42L)
                                   .fileName("100.jpg")
                                   .mimeType("image/jpeg")
                                   .fileSize(1000L)
                                   .fileChecksum("abc123")
                                   .creator(user)
                                   .createdAt(Instant.now())
                                   .build();
    }

    @Test
    void findAllAvatarFiles_urlContainsApiFilesAvatarsPath() {
        when(avatarFileRepository.findAll()).thenReturn(List.of(testAvatarFile));

        var result = avatarFileTransferService.findAllAvatarFiles();

        assertNotNull(result);
        assertEquals(1, result.size());
        var url = result.getFirst()
                        .getUrl();
        assertNotNull(url, "URL must not be null");
        assertTrue(url.contains("/api/files/avatars/"), "URL must contain '/api/files/avatars/' path segment");
    }

    @Test
    void findAllAvatarFiles_urlUsesDatabaseIdNotFilename() {
        when(avatarFileRepository.findAll()).thenReturn(List.of(testAvatarFile));

        var result = avatarFileTransferService.findAllAvatarFiles();

        var url = result.getFirst()
                        .getUrl();
        // Avatar DB id is 42, physical filename is "100.jpg"
        assertTrue(url.endsWith("/42"), "URL must end with the avatar DB id (42), not with the physical filename");
        assertFalse(url.contains("100.jpg"), "URL must not contain the physical filename");
    }

    @Test
    void findAllAvatarFiles_urlHasProperSeparatorBetweenHostAndPath() {
        when(avatarFileRepository.findAll()).thenReturn(List.of(testAvatarFile));

        var result = avatarFileTransferService.findAllAvatarFiles();

        var url = result.getFirst()
                        .getUrl();
        assertEquals("http://localhost:8080/api/files/avatars/42", url,
                "URL must have a '/' separator between host and path - was missing before the fix");
    }

    @Test
    void findAllAvatarFiles_urlIsWellFormedWhenBackendUrlHasTrailingSlash() {
        ReflectionTestUtils.setField(avatarFileTransferService, "backendUrl", "http://localhost:8080/");
        when(avatarFileRepository.findAll()).thenReturn(List.of(testAvatarFile));

        var result = avatarFileTransferService.findAllAvatarFiles();

        var url = result.getFirst()
                        .getUrl();
        assertFalse(url.contains("//api"), "URL must not contain a double-slash before the API path");
        assertEquals("http://localhost:8080/api/files/avatars/42", url);
    }

    @Test
    void uploadAvatarFile_existingAvatarUpdatesMetadataAndReplacesOldPhysicalFile() throws IOException {
        ReflectionTestUtils.setField(avatarFileTransferService, "uploadMainDirectory", tempDir.toString());

        var user = User.builder()
                       .id(100L)
                       .firstName("John")
                       .lastName("Doe")
                       .build();

        var existingAvatar = AvatarFile.builder()
                                       .id(11L)
                                       .creator(user)
                                       .fileName("100.png")
                                       .fileChecksum("old")
                                       .fileSize(10L)
                                       .mimeType("image/png")
                                       .createdAt(Instant.now())
                                       .build();

        var avatarDirectory = tempDir.resolve(AVATARS);
        Files.createDirectories(avatarDirectory);
        var oldFilePath = avatarDirectory.resolve("100.png");
        Files.write(oldFilePath, new byte[] { 1, 2, 3 });

        MultipartFile uploadFile = mock(MultipartFile.class);
        when(uploadFile.getOriginalFilename()).thenReturn("new-avatar.jpg");
        when(uploadFile.getContentType()).thenReturn("image/jpeg");
        when(uploadFile.getSize()).thenReturn(3L);
        when(uploadFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 7, 8, 9 }));

        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(avatarFileRepository.findByCreator(user)).thenReturn(Optional.of(existingAvatar));
        when(avatarFileRepository.save(existingAvatar)).thenReturn(existingAvatar);

        UploadResponse response = avatarFileTransferService.uploadAvatarFile(uploadFile, 100L);

        assertNotNull(response);
        assertEquals("http://localhost:8080/api/files/avatars/11", response.getUrl());
        assertFalse(Files.exists(oldFilePath), "Old avatar file must be deleted when extension changes");
        assertTrue(Files.exists(avatarDirectory.resolve("100.jpg")), "New avatar file should be written");
        verify(avatarFileRepository, never()).delete(any());
        verify(avatarFileRepository, never()).findById(anyLong());
    }

    @Test
    void getAvatarUrlByUserId_returnsAvatarUrlWhenAvatarExists() {
        when(avatarFileRepository.findByUserId(100L)).thenReturn(Optional.of(testAvatarFile));

        var url = avatarFileTransferService.getAvatarUrlByUserId(100L);

        assertEquals("http://localhost:8080/api/files/avatars/42", url);
    }

    @Test
    void getAvatarUrlByUserId_returnsNullWhenNoAvatarExists() {
        when(avatarFileRepository.findByUserId(100L)).thenReturn(Optional.empty());

        var url = avatarFileTransferService.getAvatarUrlByUserId(100L);

        assertNull(url);
    }
}

