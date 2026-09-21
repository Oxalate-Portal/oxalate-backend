package io.oxalate.backend.api.response.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AggregateResponse {
    @Schema(description = "List of yearly event counts")
    List<MultiYearValueResponse> eventsPerYear;

    @Schema(description = "List of yearly event counts per type")
    List<MultiYearValueResponse> eventTypesPerYear;

    @Schema(description = "List of yearly diver counts")
    List<MultiYearValueResponse> diversPerYear;

    @Schema(description = "List of yearly dive counts per type")
    List<MultiYearValueResponse> diverTypesPerYear;
}
