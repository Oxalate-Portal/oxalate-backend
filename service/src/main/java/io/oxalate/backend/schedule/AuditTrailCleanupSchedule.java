package io.oxalate.backend.schedule;

import io.oxalate.backend.service.ApplicationAuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuditTrailCleanupSchedule {

    private static final long DELAY = 24 * 60 * 60 * 1000L; // Once every 24 hours
    private final ApplicationAuditEventService applicationAuditEventService;

    @Scheduled(fixedDelay = DELAY, initialDelay = DELAY)
    public void cleanupAuditTrail() {
        log.info("Audit trail cleanup started");
        var count = applicationAuditEventService.cleanupAuditTrail();
        log.info("Audit trail cleanup finished, deleted {} entries", count);
    }
}
