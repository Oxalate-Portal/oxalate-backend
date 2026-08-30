package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateClassificationResponse {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("titles")
    private Map<String, String> titles;
    @JsonProperty("description")
    private String description;
    @JsonProperty("order")
    private Integer order;
}
