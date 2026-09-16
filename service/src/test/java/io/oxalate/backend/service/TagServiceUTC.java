package io.oxalate.backend.service;

import io.oxalate.backend.api.TagGroupEnum;
import io.oxalate.backend.api.request.TagGroupRequest;
import io.oxalate.backend.api.request.TagRequest;
import io.oxalate.backend.api.response.TagResponse;
import io.oxalate.backend.model.Tag;
import io.oxalate.backend.model.TagGroup;
import io.oxalate.backend.model.TagTranslation;
import io.oxalate.backend.repository.TagGroupRepository;
import io.oxalate.backend.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceUTC {

    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagGroupRepository tagGroupRepository;
    @InjectMocks
    private TagService tagService;

    @Test
    void getAllTagGroupsReturnsEmptyWhenRepositoryHasNoGroups() {
        when(tagGroupRepository.findAllWithTagsAndTranslations()).thenReturn(List.of());

        assertTrue(tagService.getAllTagGroups()
                             .isEmpty());
    }

    @Test
    void getTagGroupByIdReturnsNullWhenMissing() {
        when(tagGroupRepository.findByIdWithTranslations(9L)).thenReturn(Optional.empty());

        assertTrue(tagService.getTagGroupById(9L) == null);
    }

    @Test
    void createTagGroupRejectsBlankAndDuplicateCodes() {
        var blank = new TagGroupRequest(null, " ", null, null, TagGroupEnum.EVENT);
        assertThrows(IllegalArgumentException.class, () -> tagService.createTagGroup(blank));

        var duplicate = new TagGroupRequest(null, "diving", null, null, TagGroupEnum.EVENT);
        when(tagGroupRepository.existsByCode("diving")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> tagService.createTagGroup(duplicate));
        verify(tagGroupRepository, never()).save(any(TagGroup.class));
    }

    @Test
    void createTagGroupPersistsTranslations() {
        var request = new TagGroupRequest(null, "diving", Map.of("en", "Diving"), null, TagGroupEnum.EVENT);
        var saved = TagGroup.builder()
                            .id(3L)
                            .code("diving")
                            .type(TagGroupEnum.EVENT)
                            .build();
        when(tagGroupRepository.existsByCode("diving")).thenReturn(false);
        when(tagGroupRepository.save(any(TagGroup.class))).thenReturn(saved);

        var result = tagService.createTagGroup(request);

        assertEquals(3L, result.getId());
        verify(tagGroupRepository).save(any(TagGroup.class));
    }

    @Test
    void createTagRejectsMissingGroup() {
        var request = TagResponse.builder()
                                 .code("cave")
                                 .tagGroupCode("missing")
                                 .build();
        when(tagRepository.existsByCode("cave")).thenReturn(false);
        when(tagGroupRepository.findByCode("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> tagService.createTag(request));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void createTagPersistsGroupAndTranslations() {
        var group = TagGroup.builder()
                            .id(2L)
                            .code("dives")
                            .build();
        var request = TagResponse.builder()
                                 .code("cave")
                                 .tagGroupId(2L)
                                 .names(Map.of("en", "Cave"))
                                 .build();
        var saved = Tag.builder()
                       .id(7L)
                       .code("cave")
                       .tagGroup(group)
                       .build();
        when(tagRepository.existsByCode("cave")).thenReturn(false);
        when(tagGroupRepository.findByIdWithTranslations(2L)).thenReturn(Optional.of(group));
        when(tagRepository.save(any(Tag.class))).thenReturn(saved);

        var result = tagService.createTag(request);

        assertEquals(7L, result.getId());
        assertEquals(2L, result.getTagGroupId());
    }

    @Test
    void updateTagChangesExistingAndAddsNewTranslations() {
        var group = TagGroup.builder()
                            .id(2L)
                            .code("dives")
                            .build();
        var existing = TagTranslation.builder()
                                     .language("en")
                                     .name("Old")
                                     .build();
        var translations = new HashSet<TagTranslation>();
        translations.add(existing);
        var tag = Tag.builder()
                     .id(7L)
                     .code("old")
                     .tagGroup(group)
                     .translations(translations)
                     .build();
        when(tagRepository.findByIdWithTranslations(7L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByCode("new")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(tag);

        var result = tagService.updateTag(new TagRequest(7L, "new", Map.of("en", "New", "fi", "Uusi"), null, null));

        assertEquals("new", result.getCode());
        assertEquals("New", tag.getTranslations()
                               .stream()
                               .filter(t -> t.getLanguage()
                                             .equals("en"))
                               .findFirst()
                               .orElseThrow()
                               .getName());
        assertEquals(2, tag.getTranslations()
                           .size());
    }
}
