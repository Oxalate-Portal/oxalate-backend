package io.oxalate.backend.configuration;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@SecurityScheme(
        name = JWT_COOKIE,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = JWT_TOKEN
)
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
