package io.oxalate.backend.service;

import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThirdPartyEventServiceUTC {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ThirdPartyTokenService tokenService;
    @InjectMocks
    private ThirdPartyEventService service;

    @Test
    void projectsPublishedUpcomingEvents() {
        var date = Instant.now()
                          .plusSeconds(3600);
        when(eventRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(
                org.mockito.ArgumentMatchers.eq(EventStatusEnum.PUBLISHED), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(Event.builder()
                                         .startTime(date)
                                         .title("Future event")
                                         .build()));

        var response = service.getUpcomingEvents("valid");

        assertEquals(date, response.getFirst()
                                   .getEventDate());
        assertEquals("Future event", response.getFirst()
                                             .getEventName());
    }
}
