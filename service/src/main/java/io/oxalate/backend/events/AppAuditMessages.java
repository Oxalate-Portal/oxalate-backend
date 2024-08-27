package io.oxalate.backend.events;

public class AppAuditMessages {
    // AuthController
    public static final String AUTH_AUTHENTICATION_FAIL = "User attempted to log in but the authentication failed: ";
    public static final String AUTH_AUTHENTICATION_START = "User attempted to log in: ";
    public static final String AUTH_AUTHENTICATION_NON_ACTIVE = "User has non-active status: ";
    public static final String AUTH_AUTHENTICATION_NO_ROLES = "User has no defined roles: ";
    public static final String AUTH_AUTHENTICATION_OK = "User logged in: ";

    public static final String AUTH_REGISTRATION_START = "Registering username: ";
    public static final String AUTH_REGISTRATION_TAKEN = "Username is already registered: ";
    public static final String AUTH_REGISTRATION_OK = "Username was registered: ";

    public static final String AUTH_UPDATE_PASSWORD_START = "User updating password";
    public static final String AUTH_UPDATE_PASSWORD_UNAUTHORIZED = "User not authorized to update password for: ";
    public static final String AUTH_UPDATE_PASSWORD_INACTIVE_STATUS = "Password not possible with inactive status";
    public static final String AUTH_UPDATE_PASSWORD_OLD_MISMATCH = "Old password did not match";
    public static final String AUTH_UPDATE_PASSWORD_NEW_SAME_AS_OLD = "New password same as old password";
    public static final String AUTH_UPDATE_PASSWORD_FAIL_REQUIREMENTS = "New password does not qualify";
    public static final String AUTH_UPDATE_PASSWORD_NEW_MISMATCH = "New passwords do not match";
    public static final String AUTH_UPDATE_PASSWORD_OK = "New password accepted";

    public static final String AUTH_REGISTRATION_VERIFY_START = "Verifying registration token";
    public static final String AUTH_REGISTRATION_VERIFY_INVALID_TOKEN = "Registration token was invalid";
    public static final String AUTH_REGISTRATION_VERIFY_OK = "Registration successful";

    public static final String AUTH_RESEND_EMAIL_START = "Email resend requested";
    public static final String AUTH_RESEND_EMAIL_INVALID_TOKEN = "Email resend token is invalid";
    public static final String AUTH_RESEND_EMAIL_INVALID_USER = "Email resend token refers to non-existing user";
    public static final String AUTH_RESEND_EMAIL_EXPIRED_TOKEN = "Email resend token has expired";
    public static final String AUTH_RESEND_EMAIL_USED_TOKEN = "Email resend token has already been used";
    public static final String AUTH_RESEND_EMAIL_OK = "Email is resent";

    public static final String AUTH_LOST_PASSWORD_START = "Lost password for user requested: ";
    public static final String AUTH_LOST_PASSWORD_INACTIVE_STATUS = "Lost password requested for inactive user: ";
    public static final String AUTH_LOST_PASSWORD_FAIL = "Failed sending password update email for user: ";
    public static final String AUTH_LOST_PASSWORD_OK = "Password update email sent for user: ";

    public static final String AUTH_RESET_PASSWORD_START = "Reset password requested";
    public static final String AUTH_RESET_PASSWORD_INVALID_TOKEN = "Reset password requested with invalid token";
    public static final String AUTH_RESET_PASSWORD_EXPIRED_TOKEN = "Reset password requested with with an expired token";
    public static final String AUTH_RESET_PASSWORD_FAIL_REQUIREMENTS = "Reset password requested did not pass password check";
    public static final String AUTH_RESET_PASSWORD_MISMATCH = "New and confirmed passwords do not match";
    public static final String AUTH_RESET_PASSWORD_INVALID_USER = "Invalid user";
    public static final String AUTH_RESET_PASSWORD_INACTIVE_STATUS = "User status does not allow for password reset: ";
    public static final String AUTH_RESET_PASSWORD_UNKNOWN_ERROR = "User password failed for unknown reason";
    public static final String AUTH_RESET_PASSWORD_OK = "User password updated";

