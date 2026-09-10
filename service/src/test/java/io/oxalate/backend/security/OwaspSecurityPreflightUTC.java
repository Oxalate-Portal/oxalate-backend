package io.oxalate.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * OWASP A02:2025 (Security Misconfiguration) and A04:2025 (Cryptographic Failures).
 * <p>
 * The repository ships a working development configuration, and {@code application.yaml} defaults the active
 * profile to {@code local}. That combination means a deployment which forgets to set
 * {@code SPRING_PROFILES_ACTIVE} would boot with a JWT signing key that is published in the source tree,
 * letting anyone mint a token for any account. {@link SecurityPreflight} converts that silent catastrophe into
 * a refusal to start, and these tests make sure it keeps doing so.
 */
@DisplayName("OWASP A02: insecure configuration prevents startup")
class OwaspSecurityPreflightUTC {

    private static final String STRONG_SECRET = "c3Ryb25nLXNlY3JldC1zdHJvbmctc2VjcmV0LXN0cm9uZy1zZWNyZXQtc3Ryb25nLXNlY3JldC1zdHJvbmctc2VjcmV0LTEyMzQ=";

    private final SecurityPreflight preflight = new SecurityPreflight();

    @Test
    void findViolationsWithHardenedProductionConfigurationOk() {
        assertTrue(preflight.findViolations(productionEnvironment())
                            .isEmpty(), "A correctly configured production environment must start");
    }

    @Test
    void findViolationsWithCommittedJwtSecretFail() {
        for (var knownSecret : SecurityPreflight.KNOWN_DEVELOPMENT_JWT_SECRETS) {
            var env = productionEnvironment();
            env.setProperty("oxalate.app.jwt-secret", knownSecret);

            assertTrue(containsMention(preflight.findViolations(env), "jwt-secret"),
                    "The committed development key " + knownSecret.substring(0, 8) + "... must be refused in production");
        }
    }

    @Test
    void findViolationsWithShortJwtSecretFail() {
        var env = productionEnvironment();
        // 32 bytes, far below what HS512 needs
        env.setProperty("oxalate.app.jwt-secret", "c2hvcnQta2V5LXNob3J0LWtleS1zaG9ydCE=");

        assertTrue(containsMention(preflight.findViolations(env), "jwt-secret"), "A key shorter than 512 bits must be refused");
    }

    @Test
    void findViolationsWithMissingJwtSecretFail() {
        var env = productionEnvironment();
        env.setProperty("oxalate.app.jwt-secret", "");

        assertTrue(containsMention(preflight.findViolations(env), "jwt-secret"));
    }

    @Test
    void findViolationsWithSameSiteNoneFail() {
        var env = productionEnvironment();
        env.setProperty("oxalate.app.jwt-same-site", "None");

        assertTrue(containsMention(preflight.findViolations(env), "jwt-same-site"),
                "SameSite=None would let any site ride the session cookie, which is the CSRF defence");
    }

    @Test
    void findViolationsWithInsecureCookieFail() {
        var env = productionEnvironment();
        env.setProperty("oxalate.app.jwt-secure", "false");

        assertTrue(containsMention(preflight.findViolations(env), "jwt-secure"), "The session cookie must never travel over plain HTTP");
    }

    @Test
    void findViolationsWithCommittedCaptchaSecretFail() {
        var env = productionEnvironment();
        env.setProperty("oxalate.captcha.enabled", "true");
        env.setProperty("oxalate.captcha.secret-key", "test-secret-key");

        assertTrue(containsMention(preflight.findViolations(env), "captcha"));
    }

    /**
     * The {@code local} profile activates {@code TestController}, whose {@code /api/test/**} endpoints are
     * unauthenticated by design.
     */
    @Test
    void findViolationsWithLocalProfileActiveInProductionFail() {
        var env = productionEnvironment();
        env.setActiveProfiles("local");

        assertTrue(containsMention(preflight.findViolations(env), "local"));
    }

    @Test
    void findViolationsInLocalEnvironmentOk() {
        var env = new MockEnvironment();
        env.setProperty("oxalate.app.env", "local");
        env.setProperty("oxalate.app.jwt-secret", SecurityPreflight.KNOWN_DEVELOPMENT_JWT_SECRETS.iterator()
                                                                                                 .next());
        env.setProperty("oxalate.app.jwt-same-site", "None");
        env.setProperty("oxalate.app.jwt-secure", "false");

        assertTrue(preflight.findViolations(env)
                            .isEmpty(), "Development conveniences are allowed in the local environment");
    }

    @Test
    void findViolationsInTestEnvironmentOk() {
        var env = new MockEnvironment();
        env.setProperty("oxalate.app.env", "test");

        assertTrue(preflight.findViolations(env)
                            .isEmpty());
    }

    @Test
    void findViolationsReportsEveryProblemAtOnceOk() {
        var env = productionEnvironment();
        env.setProperty("oxalate.app.jwt-secret", "");
        env.setProperty("oxalate.app.jwt-same-site", "None");
        env.setProperty("oxalate.app.jwt-secure", "false");

        assertEquals(3, preflight.findViolations(env)
                                 .size(), "The operator should see every misconfiguration in one go");
    }

    @Test
    void knownDevelopmentSecretsCoverTheCommittedConfigurationOk() {
        assertFalse(SecurityPreflight.KNOWN_DEVELOPMENT_JWT_SECRETS.isEmpty(),
                "Every key committed to the repository must be listed so it can never be used in production");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MockEnvironment productionEnvironment() {
        var env = new MockEnvironment();
        env.setProperty("oxalate.app.env", "prod");
        env.setProperty("oxalate.app.jwt-secret", STRONG_SECRET);
        env.setProperty("oxalate.app.jwt-same-site", "Strict");
        env.setProperty("oxalate.app.jwt-secure", "true");
        env.setProperty("oxalate.captcha.enabled", "true");
        env.setProperty("oxalate.captcha.secret-key", "a-real-captcha-secret");
        return env;
    }

    private boolean containsMention(java.util.List<String> violations, String needle) {
        return violations.stream()
                         .anyMatch(violation -> violation.contains(needle));
    }
}
