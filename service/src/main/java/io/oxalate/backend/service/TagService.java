package io.oxalate.backend.service;

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

    @Transactional
    public TagGroupResponse createTagGroup(TagGroupResponse req) {
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new IllegalArgumentException("Tag group code is required");
        }
        if (tagGroupRepository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Tag group code already exists");
        }

        var entity = new TagGroup();
        entity.setCode(req.getCode());
        entity.setTranslations(new HashSet<>());
        if (req.getNames() != null) {
            for (Map.Entry<String, String> e : req.getNames().entrySet()) {
                var t = new TagGroupTranslation();
                t.setTagGroup(entity);
                t.setLanguage(e.getKey());
                t.setName(e.getValue());
                entity.getTranslations().add(t);
            }
        }

        var saved = tagGroupRepository.save(entity);
        return toResponse(saved, List.of());
    }

    @Transactional
    public TagGroupResponse updateTagGroup(long id, TagGroupResponse req) {
        var groupOpt = tagGroupRepository.findByIdWithTranslations(id);
        if (groupOpt.isEmpty()) {
            return null;
        }
        var entity = groupOpt.get();

        if (req.getCode() != null && !Objects.equals(req.getCode(), entity.getCode())) {
            if (tagGroupRepository.existsByCode(req.getCode())) {
                throw new IllegalArgumentException("Tag group code already exists");
            }
            entity.setCode(req.getCode());
        }

        if (req.getNames() != null) {
            entity.getTranslations().clear();
            for (Map.Entry<String, String> e : req.getNames().entrySet()) {
                var t = new TagGroupTranslation();
                t.setTagGroup(entity);
                t.setLanguage(e.getKey());
                t.setName(e.getValue());
                entity.getTranslations().add(t);
            }
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
        return tags.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TagResponse getTagById(long id) {
        return tagRepository.findByIdWithTranslations(id).map(this::toResponse).orElse(null);
    }

    @Transactional
    public TagResponse createTag(TagResponse req) {
        if (req.getCode() == null || req.getCode().isBlank()) {
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
            for (Map.Entry<String, String> e : req.getNames().entrySet()) {
                var t = new TagTranslation();
                t.setTag(entity);
                t.setLanguage(e.getKey());
                t.setName(e.getValue());
                entity.getTranslations().add(t);
            }
        }

        var saved = tagRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public TagResponse updateTag(long id, TagResponse req) {
        var tagOpt = tagRepository.findByIdWithTranslations(id);
        if (tagOpt.isEmpty()) {
            return null;
        }
        var entity = tagOpt.get();

        if (req.getCode() != null && !Objects.equals(req.getCode(), entity.getCode())) {
            if (tagRepository.existsByCode(req.getCode())) {
                throw new IllegalArgumentException("Tag code already exists");
            }
            entity.setCode(req.getCode());
        }

        if (req.getTagGroupId() != null || (req.getTagGroupCode() != null && !req.getTagGroupCode().isBlank())) {
            var group = resolveGroup(req.getTagGroupId(), req.getTagGroupCode());
            if (group == null) {
                throw new IllegalArgumentException("Tag group not found");
            }
            entity.setTagGroup(group);
        }

        if (req.getNames() != null) {
            entity.getTranslations().clear();
            for (Map.Entry<String, String> e : req.getNames().entrySet()) {
                var t = new TagTranslation();
                t.setTag(entity);
                t.setLanguage(e.getKey());
                t.setName(e.getValue());
                entity.getTranslations().add(t);
            }
        }

        var saved = tagRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTag(long id) {
        tagRepository.deleteById(id);
    }

    // ---- Helpers ----

    private TagGroup resolveGroup(Long id, String code) {
        if (id != null) {
            return tagGroupRepository.findByIdWithTranslations(id).orElse(null);
        }
        if (code != null && !code.isBlank()) {
            return tagGroupRepository.findByCode(code).orElse(null);
        }
        return null;
    }

    private TagGroupResponse toResponse(TagGroup g, List<Tag> tags) {
        Map<String, String> names = g.getTranslations() == null ? Map.of() :
                g.getTranslations().stream().collect(
                        Collectors.toMap(
                                TagGroupTranslation::getLanguage,
                                TagGroupTranslation::getName,
                                (existing, duplicate) -> existing // tolerate duplicates
                        )
                );
        var resp = TagGroupResponse.builder()
                .id(g.getId())
                .code(g.getCode())
                .names(names)
                .build();

        if (tags != null && !tags.isEmpty()) {
            List<TagResponse> tagResponses = tags.stream().map(this::toResponse).collect(Collectors.toList());
            resp.setTags(tagResponses);
        }
        return resp;
    }

    private TagResponse toResponse(Tag t) {
        Map<String, String> names = t.getTranslations() == null ? Map.of() :
                t.getTranslations().stream().collect(
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
            resp.setTagGroupId(t.getTagGroup().getId());
            resp.setTagGroupCode(t.getTagGroup().getCode());
        }
        return resp;
    }
}
