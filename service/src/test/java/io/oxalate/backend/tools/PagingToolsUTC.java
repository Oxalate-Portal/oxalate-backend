package io.oxalate.backend.tools;

import io.oxalate.backend.api.request.PagedRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PagingToolsUTC {

    private static final Map<String, String> SORTABLE = Map.of("username", "username", "organizer", "organizer.lastName");

    @Mock
    private Root<Object> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Test
    void toPageableMapsAllowedColumnOk() {
        var request = PagedRequest.builder()
                                  .page(2)
                                  .size(25)
                                  .sortBy("organizer")
                                  .direction(Sort.Direction.ASC)
                                  .build();

        var pageable = PagingTools.toPageable(request, SORTABLE, "createdAt", Sort.Direction.DESC);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        var order = pageable.getSort()
                            .getOrderFor("organizer.lastName");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void toPageableUnknownColumnFallsBackToDefaultOk() {
        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .sortBy("password")
                                  .build();

        var pageable = PagingTools.toPageable(request, SORTABLE, "createdAt", Sort.Direction.DESC);

        var order = pageable.getSort()
                            .getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
        assertNull(pageable.getSort()
                           .getOrderFor("password"));
    }

    @Test
    void toPageableBlankColumnUsesDefaultOk() {
        var request = PagedRequest.builder()
                                  .page(0)
                                  .size(10)
                                  .sortBy("  ")
                                  .build();

        var pageable = PagingTools.toPageable(request, SORTABLE, "createdAt", Sort.Direction.ASC);

        assertNotNull(pageable.getSort()
                              .getOrderFor("createdAt"));
    }

    @Test
    void toPageableClampsPageAndSizeOk() {
        var request = PagedRequest.builder()
                                  .page(-5)
                                  .size(100_000)
                                  .build();

        var pageable = PagingTools.toPageable(request, SORTABLE, "createdAt", Sort.Direction.DESC);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(PagingTools.MAX_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    void sanitizePageSizeOk() {
        assertEquals(PagedRequest.DEFAULT_PAGE_SIZE, PagingTools.sanitizePageSize(0));
        assertEquals(PagedRequest.DEFAULT_PAGE_SIZE, PagingTools.sanitizePageSize(-3));
        assertEquals(50, PagingTools.sanitizePageSize(50));
        assertEquals(PagingTools.MAX_PAGE_SIZE, PagingTools.sanitizePageSize(PagingTools.MAX_PAGE_SIZE + 1));
    }

    @Test
    void searchSpecificationBlankSearchIsNullOk() {
        assertNull(PagingTools.<Object>searchSpecification(PagedRequest.builder()
                                                                       .build(), "username"));
        assertNull(PagingTools.<Object>searchSpecification(PagedRequest.builder()
                                                                       .search("   ")
                                                                       .build(), "username"));
        assertNull(PagingTools.<Object>searchSpecification(PagedRequest.builder()
                                                                       .search("abc")
                                                                       .build()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchSpecificationCaseInsensitiveLowersAndEscapesOk() {
        var request = PagedRequest.builder()
                                  .search(" Ab%_c ")
                                  .caseSensitive(false)
                                  .build();
        var usernamePath = (Path<Object>) mock(Path.class);
        var organizerPath = (Path<Object>) mock(Path.class);
        var lastNamePath = (Path<Object>) mock(Path.class);
        var lowered = (Expression<String>) mock(Expression.class);
        var predicate = mock(Predicate.class);
        var combined = mock(Predicate.class);

        when(root.get("username")).thenReturn(usernamePath);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("lastName")).thenReturn(lastNamePath);
        when(criteriaBuilder.lower(any())).thenReturn(lowered);
        when(criteriaBuilder.like(eq(lowered), anyString(), eq('\\'))).thenReturn(predicate);
        when(criteriaBuilder.or(any(Predicate[].class))).thenReturn(combined);

        var specification = PagingTools.<Object>searchSpecification(request, "username", "organizer.lastName");

        assertNotNull(specification);
        assertEquals(combined, specification.toPredicate(root, query, criteriaBuilder));
        verify(criteriaBuilder, times(2)).like(eq(lowered), eq("%ab\\%\\_c%"), eq('\\'));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchSpecificationCaseSensitiveKeepsCaseOk() {
        var request = PagedRequest.builder()
                                  .search("Ab")
                                  .caseSensitive(true)
                                  .build();
        var usernamePath = (Path<Object>) mock(Path.class);
        var predicate = mock(Predicate.class);
        var combined = mock(Predicate.class);

        when(root.get("username")).thenReturn(usernamePath);
        when(criteriaBuilder.like(any(Expression.class), eq("%Ab%"), eq('\\'))).thenReturn(predicate);
        when(criteriaBuilder.or(any(Predicate[].class))).thenReturn(combined);

        var specification = PagingTools.<Object>searchSpecification(request, "username");

        assertNotNull(specification);
        assertEquals(combined, specification.toPredicate(root, query, criteriaBuilder));
        verify(criteriaBuilder, never()).lower(any());
    }

    @Test
    void escapeLikeOk() {
        assertEquals("plain", PagingTools.escapeLike("plain"));
        assertEquals("100\\%", PagingTools.escapeLike("100%"));
        assertEquals("a\\_b", PagingTools.escapeLike("a_b"));
        assertEquals("c\\\\d", PagingTools.escapeLike("c\\d"));
    }

    @Test
    void allOfSkipsNullsOk() {
        var specification = PagingTools.<Object>allOf(null, null);
        assertNotNull(specification);
        assertTrue(PagingTools.<Object>allOf((root, query, cb) -> null, null) != null);
    }
}
