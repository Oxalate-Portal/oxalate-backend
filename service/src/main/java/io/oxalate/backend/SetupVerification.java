package io.oxalate.backend;

import io.oxalate.backend.api.UploadDirectoryConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class SetupVerification implements ApplicationContextAware {
    private final String[][] requiredKeys = {
            { "oxalate.app.env", "string" },
            { "oxalate.app.backend-url", "string" },
            { "oxalate.app.frontend-url", "string" },
            { "oxalate.app.jwt-secret", "string" },
            { "oxalate.app.jwt-expiration", "number" },
            { "oxalate.cors.allowed-origins", "string" },
            { "oxalate.captcha.enabled", "boolean" },
            { "oxalate.captcha.site-key", "string" },
            { "oxalate.captcha.secret-key", "string" },
            { "oxalate.captcha.threshold", "number" },
            { "oxalate.upload.directory", "string" }
    };

    @Value("${oxalate.app.env:false}")
    private String oxalateEnvironment;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void printAllConfiguration(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext()
                                     .getEnvironment();

        if (!oxalateEnvironment.equals("prod")) {
            log.info("====== Environment and configuration for {} ======", oxalateEnvironment);
            log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
            final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
            StreamSupport.stream(sources.spliterator(), false)
                         .filter(ps -> ps instanceof EnumerablePropertySource)
                         .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
                         .flatMap(Arrays::stream)
                         .distinct()
                         .filter(prop -> !(prop.contains("credentials") || prop.contains("password")))
                         .forEach(prop -> printProperty(env, prop));
            log.info("===========================================");
        } else {
            log.info("Skipping printing of all configuration in {} environment", oxalateEnvironment);
        }
    }

    @EventListener
    public void startupChecks(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext()
                                     .getEnvironment();
        checkEssentialConfigurations(env);
        checkEssentialDirectories(env);
    }

    public void checkEssentialConfigurations(Environment env) {
        for (String[] keyPair : requiredKeys) {
            log.info("Verifying that key {} is defined and not empty", keyPair[0]);
            String value = env.getProperty(keyPair[0]);

            if (value == null || value.isEmpty()) {
                closeApplication("Missing configuration value for key: " + keyPair[0]);
            } else {
                switch (keyPair[1]) {
                case "number":
                    if (!NumberUtils.isCreatable(value)) {
                        closeApplication("Failed to parse number from configuration " + keyPair[0] + " value " + value);
                    }

                    break;
                case "path":
                    Path path = Paths.get(value);

                    if (!Files.exists(path)) {
                        closeApplication("Path from configuration " + keyPair[0] + " pointing to " + value + " does not exist");
                    }
                    break;
                case "string":
                    // For now we don't do any verification, might split this further to other types
                    break;
                case "boolean":
                    // For now we don't do any verification
                    break;
                default:
                    closeApplication("Unknown configuration type: " + keyPair[1]);
                }
            }
        }

        // Check if oxalate.first-time is set to true, then require the following keys to also be set: oxalate.admin.username, oxalate.admin.hashed-password
        if (env.getProperty("oxalate.first-time", Boolean.class, false)) {
            if (env.getProperty("oxalate.admin.username") == null || env.getProperty("oxalate.admin.hashed-password") == null) {
                closeApplication("First-time setup requires oxalate.admin.username and oxalate.admin.hashed-password to be set");
            }
        }
    }

    private void checkEssentialDirectories(Environment env) {
        String uploadDir = env.getProperty("oxalate.upload.directory", String.class, "");

        if (uploadDir.isEmpty()) {
            closeApplication("Missing configuration value for key: oxalate.upload.directory");
        }

        for (var subDir : UploadDirectoryConstants.getAllDirectories()) {
            Path dirPath = Paths.get(uploadDir, subDir);
            verifyDirectory(dirPath);
        }
    }

    private void verifyDirectory(Path fileDir) {
        log.debug("Verifying directory: {}", fileDir);

        if (!Files.exists(fileDir)) {
            // If the sub-directory of them are missing, then create them
            try {
                Files.createDirectories(fileDir);
            } catch (Exception e) {
                closeApplication("Could not create directory: " + fileDir);
            }
            // Check whether the directory is writable
            if (!Files.isWritable(fileDir)) {
                closeApplication("No write access to directory: " + fileDir);
            }
        }
    }

    private void closeApplication(String message) {
        log.error(message);
        log.error("Shutting down application");
        ((ConfigurableApplicationContext) applicationContext).close();
    }

    private void printProperty(Environment env, String key) {
        try {
            log.info("{}: {}", key, env.getProperty(key));
        } catch (Exception e) {
            log.error("Failed to fetch property value for {}", key);
        }
    }
}
