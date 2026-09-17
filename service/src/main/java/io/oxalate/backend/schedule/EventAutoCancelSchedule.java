package io.oxalate.backend.schedule;

import static io.oxalate.backend.api.PortalConfigEnum.GENERAL;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.AUTO_CANCEL_EVENTS;
import io.oxalate.backend.service.EventService;
import io.oxalate.backend.service.PortalConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancels started, underbooked published events when automatic cancellation is enabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventAutoCancelSchedule {

    static final long RATE_MS = 10 * 60 * 1_000L;

    private final EventService eventService;
    private final PortalConfigurationService portalConfigurationService;

    @Scheduled(fixedRate = RATE_MS, initialDelay = 60_000L)
    public void cancelUnderbookedEvents() {
        if (!portalConfigurationService.getBooleanConfiguration(GENERAL.group, AUTO_CANCEL_EVENTS.key)) {
            log.debug("Automatic cancellation of underbooked events is disabled");
            return;
        }

        var cancelledCount = eventService.cancelUnderbookedStartedEvents();
        log.info("Automatically cancelled {} underbooked events", cancelledCount);
    }
}
