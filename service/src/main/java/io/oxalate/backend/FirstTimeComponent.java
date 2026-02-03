package io.oxalate.backend;

import io.oxalate.backend.repository.UserRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FirstTimeComponent implements ApplicationListener<ApplicationReadyEvent> {
    private final UserRepository userRepository;

    @Value("${oxalate.first-time:false}")
    private boolean firstTime;

    @Value("${oxalate.admin.username:}")
    private String adminUsername;

    @Value("${oxalate.admin.hashed-password:}")
    private String adminHashedPassword;

    @Override
    public void onApplicationEvent(@Nullable ApplicationReadyEvent event) {
        log.debug("ApplicationStartedEvent: First time {} with event: {}", firstTime, event);

        if (firstTime) {
            log.info("First time running the application, updating admin user credentials");
            updateAdminUserCredentials();
        }
    }

    private void updateAdminUserCredentials() {
        var adminUser = userRepository.findById(1L).orElse(null);

        if (adminUser == null) {
            log.error("Failed to find admin user with ID 1, exiting setup");
            return;
        }

        if (StringUtils.isEmpty(adminUsername) || StringUtils.isEmpty(adminHashedPassword)) {
            log.error("Admin username or password is empty, exiting setup");
            return;
        }

        adminUser.setPassword(adminHashedPassword);
        adminUser.setUsername(adminUsername);
        userRepository.save(adminUser);
    }
}
