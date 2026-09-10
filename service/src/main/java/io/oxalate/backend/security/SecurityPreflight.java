package io.oxalate.backend.security;

import io.jsonwebtoken.io.Decoders;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Startup guard for the security-critical parts of the configuration.
 * <p>
 * OWASP A02:2025 (Security Misconfiguration) and A04:2025 (Cryptographic Failures). The repository ships a
 * development configuration so that {@code ./mvnw spring-boot:run} works out of the box. Without this guard,
 * a deployment that forgets to set {@code SPRING_PROFILES_ACTIVE} or the secret environment variables would
 * silently boot with a signing key that is public in the source tree, which is a complete authentication
 * bypass. This class turns that silent failure into a refusal to start.
 * <p>
 * The rules only relax for the {@code local} and {@code test} environments, which are never internet facing.
 */
@Slf4j
@Component
public class SecurityPreflight implements ApplicationContextAware {

    /**
     * Environments in which development conveniences are tolerated.
     */
    static final Set<String> NON_DEPLOYED_ENVIRONMENTS = Set.of("local", "test");

    /**
     * Minimum key length in bytes. HS512 requires at least 512 bits of key material.
     */
    static final int MINIMUM_JWT_SECRET_BYTES = 64;

    /**
     * Signing keys that are committed to the repository and therefore public knowledge.
     */
    static final Set<String> KNOWN_DEVELOPMENT_JWT_SECRETS = Set.of(
            "ZGV2ZWxvcG1lbnQtb25seS1pbnNlY3VyZS1rZXktZG8tbm90LXVzZS1hbnl3aGVyZS1idXQtbG9jYWxob3N0ISE=",
            "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
            "4f0dd9c339d77e7a98a7e90fc3d6572d40426f4bf4b4a845389f7773923a3dc0da8608c3e92028aea5ebabfaeaf85154f2737af04c137bc090d92c862a9d6b32"
    );

    /**
     * Captcha credentials that are committed to the repository.
     */
    static final Set<String> KNOWN_DEVELOPMENT_CAPTCHA_SECRETS = Set.of(
            "local-development-secret-key",
            "test-secret-key",
            "6LfGtFImAAAAACF1aaNeVgMdgfxdGYTR-gPidaeS"
    );

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void verifySecurityConfiguration(ContextRefreshedEvent event) {
        var violations = findViolations(event.getApplicationContext()
                                             .getEnvironment());

        if (violations.isEmpty()) {
            log.info("Security preflight checks passed");
            return;
        }

        violations.forEach(violation -> log.error("Security preflight failure: {}", violation));
        log.error("Refusing to start with an insecure security configuration, shutting down application");
        ((ConfigurableApplicationContext) applicationContext).close();
    }

    /**
     * Evaluates the security configuration. Kept free of Spring context access so it can be unit tested
     * directly.
     *
     * @param env the resolved environment
     * @return a human-readable description of every violation found, empty when the configuration is sound
     */
    List<String> findViolations(Environment env) {
        var violations = new ArrayList<String>();
        var environmentName = env.getProperty("oxalate.app.env", "");

        if (isNonDeployedEnvironment(environmentName)) {
            log.info("Running in {} environment, relaxed security configuration is permitted", environmentName);
            return violations;
        }

        verifyJwtSecret(env, violations);
        verifyCookieFlags(env, violations);
        verifyCaptcha(env, violations);
        verifyProfiles(env, violations);

        return violations;
    }

    private boolean isNonDeployedEnvironment(String environmentName) {
        return NON_DEPLOYED_ENVIRONMENTS.contains(environmentName.toLowerCase(Locale.ROOT));
    }

    private void verifyJwtSecret(Environment env, List<String> violations) {
        var jwtSecret = env.getProperty("oxalate.app.jwt-secret", "");

        if (jwtSecret.isBlank()) {
            violations.add("oxalate.app.jwt-secret is not set");
            return;
        }

        if (KNOWN_DEVELOPMENT_JWT_SECRETS.contains(jwtSecret)) {
            violations.add("oxalate.app.jwt-secret is a development key that is committed to the repository, "
                    + "generate a new one with 'openssl rand -base64 64' and set it via OXALATE_JWT_SECRET");
            return;
        }

        try {
            var keyLength = Decoders.BASE64.decode(jwtSecret).length;

            if (keyLength < MINIMUM_JWT_SECRET_BYTES) {
                violations.add("oxalate.app.jwt-secret decodes to " + keyLength + " bytes, HS512 requires at least "
                        + MINIMUM_JWT_SECRET_BYTES);
            }
        } catch (Exception e) {
            violations.add("oxalate.app.jwt-secret is not valid base64");
        }
    }

    private void verifyCookieFlags(Environment env, List<String> violations) {
        var sameSite = env.getProperty("oxalate.app.jwt-same-site", "");

        if (!"Strict".equalsIgnoreCase(sameSite) && !"Lax".equalsIgnoreCase(sameSite)) {
            violations.add("oxalate.app.jwt-same-site is '" + sameSite + "', which allows the session cookie to be sent "
                    + "on cross-site requests, expected Strict or Lax");
        }

        if (!env.getProperty("oxalate.app.jwt-secure", Boolean.class, false)) {
            violations.add("oxalate.app.jwt-secure is false, the session cookie would be sent over plain HTTP");
        }
    }

    private void verifyCaptcha(Environment env, List<String> violations) {
        if (!env.getProperty("oxalate.captcha.enabled", Boolean.class, false)) {
            log.warn("Captcha is disabled outside a development environment, login and registration endpoints have no bot protection");
            return;
        }

        var secretKey = env.getProperty("oxalate.captcha.secret-key", "");

        if (KNOWN_DEVELOPMENT_CAPTCHA_SECRETS.contains(secretKey)) {
            violations.add("oxalate.captcha.secret-key is a development value that is committed to the repository");
        }
    }

    private void verifyProfiles(Environment env, List<String> violations) {
        for (var profile : env.getActiveProfiles()) {
            if ("local".equalsIgnoreCase(profile)) {
                violations.add("The 'local' Spring profile is active outside a local environment, which exposes the "
                        + "unauthenticated /api/test endpoints");
            }
        }
    }
}
