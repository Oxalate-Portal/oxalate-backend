package io.oxalate.backend.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Enumeration of user groups for notification targeting.
 * Used to send notifications to specific categories of users based on their activity, membership, or account status.
 */
@Getter
public enum NotificationGroupEnum {
    @JsonProperty("all_registered")
    ALL_REGISTERED("All registered users"),

    @JsonProperty("inactive_days")
    INACTIVE_DAYS("Users inactive for a specified number of days"),

    @JsonProperty("active_membership")
    ACTIVE_MEMBERSHIP("Users with active membership"),

    @JsonProperty("no_active_membership")
    NO_ACTIVE_MEMBERSHIP("Users without active membership"),

    @JsonProperty("locked_accounts")
    LOCKED_ACCOUNTS("Users with locked accounts"),

    @JsonProperty("never_had_membership")
    NEVER_HAD_MEMBERSHIP("Registered users who have never had a membership");

    private final String description;

    NotificationGroupEnum(String description) {
        this.description = description;
    }
}
