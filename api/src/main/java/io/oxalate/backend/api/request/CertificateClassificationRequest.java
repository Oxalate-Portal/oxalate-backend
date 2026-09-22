package io.oxalate.backend.api.request;

import io.oxalate.backend.api.response.CertificateClassificationResponse;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CertificateClassificationRequest extends CertificateClassificationResponse {
}
