package io.oxalate.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Required for scheduled tasks
@EnableAsync // Required for asynchronous handling of application events
public class OxalateBackendApp {

    static void main(String[] args) {
        SpringApplication.run(OxalateBackendApp.class, args);
    }
}
