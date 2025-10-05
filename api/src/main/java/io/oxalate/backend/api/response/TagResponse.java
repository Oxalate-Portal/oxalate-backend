package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {

    @Schema(description = "Unique identifier of the tag", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Unique code of the tag", example = "chicken", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("code")
    private String code;

    @Schema(description = "Map of translated names by language code", example = "{\"en\":\"Chicken\",\"fi\":\"Kana\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("names")
    private Map<String, String> names;

    @Schema(description = "ID of the tag group this tag belongs to", example = "1")
    @JsonProperty("tagGroupId")
    private Long tagGroupId;

    @Schema(description = "Code of the tag group this tag belongs to", example = "birds")
    @JsonProperty("tagGroupCode")
    private String tagGroupCode;
}

