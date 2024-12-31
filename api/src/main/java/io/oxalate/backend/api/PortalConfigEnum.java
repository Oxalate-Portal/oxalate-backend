package io.oxalate.backend.api;

public enum PortalConfigEnum {
    EMAIL("email", EmailConfigEnum.class),
    FRONTEND("frontend", FrontendConfigEnum.class),
    GENERAL("general", GeneralConfigEnum.class),
    MEMBERSHIP("membership", MembershipConfigEnum.class),
    PAYMENT("payment", PaymentConfigEnum.class);

    public final String group;
    public final Class<?> subEnum;

    PortalConfigEnum(String group, Class<?> subEnum) {
        this.group = group;
        this.subEnum = subEnum;
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
        MEMBERSHIP_PERIOD_LENGTH("membership-period-length"),
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
        PAYMENT_ENABLED("payment-enabled"),
        PAYMENT_PERIOD_LENGTH("payment-period-length"),
        PERIODICAL_PAYMENT_METHOD_TYPE("periodical-payment-method-type"),
        PERIODICAL_PAYMENT_METHOD_UNIT("periodical-payment-method-unit"),
        PERIOD_START_POINT("period-start-point"),
        SINGLE_PAYMENT_ENABLED("single-payment-enabled");

        public final String key;

        PaymentConfigEnum(String key) {
            this.key = key;
        }
    }
}
