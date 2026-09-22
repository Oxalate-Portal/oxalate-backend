package io.oxalate.backend.configuration;

import io.oxalate.backend.api.request.PagedRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

class PagedRequestArgumentResolverUTC {

    private final PagedRequestArgumentResolver resolver = new PagedRequestArgumentResolver();

    @SuppressWarnings("unused")
    void handler(PagedRequest pagedRequest, String language) {
    }

    @Test
    void supportsOnlyPagedRequestParametersOk() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("handler", PagedRequest.class, String.class);

        assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
        assertFalse(resolver.supportsParameter(new MethodParameter(method, 1)));
    }

    @Test
    void resolveWithoutParametersUsesDefaultsOk() throws Exception {
        var result = resolveArgument(new MockHttpServletRequest());

        assertEquals(0, result.getPage());
        assertEquals(PagedRequest.DEFAULT_PAGE_SIZE, result.getSize());
        assertNull(result.getSortBy());
        assertNull(result.getDirection());
        assertNull(result.getSearch());
        assertNull(result.getCaseSensitive());
    }

    @Test
    void resolveReadsSnakeCaseParametersOk() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("page", "3");
        request.setParameter("size", "50");
        request.setParameter("sort_by", "created_at");
        request.setParameter("direction", "asc");
        request.setParameter("search", " needle ");
        request.setParameter("case_sensitive", "true");

        var result = resolveArgument(request);

        assertEquals(3, result.getPage());
        assertEquals(50, result.getSize());
        assertEquals("created_at", result.getSortBy());
        assertEquals(Sort.Direction.ASC, result.getDirection());
        assertEquals("needle", result.getSearch());
        assertEquals(Boolean.TRUE, result.getCaseSensitive());
    }

    @Test
    void resolveIgnoresCamelCaseParameterNamesOk() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("sortBy", "created_at");
        request.setParameter("caseSensitive", "true");

        var result = resolveArgument(request);

        assertNull(result.getSortBy());
        assertNull(result.getCaseSensitive());
    }

    @Test
    void resolveWithInvalidNumbersFallsBackToDefaultsOk() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("page", "abc");
        request.setParameter("size", "9999999999999");

        var result = resolveArgument(request);

        assertEquals(0, result.getPage());
        assertEquals(PagedRequest.DEFAULT_PAGE_SIZE, result.getSize());
    }

    @Test
    void resolveWithInvalidDirectionFallsBackToNullOk() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("direction", "sideways");

        assertNull(resolveArgument(request).getDirection());
    }

    @Test
    void resolveWithBlankValuesTreatsThemAsAbsentOk() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("page", " ");
        request.setParameter("size", "");
        request.setParameter("sort_by", "  ");
        request.setParameter("direction", "");
        request.setParameter("search", "");
        request.setParameter("case_sensitive", " ");

        var result = resolveArgument(request);

        assertEquals(0, result.getPage());
        assertEquals(PagedRequest.DEFAULT_PAGE_SIZE, result.getSize());
        assertNull(result.getSortBy());
        assertNull(result.getDirection());
        assertNull(result.getSearch());
        assertNull(result.getCaseSensitive());
    }

    @Test
    void resolveWithNonTrueCaseSensitiveIsFalseOk() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("case_sensitive", "yes");

        assertEquals(Boolean.FALSE, resolveArgument(request).getCaseSensitive());
    }

    @Test
    void configurationRegistersResolverOk() {
        var resolvers = new java.util.ArrayList<HandlerMethodArgumentResolver>();

        new WebMvcConfiguration().addArgumentResolvers(resolvers);

        assertEquals(1, resolvers.size());
        assertEquals(PagedRequestArgumentResolver.class, List.copyOf(resolvers)
                                                             .getFirst()
                                                             .getClass());
    }

    private PagedRequest resolveArgument(MockHttpServletRequest request) throws Exception {
        var method = getClass().getDeclaredMethod("handler", PagedRequest.class, String.class);
        NativeWebRequest webRequest = new ServletWebRequest(request);
        return (PagedRequest) resolver.resolveArgument(new MethodParameter(method, 0), null, webRequest, null);
    }
}
