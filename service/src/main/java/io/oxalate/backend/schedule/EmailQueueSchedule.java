package io.oxalate.backend.schedule;

import io.oxalate.backend.service.EmailQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailQueueSchedule {

    private final EmailQueueService emailQueueService;

    @Transactional
    @Scheduled(fixedRate = 15 * 60_000)
    public void processQueuedEmails() {
        emailQueueService.flushQueue();
    }
}
