package io.oxalate.backend.service;

import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.PageStatusEnum;
import io.oxalate.backend.api.RoleEnum;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PageServiceITC extends AbstractIntegrationTest {

    private static final long BLOG_PAGE_GROUP_ID = 3L;

    @Autowired
    private PageService pageService;
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

    private User testUser;
    private Set<RoleEnum> anonymousRoles;
    private Set<RoleEnum> userRoles;
    private Set<RoleEnum> adminRoles;

    @BeforeEach
    void setUp() {
        // Clean up any test data from previous runs
        pageRoleAccessRepository.deleteAll();
        pageVersionRepository.deleteAll();
        pageRepository.deleteAll();

        // Create test user
        testUser = generateUser(UserStatusEnum.ACTIVE, RoleEnum.ROLE_USER);

        // Set up role sets
        anonymousRoles = new HashSet<>();
        anonymousRoles.add(RoleEnum.ROLE_ANONYMOUS);

        userRoles = new HashSet<>();
        userRoles.add(RoleEnum.ROLE_ANONYMOUS);
        userRoles.add(RoleEnum.ROLE_USER);

        adminRoles = new HashSet<>();
        adminRoles.add(RoleEnum.ROLE_ANONYMOUS);
        adminRoles.add(RoleEnum.ROLE_USER);
        adminRoles.add(RoleEnum.ROLE_ADMIN);
    }

    @Test
    void getBlogArticlesValidRequestOk() {
        // Given
        createBlogPage("Test Blog Article", "Test Ingress", "Test body content", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertFalse(result.isEmpty());
        assertEquals("Test Blog Article", result.getContent()
                                                .get(0)
                                                .getPageVersions()
                                                .get(0)
                                                .getTitle());
    }

    @Test
    void getBlogArticlesEmptyResultOk() {
        // Given - no blog pages created
        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isEmpty());
    }

    @Test
    void getBlogArticlesSearchCaseInsensitiveOk() {
        // Given
        createBlogPage("Special Article", "Contains keyword", "Body text", "en", true, false);
        createBlogPage("Another Article", "Different content", "No match here", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("special")
                                  .caseSensitive(false)
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Special Article", result.getContent()
                                              .get(0)
                                              .getPageVersions()
                                              .get(0)
                                              .getTitle());
    }

    @Test
    void getBlogArticlesSearchCaseSensitiveOk() {
        // Given
        createBlogPage("Special Article", "Contains keyword", "Body text", "en", true, false);
        createBlogPage("special article", "Different content", "No match here", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("Special")
                                  .caseSensitive(true)
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Special Article", result.getContent()
                                              .get(0)
                                              .getPageVersions()
                                              .get(0)
                                              .getTitle());
    }

    @Test
    void getBlogArticlesSearchInIngressOk() {
        // Given
        createBlogPage("Article Title", "This ingress contains unique keyword", "Body text", "en", true, false);
        createBlogPage("Another Article", "Different content", "No match here", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("unique")
                                  .caseSensitive(false)
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getBlogArticlesSearchInBodyOk() {
        // Given
        createBlogPage("Article Title", "Normal ingress", "Body with special searchterm", "en", true, false);
        createBlogPage("Another Article", "Different content", "No match here", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .search("searchterm")
                                  .caseSensitive(false)
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getBlogArticlesSortByCreatedAtDescOk() {
        // Given
        var page1 = createBlogPage("First Article", "Ingress", "Body", "en", true, false);
        // Add a small delay to ensure different timestamps
        var page2 = createBlogPage("Second Article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .sortBy("createdAt")
                                  .direction(SortDirectionEnum.DESC)
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        // The second article should come first (DESC order)
        assertEquals("Second Article", result.getContent()
                                             .get(0)
                                             .getPageVersions()
                                             .get(0)
                                             .getTitle());
    }

    @Test
    void getBlogArticlesSortByCreatedAtAscOk() {
        // Given
        createBlogPage("First Article", "Ingress", "Body", "en", true, false);
        createBlogPage("Second Article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .sortBy("createdAt")
                                  .direction(SortDirectionEnum.ASC)
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        // The first article should come first (ASC order)
        assertEquals("First Article", result.getContent()
                                            .get(0)
                                            .getPageVersions()
                                            .get(0)
                                            .getTitle());
    }

    @Test
    void getBlogArticlesSortByTitleAscOk() {
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

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Alpha Article", result.getContent()
                                            .get(0)
                                            .getPageVersions()
                                            .get(0)
                                            .getTitle());
    }

    @Test
    void getBlogArticlesSortByTitleDescOk() {
        // Given
        createBlogPage("Alpha Article", "Ingress", "Body", "en", true, false);
        createBlogPage("Zebra Article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .sortBy("title")
                                  .direction(SortDirectionEnum.DESC)
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Zebra Article", result.getContent()
                                            .get(0)
                                            .getPageVersions()
                                            .get(0)
                                            .getTitle());
    }

    @Test
    void getBlogArticlesPaginationOk() {
        // Given - create 15 blog articles
        for (int i = 1; i <= 15; i++) {
            createBlogPage("Article " + String.format("%02d", i), "Ingress", "Body", "en", true, false);
        }

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(5)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(5, result.getContent()
                              .size());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());
    }

    @Test
    void getBlogArticlesPaginationSecondPageOk() {
        // Given - create 15 blog articles
        for (int i = 1; i <= 15; i++) {
            createBlogPage("Article " + String.format("%02d", i), "Ingress", "Body", "en", true, false);
        }

        var request = PagedRequest.builder()
                                  .page(1)
                                  .size(5)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(5, result.getContent()
                              .size());
        assertFalse(result.isFirst());
        assertFalse(result.isLast());
    }

    @Test
    void getBlogArticlesPaginationLastPageOk() {
        // Given - create 15 blog articles
        for (int i = 1; i <= 15; i++) {
            createBlogPage("Article " + String.format("%02d", i), "Ingress", "Body", "en", true, false);
        }

        var request = PagedRequest.builder()
                                  .page(2)
                                  .size(5)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(5, result.getContent()
                              .size());
        assertFalse(result.isFirst());
        assertTrue(result.isLast());
    }

    @Test
    void getBlogArticlesLanguageFilterOk() {
        // Given
        createBlogPage("English Article", "Ingress", "Body", "en", true, false);
        createBlogPage("Finnish Article", "Ingress", "Body", "fi", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("English Article", result.getContent()
                                              .get(0)
                                              .getPageVersions()
                                              .get(0)
                                              .getTitle());
    }

    @Test
    void getBlogArticlesAnonymousAccessOk() {
        // Given - create page accessible to anonymous users
        createBlogPage("Public Article", "Ingress", "Body", "en", true, false);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, anonymousRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getBlogArticlesAuthenticatedOnlyOk() {
        // Given - create page accessible only to authenticated users
        createBlogPage("Members Only Article", "Ingress", "Body", "en", false, true);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When - anonymous users
        var anonymousResult = pageService.getBlogArticles(request, anonymousRoles);

        // Then - should not see the article
        assertNotNull(anonymousResult);
        assertEquals(0, anonymousResult.getTotalElements());

        // When - authenticated users
        var userResult = pageService.getBlogArticles(request, userRoles);

        // Then - should see the article
        assertNotNull(userResult);
        assertEquals(1, userResult.getTotalElements());
    }

    @Test
    void getBlogArticlesUnpublishedNotVisibleOk() {
        // Given - create unpublished page
        createBlogPageWithStatus("Draft Article", "Ingress", "Body", "en", true, false, PageStatusEnum.DRAFTED);

        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .language("en")
                                  .build();

        // When
        var result = pageService.getBlogArticles(request, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    private Page createBlogPage(String title, String ingress, String body, String language, boolean anonymousAccess, boolean authenticatedAccess) {
        return createBlogPageWithStatus(title, ingress, body, language, anonymousAccess, authenticatedAccess, PageStatusEnum.PUBLISHED);
    }

    private Page createBlogPageWithStatus(String title, String ingress, String body, String language, boolean anonymousAccess, boolean authenticatedAccess,
            PageStatusEnum status) {
        var page = Page.builder()
                       .pageGroupId(BLOG_PAGE_GROUP_ID)
                       .status(status)
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
            var anonymousAccess_pra = PageRoleAccess.builder()
                                                    .pageId(savedPage.getId())
                                                    .role(RoleEnum.ROLE_ANONYMOUS)
                                                    .readPermission(true)
                                                    .writePermission(false)
                                                    .build();
            pageRoleAccessRepository.save(anonymousAccess_pra);
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

    private User generateUser(UserStatusEnum userStatusEnum, RoleEnum roleEnum) {
        var randomUsername = "test-" + Instant.now()
                                              .toEpochMilli() + "@test.tld";
        var user = User.builder()
                       .username(randomUsername)
                       .password("password")
                       .firstName("Max")
                       .lastName("Mustermann")
                       .status(userStatusEnum)
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
        var optionalRole = roleRepository.findByName(roleEnum);
        assertFalse(optionalRole.isEmpty());
        var role = optionalRole.get();
        roleRepository.addUserRole(newUser.getId(), role.getId());
        return newUser;
    }
}
