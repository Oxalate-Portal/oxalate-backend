package io.oxalate.backend.rest;

import io.oxalate.backend.api.request.EventDiveListRequest;
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.api.response.EventDiveListResponse;
import io.oxalate.backend.api.response.EventListResponse;
import io.oxalate.backend.api.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "EventAPI", description = "Event REST endpoints")
public interface EventAPI {
    String BASE_PATH = "/api/events";

    @Operation(description = "Get a list of all future events. Note that the result depend on the role of the user. ORGANIZER and ADMIN will see all events,"
            + "the USER will only see published events", tags = "EventAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EventResponse>> getFutureEvents(HttpServletRequest request);

    @Operation(description = "Get a list of all ongoing events. The list is empty if no ongoing events exist.", tags = "EventAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/ongoing", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EventResponse>> getOngoingEvents(HttpServletRequest request);

    @Operation(description = "Get a list of all past events.", tags = "EventAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/past", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EventResponse>> getPastEvents(HttpServletRequest request);

    @Operation(description = "Get a list of all events for a specific user", tags = "EventAPI")
    @Parameter(name = "userId", description = "User ID for which all events should be fetched", example = "123")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EventListResponse>> getEventsForUser(@PathVariable(name = "userId") long userId, HttpServletRequest request);

    @Operation(description = "Create a new event", tags = "EventAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New event request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New event created successfully"),
            @ApiResponse(responseCode = "400", description = "Creation failed, the event may conflict with an existing one"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest eventRequest, HttpServletRequest request);

    @Operation(description = "Update event", tags = "EventAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated event request", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "404", description = "Event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EventResponse> updateEvent(@RequestBody EventRequest eventRequest, HttpServletRequest request);

    @Operation(description = "Fetch event based on the ID", tags = "EventAPI")
    @Parameter(name = "eventId", description = "ID of the event", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EventResponse> getEventById(@NotNull @PathVariable("eventId") long eventId, HttpServletRequest request);

    @Operation(description = "Get the dive counts of an event", tags = "EventAPI")
    @Parameter(name = "eventId", description = "ID of the event for which the dives are to be fetched", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event dives retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = BASE_PATH + "/{eventId}/dives", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EventDiveListResponse> getEventDives(@PathVariable("eventId") long eventId, HttpServletRequest request);

    @Operation(description = "Update the dive counts of an event", tags = "EventAPI")
    @Parameter(name = "eventId", description = "ID of the event for which the dives are to be updated", example = "123", required = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated dives of an event", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event dives updated successfully"),
            @ApiResponse(responseCode = "404", description = "Event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH + "/{eventId}/dives", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EventDiveListResponse> updateEventDives(@PathVariable("eventId") long eventId, @RequestBody EventDiveListRequest eventDiveListRequest,
            HttpServletRequest request);

    @Operation(description = "Cancel the event", tags = "EventAPI")
    @Parameter(name = "eventId", description = "ID of the event to be cancelled", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(path = BASE_PATH + "/{eventId}")
    ResponseEntity<HttpStatus> cancelEvent(@PathVariable("eventId") long eventId, HttpServletRequest request);

    @Operation(description = "Subscribe to an event", tags = "EventAPI")
    @Parameter(name = "eventId", description = "ID of the event to which the user subscribes to", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription successful"),
            @ApiResponse(responseCode = "404", description = "Event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(path = BASE_PATH + "/{eventId}/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EventResponse> subscribe(Authentication auth, @PathVariable(name = "eventId") long eventId, HttpServletRequest request);

    @Operation(description = "Unsubscribe from an event", tags = "EventAPI")
    @Parameter(name = "eventId", description = "ID of the event to which the user unsubscribes from", example = "123", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unsubscription successful"),
            @ApiResponse(responseCode = "404", description = "Event does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(path = BASE_PATH + "/{eventId}/unsubscribe")
    ResponseEntity<EventResponse> unSubscribe(Authentication auth, @PathVariable(name = "eventId") long eventId, HttpServletRequest request);
}
