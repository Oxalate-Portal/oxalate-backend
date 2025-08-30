package io.oxalate.backend.events;

import io.oxalate.backend.api.AuditLevelEnum;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AppAuditEvent {
    private final String traceId;
    private final Object source;
    private final AuditLevelEnum level;
    private final Long userId;
    private final String address;
    private final String message;
    private final Instant createdAt;
}
