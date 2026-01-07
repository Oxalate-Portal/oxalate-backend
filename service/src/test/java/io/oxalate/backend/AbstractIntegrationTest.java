package io.oxalate.backend;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class AbstractIntegrationTest {

    static PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer(
            "postgres:18-alpine"
    );

    @BeforeAll
    static void beforeAll() {
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("oxalate.app.jwt-expiration", () -> 3600);
        registry.add("oxalate.upload.directory", () -> "/tmp/oxalate");
    }
}
