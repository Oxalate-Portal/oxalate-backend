package io.oxalate.backend.api;

public enum PortalConfigEnum {
    EMAIL("email", EmailConfigEnum.class),
    FRONTEND("frontend", FrontendConfigEnum.class),
    GENERAL("general", GeneralConfigEnum.class),
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
        TYPES_OF_EVENT("types-of-event");

        public final String key;

        FrontendConfigEnum(String key) {
            this.key = key;
        }
    }

    // Sub-enum for general-related settings
    public enum GeneralConfigEnum {
        DEFAULT_LANGUAGE("default-language"),
        ORG_NAME("org-name");

        public final String key;

        GeneralConfigEnum(String key) {
            this.key = key;
        }
    }

    // Sub-enum for payment-related settings
    public enum PaymentConfigEnum {
        ENABLED_PAYMENT_METHODS("enabled-payment-methods"),
        EVENT_REQUIRE_PAYMENT("event-require-payment"),
        START_MONTH("start-month");

        public final String key;

        PaymentConfigEnum(String key) {
            this.key = key;
        }
    }
}
