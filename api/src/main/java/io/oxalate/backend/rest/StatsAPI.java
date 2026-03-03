package io.oxalate.backend.rest;

import static io.oxalate.backend.api.SecurityConstants.JWT_COOKIE;
import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.api.response.stats.AggregateResponse;
import io.oxalate.backend.api.response.stats.EventPeriodReportResponse;
import io.oxalate.backend.api.response.stats.MultiYearValueResponse;
import io.oxalate.backend.api.response.stats.YearlyDiversListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "StatsAPI", description = "Statistics REST endpoints")
public interface StatsAPI {
    String BASE_PATH = API + "/stats";

    // Time series endpoints
    @Operation(description = "Produces a multivalue (registrations, cumulative) yearly table for number of registrations as well as cumulative number", tags = "StatsAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/yearly-registrations", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MultiYearValueResponse>> getYearlyRegistrationTimeSeries();

    @Operation(description = "Produces a multivalue (events, cumulative) yearly table for number of events as well as cumulative number", tags = "StatsAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/yearly-events", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MultiYearValueResponse>> getYearlyEventTimeSeries();

    @Operation(description = "Produces a multivalue (events, cumulative) yearly table for number of events as well as cumulative number", tags = "StatsAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/yearly-organizers", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MultiYearValueResponse>> getYearlyOrganizerTimeSeries();

    @Operation(description = "Produces a multivalue (period, one time) yearly table for number of payments by their types", tags = "StatsAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/yearly-payments", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MultiYearValueResponse>> getYearlyPaymentTimeSeries();

    // Aggregate endpoints
    @Operation(description = "Aggregated data of events and dives", tags = "StatsAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aggregates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/yearly-aggregates", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AggregateResponse> getAggregateStats();

    // Report endpoints
    @Operation(description = "Produces the 6 months report of events for every year since the data begins", tags = "StatsAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/event-report", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EventPeriodReportResponse>> getEventReports();

    // Top lists
    @Operation(description = "List of top 20 divers, per year", tags = "StatsAPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lists retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = JWT_COOKIE)
    @GetMapping(value = BASE_PATH + "/yearly-diver-list", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<YearlyDiversListResponse>> yearlyDiverList();
}
