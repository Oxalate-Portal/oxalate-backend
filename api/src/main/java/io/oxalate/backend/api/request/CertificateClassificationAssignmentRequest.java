package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertificateClassificationAssignmentRequest {
    @JsonProperty("certificateId")
    private Long certificateId;
    @JsonProperty("certificateNames")
    private List<String> certificateNames;
    @JsonProperty("classificationId")
    private Long classificationId;
}
