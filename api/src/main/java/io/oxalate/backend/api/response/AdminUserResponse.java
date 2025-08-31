package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * An extended user which contains additional fields displayed for administrators
 */
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserResponse extends UserResponse {

    @Schema(description = "Set of roles", example = "[ROLE_USER, ROLE_ADMIN]", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("roles")
    private Set<String> roles;

    @Schema(description = "Timestamp of the last time the user was seen", example = "2023-10-05T14:48:00Z")
    @JsonProperty("lastSeen")
    protected Instant lastSeen;
}
