package io.oxalate.backend.api;

public class UploadDirectoryConstants {
    public static final String PAGE_FILES = "page-files";
    public static final String UPLOAD_FILES = "upload-files";
    public static final String CERTIFICATES = "certificates";
    public static final String DOCUMENTS = "documents";
    public static final String DIVE_PLANS = "dive-plans";
    public static final String AVATARS = "avatars";

    // Method to return all directory constants as an array
    public static String[] getAllDirectories() {
        return new String[] {
                PAGE_FILES,
                UPLOAD_FILES,
                CERTIFICATES,
                DOCUMENTS,
                DIVE_PLANS,
                AVATARS
        };
    }
}
