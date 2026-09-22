package io.oxalate.backend.api.response;

import io.oxalate.backend.api.AuditLevelEnum;
import java.time.Instant;
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
public class AuditEntryResponse {
    private long id;
    private String traceId;
    private String source;
    private AuditLevelEnum level;
    private long userId;
    private String userName;
    private String address;
    private String message;
    private Instant createdAt;

}
