package io.oxalate.backend.controller;

import io.oxalate.backend.api.response.ThirdPartyEventResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_EVENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.THIRD_PARTY_EVENTS_START;
import io.oxalate.backend.rest.ThirdPartyAPI;
import io.oxalate.backend.service.ThirdPartyEventService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@AuditSource("ThirdPartyController")
public class ThirdPartyController implements ThirdPartyAPI {
    private final ThirdPartyEventService eventService;

    @Override
    @Audited(startMessage = THIRD_PARTY_EVENTS_START, okMessage = THIRD_PARTY_EVENTS_OK)
    public ResponseEntity<List<ThirdPartyEventResponse>> getUpcomingEvents(String tokenValue) {
        return ResponseEntity.ok(eventService.getUpcomingEvents(tokenValue));
    }
}
