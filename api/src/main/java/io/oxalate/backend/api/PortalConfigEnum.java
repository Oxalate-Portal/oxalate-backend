package io.oxalate.backend.api;

public enum PortalConfigEnum {
    EMAIL("email", EmailConfigEnum.class),
    FRONTEND("frontend", FrontendConfigEnum.class),
    GENERAL("general", GeneralConfigEnum.class),
    MEMBERSHIP("membership", MembershipConfigEnum.class),
    PAYMENT("payment", PaymentConfigEnum.class),
    COMMENTING("commenting", CommentConfigEnum.class);

    public final String group;
    public final Class<?> subEnum;

    PortalConfigEnum(String group, Class<?> subEnum) {
        this.group = group;
        this.subEnum = subEnum;
    }

    // Sub-enum for comment-related settings
    public enum CommentConfigEnum {
        COMMENT_ENABLED("commenting-enabled"),
        COMMENT_ALLOW_EDITING("commenting-allow-editing"),
        COMMENT_ENABLED_FEATURES("commenting-enabled-features"),
        COMMENT_REPORT_TRIGGER_LEVEL("comments-report-trigger-level"),
        COMMENT_REQUIRE_REVIEW("comments-require-review"),;

        public final String key;

        CommentConfigEnum(String key) {
            this.key = key;
        }
    }
    // Sub-enum for email-related settings
    public enum EmailConfigEnum {
        EMAIL_ENABLED("email-enabled"),
        EMAIL_NOTIFICATIONS("email-notifications"),
        EMAIL_NOTIFICATION_RETRIES("email-notification-retries"),
        ORG_EMAIL("org-email"),
        SUPPORT_EMAIL("support-email"),
        SYSTEM_EMAIL("system-email");

        public final String key;

        EmailConfigEnum(String key) {
            this.key = key;
        }
    }

    // Sub-enum for frontend-related settings
    public enum FrontendConfigEnum {
        MAX_DEPTH("max-depth"),
        MAX_DIVE_LENGTH("max-dive-length"),
        MAX_EVENT_LENGTH("max-event-length"),
        MAX_PARTICIPANTS("max-participants"),
        MIN_EVENT_LENGTH("min-event-length"),
        MIN_PARTICIPANTS("min-participants"),
        TYPES_OF_EVENT("types-of-event"),
        MAX_CERTIFICATES("max-certificates");

        public final String key;

        FrontendConfigEnum(String key) {
            this.key = key;
        }
    }

    // Sub-enum for general-related settings
    public enum GeneralConfigEnum {
        DEFAULT_LANGUAGE("default-language"),
        TIMEZONE("timezone"),
        ENABLED_LANGUAGES("enabled-language"),
        ORG_NAME("org-name"),
        TOP_DIVER_LIST_SIZE("top-divers-list-size");

        public final String key;

        GeneralConfigEnum(String key) {
            this.key = key;
        }
    }

    public enum MembershipConfigEnum {
        EVENT_REQUIRE_MEMBERSHIP("event-require-membership"),
        MEMBERSHIP_PERIOD_LENGTH("membership-period-length"),
        MEMBERSHIP_PERIOD_START("membership-period-start"),
        MEMBERSHIP_PERIOD_START_POINT("membership-period-start-point"),
        MEMBERSHIP_PERIOD_UNIT("membership-period-unit"),
        MEMBERSHIP_TYPE("membership-type");

        public final String key;

        MembershipConfigEnum(String key) {
            this.key = key;
        }
    }

    // Sub-enum for payment-related settings
    public enum PaymentConfigEnum {
        EVENT_REQUIRE_PAYMENT("event-require-payment"),
        ONE_TIME_PAYMENT_EXPIRATION_TYPE("one-time-expiration-type"),
        ONE_TIME_PAYMENT_EXPIRATION_UNIT("one-time-expiration-unit"),
        ONE_TIME_PAYMENT_EXPIRATION_LENGTH("one-time-expiration-length"),
        PAYMENT_ENABLED("payment-enabled"),
        PAYMENT_PERIOD_LENGTH("payment-period-length"),
        PAYMENT_PERIOD_START("payment-period-start"),
        PAYMENT_PERIOD_START_POINT("payment-period-start-point"),
        PERIODICAL_PAYMENT_METHOD_TYPE("periodical-payment-method-type"),
        PERIODICAL_PAYMENT_METHOD_UNIT("periodical-payment-method-unit"),
        SINGLE_PAYMENT_ENABLED("single-payment-enabled");

        public final String key;

        PaymentConfigEnum(String key) {
            this.key = key;
        }
    }
}