    // BlockedDateController
    public static final String BLOCKED_DATE_GET_ALL_START = "Getting all blocked dates";
    public static final String BLOCKED_DATE_GET_ALL_UNAUTHORIZED = "User not authorized to get list of blocked dates";
    public static final String BLOCKED_DATE_GET_ALL_OK = "Return list of blocked dates";

    public static final String BLOCKED_DATE_ADD_START = "Adding new blocked date";
    public static final String BLOCKED_DATE_ADD_UNAUTHORIZED = "User not authorized to add new blocked date";
    public static final String BLOCKED_DATE_ADD_OK = "New blocked date added";

    public static final String BLOCKED_DATE_REMOVE_START = "Removing blocked date";
    public static final String BLOCKED_DATE_REMOVE_UNAUTHORIZED = "User not authorized to remove blocked date";
    public static final String BLOCKED_DATE_REMOVE_NOT_FOUND = "Blocked date not found";
    public static final String BLOCKED_DATE_REMOVE_OK = "Blocked date removed";

    // CertificateController
    public static final String CERTIFICATES_GET_ALL_START = "Getting all certificates";
    public static final String CERTIFICATES_GET_ALL_UNAUTHORIZED = "User is not authorized to get certificates";
    public static final String CERTIFICATES_GET_ALL_OK = "Retrieved all certificates";

    public static final String CERTIFICATES_GET_ALL_USER_START = "Getting all certificates for user ID: ";
    public static final String CERTIFICATES_GET_ALL_USER_UNAUTHORIZED = "User is not authorized to get certificates for user ID: ";
    public static final String CERTIFICATES_GET_ALL_USER_OK = "Retrieved all certificates for user ID: ";

    public static final String CERTIFICATES_GET_START = "Retrieving certificate with ID: ";
    public static final String CERTIFICATES_GET_NOT_FOUND = "Could not find certificate ID: ";
    public static final String CERTIFICATES_GET_UNAUTHORIZED = "User not authorized to get certificate for user ID: ";
    public static final String CERTIFICATES_GET_OK = "Certificate information retrieved";

    public static final String CERTIFICATES_ADD_START = "Adding a new certificate";
    public static final String CERTIFICATES_ADD_FAIL = "Failed to add a new certificate";
    public static final String CERTIFICATES_ADD_OK = "Certificate added";

    public static final String CERTIFICATES_UPDATE_START = "Updating certificate: ";
    public static final String CERTIFICATES_UPDATE_FAIL = "Failed to update certificate: ";
    public static final String CERTIFICATES_UPDATE_UNAUTHORIZED = "User was not authorized to update certificate ID: ";
    public static final String CERTIFICATES_UPDATE_OK = "Certificate updated: ";

    public static final String CERTIFICATES_DELETE_START = "Deleting certificate: ";
    public static final String CERTIFICATES_DELETE_FAIL = "Failed to delete certificate: ";
    public static final String CERTIFICATES_DELETE_UNAUTHORIZED = "User was not authorized to delete certificate ID: ";
    public static final String CERTIFICATES_DELETE_OK = "Certificate deleted: ";

    // DataDownloadController
    public static final String DATA_DOWNLOAD_CERTIFICATES_START = "Downloading all certificates";
    public static final String DATA_DOWNLOAD_CERTIFICATES_UNAUTHORIZED = "User is not authorized to download certificates";
    public static final String DATA_DOWNLOAD_CERTIFICATES_OK = "Downloaded all certificates";

    public static final String DATA_DOWNLOAD_DIVES_START = "Downloading dives";
    public static final String DATA_DOWNLOAD_DIVES_UNAUTHORIZED = "User not authorized to download dive information";
    public static final String DATA_DOWNLOAD_DIVES_OK = "Dive information has been downloaded";

