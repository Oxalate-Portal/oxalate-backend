package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertificateValueReplacementRequest {
    @JsonProperty("existingValues")
    private List<String> existingValues;
    @JsonProperty("newValue")
    private String newValue;
}
