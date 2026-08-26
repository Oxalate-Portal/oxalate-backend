package io.oxalate.backend.service;

import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.api.response.ThirdPartyEventResponse;
import io.oxalate.backend.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThirdPartyEventService {
    private final EventRepository eventRepository;
    private final ThirdPartyTokenService tokenService;

    @Transactional(readOnly = true)
    public List<ThirdPartyEventResponse> getUpcomingEvents(String tokenValue) {
        tokenService.validate(tokenValue);
        return eventRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(EventStatusEnum.PUBLISHED, Instant.now())
                              .stream()
                              .map(event -> ThirdPartyEventResponse.builder()
                                                                   .eventDate(event.getStartTime())
                                                                   .eventName(event.getTitle())
                                                                   .build())
                              .toList();
    }
}
