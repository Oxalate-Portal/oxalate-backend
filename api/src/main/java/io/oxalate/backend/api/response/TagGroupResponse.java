package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagGroupResponse {

    @Schema(description = "Unique identifier of the tag group", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Unique code of the tag group", example = "birds", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("code")
    private String code;

    @Schema(description = "Map of translated names by language code", example = "{\"en\":\"Birds\",\"fi\":\"Linnut\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("names")
    private Map<String, String> names;

    @Schema(description = "List of tags in this group")
    @JsonProperty("tags")
    private List<TagResponse> tags;
}

