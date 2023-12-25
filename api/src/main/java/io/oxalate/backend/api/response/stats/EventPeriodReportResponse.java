package io.oxalate.backend.api.response.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventPeriodReportResponse {
    @JsonProperty("periodStart")
    private Instant periodStart;
    @JsonProperty("period")
    private String period;
    @JsonProperty("events")
    private List<EventReportResponse> events;
}