    public static final String DATA_DOWNLOAD_PAYMENTS_START = "Downloading payments";
    public static final String DATA_DOWNLOAD_PAYMENTS_UNAUTHORIZED = "User not authorized to download payment information";
    public static final String DATA_DOWNLOAD_PAYMENTS_OK = "Payment information has been downloaded";

    // EventController
    public static final String EVENTS_GET_FUTURE_START = "Retrieving all future events";
    public static final String EVENTS_GET_FUTURE_TERMS_NOT_ACCEPTED = "User has not accepted terms and conditions";
    public static final String EVENTS_GET_FUTURE_FAIL = "Failed retrieving future events";
    public static final String EVENTS_GET_FUTURE_OK = "Future events retrieved";

    public static final String EVENTS_GET_CURRENT_START = "Retrieving currently ongoing events";
    public static final String EVENTS_GET_CURRENT_TERMS_NOT_ACCEPTED = "User has not accepted terms and conditions";
    public static final String EVENTS_GET_CURRENT_OK = "Ongoing events retrieved";

    public static final String EVENTS_GET_PAST_START = "Retrieving past events";
    public static final String EVENTS_GET_PAST_TERMS_NOT_ACCEPTED = "User has not accepted terms and conditions";
    public static final String EVENTS_GET_PAST_OK = "Past events retrieved";

    public static final String EVENTS_GET_USER_START = "Retrieving events for user ID: ";
    public static final String EVENTS_GET_USER_UNAUTHORIZED = "User was not authorized to retrieve events for user ID: ";
    public static final String EVENTS_GET_USER_OK = "Past events retrieved for user ID: ";

    public static final String EVENTS_GET_SINGLE_START = "Retrieving event with ID: ";
    public static final String EVENTS_GET_SINGLE_NOT_FOUND = "Can not find event with ID: ";
    public static final String EVENTS_GET_SINGLE_OK = "Event retrieved with ID: ";

    public static final String EVENTS_CREATE_START = "Creating new event";
    public static final String EVENTS_CREATE_INVALID_ORGANIZER = "Event has invalid organizer ID: ";
    public static final String EVENTS_CREATE_INVALID_DATETIME = "Start time is in the past: ";
    public static final String EVENTS_CREATE_INVALID_PARTICIPANTS_COUNT = "Number of participants higher than maximum allowed: ";
    public static final String EVENTS_CREATE_FAIL = "Failed to create the event";
    public static final String EVENTS_CREATE_OK = "Event created";

    public static final String EVENTS_UPDATE_START = "Updating event with ID: ";
    public static final String EVENTS_UPDATE_NOT_FOUND = "Event to be updated can not be found with ID: ";
    public static final String EVENTS_UPDATE_ORGANIZER_NOT_FOUND = "Organizer of the event can not be found with ID: ";
    public static final String EVENTS_UPDATE_INVALID_PARTICIPANTS_COUNT = "Number of participants higher than maximum allowed: ";
    public static final String EVENTS_UPDATE_INVALID_DATETIME = "The event has already ended: ";
    public static final String EVENTS_UPDATE_OK = "Event updated with ID: ";

    public static final String EVENTS_GET_DIVES_START = "Get dives for event with ID: ";
    public static final String EVENTS_GET_DIVES_UNAUTHORIZED = "User can not access dive information for event with ID: ";
    public static final String EVENTS_GET_DIVES_NOT_FOUND = "Event not found with ID: ";
    public static final String EVENTS_GET_DIVES_NO_PARTICIPANTS = "Event has no participants: ";
    public static final String EVENTS_GET_DIVES_NO_DIVES = "Event has no dives: ";
    public static final String EVENTS_GET_DIVES_OK = "Dives retrieved for event with ID: ";

