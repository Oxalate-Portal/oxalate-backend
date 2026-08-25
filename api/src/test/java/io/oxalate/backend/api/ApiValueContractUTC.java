package io.oxalate.backend.api;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiValueContractUTC {

    @Test
    void rolesRoundTripAndRejectUnknownValues() {
        for (var role : RoleEnum.values()) {
            assertEquals(role, RoleEnum.fromString(role.toString()));
        }
        assertNull(RoleEnum.fromString("ROLE_UNKNOWN"));
    }

    @Test
    void supportedLanguageCodesAreValidIso6391Tags() {
        for (var language : new String[]{"de", "en", "fi", "sv"}) {
            assertEquals(language, Locale.forLanguageTag(language).getLanguage());
            assertEquals(2, language.length());
        }
    }

}
