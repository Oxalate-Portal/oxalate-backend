package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Common paging, sorting and search parameters for list endpoints. The class is bound from a JSON request body using
 * snake_case names. Sort columns are always validated against a per-endpoint allow-list in the service layer before
 * they reach Spring Data.
 */
@Schema(description = "Paging, sorting and search parameters")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PagedRequest {

    /**
     * Page size used when the client sends none.
     */
    public static final int DEFAULT_PAGE_SIZE = 25;

    /**
     * OpenAPI descriptions shared by every paged endpoint so the documentation stays consistent. The {@code sort_by}
     * description is endpoint specific because it lists the allowed columns.
     */
    public static final String PAGE_DESCRIPTION = "Index of the page to be retrieved, 0-based. Negative values are treated as 0";
    public static final String SIZE_DESCRIPTION = "Number of items on the page, defaults to 25 and is capped at 200";
    public static final String DIRECTION_DESCRIPTION = "Sort direction, ASC or DESC. Defaults to the endpoint specific direction";
    public static final String SEARCH_DESCRIPTION = "Free text search, matched as a substring against the searchable columns of the endpoint";
    public static final String CASE_SENSITIVE_DESCRIPTION = "Whether the search is case sensitive, defaults to false";

    @Schema(description = "Page number (0-based)", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(0)
    @Builder.Default
    private int page = 0;

    @Schema(description = "Size of the page", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(1)
    @Builder.Default
    private int size = DEFAULT_PAGE_SIZE;

    @Schema(description = "Sort by field, a snake_case response field name", example = "created_at", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Nullable
    private String sortBy;

    @Schema(description = "Sort direction", example = "DESC", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Sort.@Nullable Direction direction;

    @Schema(description = "Search query", example = "holiday", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Nullable
    private String search;

    @Schema(description = "Whether the search is case sensitive", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Nullable
    private Boolean caseSensitive;

    @Schema(description = "Column to which the search applies", example = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Nullable
    private String filterColumn;
}
