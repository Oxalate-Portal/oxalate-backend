package io.oxalate.backend;

import io.oxalate.backend.configuration.CaptchaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Required for scheduled tasks
@EnableAsync // Required for asynchronous handling of application events
@EnableConfigurationProperties(CaptchaProperties.class)
public class OxalateBackendApp {

    public static void main(String[] args) {
        SpringApplication.run(OxalateBackendApp.class, args);
    }
}
