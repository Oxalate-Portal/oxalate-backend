package io.oxalate.backend.api.response.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearlyDiversListResponse {
    @JsonProperty("year")
    private long year;

    @JsonProperty("divers")
    private List<DiverListItemResponse> divers;
}
