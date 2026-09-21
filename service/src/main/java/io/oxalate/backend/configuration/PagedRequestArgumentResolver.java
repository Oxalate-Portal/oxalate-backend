package io.oxalate.backend.configuration;

import io.oxalate.backend.api.request.PagedRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Binds the {@link PagedRequest} of a paged GET endpoint from snake_case query parameters, so the query string uses
 * the same naming as the JSON bodies: {@code page}, {@code size}, {@code sort_by}, {@code direction}, {@code search}
 * and {@code case_sensitive}. Spring's {@code @ModelAttribute} binding would only accept the Java field names.
 * <p>
 * OWASP A05/A10:2025 - every value arrives straight from the client. A value that does not parse (a non-numeric page,
 * an unknown direction) falls back to the default instead of surfacing as a binding error or a 500; the page size and
 * the sort column are sanitized further by {@code PagingTools} in the service layer.
 */
@Slf4j
public class PagedRequestArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String PAGE_PARAMETER = "page";
    public static final String SIZE_PARAMETER = "size";
    public static final String SORT_BY_PARAMETER = "sort_by";
    public static final String DIRECTION_PARAMETER = "direction";
    public static final String SEARCH_PARAMETER = "search";
    public static final String CASE_SENSITIVE_PARAMETER = "case_sensitive";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PagedRequest.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) {
        return resolve(webRequest);
    }

    /**
     * Builds the request from the query parameters of the given web request.
     */
    public static PagedRequest resolve(NativeWebRequest webRequest) {
        return PagedRequest.builder()
                           .page(parseInt(webRequest.getParameter(PAGE_PARAMETER), PAGE_PARAMETER, 0))
                           .size(parseInt(webRequest.getParameter(SIZE_PARAMETER), SIZE_PARAMETER, PagedRequest.DEFAULT_PAGE_SIZE))
                           .sortBy(blankToNull(webRequest.getParameter(SORT_BY_PARAMETER)))
                           .direction(parseDirection(webRequest.getParameter(DIRECTION_PARAMETER)))
                           .search(blankToNull(webRequest.getParameter(SEARCH_PARAMETER)))
                           .caseSensitive(parseBoolean(webRequest.getParameter(CASE_SENSITIVE_PARAMETER)))
                           .build();
    }

    private static int parseInt(@Nullable String value, String name, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Ignoring non-numeric {} query parameter, falling back to {}", name, defaultValue);
            return defaultValue;
        }
    }

    private static Sort.@Nullable Direction parseDirection(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        var direction = Sort.Direction.fromOptionalString(value.trim());

        if (direction.isEmpty()) {
            log.warn("Ignoring unknown sort direction, falling back to the endpoint default");
        }

        return direction.orElse(null);
    }

    @Nullable
    private static Boolean parseBoolean(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Boolean.parseBoolean(value.trim());
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
