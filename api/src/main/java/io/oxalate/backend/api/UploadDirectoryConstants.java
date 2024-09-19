package io.oxalate.backend.api;

public class UploadDirectoryConstants {
    public static final String AVATARS = "avatars";
    public static final String CERTIFICATES = "certificates";
    public static final String DIVE_FILES = "dive-files";
    public static final String DOCUMENTS = "documents";
    public static final String PAGE_FILES = "page-files";

    // Method to return all directory constants as an array
    public static String[] getAllDirectories() {
        return new String[] {
                AVATARS,
                CERTIFICATES,
                DIVE_FILES,
                DOCUMENTS,
                PAGE_FILES
        };
    }
}
