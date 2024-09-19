package io.oxalate.backend.tools;

import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class FileTools {

    // Define a pattern for invalid characters (e.g., \, /, :, *, ?, ", <, >, |)
    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    public static String sanitizeFileName(String fileName) {

        if (fileName == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }

        // Step 1: Normalize the filename (NFD: Normalization Form Decomposition)
        fileName = Normalizer.normalize(fileName, Normalizer.Form.NFD);

        // Step 2: Remove all invalid characters
        fileName = INVALID_CHAR_PATTERN.matcher(fileName)
                                       .replaceAll("");

        // Step 3: Strip any directory traversal attempts (e.g., "../")
        fileName = fileName.replaceAll("\\.\\.+", ""); // remove any multiple dots (e.g., ... or ..)
        fileName = fileName.replaceAll("^\\.+", "");   // remove leading dots (e.g., .filename)

        // Step 4: Trim any leading or trailing whitespace
        fileName = fileName.trim();

        // Replace any spaces with underscores
        fileName = fileName.replaceAll(" ", "_");
        // Step 5: Ensure the filename is within a valid length, and not empty after stripping
        if (fileName.isEmpty() || fileName.length() > 255) {
            throw new IllegalArgumentException("Invalid file name after sanitization");
        }

        return fileName;
    }

    public static String getSha1OfFile(File file) {
        MessageDigest sha1;

        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not find message digest SHA-1");
            return null;
        }

        try (InputStream input = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }

            return new HexBinaryAdapter().marshal(sha1.digest());
        } catch (FileNotFoundException e) {
            log.error("Failed to read file {}", file);
        } catch (IOException e) {
            log.error("Failed to read file {}", file, e);
        }

        return null;
    }

    public static String getFileSuffix(MultipartFile file) {
        // Get the original file suffix
        var fileSuffix = switch (file.getContentType()) {
            case "application/pdf" -> ".pdf";
            case "application/zip" -> ".zip";
            case "image/gif" -> ".gif";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "text/plain" -> ".txt";
            case "x-zip-compressed" -> ".zip"; // Buggy Windows MIME type
            case null -> null;
            default ->  null;
        };

        return fileSuffix;
    }

    public static void verifyUploadPath(Path uploadPath) {
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

    public static void removeFile(Path filePath, String filetype) {
        try {
            Files.delete(filePath);
            log.info("{} deleted: {}", filetype, filePath);
        } catch (IOException e) {
            log.error("Could not delete {}: {}", filetype, filePath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete file");
        }
    }

    public static ResponseEntity<byte[]> readFileToResponseEntity(File file, String mimetype, String filename) throws IOException {
        var fileContent = FileCopyUtils.copyToByteArray(new FileInputStream(file));
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimetype));

        if (mimetype.startsWith("image/")) {
            headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
        } else {
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        }

        return ResponseEntity.ok()
                             .headers(headers)
                             .body(fileContent);    }
}
