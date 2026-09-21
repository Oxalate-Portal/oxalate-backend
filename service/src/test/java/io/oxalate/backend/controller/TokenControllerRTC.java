package io.oxalate.backend.controller;

import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.model.ThirdPartyToken;
import io.oxalate.backend.repository.ThirdPartyTokenRepository;
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
 * Paged third-party token list, {@code GET /api/tokens}.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenControllerRTC extends PagedRestTestSupport {

    private static final String TOKENS_ENDPOINT = "/api/tokens/paged";

    @Autowired
    private ThirdPartyTokenRepository thirdPartyTokenRepository;

    private final List<ThirdPartyToken> createdTokens = new ArrayList<>();
    private String marker;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        marker = marker();
        var admin = createUser("Admin", "Administrator", RoleEnum.ROLE_ADMIN);
        adminJwt = jwtFor(admin, RoleEnum.ROLE_ADMIN);
        createToken(marker + " one", Instant.now()
                                            .minus(3, ChronoUnit.HOURS));
        createToken(marker + " two", Instant.now()
                                            .minus(2, ChronoUnit.HOURS));
        createToken(marker + " three", Instant.now()
                                              .minus(1, ChronoUnit.HOURS));
    }

    @Override
    protected void cleanUpFixtures() {
        for (var token : createdTokens) {
            thirdPartyTokenRepository.deleteById(token.getTokenId());
        }

        createdTokens.clear();
    }

    @Test
    void listTokensPageShapeAndDefaultOrderOk() throws Exception {
        mockMvc.perform(get(TOKENS_ENDPOINT).queryParam("search", marker)
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
               // Newest first by default
               .andExpect(jsonPath("$.content[0].description", is(marker + " three")));
    }

    @Test
    void listTokensLastPageOk() throws Exception {
        mockMvc.perform(get(TOKENS_ENDPOINT).queryParam("search", marker)
                                            .queryParam("size", "2")
                                            .queryParam("page", "1")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.last", is(true)))
               .andExpect(jsonPath("$.content[0].description", is(marker + " one")));
    }

    @Test
    void listTokensSortByDescriptionAscOk() throws Exception {
        mockMvc.perform(get(TOKENS_ENDPOINT).queryParam("search", marker)
                                            .queryParam("sort_by", "description")
                                            .queryParam("direction", "ASC")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].description", is(marker + " one")))
               .andExpect(jsonPath("$.content[1].description", is(marker + " three")))
               .andExpect(jsonPath("$.content[2].description", is(marker + " two")));
    }

    @Test
    void listTokensSearchMatchesTokenValueOk() throws Exception {
        mockMvc.perform(get(TOKENS_ENDPOINT).queryParam("search", createdTokens.getFirst()
                                                                               .getTokenValue())
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].description", is(marker + " one")));
    }

    @Test
    void listTokensForbiddenSortColumnFallsBackToDefaultOk() throws Exception {
        mockMvc.perform(get(TOKENS_ENDPOINT).queryParam("search", marker)
                                            .queryParam("sort_by", "token_value")
                                            .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.content[0].description", is(marker + " three")));
    }

    private void createToken(String description, Instant createdAt) {
        var token = thirdPartyTokenRepository.save(ThirdPartyToken.builder()
                                                                  .tokenValue("token-" + System.nanoTime())
                                                                  .createdAt(createdAt)
                                                                  .expiresAt(Instant.now()
                                                                                    .plus(30, ChronoUnit.DAYS))
                                                                  .description(description)
                                                                  .build());
        createdTokens.add(token);
    }
}
