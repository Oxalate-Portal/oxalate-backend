package io.oxalate.backend.api;

/**
 * Enum for the payment type of a dive. Note that we only have period here since the durational type is just a period that is calculated in a different manner
 */

public enum PaymentTypeEnum {
    PERIOD,
    ONE_TIME,
    NONE // This is used when the diver is diving for free, or is e.g. the organizer
}
