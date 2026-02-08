package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.SortDirectionEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedRequest {

    @Min(0)
    @JsonProperty("page")
    private int page;

    @Min(1)
    @JsonProperty("size")
    private int size;

    @JsonProperty("sort_by")
    private String sortBy;

    @JsonProperty("direction")
    private SortDirectionEnum direction;

    @JsonProperty("search")
    private String search;

    @JsonProperty("case_sensitive")
    private Boolean caseSensitive;

    @NotNull
    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @JsonProperty("language")
    private String language;
}
