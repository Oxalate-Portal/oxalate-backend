package io.oxalate.backend.tools;

import io.oxalate.backend.api.response.TagResponse;
import io.oxalate.backend.model.Tag;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TagTools {
    public static Set<TagResponse> collectTagResponses(Set<Tag> tags) {
        Set<TagResponse> tagResponses;

        if (tags != null && !tags.isEmpty()) {
            tagResponses = tags.stream()
                               .map(tag -> TagResponse.builder()
                                                      .id(tag.getId())
                                                      .code(tag.getCode())
                                                      .names(tag.getTranslatedNames())
                                                      .tagGroupId(tag.getTagGroup() != null ?
                                                              tag.getTagGroup()
                                                                 .getId() :
                                                              null)
                                                      .tagGroupCode(tag.getTagGroup() != null ?
                                                              tag.getTagGroup()
                                                                 .getCode() :
                                                              null)
                                                      .build())
                               .collect(Collectors.toSet());
        } else {
            tagResponses = new HashSet<>();
        }

        return tagResponses;
    }
}
