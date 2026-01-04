package io.oxalate.backend.api.response.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregateResponse {
    @Schema(description = "List of yearly event counts")
    @JsonProperty("eventsPerYear")
    List<MultiYearValueResponse> eventsPerYear;

    @Schema(description = "List of yearly event counts per type")
    @JsonProperty("eventTypesPerYear")
    List<MultiYearValueResponse> eventTypesPerYear;

    @Schema(description = "List of yearly diver counts")
    @JsonProperty("diversPerYear")
    List<MultiYearValueResponse> diversPerYear;

    @Schema(description = "List of yearly dive counts per type")
    @JsonProperty("diverTypesPerYear")
    List<MultiYearValueResponse> diverTypesPerYear;
}
