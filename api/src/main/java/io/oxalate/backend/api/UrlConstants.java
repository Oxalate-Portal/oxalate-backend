package io.oxalate.backend.api;

/**
 * These paths are controlled in the WebSecurityConfig class so in order to minimize mismatch with paths in the REST APIs we have centralized them here.
 */

public class UrlConstants {
    public static final String API = "/api";
    public static final String DOWNLOAD_URL = API + "/files/download";
    public static final String PAGES_URL = API + "/pages";
}