    public static final String EVENTS_UPDATE_DIVES_START = "Update dives for event with ID: ";
    public static final String EVENTS_UPDATE_DIVES_UNAUTHORIZED = "User can not update dive information for event with ID: ";
    public static final String EVENTS_UPDATE_DIVES_NOT_FOUND = "Event not found with ID: ";
    public static final String EVENTS_UPDATE_DIVES_NO_PARTICIPANTS = "Event has no participants: ";
    public static final String EVENTS_UPDATE_DIVES_NO_DIVES = "Event has no dives: ";
    public static final String EVENTS_UPDATE_DIVES_FAIL = "Updating dives failed for event with ID: ";
    public static final String EVENTS_UPDATE_DIVES_OK = "Dives updated for event with ID: ";

    public static final String EVENTS_CANCEL_START = "Cancelling event with ID: ";
    public static final String EVENTS_CANCEL_FAIL = "Failed to cancel event with ID: ";
    public static final String EVENTS_CANCEL_OK = "Event cancelled with ID: ";

    public static final String EVENTS_SUBSCRIBE_START = "Subscribing to event with ID: ";
    public static final String EVENTS_SUBSCRIBE_TERMS_NOT_ACCEPTED = "User has not accepted terms and conditions";
    public static final String EVENTS_SUBSCRIBE_UNKNOWN_USER = "Unknown user account: ";
    public static final String EVENTS_SUBSCRIBE_NOT_FOUND = "Could not find event with ID: ";
    public static final String EVENTS_SUBSCRIBE_FAIL = "Failed to subscribe to event with ID: ";
    public static final String EVENTS_SUBSCRIBE_OK = "Successfully subscribed to event with ID: ";

    public static final String EVENTS_UNSUBSCRIBE_START = "Unsubscribing from event with ID: ";
    public static final String EVENTS_UNSUBSCRIBE_UNKNOWN_USER = "Unknown user account: ";
    public static final String EVENTS_UNSUBSCRIBE_FAIL = "Failed to unsubscribe from event with ID: ";
    public static final String EVENTS_UNSUBSCRIBE_OK = "Successfully unsubscribed from event with ID: ";

    // PaymentController
    public static final String PAYMENTS_GET_ALL_ACTIVE_START = "Retrieving all active payment information";
    public static final String PAYMENTS_GET_ALL_ACTIVE_UNAUTHORIZED = "User was not authorized to retrieve all active payment information";
    public static final String PAYMENTS_GET_ALL_ACTIVE_FAIL = "Failed retrieving all active payment information";
    public static final String PAYMENTS_GET_ALL_ACTIVE_OK = "Active payments retrieved";

    public static final String PAYMENTS_GET_USER_STATUS_START = "";
    public static final String PAYMENTS_GET_USER_STATUS_UNAUTHORIZED = "Unauthorized user tried to retrieve payment status for user ID: ";
    public static final String PAYMENTS_GET_USER_STATUS_OK = "";

    public static final String PAYMENTS_ADD_START = "Adding new payment for user ID: ";
    public static final String PAYMENTS_ADD_UNAUTHORIZED = "User not authorized to add payment for user ID: ";
    public static final String PAYMENTS_ADD_OK = "New payment added for user ID: ";

    public static final String PAYMENTS_UPDATE_START = "Updating payment for user ID: ";
    public static final String PAYMENTS_UPDATE_UNAUTHORIZED = "User not authorized to update payment for user ID: ";
    public static final String PAYMENTS_UPDATE_FAIL = "Failed to update payment for user ID: ";
    public static final String PAYMENTS_UPDATE_OK = "Payment updated for user ID: ";

    public static final String PAYMENTS_RESET_START = "Resetting period payments";
    public static final String PAYMENTS_RESET_UNAUTHORIZED = "User not authorized to reset period payments";
    public static final String PAYMENTS_RESET_FAIL = "Failed to reset period payments";
    public static final String PAYMENTS_RESET_OK = "Period payments has been reset";

