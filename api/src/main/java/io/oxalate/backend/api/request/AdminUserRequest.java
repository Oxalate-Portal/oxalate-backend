package io.oxalate.backend.api.request;

import io.oxalate.backend.api.AbstractUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "User information update request")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminUserRequest extends AbstractUser {

    @Schema(description = "Set of roles", example = "[ROLE_USER, ROLE_ADMIN]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> roles;
}
