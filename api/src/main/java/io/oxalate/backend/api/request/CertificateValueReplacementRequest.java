package io.oxalate.backend.api.request;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CertificateValueReplacementRequest {
    private List<String> existingValues;
    private String newValue;
}
