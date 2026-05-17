package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionToken extends UserResponse {

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("type")
    private String type;

    @JsonProperty("roles")
    private List<String> roles;

    @JsonProperty("expiresAt")
    private Instant expiresAt;
}
