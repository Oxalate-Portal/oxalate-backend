package io.oxalate.backend.api.request;

import io.oxalate.backend.api.response.TagResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Tag request")
public class TagRequest extends TagResponse {
    // An explicit constructor is needed to call the super constructor, as Lombok's @Data does not generate one with arguments
    public TagRequest(Long id, String code, Map<String, String> names, Long tagGroupId, String tagGroupCode) {
        super(id, code, names, tagGroupId, tagGroupCode);
    }
}
