package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.TagGroupEnum;
import io.oxalate.backend.api.request.TagGroupRequest;
import io.oxalate.backend.api.request.TagRequest;
import io.oxalate.backend.api.response.TagGroupResponse;
import io.oxalate.backend.api.response.TagResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_CREATE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_DELETE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_DELETE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_DELETE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_DELETE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_BY_GROUP_TYPE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_BY_GROUP_TYPE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_BY_GROUP_TYPE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_CREATE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_DELETE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_DELETE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_DELETE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_DELETE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_ALL_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_BY_TYPE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_BY_TYPE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_BY_TYPE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_GET_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_UPDATE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_UPDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_GROUP_UPDATE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_UPDATE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_UPDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.TAGS_UPDATE_UNAUTHORIZED;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.rest.TagAPI;
import io.oxalate.backend.service.TagService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("TagController")
public class TagController implements TagAPI {

    private final TagService tagService;

    // ---- Tag Groups ----

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_GROUP_GET_ALL_START, okMessage = TAGS_GROUP_GET_ALL_OK)
    public ResponseEntity<List<TagGroupResponse>> getAllTagGroups() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            throw new OxalateUnauthorizedException(TAGS_GROUP_GET_ALL_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var groups = tagService.getAllTagGroups();
        return ResponseEntity.ok(groups);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_GROUP_GET_START, okMessage = TAGS_GROUP_GET_OK)
    public ResponseEntity<TagGroupResponse> getTagGroupById(long id) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            throw new OxalateUnauthorizedException(TAGS_GROUP_GET_ALL_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var group = tagService.getTagGroupById(id);
        if (group == null) {
            throw new OxalateNotFoundException(TAGS_GROUP_GET_NOT_FOUND + id);
        }

        return ResponseEntity.ok(group);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_GROUP_CREATE_START, okMessage = TAGS_GROUP_CREATE_OK)
    public ResponseEntity<TagGroupResponse> createTagGroup(TagGroupRequest tagGroupRequest) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(TAGS_GROUP_CREATE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        try {
            var created = tagService.createTagGroup(tagGroupRequest);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_GROUP_UPDATE_START, okMessage = TAGS_GROUP_UPDATE_OK)
    public ResponseEntity<TagGroupResponse> updateTagGroup(TagGroupRequest tagGroupRequest) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(TAGS_GROUP_UPDATE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        try {
            var updated = tagService.updateTagGroup(tagGroupRequest);
            if (updated == null) {
                throw new OxalateNotFoundException(TAGS_GROUP_UPDATE_NOT_FOUND + tagGroupRequest.getId());
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_GROUP_DELETE_START, okMessage = TAGS_GROUP_DELETE_OK)
    public ResponseEntity<Void> deleteTagGroup(long id) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(TAGS_GROUP_DELETE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var existing = tagService.getTagGroupById(id);
        if (existing == null) {
            throw new OxalateNotFoundException(TAGS_GROUP_DELETE_NOT_FOUND + id);
        }

        tagService.deleteTagGroup(id);
        return ResponseEntity.ok().build();
    }

    @Override
    @Audited(startMessage = TAGS_GROUP_GET_BY_TYPE_START, okMessage = TAGS_GROUP_GET_BY_TYPE_OK)
    public ResponseEntity<List<TagGroupResponse>> getTagGroupsByType(TagGroupEnum type) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            throw new OxalateUnauthorizedException(TAGS_GROUP_GET_BY_TYPE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var groups = tagService.getTagGroupsByType(type);
        return ResponseEntity.ok(groups);
    }

    // ---- Tags ----

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = TAGS_GET_ALL_START, okMessage = TAGS_GET_ALL_OK)
    public ResponseEntity<List<TagResponse>> getAllTags(Long groupId) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            throw new OxalateUnauthorizedException(TAGS_GET_ALL_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var tags = tagService.getAllTags(groupId);
        return ResponseEntity.ok(tags);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_GET_START, okMessage = TAGS_GET_OK)
    public ResponseEntity<TagResponse> getTagById(long id) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            throw new OxalateUnauthorizedException(TAGS_GET_ALL_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var tag = tagService.getTagById(id);
        if (tag == null) {
            throw new OxalateNotFoundException(TAGS_GET_NOT_FOUND + id);
        }

        return ResponseEntity.ok(tag);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_CREATE_START, okMessage = TAGS_CREATE_OK)
    public ResponseEntity<TagResponse> createTag(TagRequest tag) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(TAGS_CREATE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        try {
            var created = tagService.createTag(tag);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_UPDATE_START, okMessage = TAGS_UPDATE_OK)
    public ResponseEntity<TagResponse> updateTag(TagRequest tagRequest) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(TAGS_UPDATE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        try {
            var updated = tagService.updateTag(tagRequest);
            if (updated == null) {
                throw new OxalateNotFoundException(TAGS_UPDATE_NOT_FOUND + tagRequest.getId());
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = TAGS_DELETE_START, okMessage = TAGS_DELETE_OK)
    public ResponseEntity<Void> deleteTag(long id) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException(TAGS_DELETE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var existing = tagService.getTagById(id);
        if (existing == null) {
            throw new OxalateNotFoundException(TAGS_DELETE_NOT_FOUND + id);
        }

        tagService.deleteTag(id);
        return ResponseEntity.ok().build();
    }

    @Override
    @Audited(startMessage = TAGS_GET_BY_GROUP_TYPE_START, okMessage = TAGS_GET_BY_GROUP_TYPE_OK)
    public ResponseEntity<List<TagResponse>> getTagsByGroupType(TagGroupEnum type) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            throw new OxalateUnauthorizedException(TAGS_GET_BY_GROUP_TYPE_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        var tags = tagService.getTagsByGroupType(type);
        return ResponseEntity.ok(tags);
    }
}
