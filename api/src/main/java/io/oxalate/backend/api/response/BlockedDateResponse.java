package io.oxalate.backend.api.response;

import java.time.Instant;
import java.util.Date;
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
public class BlockedDateResponse {
    private long id;
    private Date blockedDate;
    private Instant createdAt;
    private String creatorName;
    private String reason;
}
