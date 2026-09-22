package io.oxalate.backend.controller;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.model.ApplicationAuditEvent;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.ApplicationAuditEventRepository;
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
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paged audit trail, {@code GET /api/audits} and {@code GET /api/audits/{userId}}. The application itself writes audit
 * entries while the tests run, so every assertion is scoped by a unique marker or by the user id.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditControllerRTC extends PagedRestTestSupport {

    private static final String AUDITS_ENDPOINT = "/api/audits";
    private static final String USER_AUDITS_ENDPOINT = "/api/audits/";

    @Autowired
    private ApplicationAuditEventRepository applicationAuditEventRepository;

    private final List<ApplicationAuditEvent> createdEvents = new ArrayList<>();
    private String marker;
    private String adminJwt;
    private User target;

    @BeforeEach
    void setUp() {
        marker = marker();
        var admin = createUser("Admin", "Administrator", RoleEnum.ROLE_ADMIN);
        adminJwt = jwtFor(admin, RoleEnum.ROLE_ADMIN);
        target = createUser("Tara", marker, RoleEnum.ROLE_USER);

        createAuditEvent(target, AuditLevelEnum.INFO, marker + " one", Instant.now()
                                                                             .minus(3, ChronoUnit.MINUTES));
        createAuditEvent(target, AuditLevelEnum.WARN, marker + " two", Instant.now()
                                                                             .minus(2, ChronoUnit.MINUTES));
        createAuditEvent(target, AuditLevelEnum.ERROR, marker + " three", Instant.now()
                                                                                .minus(1, ChronoUnit.MINUTES));
    }

    @Override
    protected void cleanUpFixtures() {
        for (var event : createdEvents) {
            applicationAuditEventRepository.deleteById(event.getId());
        }

        createdEvents.clear();
    }

    @Test
    void getAuditEventsFilteredByMessagePageShapeOk() throws Exception {
        mockMvc.perform(get(AUDITS_ENDPOINT).queryParam("filter_column", "message")
                                            .queryParam("search", marker)
                                            .queryParam("size", "2")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(2)))
               .andExpect(jsonPath("$.page", is(0)))
               .andExpect(jsonPath("$.size", is(2)))
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)))
               .andExpect(jsonPath("$.empty", is(false)))
               // Newest first by default, with the user name resolved
               .andExpect(jsonPath("$.content[0].message", is(marker + " three")))
               .andExpect(jsonPath("$.content[0].user_name", is(marker + " Tara")));
    }

    @Test
    void getAuditEventsLastPageOk() throws Exception {
        mockMvc.perform(get(AUDITS_ENDPOINT).queryParam("filter_column", "message")
                                            .queryParam("search", marker)
                                            .queryParam("size", "2")
                                            .queryParam("page", "1")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.last", is(true)))
               .andExpect(jsonPath("$.content[0].message", is(marker + " one")));
    }

    @Test
    void getAuditEventsSortByLevelAscOk() throws Exception {
        mockMvc.perform(get(AUDITS_ENDPOINT).queryParam("filter_column", "message")
                                            .queryParam("search", marker)
                                            .queryParam("sort_by", "level")
                                            .queryParam("direction", "ASC")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].level", is("ERROR")))
               .andExpect(jsonPath("$.content[1].level", is("INFO")))
               .andExpect(jsonPath("$.content[2].level", is("WARN")));
    }

    @Test
    void getAuditEventsFilteredByUserNameOk() throws Exception {
        mockMvc.perform(get(AUDITS_ENDPOINT).queryParam("filter_column", "user_name")
                                            .queryParam("search", marker)
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.content[0].user_id", is(target.getId()
                                                                     .intValue())));
    }

    @Test
    void getAuditEventsUnknownUserNameMatchesNothingOk() throws Exception {
        mockMvc.perform(get(AUDITS_ENDPOINT).queryParam("filter_column", "user_name")
                                            .queryParam("search", "nobody-" + marker)
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(0)))
               .andExpect(jsonPath("$.empty", is(true)));
    }

    @Test
    void getAuditEventsForbiddenSortAndFilterColumnsFallBackOk() throws Exception {
        mockMvc.perform(get(AUDITS_ENDPOINT).queryParam("filter_column", "message")
                                            .queryParam("search", marker)
                                            .queryParam("sort_by", "id; DROP TABLE users")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.content[0].message", is(marker + " three")));

        // An unknown filter column is ignored rather than failing, so the page is simply unfiltered
        mockMvc.perform(get(AUDITS_ENDPOINT).queryParam("filter_column", "password")
                                            .queryParam("search", marker)
                                            .queryParam("sort_by", "address")
                                            .queryParam("size", "1")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getAuditEventsByUserIdOk() throws Exception {
        mockMvc.perform(get(USER_AUDITS_ENDPOINT + target.getId()).queryParam("size", "2")
                                                                    .queryParam("sort_by", "created_at")
                                                                    .queryParam("direction", "ASC")
                                                                    .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(2)))
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)))
               .andExpect(jsonPath("$.content[0].message", is(marker + " one")))
               .andExpect(jsonPath("$.content[0].user_name", is(marker + " Tara")));
    }

    @Test
    void getAuditEventsByUserIdForbiddenSortColumnFallsBackOk() throws Exception {
        mockMvc.perform(get(USER_AUDITS_ENDPOINT + target.getId()).queryParam("sort_by", "user_name; DROP TABLE users")
                                                                    .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.content[0].message", is(marker + " three")));
    }

    private void createAuditEvent(User user, AuditLevelEnum level, String message, Instant createdAt) {
        var event = applicationAuditEventRepository.save(ApplicationAuditEvent.builder()
                                                                              .userId(user.getId())
                                                                              .level(level)
                                                                              .traceId("trace-" + System.nanoTime())
                                                                              .ipAddress("127.0.0.1")
                                                                              .source("AuditControllerRTC")
                                                                              .message(message)
                                                                              .createdAt(createdAt)
                                                                              .build());
        createdEvents.add(event);
    }
}
