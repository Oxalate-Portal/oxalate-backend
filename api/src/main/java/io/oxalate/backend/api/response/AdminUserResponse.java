package io.oxalate.backend.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * An extended user which contains additional fields displayed for administrators
 */
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminUserResponse extends UserResponse {

    @Schema(description = "Set of roles", example = "[ROLE_USER, ROLE_ADMIN]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> roles;

    @Schema(description = "Timestamp of the last time the user was seen", example = "2023-10-05T14:48:00Z")
    protected Instant lastSeen;
}
