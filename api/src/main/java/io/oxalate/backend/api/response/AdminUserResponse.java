package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("roles")
    private Set<String> roles;

    @JsonProperty("status")
    private String status;

    @JsonProperty("privacy")
    private boolean privacy;

    @JsonProperty("nextOfKin")
    private String nextOfKin;
}
