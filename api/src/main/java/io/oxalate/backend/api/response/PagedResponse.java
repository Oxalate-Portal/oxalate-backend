package io.oxalate.backend.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One page of a list endpoint. The wire format is stable and independent of Spring Data's {@link Page}
 * serialization, so the frontend has a single shape to consume.
 *
 * @param <T> type of the items on the page
 */
@Schema(description = "One page of items together with the paging metadata")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PagedResponse<T> {

    @Schema(description = "List of items on the current page")
    @NotNull
    private List<T> content;

    @Schema(description = "Current page index (0-based)", example = "0")
    @Min(0)
    private int page;

    @Schema(description = "Requested page size", example = "50")
    @Min(1)
    private int size;

    @Schema(description = "Total amount of items", example = "70000")
    @Min(0)
    private long totalElements;

    @Schema(description = "Total number of pages", example = "1400")
    @Min(0)
    private int totalPages;

    @Schema(description = "Is this the first page", example = "true")
    private boolean first;

    @Schema(description = "Is this the last page", example = "false")
    private boolean last;

    @Schema(description = "Is this the response empty", example = "false")
    private boolean empty;

    /**
     * Convenience factory that takes every piece of metadata explicitly.
     */
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
        var pagedResponse = new PagedResponse<T>();
        pagedResponse.setContent(content);
        pagedResponse.setPage(page);
        pagedResponse.setSize(size);
        pagedResponse.setTotalElements(totalElements);
        pagedResponse.setTotalPages(totalPages);
        pagedResponse.setFirst(first);
        pagedResponse.setLast(last);
        pagedResponse.setEmpty(content == null || content.isEmpty());
        return pagedResponse;
    }

    /**
     * Convenience factory that derives the page count and the first/last flags from the totals.
     */
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return of(content, page, size, totalElements, totalPages, page == 0, page >= totalPages - 1 || totalPages == 0);
    }

    /**
     * Builds a response from a Spring Data page whose entities are converted with the given mapper.
     *
     * @param springPage page returned by a repository
     * @param mapper     converts one entity to its response DTO
     * @param <E>        entity type
     * @param <T>        response type
     * @return the mapped page
     */
    public static <E, T> PagedResponse<T> fromPage(Page<E> springPage, Function<E, T> mapper) {
        var content = springPage.getContent()
                                .stream()
                                .map(mapper)
                                .toList();
        return of(content, springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages(),
                springPage.isFirst(), springPage.isLast());
    }
}
