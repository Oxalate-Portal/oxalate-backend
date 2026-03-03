package io.oxalate.backend.events;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.audit.AuditContext;
import static io.oxalate.backend.tools.HttpTools.getRemoteIp;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RequiredArgsConstructor
@Component
public class AppEventPublisher {
    private final ApplicationEventPublisher publisher;

    public UUID publishAuditEvent(String message, AuditLevelEnum auditLevelEnum, HttpServletRequest request, String source, Long userId) {
        var traceId = UUID.randomUUID();

        publishAuditEvent(message, auditLevelEnum, request, source, userId, traceId);
        return traceId;
    }

    public void publishAuditEvent(String message, AuditLevelEnum auditLevelEnum, HttpServletRequest request, String source, Long userId, UUID traceId) {
        var address = (request != null) ? getRemoteIp(request) : null;

        log.debug("Publish audit event: {}", message);
        var appAuditEvent = AppAuditEvent.builder()
                                         .traceId(traceId.toString())
                                         .message(message)
                                         .level(auditLevelEnum)
                                         .userId(userId)
                                         .address(address)
                                         .source(source)
                                         .createdAt(Instant.now())
                                         .build();
        publisher.publishEvent(appAuditEvent);
    }

    /**
     * Convenience overload that reads the {@link HttpServletRequest} from
     * {@link RequestContextHolder} and the trace ID from {@link AuditContext}.
     * Useful for service-layer code that needs to emit correlated audit events
     * without receiving the request and trace ID as method parameters.
     */
    public void publishAuditEvent(String message, AuditLevelEnum auditLevelEnum, String source, Long userId) {
        HttpServletRequest request = null;
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            request = servletAttrs.getRequest();
        }

        var traceId = AuditContext.getTraceId();
        if (traceId == null) {
            traceId = UUID.randomUUID();
        }

        publishAuditEvent(message, auditLevelEnum, request, source, userId, traceId);
    }
}
