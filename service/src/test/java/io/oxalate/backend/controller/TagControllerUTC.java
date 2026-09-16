package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.TagGroupEnum;
import io.oxalate.backend.api.request.TagGroupRequest;
import io.oxalate.backend.api.request.TagRequest;
import io.oxalate.backend.api.response.TagGroupResponse;
import io.oxalate.backend.api.response.TagResponse;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.service.TagService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;

class TagControllerUTC {

    private final TagService tagService = mock(TagService.class);
    private final TagController controller = new TagController(tagService);

    @Test
    void getsGroupsForOrganizer() {
        var groups = List.of(mock(TagGroupResponse.class));
        when(tagService.getAllTagGroups()).thenReturn(groups);
        try (MockedStatic<AuthTools> auth = adminOrOrganizer(true)) {
            assertEquals(groups, controller.getAllTagGroups()
                                           .getBody());
        }
    }

    @Test
    void rejectsGroupReadWithoutRole() {
        try (MockedStatic<AuthTools> auth = adminOrOrganizer(false)) {
            assertThrows(OxalateUnauthorizedException.class, controller::getAllTagGroups);
        }
    }

    @Test
    void getsAndRejectsMissingGroup() {
        var group = mock(TagGroupResponse.class);
        when(tagService.getTagGroupById(4L)).thenReturn(group);
        try (MockedStatic<AuthTools> auth = adminOrOrganizer(true)) {
            assertEquals(group, controller.getTagGroupById(4L)
                                          .getBody());
            when(tagService.getTagGroupById(4L)).thenReturn(null);
            assertThrows(OxalateNotFoundException.class, () -> controller.getTagGroupById(4L));
        }
    }

    @Test
    void createsAndConvertsInvalidGroupRequest() {
        var request = new TagGroupRequest();
        var response = mock(TagGroupResponse.class);
        when(tagService.createTagGroup(request)).thenReturn(response);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(true);
            assertEquals(response, controller.createTagGroup(request)
                                             .getBody());
            when(tagService.createTagGroup(request)).thenThrow(new IllegalArgumentException());
            assertEquals(HttpStatus.BAD_REQUEST, controller.createTagGroup(request)
                                                           .getStatusCode());
        }
    }

    @Test
    void updatesAndDeletesGroupsWithFailures() {
        var request = new TagGroupRequest();
        request.setId(4L);
        var response = mock(TagGroupResponse.class);
        when(tagService.updateTagGroup(request)).thenReturn(response);
        when(tagService.getTagGroupById(4L)).thenReturn(response);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(true);
            assertEquals(response, controller.updateTagGroup(request)
                                             .getBody());
            when(tagService.updateTagGroup(request)).thenReturn(null);
            assertThrows(OxalateNotFoundException.class, () -> controller.updateTagGroup(request));
            assertEquals(HttpStatus.OK, controller.deleteTagGroup(4L)
                                                  .getStatusCode());
            when(tagService.getTagGroupById(4L)).thenReturn(null);
            assertThrows(OxalateNotFoundException.class, () -> controller.deleteTagGroup(4L));
        }
        verify(tagService).deleteTagGroup(4L);
    }

    @Test
    void getsGroupsAndTagsByType() {
        var groups = List.of(mock(TagGroupResponse.class));
        var tags = List.of(mock(TagResponse.class));
        when(tagService.getTagGroupsByType(TagGroupEnum.EVENT)).thenReturn(groups);
        when(tagService.getTagsByGroupType(TagGroupEnum.EVENT)).thenReturn(tags);
        try (MockedStatic<AuthTools> auth = adminOrOrganizer(true)) {
            assertEquals(groups, controller.getTagGroupsByType(TagGroupEnum.EVENT)
                                           .getBody());
            assertEquals(tags, controller.getTagsByGroupType(TagGroupEnum.EVENT)
                                         .getBody());
        }
    }

    @Test
    void createsUpdatesAndDeletesTags() {
        var request = new TagRequest();
        request.setId(6L);
        var response = mock(TagResponse.class);
        when(tagService.createTag(request)).thenReturn(response);
        when(tagService.updateTag(request)).thenReturn(response);
        when(tagService.getTagById(6L)).thenReturn(response);
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(true);
            assertEquals(response, controller.createTag(request)
                                             .getBody());
            assertEquals(response, controller.updateTag(request)
                                             .getBody());
            assertEquals(HttpStatus.OK, controller.deleteTag(6L)
                                                  .getStatusCode());
            when(tagService.updateTag(request)).thenThrow(new IllegalArgumentException());
            assertEquals(HttpStatus.BAD_REQUEST, controller.updateTag(request)
                                                           .getStatusCode());
        }
        verify(tagService).deleteTag(6L);
    }

    @Test
    void tagReadsAndMissingUpdateAreHandled() {
        var request = new TagRequest();
        request.setId(6L);
        var response = mock(TagResponse.class);
        when(tagService.getAllTags(2L)).thenReturn(List.of(response));
        when(tagService.getTagById(6L)).thenReturn(response);
        when(tagService.updateTag(request)).thenReturn(null);
        try (MockedStatic<AuthTools> auth = adminOrOrganizer(true)) {
            assertEquals(List.of(response), controller.getAllTags(2L)
                                                      .getBody());
            assertEquals(response, controller.getTagById(6L)
                                             .getBody());
            when(tagService.getTagById(6L)).thenReturn(null);
            assertThrows(OxalateNotFoundException.class, () -> controller.getTagById(6L));
        }
        try (MockedStatic<AuthTools> auth = Mockito.mockStatic(AuthTools.class)) {
            auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN))
                .thenReturn(true);
            assertThrows(OxalateNotFoundException.class, () -> controller.updateTag(request));
        }
    }

    private MockedStatic<AuthTools> adminOrOrganizer(boolean allowed) {
        var auth = Mockito.mockStatic(AuthTools.class);
        auth.when(() -> AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER))
            .thenReturn(allowed);
        return auth;
    }
}
