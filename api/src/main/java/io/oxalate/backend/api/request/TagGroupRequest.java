package io.oxalate.backend.api.request;

import io.oxalate.backend.api.TagGroupEnum;
import io.oxalate.backend.api.response.TagGroupResponse;
import io.oxalate.backend.api.response.TagResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Tag group request")
public class TagGroupRequest extends TagGroupResponse {
    // An explicit constructor is needed to call the super constructor, as Lombok's @Data does not generate one with arguments
    public TagGroupRequest(Long id, String code, Map<String, String> names, List<TagResponse> tags, TagGroupEnum type) {
        super(id, code, names, tags, type);
    }
}
