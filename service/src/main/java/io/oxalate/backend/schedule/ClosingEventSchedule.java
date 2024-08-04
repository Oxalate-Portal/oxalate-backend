package io.oxalate.backend.schedule;

import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.repository.EventRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClosingEventSchedule {

    private final EventRepository eventRepository;

    @Scheduled(fixedRate = 30 * 60 * 1_000)
    @Transactional
    public void closePastEvents() {
        List<Event> eventsToMarkAsHeld = eventRepository.findEventsToMarkAsHeld();
        var counter = 0;

        for (Event event : eventsToMarkAsHeld) {
            eventRepository.updateEventStatus(event.getId(), EventStatusEnum.HELD);
            counter++;
        }

        log.info("Closed {} events", counter);
    }
}
