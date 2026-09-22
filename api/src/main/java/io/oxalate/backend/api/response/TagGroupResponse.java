package io.oxalate.backend.api.response;

import io.oxalate.backend.api.TagGroupEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TagGroupResponse {

    @Schema(description = "Unique identifier of the tag group", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Unique code of the tag group", example = "birds", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "Map of translated names by language code", example = "{\"en\":\"Birds\",\"fi\":\"Linnut\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> names;

    @Schema(description = "List of tags in this group")
    private List<TagResponse> tags;

    @Schema(description = "Type of tag group", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    private TagGroupEnum type;
}
