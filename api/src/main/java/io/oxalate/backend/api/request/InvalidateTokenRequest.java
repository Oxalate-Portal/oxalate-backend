package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvalidateTokenRequest {
    @NotBlank
    @JsonAlias("token")
    private String tokenValue;
}
