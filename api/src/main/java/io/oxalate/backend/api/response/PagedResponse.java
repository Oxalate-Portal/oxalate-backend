package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    @JsonProperty("content")
    private List<T> content;

    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    @JsonProperty("total_elements")
    private long totalElements;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("first")
    private boolean first;

    @JsonProperty("last")
    private boolean last;

    @JsonProperty("empty")
    private boolean empty;

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PagedResponse.<T>builder()
                            .content(content)
                            .page(page)
                            .size(size)
                            .totalElements(totalElements)
                            .totalPages(totalPages)
                            .first(page == 0)
                            .last(page >= totalPages - 1 || totalPages == 0)
                            .empty(content == null || content.isEmpty())
                            .build();
    }
}
