package io.oxalate.backend.api.response.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiYearValueResponse {
    @JsonProperty("year")
    private long year;
    @JsonProperty("value")
    private long value;
    @JsonProperty("type")
    private String type;
}
