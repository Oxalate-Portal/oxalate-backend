package io.oxalate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.PageStatusEnum;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.SortDirectionEnum;
import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.model.Page;
import io.oxalate.backend.model.PageRoleAccess;
import io.oxalate.backend.model.PageVersion;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.PageRepository;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.PageVersionRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PageControllerRTC extends AbstractIntegrationTest {
    private static final long BLOG_PAGE_GROUP_ID = 3L;
    private static final String BLOG_ENDPOINT = "/api/pages/blogs";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private PageVersionRepository pageVersionRepository;
    @Autowired
    private PageRoleAccessRepository pageRoleAccessRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JwtUtils jwtUtils;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with Spring Security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Clean up test data from previous runs
        pageRoleAccessRepository.deleteAll();
        pageVersionRepository.deleteAll();
        pageRepository.deleteAll();

        // Create test user
        testUser = createTestUser();

        // Generate JWT token for the test user
        jwtToken = generateJwtTokenForUser(testUser);
    }

    @Test
    void getBlogArticlesAnonymousAccessOk() throws Exception {
        // Given - create public blog page
        createBlogPage("Public Article", "Public ingress", "Public body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When & Then - no authentication
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.content[0].pageVersions[0].title", is("Public Article")));
    }

    @Test
    void getBlogArticlesAuthenticatedAccessOk() throws Exception {
        // Given - create page accessible only to authenticated users
        createBlogPage("Members Only", "Private ingress", "Private body", "en", false, true);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When & Then - with authentication
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request))
                       .cookie(new Cookie(JWT_TOKEN, jwtToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.content[0].pageVersions[0].title", is("Members Only")));
    }

    @Test
    void getBlogArticlesAnonymousCannotSeeAuthenticatedOnlyOk() throws Exception {
        // Given - create page accessible only to authenticated users
        createBlogPage("Members Only", "Private ingress", "Private body", "en", false, true);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When & Then - without authentication
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(0)))
               .andExpect(jsonPath("$.content", hasSize(0)))
               .andExpect(jsonPath("$.empty", is(true)));
    }

    @Test
    void getBlogArticlesMixedAccessOk() throws Exception {
        // Given - create both public and private pages
        createBlogPage("Public Article", "Public ingress", "Public body", "en", true, false);
        createBlogPage("Members Only", "Private ingress", "Private body", "en", false, true);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When & Then - anonymous user sees only public
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)));

        // When & Then - authenticated user sees both
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request))
                       .cookie(new Cookie(JWT_TOKEN, jwtToken)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(2)));
    }

    @Test
    void getBlogArticlesPaginationOk() throws Exception {
        // Given - create 15 blog pages
        for (int i = 1; i <= 15; i++) {
            createBlogPage("Article " + String.format("%02d", i), "Ingress", "Body", "en", true, false);
        }

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(5)
                                  .language("en")
                                  .build();

        // When & Then - first page
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(15)))
               .andExpect(jsonPath("$.total_pages", is(3)))
               .andExpect(jsonPath("$.content", hasSize(5)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)));

        // Second page
        request.setPage(1);
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.page", is(1)))
               .andExpect(jsonPath("$.first", is(false)))
               .andExpect(jsonPath("$.last", is(false)));

        // Last page
        request.setPage(2);
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.page", is(2)))
               .andExpect(jsonPath("$.first", is(false)))
               .andExpect(jsonPath("$.last", is(true)));
    }

    @Test
    void getBlogArticlesSortByCreatedAtDescOk() throws Exception {
        // Given
        createBlogPage("First Article", "Ingress", "Body", "en", true, false);
        createBlogPage("Second Article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .sortBy("createdAt")
                                  .direction(SortDirectionEnum.DESC)
                                  .build();

        // When & Then - newest first
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].pageVersions[0].title", is("Second Article")));
    }

    @Test
    void getBlogArticlesSortByTitleAscOk() throws Exception {
        // Given
        createBlogPage("Zebra Article", "Ingress", "Body", "en", true, false);
        createBlogPage("Alpha Article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .sortBy("title")
                                  .direction(SortDirectionEnum.ASC)
                                  .build();

        // When & Then
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].pageVersions[0].title", is("Alpha Article")));
    }

    @Test
    void getBlogArticlesSearchCaseInsensitiveOk() throws Exception {
        // Given
        createBlogPage("Special Article", "Ingress", "Body", "en", true, false);
        createBlogPage("Regular Article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("special")
                                  .caseSensitive(false)
                                  .build();

        // When & Then
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].pageVersions[0].title", is("Special Article")));
    }

    @Test
    void getBlogArticlesSearchCaseSensitiveOk() throws Exception {
        // Given
        createBlogPage("Special Article", "Ingress", "Body", "en", true, false);
        createBlogPage("special article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("Special")
                                  .caseSensitive(true)
                                  .build();

        // When & Then
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].pageVersions[0].title", is("Special Article")));
    }

    @Test
    void getBlogArticlesLanguageFilterOk() throws Exception {
        // Given
        createBlogPage("English Article", "Ingress", "Body", "en", true, false);
        createBlogPage("Finnish Article", "Ingress", "Body", "fi", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("fi")
                                  .build();

        // When & Then
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].pageVersions[0].title", is("Finnish Article")));
    }

    @Test
    void getBlogArticlesEmptyResultOk() throws Exception {
        // Given - no blog pages

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When & Then
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(0)))
               .andExpect(jsonPath("$.empty", is(true)));
    }

    @Test
    void getBlogArticlesSearchInIngressOk() throws Exception {
        // Given
        createBlogPage("Article Title", "Unique searchterm in ingress", "Body", "en", true, false);
        createBlogPage("Another Article", "Different content", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("searchterm")
                                  .caseSensitive(false)
                                  .build();

        // When & Then
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)));
    }

    @Test
    void getBlogArticlesSearchInBodyOk() throws Exception {
        // Given
        createBlogPage("Article Title", "Ingress", "Body with unique searchterm", "en", true, false);
        createBlogPage("Another Article", "Ingress", "Different content", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("searchterm")
                                  .caseSensitive(false)
                                  .build();

        // When & Then
        mockMvc.perform(post(BLOG_ENDPOINT)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)));
    }

    private Page createBlogPage(String title, String ingress, String body, String language, boolean anonymousAccess, boolean authenticatedAccess) {
        var page = Page.builder()
                       .pageGroupId(BLOG_PAGE_GROUP_ID)
                       .status(PageStatusEnum.PUBLISHED)
                       .creator(testUser.getId())
                       .createdAt(Instant.now())
                       .build();

        var savedPage = pageRepository.save(page);

        var pageVersion = PageVersion.builder()
                                     .pageId(savedPage.getId())
                                     .language(language)
                                     .title(title)
                                     .ingress(ingress)
                                     .body(body)
                                     .build();

        pageVersionRepository.save(pageVersion);

        // Add role access
        if (anonymousAccess) {
            var anonymousAccessPra = PageRoleAccess.builder()
                                                   .pageId(savedPage.getId())
                                                   .role(RoleEnum.ROLE_ANONYMOUS)
                                                   .readPermission(true)
                                                   .writePermission(false)
                                                   .build();
            pageRoleAccessRepository.save(anonymousAccessPra);
        }

        if (authenticatedAccess) {
            var userAccess = PageRoleAccess.builder()
                                           .pageId(savedPage.getId())
                                           .role(RoleEnum.ROLE_USER)
                                           .readPermission(true)
                                           .writePermission(false)
                                           .build();
            pageRoleAccessRepository.save(userAccess);
        }

        return savedPage;
    }

    private User createTestUser() {
        var randomUsername = "test-" + Instant.now()
                                              .toEpochMilli() + "@test.tld";
        var user = User.builder()
                       .username(randomUsername)
                       .password("password")
                       .firstName("Max")
                       .lastName("Mustermann")
                       .status(UserStatusEnum.ACTIVE)
                       .phoneNumber("123456789")
                       .privacy(false)
                       .nextOfKin("Maxine Mustermann")
                       .registered(Instant.now()
                                          .minus(1000L, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .language("en")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();

        var newUser = userRepository.save(user);
        var optionalRole = roleRepository.findByName(RoleEnum.ROLE_USER);
        if (optionalRole.isPresent()) {
            var role = optionalRole.get();
            roleRepository.addUserRole(newUser.getId(), role.getId());
        }
        return newUser;
    }

    private String generateJwtTokenForUser(User user) {
        var authorities = List.of(
                new SimpleGrantedAuthority(RoleEnum.ROLE_ANONYMOUS.name()),
                new SimpleGrantedAuthority(RoleEnum.ROLE_USER.name())
        );

        var userDetails = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                user.isApprovedTerms(),
                user.getHealthCheckId(),
                false,
                user.getLanguage()
        );

        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        return jwtUtils.generateJwtToken(authentication);
    }
}
