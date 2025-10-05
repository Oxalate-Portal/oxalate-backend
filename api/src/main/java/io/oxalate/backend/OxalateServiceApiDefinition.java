package io.oxalate.backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.experimental.UtilityClass;

@OpenAPIDefinition(info = @Info(version = "${info.build.version}", title = "Oxalate Service REST endpoints"),
        servers = @Server(url = "http://localhost:8080/", description = "current server"),
        security = {@SecurityRequirement(name = "bearerToken"), @SecurityRequirement(name = "cookie")})
@UtilityClass
public class OxalateServiceApiDefinition {
}