    // UserController
    public static final String USERS_GET_DETAILS_START = "Retrieving user details for user ID: ";
    public static final String USERS_GET_DETAILS_UNAUTHORIZED = "User not authorized to retrieve user details for user ID: ";
    public static final String USERS_GET_DETAILS_NOT_FOUND = "User details not found for user ID: ";
    public static final String USERS_GET_DETAILS_OK = "User details retrieved for user ID: ";

    public static final String USERS_UPDATE_START = "Updating user for user ID: ";
    public static final String USERS_UPDATE_UNAUTHORIZED = "User not authorized to update user for user ID: ";
    public static final String USERS_UPDATE_NOT_FOUND = "User not found with user ID: ";
    public static final String USERS_UPDATE_ANONYMIZED = "User has been anonymized with user ID: ";
    public static final String USERS_UPDATE_USERNAME_CHANGED = "Username attempted to change for user ID: ";
    public static final String USERS_UPDATE_FAIL = "Failed to update user for user ID: ";
    public static final String USERS_UPDATE_OK = "Updated for user ID: ";

    public static final String USERS_UPDATE_STATUS_START = "Updating status for user ID: ";
    public static final String USERS_UPDATE_STATUS_UNAUTHORIZED = "User not authorized to update user status for user ID: ";
    public static final String USERS_UPDATE_STATUS_NOT_FOUND = "User not found with user ID: ";
    public static final String USERS_UPDATE_STATUS_FAIL = "Failed to update user status for user ID: ";
    public static final String USERS_UPDATE_STATUS_ANONYMIZED = "User has been anonymized with user ID: ";
    public static final String USERS_UPDATE_STATUS_OK = "User status updated for user ID: ";

    public static final String USERS_GET_START = "Fetching users";
    public static final String USERS_GET_UNAUTHORIZED = "User not authorized to get list of all users";
    public static final String USERS_GET_OK = "Users retrieved";

    public static final String USERS_GET_WITH_ROLE_START = "Fetching users with role: ";
    public static final String USERS_GET_WITH_ROLE_UNAUTHORIZED = "User not authorized to fetch users with role: ";
    public static final String USERS_GET_WITH_ROLE_OK = "Users retrieved with role: ";

    public static final String USERS_SET_TERM_START = "Setting terms and conditions for user";
    public static final String USERS_SET_TERM_UNAUTHORIZED = "User not authorized to set terms and conditions";
    public static final String USERS_SET_TERM_OK = "Terms and conditions set for user";

    public static final String USERS_RESET_TERM_START = "Resetting terms and conditions";
    public static final String USERS_RESET_TERM_UNAUTHORIZED = "User not authorized to reset terms and conditions";
    public static final String USERS_RESET_TERM_OK = "Terms and conditions reset";

    // AuditController
    public static final String AUDIT_GET_START = "Getting audit logs";
    public static final String AUDIT_GET_OK = "Audit logs retrieved";

    public static final String AUDIT_GET_USER_START = "Getting audit log for user ID: ";
    public static final String AUDIT_GET_USER_OK = "Audit log retrieved for user ID: ";

    // RecaptchaFilter
    public static final String RECAPTCHA_FILTER_START = "Recaptcha filter start";
    public static final String RECAPTCHA_FILTER_EMPTY = "Login request did not contain recaptcha header";
    public static final String RECAPTCHA_FILTER_DISABLED = "Recaptcha seems to have been disabled as we did not get any response from Google";
    public static final String RECAPTCHA_FILTER_LOW_SCORE = "Recaptcha score was too low: ";
    public static final String RECAPTCHA_FILTER_OK = "Recaptcha filter completed with score: ";

    // StatsController
    public static final String STATS_GET_YEARLY_REGISTRATION_START = "Fetching yearly registration stats";
    public static final String STATS_GET_YEARLY_REGISTRATION_UNAUTHORIZED = "User not authorized to get list of yearly registration stats";
    public static final String STATS_GET_YEARLY_REGISTRATION_OK = "Return yearly registration stats";

