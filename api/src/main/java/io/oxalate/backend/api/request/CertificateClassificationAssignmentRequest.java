package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertificateClassificationAssignmentRequest {
    @JsonProperty("certificateId")
    private Long certificateId;
    @JsonProperty("certificateName")
    private String certificateName;
    @JsonProperty("classificationId")
    private Long classificationId;
}
