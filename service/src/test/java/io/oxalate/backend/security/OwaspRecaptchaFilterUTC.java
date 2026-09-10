package io.oxalate.backend.security;

import static io.oxalate.backend.api.UrlConstants.API;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OWASP A07:2025 - Authentication Failures.
 * <p>
 * The captcha used to guard only {@code POST /api/auth/login}. Registration, password reset and confirmation
 * resend were left open, which allows automated account enumeration and lets the mail sender be used to bomb
 * arbitrary addresses. This test pins the full set of protected endpoints so a new unauthenticated POST is a
 * deliberate decision.
 */
@DisplayName("OWASP A07: all unauthenticated write endpoints are captcha protected")
class OwaspRecaptchaFilterUTC {

    @Test
    void protectedPathsCoverEveryAbusableAuthEndpointOk() {
        var expected = java.util.Set.of(
                API + "/auth/login",
                API + "/auth/register",
                API + "/auth/lost-password",
                API + "/auth/reset-password",
                API + "/auth/registrations/resend-confirmation"
        );

        for (var path : expected) {
            assertTrue(RecaptchaFilter.PROTECTED_PATHS.contains(path), path + " must be captcha protected");
        }
    }

    @Test
    void protectedPathsAreAllUnderTheAuthApiOk() {
        for (var path : RecaptchaFilter.PROTECTED_PATHS) {
            assertTrue(path.startsWith(API + "/auth/"), "Unexpected captcha protected path: " + path);
            assertFalse(path.endsWith("*"), "Captcha paths are matched exactly, wildcards would silently never match");
        }
    }
}
