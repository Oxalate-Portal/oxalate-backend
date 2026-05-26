package io.oxalate.backend.scheduler;

import io.oxalate.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingListScheduler {
    private final EventService eventService;

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void processWaitingList() {
        eventService.processWaitingListTimeouts();
        log.debug("Waiting list queue processed");
    }
}

