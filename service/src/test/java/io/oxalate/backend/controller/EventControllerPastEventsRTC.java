package io.oxalate.backend.controller;

import io.oxalate.backend.api.DiveTypeEnum;
import io.oxalate.backend.api.EventStatusEnum;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.EventRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paged past events, {@code POST /api/events/past}.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventControllerPastEventsRTC extends PagedRestTestSupport {

    private static final String PAST_EVENTS_ENDPOINT = "/api/events/past";

    @Autowired
    private EventRepository eventRepository;

    private final List<Event> createdEvents = new ArrayList<>();
    private String marker;
    private String memberJwt;

    @BeforeEach
    void setUp() {
        marker = marker();
        var organizer = createUser("Olga", "Organizer", RoleEnum.ROLE_ORGANIZER);
        var member = createUser("Mia", "Member", RoleEnum.ROLE_USER);
        memberJwt = jwtFor(member, RoleEnum.ROLE_USER);

        createEvent(organizer, marker + " event A", Instant.now()
                                                          .minus(3, ChronoUnit.DAYS));
        createEvent(organizer, marker + " event B", Instant.now()
                                                          .minus(2, ChronoUnit.DAYS));
        createEvent(organizer, marker + " event C", Instant.now()
                                                          .minus(1, ChronoUnit.DAYS));
        // A future event with the same marker must never be part of the past events page
        createEvent(organizer, marker + " event D", Instant.now()
                                                          .plus(1, ChronoUnit.DAYS));
    }

    @Override
    protected void cleanUpFixtures() {
        for (var event : createdEvents) {
            eventRepository.deleteById(event.getId());
        }

        createdEvents.clear();
    }

    @Test
    void getPastEventsPageShapeAndDefaultOrderOk() throws Exception {
        mockMvc.perform(get(PAST_EVENTS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("size", "2")
                                                 .cookie(new Cookie(JWT_TOKEN, memberJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(2)))
               .andExpect(jsonPath("$.page", is(0)))
               .andExpect(jsonPath("$.size", is(2)))
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)))
               .andExpect(jsonPath("$.empty", is(false)))
               // Default order is start time descending, so the most recent past event comes first
               .andExpect(jsonPath("$.content[0].title", is(marker + " event C")))
               .andExpect(jsonPath("$.content[0].organizer.first_name", is("Olga")));
    }

    @Test
    void getPastEventsSecondPageOk() throws Exception {
        mockMvc.perform(get(PAST_EVENTS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("size", "2")
                                                 .queryParam("page", "1")
                                                 .cookie(new Cookie(JWT_TOKEN, memberJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.last", is(true)))
               .andExpect(jsonPath("$.content[0].title", is(marker + " event A")));
    }

    @Test
    void getPastEventsSortByTitleAscOk() throws Exception {
        mockMvc.perform(get(PAST_EVENTS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("sort_by", "title")
                                                 .queryParam("direction", "ASC")
                                                 .cookie(new Cookie(JWT_TOKEN, memberJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].title", is(marker + " event A")))
               .andExpect(jsonPath("$.content[2].title", is(marker + " event C")));
    }

    @Test
    void getPastEventsSearchMatchesDescriptionOk() throws Exception {
        mockMvc.perform(get(PAST_EVENTS_ENDPOINT).queryParam("search", "description of " + marker + " event b")
                                                 .queryParam("filter_column", "description")
                                                 .cookie(new Cookie(JWT_TOKEN, memberJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].title", is(marker + " event B")));
    }

    @Test
    void postPastEventsSearchByOrganizerFiltersResultsOk() throws Exception {
        var matchingOrganizer = createUser(marker + " Organizer", "Name", RoleEnum.ROLE_ORGANIZER);
        createEvent(matchingOrganizer, "Organizer-only event", Instant.now()
                                                                      .minus(1, ChronoUnit.DAYS));

        mockMvc.perform(post(PAST_EVENTS_ENDPOINT)
                       .contentType(APPLICATION_JSON)
                       .content("""
                               {
                                 "page": 0,
                                 "size": 10,
                                 "sort_by": "start_time",
                                 "direction": "DESC",
                                 "search": "%s",
                                 "case_sensitive": false,
                                 "filter_column": "organizer"
                               }
                               """.formatted(marker))
                       .cookie(new Cookie(JWT_TOKEN, memberJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].title", is("Organizer-only event")))
               .andExpect(jsonPath("$.content[0].organizer.first_name", is(marker + " Organizer")));
    }

    @Test
    void getPastEventsSearchByTitleDoesNotIgnoreFilterOk() throws Exception {
        mockMvc.perform(get(PAST_EVENTS_ENDPOINT).queryParam("search", marker + " event B")
                                                 .queryParam("filter_column", "title")
                                                 .cookie(new Cookie(JWT_TOKEN, memberJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].title", is(marker + " event B")));
    }

    @Test
    void getPastEventsForbiddenSortColumnFallsBackToDefaultOk() throws Exception {
        mockMvc.perform(get(PAST_EVENTS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("sort_by", "organizer_id")
                                                 .cookie(new Cookie(JWT_TOKEN, memberJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.content[0].title", is(marker + " event C")));
    }

    private void createEvent(User organizer, String title, Instant startTime) {
        var event = eventRepository.save(Event.builder()
                                              .title(title)
                                              .description("Description of " + title + ", long enough for validation")
                                              .type(DiveTypeEnum.OPEN_WATER)
                                              .startTime(startTime)
                                              .eventDuration(4)
                                              .maxDuration(60)
                                              .maxDepth(30)
                                              .maxParticipants(8)
                                              .organizerId(organizer.getId())
                                              .status(EventStatusEnum.HELD)
                                              .build());
        createdEvents.add(event);
    }
}
