package io.oxalate.backend.controller;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import jakarta.servlet.http.Cookie;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paged member administration list, {@code GET /api/users}.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerRTC extends PagedRestTestSupport {

    private static final String USERS_ENDPOINT = "/api/users";

    private String marker;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        marker = marker();
        var admin = createUser("Admin", "Administrator", RoleEnum.ROLE_ADMIN);
        adminJwt = jwtFor(admin, RoleEnum.ROLE_ADMIN);
        createUser("Alpha", marker, RoleEnum.ROLE_USER);
        createUser("Bravo", marker, RoleEnum.ROLE_USER);
        createUser("Charlie", marker, RoleEnum.ROLE_USER);
    }

    @Test
    void getUsersFirstPageShapeOk() throws Exception {
        mockMvc.perform(get(USERS_ENDPOINT).queryParam("search", marker)
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
               .andExpect(jsonPath("$.content[0].first_name", is("Alpha")))
               .andExpect(jsonPath("$.content[0].last_name", is(marker)));
    }

    @Test
    void getUsersLastPageOk() throws Exception {
        mockMvc.perform(get(USERS_ENDPOINT).queryParam("search", marker)
                                           .queryParam("size", "2")
                                           .queryParam("page", "1")
                                           .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.page", is(1)))
               .andExpect(jsonPath("$.first", is(false)))
               .andExpect(jsonPath("$.last", is(true)))
               .andExpect(jsonPath("$.content[0].first_name", is("Charlie")));
    }

    @Test
    void getUsersSortByFirstNameDescOk() throws Exception {
        mockMvc.perform(get(USERS_ENDPOINT).queryParam("search", marker)
                                           .queryParam("sort_by", "first_name")
                                           .queryParam("direction", "DESC")
                                           .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].first_name", is("Charlie")))
               .andExpect(jsonPath("$.content[2].first_name", is("Alpha")));
    }

    @Test
    void getUsersSearchIsCaseInsensitiveByDefaultOk() throws Exception {
        mockMvc.perform(get(USERS_ENDPOINT).queryParam("search", marker.toUpperCase())
                                           .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)));

        mockMvc.perform(get(USERS_ENDPOINT).queryParam("search", marker.toUpperCase())
                                           .queryParam("case_sensitive", "true")
                                           .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(0)))
               .andExpect(jsonPath("$.empty", is(true)));
    }

    @Test
    void getUsersForbiddenSortColumnFallsBackToDefaultOk() throws Exception {
        mockMvc.perform(get(USERS_ENDPOINT).queryParam("search", marker)
                                           .queryParam("sort_by", "password")
                                           .queryParam("direction", "DESC")
                                           .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)))
               // Default is id, so DESC lists the most recently created user first
               .andExpect(jsonPath("$.content[0].first_name", is("Charlie")));
    }
}
