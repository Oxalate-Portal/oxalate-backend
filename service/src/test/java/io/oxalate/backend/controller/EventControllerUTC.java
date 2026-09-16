package io.oxalate.backend.controller;

import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.api.response.EventResponse;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.service.EventService;
import io.oxalate.backend.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventControllerUTC {

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventController eventController;

    @Test
    void createEventValidRequestReturnsCreated() {
        var request = validRequest();
        var response = EventResponse.builder()
                                    .id(42L)
                                    .build();
        when(eventService.createEvent(request, 7L)).thenReturn(response);

        var result = eventController.createEvent(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void createEventRejectsInvalidOrganizer() {
        var request = validRequest();
        request.setOrganizerId(0L);

        assertThrows(OxalateValidationException.class, () -> eventController.createEvent(request));
        verify(eventService, never()).createEvent(any(), any(Long.class));
    }

    @Test
    void createEventRejectsPastStartTime() {
        var request = validRequest();
        request.setStartTime(Instant.now()
                                    .minus(1, ChronoUnit.MINUTES));

        assertThrows(OxalateValidationException.class, () -> eventController.createEvent(request));
        verify(eventService, never()).createEvent(any(), any(Long.class));
    }

    @Test
    void createEventRejectsTooManyParticipants() {
        var request = validRequest();
        request.setMaxParticipants(1);
        request.setParticipants(Set.of(11L, 12L));

        assertThrows(OxalateValidationException.class, () -> eventController.createEvent(request));
        verify(eventService, never()).createEvent(any(), any(Long.class));
    }

    @Test
    void createEventRejectsServiceFailure() {
        var request = validRequest();
        when(eventService.createEvent(request, 7L)).thenReturn(null);

        assertThrows(OxalateValidationException.class, () -> eventController.createEvent(request));
    }

    @Test
    void getEventByIdReturnsResponse() {
        var response = EventResponse.builder()
                                    .id(12L)
                                    .build();
        when(eventService.findById(12L)).thenReturn(response);

        assertEquals(response, eventController.getEventById(12L)
                                              .getBody());
    }

    @Test
    void getEventByIdMissingThrowsNotFound() {
        when(eventService.findById(12L)).thenReturn(null);

        assertThrows(io.oxalate.backend.exception.OxalateNotFoundException.class,
                () -> eventController.getEventById(12L));
    }

    @Test
    void updateEventMissingEventThrowsNotFound() {
        var request = validRequest();
        request.setId(12L);
        when(eventService.findById(12L)).thenReturn(null);

        assertThrows(io.oxalate.backend.exception.OxalateNotFoundException.class,
                () -> eventController.updateEvent(request));
        verify(eventService, never()).updateEvent(any());
    }

    @Test
    void updateEventMissingOrganizerThrowsValidation() {
        var request = validRequest();
        request.setId(12L);
        when(eventService.findById(12L)).thenReturn(EventResponse.builder()
                                                                 .id(12L)
                                                                 .build());
        when(userService.findUserEntityById(7L)).thenReturn(null);

        assertThrows(OxalateValidationException.class,
                () -> eventController.updateEvent(request));
        verify(eventService, never()).updateEvent(any());
    }

    private EventRequest validRequest() {
        return EventRequest.builder()
                           .organizerId(7L)
                           .startTime(Instant.now()
                                             .plus(1, ChronoUnit.DAYS))
                           .eventDuration(2)
                           .maxParticipants(10)
                           .participants(Set.of())
                           .status(EventStatusEnum.PUBLISHED)
                           .build();
    }
}