    public static final String STATS_GET_YEARLY_EVENTS_START = "Fetching yearly events stats";
    public static final String STATS_GET_YEARLY_EVENTS_UNAUTHORIZED = "User not authorized to get list of yearly events stats";
    public static final String STATS_GET_YEARLY_EVENTS_OK = "Return yearly events stats";

    public static final String STATS_GET_YEARLY_ORGANIZERS_START = "Fetching yearly organizers stats";
    public static final String STATS_GET_YEARLY_ORGANIZERS_UNAUTHORIZED = "User not authorized to get list of yearly organizers stats";
    public static final String STATS_GET_YEARLY_ORGANIZERS_OK = "Return yearly organizers stats";

    public static final String STATS_GET_YEARLY_PAYMENTS_START = "Fetching yearly payments stats";
    public static final String STATS_GET_YEARLY_PAYMENTS_UNAUTHORIZED = "User not authorized to get list of yearly payments stats";
    public static final String STATS_GET_YEARLY_PAYMENTS_OK = "Return yearly payments stats";

    public static final String STATS_GET_YEARLY_EVENT_REPORTS_START = "Fetching yearly event reports";
    public static final String STATS_GET_YEARLY_EVENT_REPORTS_UNAUTHORIZED = "User not authorized to get list of yearly event reports";
    public static final String STATS_GET_YEARLY_EVENT_REPORTS_OK = "Return yearly event reports";

    public static final String STATS_GET_YEARLY_DIVER_LIST_START = "Fetching yearly top divers list";
    public static final String STATS_GET_YEARLY_DIVER_LIST_UNAUTHORIZED = "User not authorized to get list of yearly top divers list";
    public static final String STATS_GET_YEARLY_DIVER_LIST_OK = "Return yearly top divers list";

    // PageController
    public static final String PAGES_GET_NAVIGATION_ELEMENTS_START = "Fetching all page groups";
    public static final String PAGES_GET_NAVIGATION_ELEMENTS_UNAUTHORIZED = "User not authorized to get list of page groups";
    public static final String PAGES_GET_NAVIGATION_ELEMENTS_OK = "Return page groups";

    public static final String PAGES_GET_PAGES_BY_PATH_START = "Fetching list of all pages by path";
    public static final String PAGES_GET_PAGES_BY_PATH_UNAUTHORIZED = "User not authorized to get list of pages by path";
    public static final String PAGES_GET_PAGES_BY_PATH_OK = "Return pages by path";

    public static final String PAGES_GET_PAGE_START = "Fetching specific page by id: ";
    public static final String PAGES_GET_PAGE_NOT_FOUND = "Requested non-existing page";
    public static final String PAGES_GET_PAGE_UNAUTHORIZED = "User not authorized to get specific page";
    public static final String PAGES_GET_PAGE_OK = "Return requested page";

    // PageManagementController
    public static final String MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_START = "Fetching all page groups for management";
    public static final String MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_UNAUTHORIZED = "User not authorized to get list of page groups";
    public static final String MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_OK = "Return page groups";

    public static final String MGMNT_PAGES_GET_PAGE_GROUP_START = "Fetch page group for management";
    public static final String MGMNT_PAGES_GET_PAGE_GROUP_OK = "Return page group for management";

    public static final String MGMNT_PAGES_CREATE_GROUP_START = "Creating new page group";
    public static final String MGMNT_PAGES_CREATE_GROUP_UNAUTHORIZED = "User not authorized to create a new page group";
    public static final String MGMNT_PAGES_CREATE_GROUP_NONE_CREATED = "Failed to create a new page group";
    public static final String MGMNT_PAGES_CREATE_GROUP_OK = "Return new page group";

    public static final String MGMNT_PAGES_UPDATE_PAGE_GROUP_START = "Updating page group";
    public static final String MGMNT_PAGES_UPDATE_PAGE_GROUP_UNAUTHORIZED = "User not authorized to update page group";
    public static final String MGMNT_PAGES_UPDATE_PAGE_GROUP_NONE_UPDATED = "Failed to update existing page group: ";
    public static final String MGMNT_PAGES_UPDATE_PAGE_GROUP_OK = "Return updated page group";

