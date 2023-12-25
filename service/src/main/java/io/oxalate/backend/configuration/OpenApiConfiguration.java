package io.oxalate.backend.configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@Configuration
public class OpenApiConfiguration {
    @Bean
    public GroupedOpenApi publicApi(@Value("${spring.application.name:DefaultOxalateApplication}") String applicationName) {
        return GroupedOpenApi.builder()
                             .group(applicationName)
                             .pathsToMatch("/**")
                             .packagesToScan("io.oxalate.backend")
                             .build();
    }

}
