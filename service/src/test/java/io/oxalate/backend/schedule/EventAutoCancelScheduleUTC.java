package io.oxalate.backend.schedule;

import static io.oxalate.backend.api.PortalConfigEnum.GENERAL;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.AUTO_CANCEL_EVENTS;
import io.oxalate.backend.service.EventService;
import io.oxalate.backend.service.PortalConfigurationService;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class EventAutoCancelScheduleUTC {

    @Mock
    private EventService eventService;
    @Mock
    private PortalConfigurationService portalConfigurationService;

    @InjectMocks
    private EventAutoCancelSchedule eventAutoCancelSchedule;

    @Test
    void cancelUnderbookedEventsDisabledByConfigurationOk() {
        when(portalConfigurationService.getBooleanConfiguration(GENERAL.group, AUTO_CANCEL_EVENTS.key)).thenReturn(false);

        eventAutoCancelSchedule.cancelUnderbookedEvents();

        verify(eventService, never()).cancelUnderbookedStartedEvents();
    }

    @Test
    void cancelUnderbookedEventsEnabledByConfigurationOk() {
        when(portalConfigurationService.getBooleanConfiguration(GENERAL.group, AUTO_CANCEL_EVENTS.key)).thenReturn(true);
        when(eventService.cancelUnderbookedStartedEvents()).thenReturn(2);

        eventAutoCancelSchedule.cancelUnderbookedEvents();

        verify(eventService).cancelUnderbookedStartedEvents();
    }

    @Test
    void cancelUnderbookedEventsIsScheduledMoreOftenThanEventClosingOk() throws NoSuchMethodException {
        var scheduled = EventAutoCancelSchedule.class.getMethod("cancelUnderbookedEvents")
                                                     .getAnnotation(Scheduled.class);
        var closing = ClosingEventSchedule.class.getMethod("closePastEvents")
                                                .getAnnotation(Scheduled.class);

        assertTrue(scheduled.fixedRate() > 0, "The auto-cancel task must run at a fixed rate");
        assertTrue(scheduled.fixedRate() < closing.fixedRate(),
                "Underbooked events must be cancelled before they can be closed to HELD");
    }
}