    public static final String MGMNT_PAGES_CLOSE_PAGE_GROUP_START = "Close existing page group";
    public static final String MGMNT_PAGES_CLOSE_PAGE_GROUP_UNAUTHORIZED = "User not authorized to close page group";
    public static final String MGMNT_PAGES_CLOSE_PAGE_GROUP_NOT_FOUND = "Fail to close non-existing page group: ";
    public static final String MGMNT_PAGES_CLOSE_PAGE_GROUP_OK = "page group closed";

    public static final String MGMNT_PAGES_GET_PAGES_START = "Fetching all pages of a page group for management";
    public static final String MGMNT_PAGES_GET_PAGES_UNAUTHORIZED = "User not authorized to get list of pages";
    public static final String MGMNT_PAGES_GET_PAGES_OK = "Return list of pages";

    public static final String MGMNT_PAGES_GET_PAGE_START = "Fetching a page";
    public static final String MGMNT_PAGES_GET_PAGE_OK = "Return page";

    public static final String MGMNT_PAGES_CREATE_PAGE_START = "Creating a new page";
    public static final String MGMNT_PAGES_CREATE_PAGE_UNAUTHORIZED = "User not authorized to create new page";
    public static final String MGMNT_PAGES_CREATE_PAGE_NONE_CREATED = "Failed to create a new page";
    public static final String MGMNT_PAGES_CREATE_PAGE_OK = "Return new page";

    public static final String MGMNT_PAGES_UPDATE_PAGE_START = "Updating page";
    public static final String MGMNT_PAGES_UPDATE_PAGE_UNAUTHORIZED = "User not authorized to update page";
    public static final String MGMNT_PAGES_CREATE_PAGE_NONE_UPDATED = "Failed to update existing page";
    public static final String MGMNT_PAGES_UPDATE_PAGE_OK = "Return updated page";

    public static final String MGMNT_PAGES_CLOSE_PAGE_START = "Closing existing page";
    public static final String MGMNT_PAGES_CLOSE_PAGE_UNAUTHORIZED = "User not authorized to close page";
    public static final String MGMNT_PAGES_CLOSE_PAGE_NOT_FOUND = "Fail to close non-existing page: ";
    public static final String MGMNT_PAGES_CLOSE_PAGE_OK = "Page closed";

    // EmailNotificationSubscriptionController
    public static final String EMAIL_SUBSCRIPTION_GET_ALL_START = "";
    public static final String EMAIL_SUBSCRIPTION_GET_ALL_UNAUTHORIZED = "";
    public static final String EMAIL_SUBSCRIPTION_GET_ALL_NOT_FOUND = "";
    public static final String EMAIL_SUBSCRIPTION_GET_ALL_OK = "";

    public static final String EMAIL_SUBSCRIPTION_SAVE_START = "";
    public static final String EMAIL_SUBSCRIPTION_SAVE_UNAUTHORIZED = "";
    public static final String EMAIL_SUBSCRIPTION_SAVE_NOT_FOUND = "";
    public static final String EMAIL_SUBSCRIPTION_SAVE_OK = "";

    public static final String EMAIL_SUBSCRIPTION__START = "";
    public static final String EMAIL_SUBSCRIPTION__UNAUTHORIZED = "";
    public static final String EMAIL_SUBSCRIPTION__NOT_FOUND = "";
    public static final String EMAIL_SUBSCRIPTION__OK = "";

    // UploadController
    public static final String UPLOAD_FILE_START = "Uploading file";
    public static final String UPLOAD_FILE_UNAUTHORIZED = "User not authorized to upload file";
    public static final String UPLOAD_FILE_FAIL = "Failed to upload file";
    public static final String UPLOAD_FILE_OK = "File uploaded";

    private AppAuditMessages() {
    }
}
