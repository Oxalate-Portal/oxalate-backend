package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(description = "User information update request")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRequest extends AbstractUser {

    @Schema(description = "Set of roles", example = "[ROLE_USER, ROLE_ADMIN]", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("roles")
    private Set<String> roles;
}
