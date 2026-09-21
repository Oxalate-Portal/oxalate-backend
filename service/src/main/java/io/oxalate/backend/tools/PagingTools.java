package io.oxalate.backend.tools;

import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.PagedResponse;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Turns a client supplied {@link PagedRequest} into safe Spring Data paging and search primitives.
 * <p>
 * OWASP A01/A10:2025 - the sort column and the page size arrive straight from the client. An unknown sort property
 * would reach Spring Data and produce a 500, and an uncapped page size lets one request pull a whole table into
 * memory. Every endpoint therefore declares its own allow-list of sortable columns (client name to entity property
 * path) and this class falls back to the default ordering for anything else, caps the page size, and builds the
 * search predicate with an escaped LIKE pattern so that wildcards in the query are matched literally.
 */
@Slf4j
public final class PagingTools {

    /**
     * Largest page any list endpoint hands out.
     */
    public static final int MAX_PAGE_SIZE = 200;

    private static final char LIKE_ESCAPE = '\\';

    private PagingTools() {
    }

    /**
     * Builds a {@link Pageable} whose sort is validated against the allow-list of the endpoint.
     *
     * @param pagedRequest      client request, may carry an unknown or missing sort column
     * @param sortableColumns   allowed client sort names mapped to the entity property path they sort by
     * @param defaultSortColumn entity property path used when the request carries no usable sort column
     * @param defaultDirection  direction used when the request carries none
     * @return a safe page request
     */
    public static Pageable toPageable(PagedRequest pagedRequest, Map<String, String> sortableColumns, String defaultSortColumn,
            Sort.Direction defaultDirection) {
        var page = Math.max(pagedRequest.getPage(), 0);
        var size = sanitizePageSize(pagedRequest.getSize());
        var direction = pagedRequest.getDirection() != null ? pagedRequest.getDirection() : defaultDirection;
        var property = defaultSortColumn;
        var requestedColumn = pagedRequest.getSortBy();

        if (requestedColumn != null && !requestedColumn.isBlank()) {
            var mapped = sortableColumns.get(requestedColumn.trim());

            if (mapped != null) {
                property = mapped;
            } else {
                log.warn("Ignoring unsupported sort column, falling back to {}", defaultSortColumn);
            }
        }

        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    /**
     * Builds an empty page that echoes the sanitized paging of the request, for endpoints that are switched off by
     * configuration but must still answer with the paged shape.
     */
    public static <T> PagedResponse<T> emptyPage(PagedRequest pagedRequest) {
        return PagedResponse.of(List.of(), Math.max(pagedRequest.getPage(), 0), sanitizePageSize(pagedRequest.getSize()), 0L);
    }

    /**
     * Clamps the requested page size to {@code 1..MAX_PAGE_SIZE}; a missing or non-positive size means the default.
     */
    public static int sanitizePageSize(int pageSize) {
        if (pageSize < 1) {
            return PagedRequest.DEFAULT_PAGE_SIZE;
        }

        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * Builds a free-text search predicate that matches when any of the given string properties contains the search
     * term of the request. Property paths may be nested ({@code user.lastName}).
     *
     * @param pagedRequest         client request carrying the search term and the case sensitivity flag
     * @param searchableProperties entity property paths of string type
     * @param <T>                  entity type
     * @return the specification, or {@code null} when the request has no search term
     */
    @Nullable
    public static <T> Specification<T> searchSpecification(PagedRequest pagedRequest, String... searchableProperties) {
        var search = pagedRequest.getSearch();

        if (search == null || search.isBlank() || searchableProperties.length == 0) {
            return null;
        }

        var caseSensitive = Boolean.TRUE.equals(pagedRequest.getCaseSensitive());
        var term = search.trim();
        var pattern = "%" + escapeLike(caseSensitive ? term : term.toLowerCase()) + "%";

        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>(searchableProperties.length);

            for (var propertyPath : searchableProperties) {
                Expression<String> expression = resolvePath(root, propertyPath);

                if (!caseSensitive) {
                    expression = criteriaBuilder.lower(expression);
                }

                predicates.add(criteriaBuilder.like(expression, pattern, LIKE_ESCAPE));
            }

            return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Combines the given specifications with AND, skipping {@code null} entries, so callers can pass the optional
     * search specification next to their mandatory filters without null checks.
     */
    @SafeVarargs
    public static <T> Specification<T> allOf(@Nullable Specification<T>... specifications) {
        List<Specification<T>> present = new ArrayList<>();

        for (var specification : specifications) {
            if (specification != null) {
                present.add(specification);
            }
        }

        return Specification.allOf(present);
    }

    /**
     * Escapes the LIKE wildcards and the escape character itself so that the search term is matched literally.
     */
    static String escapeLike(String value) {
        var builder = new StringBuilder(value.length());

        for (var character : value.toCharArray()) {
            if (character == LIKE_ESCAPE || character == '%' || character == '_') {
                builder.append(LIKE_ESCAPE);
            }

            builder.append(character);
        }

        return builder.toString();
    }

    private static <T> Path<String> resolvePath(Path<T> root, String propertyPath) {
        Path<?> path = root;

        for (var segment : propertyPath.split("\\.")) {
            path = path.get(segment);
        }

        @SuppressWarnings("unchecked")
        var stringPath = (Path<String>) path;
        return stringPath;
    }
}
