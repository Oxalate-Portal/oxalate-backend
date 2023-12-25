package io.oxalate.backend.events;

import io.oxalate.backend.service.ApplicationAuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AppEventAsyncListener {
    private final ApplicationAuditEventService applicationAuditEventService;

    @Async("AuditEventAsyncExecutor")
    @EventListener
    public void handleAsyncEvent(AppAuditEvent event) {
        log.debug("Received application audit event: {}", event);
        applicationAuditEventService.save(event);
    }
}
