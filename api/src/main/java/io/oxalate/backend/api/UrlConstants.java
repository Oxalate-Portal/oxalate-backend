package io.oxalate.backend.api;

/**
 * These paths are controlled in the WebSecurityConfig class so in order to minimize mismatch with paths in the REST APIs we have centralized them here.
 */

public class UrlConstants {
    public static final String API = "/api";
    public static final String DIVE_PLANS_URL = API + "/dive-plans";
    public static final String DOCUMENTS_URL = API + "/documents";
    public static final String FILES_URL = API + "/files";
    public static final String PAGES_URL = API + "/pages";
}
