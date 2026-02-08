package io.oxalate.backend.service;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.SortDirectionEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.PageResponse;
import io.oxalate.backend.api.response.PagedResponse;
import io.oxalate.backend.model.Page;
import io.oxalate.backend.model.PageVersion;
import io.oxalate.backend.repository.PageGroupRepository;
import io.oxalate.backend.repository.PageGroupVersionRepository;
import io.oxalate.backend.repository.PageRepository;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.PageVersionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PageServiceUTC {

    @Mock
    private PageRepository pageRepository;
    @Mock
    private PageGroupRepository pageGroupRepository;
    @Mock
    private PageGroupVersionRepository pageGroupVersionRepository;
    @Mock
    private PageRoleAccessRepository pageRoleAccessRepository;
    @Mock
    private PageVersionRepository pageVersionRepository;
    @Mock
    private EmailQueueService emailQueueService;
    @Mock
    private PortalConfigurationService portalConfigurationService;

    @InjectMocks
    private PageService pageService;

    private Set<RoleEnum> userRoles;
    private PagedRequest pagedRequest;

    @BeforeEach
    void setUp() {
        userRoles = new HashSet<>();
        userRoles.add(RoleEnum.ROLE_ANONYMOUS);
        userRoles.add(RoleEnum.ROLE_USER);

        pagedRequest = PagedRequest.builder()
                                   .page(0)
                                   .size(10)
                                   .language("en")
                                   .build();
    }

    @Test
    void getBlogArticlesValidRequestOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        var mockPage = createMockPage(1L, "Test Title", "Test Ingress", "Test Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(1L, 1L, "en", "Test Title", "Test Ingress", "Test Body");

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), any()))
                .thenReturn(1L);
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByCreatedAtDesc(eq(3L), eq("en"), anyList(), any(), eq(10), eq(0)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(1L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(1L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getContent()
                              .size());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertFalse(result.isEmpty());
    }

    @Test
    void getBlogArticlesEmptyResultOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), any()))
                .thenReturn(0L);
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByCreatedAtDesc(eq(3L), eq("en"), anyList(), any(), eq(10), eq(0)))
                .thenReturn(new ArrayList<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isEmpty());
    }

    @Test
    void getBlogArticlesUnsupportedLanguageFail() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setLanguage("xx");

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isEmpty());
    }

    @Test
    void getBlogArticlesSearchCaseInsensitiveOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setSearch("test");
        pagedRequest.setCaseSensitive(false);

        var mockPage = createMockPage(1L, "Test Title", "Test Ingress", "Test Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(1L, 1L, "en", "Test Title", "Test Ingress", "Test Body");

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), eq("test")))
                .thenReturn(1L);
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByCreatedAtDesc(eq(3L), eq("en"), anyList(), eq("test"), eq(10), eq(0)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(1L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(1L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.isEmpty());
    }

    @Test
    void getBlogArticlesSearchCaseSensitiveOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setSearch("Test");
        pagedRequest.setCaseSensitive(true);

        var mockPage = createMockPage(1L, "Test Title", "Test Ingress", "Test Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(1L, 1L, "en", "Test Title", "Test Ingress", "Test Body");

        when(pageRepository.countBlogArticlesCaseSensitive(eq(3L), eq("en"), anyList(), eq("Test")))
                .thenReturn(1L);
        when(pageRepository.findBlogArticlesCaseSensitiveOrderByCreatedAtDesc(eq(3L), eq("en"), anyList(), eq("Test"), eq(10), eq(0)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(1L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(1L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.isEmpty());
    }

    @Test
    void getBlogArticlesSortByTitleAscOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setSortBy("title");
        pagedRequest.setDirection(SortDirectionEnum.ASC);

        var mockPage = createMockPage(1L, "Alpha Title", "Ingress", "Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(1L, 1L, "en", "Alpha Title", "Ingress", "Body");

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), any()))
                .thenReturn(1L);
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByTitleAsc(eq(3L), eq("en"), anyList(), any(), eq(10), eq(0)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(1L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(1L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        verify(pageRepository).findBlogArticlesCaseInsensitiveOrderByTitleAsc(anyLong(), anyString(), anyList(), any(), anyInt(), anyInt());
    }

    @Test
    void getBlogArticlesSortByTitleDescOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setSortBy("title");
        pagedRequest.setDirection(SortDirectionEnum.DESC);

        var mockPage = createMockPage(1L, "Zebra Title", "Ingress", "Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(1L, 1L, "en", "Zebra Title", "Ingress", "Body");

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), any()))
                .thenReturn(1L);
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByTitleDesc(eq(3L), eq("en"), anyList(), any(), eq(10), eq(0)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(1L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(1L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        verify(pageRepository).findBlogArticlesCaseInsensitiveOrderByTitleDesc(anyLong(), anyString(), anyList(), any(), anyInt(), anyInt());
    }

    @Test
    void getBlogArticlesSortByCreatedAtAscOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setSortBy("createdAt");
        pagedRequest.setDirection(SortDirectionEnum.ASC);

        var mockPage = createMockPage(1L, "Title", "Ingress", "Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(1L, 1L, "en", "Title", "Ingress", "Body");

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), any()))
                .thenReturn(1L);
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByCreatedAtAsc(eq(3L), eq("en"), anyList(), any(), eq(10), eq(0)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(1L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(1L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        verify(pageRepository).findBlogArticlesCaseInsensitiveOrderByCreatedAtAsc(anyLong(), anyString(), anyList(), any(), anyInt(), anyInt());
    }

    @Test
    void getBlogArticlesPaginationSecondPageOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setPage(1);
        pagedRequest.setSize(5);

        var mockPage = createMockPage(6L, "Title 6", "Ingress", "Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(6L, 6L, "en", "Title 6", "Ingress", "Body");

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), any()))
                .thenReturn(15L); // Total 15 items, so 3 pages of 5
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByCreatedAtDesc(eq(3L), eq("en"), anyList(), any(), eq(5), eq(5)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(6L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(6L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        assertEquals(1, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(3, result.getTotalPages());
        assertFalse(result.isFirst());
        assertFalse(result.isLast());
    }

    @Test
    void getBlogArticlesNullLanguageFail() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        pagedRequest.setLanguage(null);

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, userRoles);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getBlogArticlesAnonymousRoleOk() {
        // Given
        when(portalConfigurationService.getArrayConfiguration(any(), any()))
                .thenReturn(List.of("en", "fi", "sv"));

        Set<RoleEnum> anonymousRoles = new HashSet<>();
        anonymousRoles.add(RoleEnum.ROLE_ANONYMOUS);

        var mockPage = createMockPage(1L, "Public Blog", "Ingress", "Body");
        var mockPages = List.of(mockPage);
        var mockPageVersion = createMockPageVersion(1L, 1L, "en", "Public Blog", "Ingress", "Body");

        when(pageRepository.countBlogArticlesCaseInsensitive(eq(3L), eq("en"), anyList(), any()))
                .thenReturn(1L);
        when(pageRepository.findBlogArticlesCaseInsensitiveOrderByCreatedAtDesc(eq(3L), eq("en"), anyList(), any(), eq(10), eq(0)))
                .thenReturn(mockPages);
        when(pageVersionRepository.findByPageIdAndLanguage(1L, "en"))
                .thenReturn(Optional.of(mockPageVersion));
        lenient().when(pageRoleAccessRepository.findAllByPageId(1L))
                 .thenReturn(new HashSet<>());

        // When
        PagedResponse<PageResponse> result = pageService.getBlogArticles(pagedRequest, anonymousRoles);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    private Page createMockPage(Long id, String title, String ingress, String body) {
        return Page.builder()
                   .id(id)
                   .pageGroupId(3L)
                   .creator(1L)
                   .createdAt(Instant.now())
                   .pageVersions(new ArrayList<>())
                   .build();
    }

    private PageVersion createMockPageVersion(Long id, Long pageId, String language, String title, String ingress, String body) {
        return PageVersion.builder()
                          .id(id)
                          .pageId(pageId)
                          .language(language)
                          .title(title)
                          .ingress(ingress)
                          .body(body)
                          .build();
    }
}
