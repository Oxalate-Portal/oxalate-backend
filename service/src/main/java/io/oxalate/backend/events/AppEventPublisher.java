package io.oxalate.backend.events;

import io.oxalate.backend.api.AuditLevel;
import static io.oxalate.backend.tools.HttpTools.getRemoteIp;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AppEventPublisher {
    private final ApplicationEventPublisher publisher;

    public UUID publishAuditEvent(String message, AuditLevel auditLevel, HttpServletRequest request, String source, Long userId) {
        var traceId = UUID.randomUUID();

        publishAuditEvent(message, auditLevel, request, source, userId, traceId);
        return traceId;
    }

    public void publishAuditEvent(String message, AuditLevel auditLevel, HttpServletRequest request, String source, Long userId, UUID traceId) {
        var address = getRemoteIp(request);

        log.debug("Publish audit event: {}", message);
        var appAuditEvent = AppAuditEvent.builder()
                                         .traceId(traceId.toString())
                                         .message(message)
                                         .level(auditLevel)
                                         .userId(userId)
                                         .address(address)
                                         .source(source)
                                         .createdAt(Instant.now())
                                         .build();
        publisher.publishEvent(appAuditEvent);

    }
}
