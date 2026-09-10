package io.oxalate.backend.security;

import io.oxalate.backend.tools.FileTools;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * OWASP A01:2025 - path traversal (CWE-22).
 * <p>
 * Uploaded file names are attacker supplied and end up being joined onto {@code oxalate.upload.directory}. If a
 * name can escape that directory, an upload becomes an arbitrary file write and a download becomes an
 * arbitrary file read. {@code FileTools.sanitizeFileName} is the single choke point, so it is pinned here
 * against the usual encodings and separators.
 */
@DisplayName("OWASP A01: uploaded file names cannot escape the upload directory")
class OwaspFileToolsUTC {

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd",
            "..\\..\\windows\\system32\\config\\sam",
            "....//....//etc/shadow",
            "/etc/passwd",
            "C:\\Windows\\system.ini",
            "subdir/../../secret.txt",
            ".ssh/authorized_keys",
            "....",
            "..",
            "./../.env"
    })
    void sanitizeFileNameRemovesTraversalOk(String hostileName) {
        String sanitized;

        try {
            sanitized = FileTools.sanitizeFileName(hostileName);
        } catch (IllegalArgumentException e) {
            // Rejecting outright is an equally acceptable outcome
            return;
        }

        assertFalse(sanitized.contains(".."), "Sanitized name still contains a parent reference: " + sanitized);
        assertFalse(sanitized.contains("/"), "Sanitized name still contains a path separator: " + sanitized);
        assertFalse(sanitized.contains("\\"), "Sanitized name still contains a path separator: " + sanitized);
        assertFalse(sanitized.startsWith("."), "Sanitized name must not be a dotfile: " + sanitized);

        var resolved = Paths.get("/var/tmp/oxalate")
                            .resolve(sanitized)
                            .normalize();
        assertEquals(Paths.get("/var/tmp/oxalate"), resolved.getParent(),
                "The sanitized name must resolve inside the upload directory, resolved to: " + resolved);
    }

    @Test
    void sanitizeFileNameKeepsOrdinaryNamesOk() {
        assertEquals("dive-report_2024.pdf", FileTools.sanitizeFileName("dive-report_2024.pdf"));
        assertEquals("my_holiday_photo.jpg", FileTools.sanitizeFileName("my holiday photo.jpg"));
    }

    @Test
    void sanitizeFileNameRejectsEmptyResultFail() {
        assertThrows(IllegalArgumentException.class, () -> FileTools.sanitizeFileName("/"));
        assertThrows(IllegalArgumentException.class, () -> FileTools.sanitizeFileName(""));
    }

    @Test
    void sanitizeFileNameRejectsNullFail() {
        assertThrows(IllegalArgumentException.class, () -> FileTools.sanitizeFileName(null));
    }

    @Test
    void sanitizeFileNameRejectsOverlongNameFail() {
        assertThrows(IllegalArgumentException.class, () -> FileTools.sanitizeFileName("a".repeat(256)));
    }
}
