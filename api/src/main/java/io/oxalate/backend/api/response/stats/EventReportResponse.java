package io.oxalate.backend.api.response.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventReportResponse {
    @JsonProperty("eventId")
    private long eventId;

    @JsonProperty("eventDateTime")
    private Instant eventDateTime;

    @JsonProperty("organizerName")
    private String organizerName;

    @JsonProperty("participantCount")
    private int participantCount;

    @JsonProperty("diveCount")
    private int diveCount;
}
