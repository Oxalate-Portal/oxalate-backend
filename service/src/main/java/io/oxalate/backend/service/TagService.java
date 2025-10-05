package io.oxalate.backend.service;

import io.oxalate.backend.api.TagGroupEnum;
import io.oxalate.backend.api.request.TagGroupRequest;
import io.oxalate.backend.api.request.TagRequest;
import io.oxalate.backend.api.response.TagGroupResponse;
import io.oxalate.backend.api.response.TagResponse;
import io.oxalate.backend.model.Tag;
import io.oxalate.backend.model.TagGroup;
import io.oxalate.backend.model.TagGroupTranslation;
import io.oxalate.backend.model.TagTranslation;
import io.oxalate.backend.repository.TagGroupRepository;
import io.oxalate.backend.repository.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagGroupRepository tagGroupRepository;

    // ---- Tag Groups ----

    @Transactional(readOnly = true)
    public List<TagGroupResponse> getAllTagGroups() {
        var groups = tagGroupRepository.findAllWithTagsAndTranslations();
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<TagGroupResponse> out = new ArrayList<>();
        for (TagGroup g : groups) {
            out.add(toResponse(g, g.getTags() == null ? List.of() : new ArrayList<>(g.getTags())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public TagGroupResponse getTagGroupById(long id) {
        var groupOpt = tagGroupRepository.findByIdWithTranslations(id);
        if (groupOpt.isEmpty()) {
            return null;
        }
        var tags = tagRepository.findByTagGroupIdWithTranslations(id);
        return toResponse(groupOpt.get(), tags);
    }

    @Transactional(readOnly = true)
    public List<TagGroupResponse> getTagGroupsByType(TagGroupEnum type) {
        var groups = tagGroupRepository.findByTypeWithTagsAndTranslations(type);
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<TagGroupResponse> out = new ArrayList<>();
        for (TagGroup g : groups) {
            out.add(toResponse(g, g.getTags() == null ? List.of() : new ArrayList<>(g.getTags())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTagsByGroupType(TagGroupEnum type) {
        List<TagGroup> groups = tagGroupRepository.findByTypeWithTranslations(type);
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = new ArrayList<>();
        for (TagGroup group : groups) {
            List<Tag> groupTags = tagRepository.findByTagGroupIdWithTranslations(group.getId());
            if (groupTags != null && !groupTags.isEmpty()) {
                tags.addAll(groupTags);
            }
        }

        if (tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                   .map(this::toResponse)
                   .collect(Collectors.toList());
    }

    @Transactional
    public TagGroupResponse createTagGroup(TagGroupRequest tagGroupRequest) {
        if (tagGroupRequest.getCode() == null || tagGroupRequest.getCode()
                                                                .isBlank()) {
            throw new IllegalArgumentException("Tag group code is required");
        }

        if (tagGroupRepository.existsByCode(tagGroupRequest.getCode())) {
            throw new IllegalArgumentException("Tag group code already exists");
        }

        var tagGroup = new TagGroup();
        tagGroup.setCode(tagGroupRequest.getCode());
        tagGroup.setType(tagGroupRequest.getType());
        tagGroup.setTranslations(new HashSet<>());

        if (tagGroupRequest.getNames() != null) {
            populateTagGroupWithNames(tagGroupRequest, tagGroup);
        }

        var saved = tagGroupRepository.save(tagGroup);
        return toResponse(saved, List.of());
    }

    @Transactional
    public TagGroupResponse updateTagGroup(TagGroupRequest tagGroupRequest) {
        var groupOpt = tagGroupRepository.findByIdWithTranslations(tagGroupRequest.getId());
        if (groupOpt.isEmpty()) {
            return null;
        }
        var entity = groupOpt.get();

        if (tagGroupRequest.getCode() != null && !Objects.equals(tagGroupRequest.getCode(), entity.getCode())) {
            if (tagGroupRepository.existsByCode(tagGroupRequest.getCode())) {
                throw new IllegalArgumentException("Tag group code already exists");
            }
            entity.setCode(tagGroupRequest.getCode());
        }

        if (tagGroupRequest.getType() != null) {
            entity.setType(tagGroupRequest.getType());
        }

        if (tagGroupRequest.getNames() != null) {
            if (entity.getTranslations() == null) {
                entity.setTranslations(new HashSet<>());
            }
            Map<String, TagGroupTranslation> existingByLang = entity.getTranslations()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(TagGroupTranslation::getLanguage, t -> t, (a, b) -> a));

            for (Map.Entry<String, String> e : tagGroupRequest.getNames()
                                                              .entrySet()) {
                String lang = e.getKey();
                String name = e.getValue();
                TagGroupTranslation t = existingByLang.get(lang);
                if (t != null) {
                    t.setName(name);
                } else {
                    TagGroupTranslation nt = new TagGroupTranslation();
                    nt.setTagGroup(entity);
                    nt.setLanguage(lang);
                    nt.setName(name);
                    entity.getTranslations()
                          .add(nt);
                }
            }
            // Optional removals are skipped to avoid relying on orphanRemoval.
        }

        var saved = tagGroupRepository.save(entity);
        return toResponse(saved, List.of());
    }

    @Transactional
    public void deleteTagGroup(long id) {
        tagGroupRepository.deleteById(id);
    }

    // ---- Tags ----

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags(Long groupId) {
        List<Tag> tags = (groupId == null)
                ? tagRepository.findAllWithTranslations()
                : tagRepository.findByTagGroupIdWithTranslations(groupId);
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                   .map(this::toResponse)
                   .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TagResponse getTagById(long id) {
        return tagRepository.findByIdWithTranslations(id)
                            .map(this::toResponse)
                            .orElse(null);
    }

    @Transactional
    public TagResponse createTag(TagResponse req) {
        if (req.getCode() == null || req.getCode()
                                        .isBlank()) {
            throw new IllegalArgumentException("Tag code is required");
        }
        if (tagRepository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Tag code already exists");
        }

        var group = resolveGroup(req.getTagGroupId(), req.getTagGroupCode());
        if (group == null) {
            throw new IllegalArgumentException("Tag group not found");
        }

        var entity = new Tag();
        entity.setCode(req.getCode());
        entity.setTagGroup(group);
        // Use Set to reduce duplicate entries coming from fetch joins
        entity.setTranslations(new HashSet<>());
        if (req.getNames() != null) {
            for (Map.Entry<String, String> e : req.getNames()
                                                  .entrySet()) {
                var t = new TagTranslation();
                t.setTag(entity);
                t.setLanguage(e.getKey());
                t.setName(e.getValue());
                entity.getTranslations()
                      .add(t);
            }
        }

        var saved = tagRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public TagResponse updateTag(TagRequest tagRequest) {
        var tagOpt = tagRepository.findByIdWithTranslations(tagRequest.getId());
        if (tagOpt.isEmpty()) {
            return null;
        }
        var entity = tagOpt.get();

        if (tagRequest.getCode() != null && !Objects.equals(tagRequest.getCode(), entity.getCode())) {
            if (tagRepository.existsByCode(tagRequest.getCode())) {
                throw new IllegalArgumentException("Tag code already exists");
            }
            entity.setCode(tagRequest.getCode());
        }

        if (tagRequest.getTagGroupId() != null || (tagRequest.getTagGroupCode() != null && !tagRequest.getTagGroupCode()
                                                                                                      .isBlank())) {
            var group = resolveGroup(tagRequest.getTagGroupId(), tagRequest.getTagGroupCode());
            if (group == null) {
                throw new IllegalArgumentException("Tag group not found");
            }
            entity.setTagGroup(group);
        }

        if (tagRequest.getNames() != null) {
            if (entity.getTranslations() == null) {
                entity.setTranslations(new HashSet<>());
            }
            Map<String, TagTranslation> existingByLang = entity.getTranslations()
                                                               .stream()
                                                               .collect(Collectors.toMap(TagTranslation::getLanguage, t -> t, (a, b) -> a));

            for (Map.Entry<String, String> e : tagRequest.getNames()
                                                         .entrySet()) {
                String lang = e.getKey();
                String name = e.getValue();
                TagTranslation t = existingByLang.get(lang);
                if (t != null) {
                    t.setName(name);
                } else {
                    TagTranslation nt = new TagTranslation();
                    nt.setTag(entity);
                    nt.setLanguage(lang);
                    nt.setName(name);
                    entity.getTranslations()
                          .add(nt);
                }
            }
            // Optional removals are skipped to avoid relying on orphanRemoval.
        }

        var saved = tagRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTag(long id) {
        tagRepository.deleteById(id);
    }

    // ---- Helpers ----

    private void populateTagGroupWithNames(TagGroupRequest tagGroupRequest, TagGroup tagGroup) {
        for (Map.Entry<String, String> e : tagGroupRequest.getNames()
                                                          .entrySet()) {
            var t = new TagGroupTranslation();
            t.setTagGroup(tagGroup);
            t.setLanguage(e.getKey());
            t.setName(e.getValue());
            tagGroup.getTranslations()
                    .add(t);
        }
    }

    private TagGroup resolveGroup(Long id, String code) {
        if (id != null) {
            return tagGroupRepository.findByIdWithTranslations(id)
                                     .orElse(null);
        }
        if (code != null && !code.isBlank()) {
            return tagGroupRepository.findByCode(code)
                                     .orElse(null);
        }
        return null;
    }

    private TagGroupResponse toResponse(TagGroup g, List<Tag> tags) {
        Map<String, String> names = g.getTranslations() == null ? Map.of() :
                g.getTranslations()
                 .stream()
                 .collect(
                         Collectors.toMap(
                                 TagGroupTranslation::getLanguage,
                                 TagGroupTranslation::getName,
                                 (existing, duplicate) -> existing // tolerate duplicates
                         )
                 );
        var resp = TagGroupResponse.builder()
                                   .id(g.getId())
                                   .code(g.getCode())
                                   .type(g.getType())
                                   .names(names)
                                   .build();

        if (tags != null && !tags.isEmpty()) {
            List<TagResponse> tagResponses = tags.stream()
                                                 .map(this::toResponse)
                                                 .collect(Collectors.toList());
            resp.setTags(tagResponses);
        }
        return resp;
    }

    private TagResponse toResponse(Tag t) {
        Map<String, String> names = t.getTranslations() == null ? Map.of() :
                t.getTranslations()
                 .stream()
                 .collect(
                         Collectors.toMap(
                                 TagTranslation::getLanguage,
                                 TagTranslation::getName,
                                 (existing, duplicate) -> existing // tolerate duplicates
                         )
                 );
        var resp = TagResponse.builder()
                              .id(t.getId())
                              .code(t.getCode())
                              .names(names)
                              .build();
        if (t.getTagGroup() != null) {
            resp.setTagGroupId(t.getTagGroup()
                                .getId());
            resp.setTagGroupCode(t.getTagGroup()
                                  .getCode());
        }
        return resp;
    }
}
