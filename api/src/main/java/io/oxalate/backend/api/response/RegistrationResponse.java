package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class RegistrationResponse extends ActionResponse {

    @JsonProperty("token")
    private String token;
}
