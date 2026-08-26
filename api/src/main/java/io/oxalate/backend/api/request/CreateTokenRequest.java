package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTokenRequest {
    @NotNull
    @Future
    @JsonProperty("expiresAt")
    @JsonAlias("expiration")
    private Instant expiresAt;

    @Size(max = 255)
    private String description;
}
