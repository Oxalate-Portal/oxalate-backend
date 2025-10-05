package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.TagGroupEnum;
import io.oxalate.backend.api.request.TagGroupRequest;
import io.oxalate.backend.api.request.TagRequest;
import io.oxalate.backend.api.response.TagGroupResponse;
import io.oxalate.backend.api.response.TagResponse;
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
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.TagAPI;
import io.oxalate.backend.service.TagService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
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
public class TagController implements TagAPI {

    private final TagService tagService;
    private final AppEventPublisher appEventPublisher;
    private static final String AUDIT_NAME = "TagController";

    // ---- Tag Groups ----

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TagGroupResponse>> getAllTagGroups(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_ALL_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var groups = tagService.getAllTagGroups();
        appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(groups);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagGroupResponse> getTagGroupById(long id, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_START + id, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_ALL_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var group = tagService.getTagGroupById(id);
        if (group == null) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_NOT_FOUND + id, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.notFound().build();
        }

        appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_OK + id, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(group);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagGroupResponse> createTagGroup(TagGroupRequest tagGroupRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GROUP_CREATE_START, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_CREATE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var created = tagService.createTagGroup(tagGroupRequest);
            appEventPublisher.publishAuditEvent(TAGS_GROUP_CREATE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagGroupResponse> updateTagGroup(TagGroupRequest tagGroupRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GROUP_UPDATE_START + tagGroupRequest.getId(), INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_UPDATE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var updated = tagService.updateTagGroup(tagGroupRequest);
            if (updated == null) {
                appEventPublisher.publishAuditEvent(TAGS_GROUP_UPDATE_NOT_FOUND + tagGroupRequest.getId(), INFO, request, AUDIT_NAME, userId, auditUuid);
                return ResponseEntity.notFound().build();
            }
            appEventPublisher.publishAuditEvent(TAGS_GROUP_UPDATE_OK + tagGroupRequest.getId(), INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTagGroup(long id, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GROUP_DELETE_START + id, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_DELETE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var existing = tagService.getTagGroupById(id);
        if (existing == null) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_DELETE_NOT_FOUND + id, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.notFound().build();
        }

        tagService.deleteTagGroup(id);
        appEventPublisher.publishAuditEvent(TAGS_GROUP_DELETE_OK + id, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<TagGroupResponse>> getTagGroupsByType(TagGroupEnum type, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_BY_TYPE_START + type, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_BY_TYPE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .build();
        }

        var groups = tagService.getTagGroupsByType(type);
        appEventPublisher.publishAuditEvent(TAGS_GROUP_GET_BY_TYPE_OK + type, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(groups);
    }

    // ---- Tags ----

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<TagResponse>> getAllTags(Long groupId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(TAGS_GET_ALL_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var tags = tagService.getAllTags(groupId);
        appEventPublisher.publishAuditEvent(TAGS_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(tags);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagResponse> getTagById(long id, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GET_START + id, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(TAGS_GET_ALL_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var tag = tagService.getTagById(id);
        if (tag == null) {
            appEventPublisher.publishAuditEvent(TAGS_GET_NOT_FOUND + id, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.notFound().build();
        }

        appEventPublisher.publishAuditEvent(TAGS_GET_OK + id, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(tag);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagResponse> createTag(TagRequest tag, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_CREATE_START, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(TAGS_CREATE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var created = tagService.createTag(tag);
            appEventPublisher.publishAuditEvent(TAGS_CREATE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagResponse> updateTag(TagRequest tagRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_UPDATE_START + tagRequest.getId(), INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(TAGS_UPDATE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var updated = tagService.updateTag(tagRequest);
            if (updated == null) {
                appEventPublisher.publishAuditEvent(TAGS_UPDATE_NOT_FOUND + tagRequest.getId(), INFO, request, AUDIT_NAME, userId, auditUuid);
                return ResponseEntity.notFound().build();
            }
            appEventPublisher.publishAuditEvent(TAGS_UPDATE_OK + tagRequest.getId(), INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTag(long id, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_DELETE_START + id, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(TAGS_DELETE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var existing = tagService.getTagById(id);
        if (existing == null) {
            appEventPublisher.publishAuditEvent(TAGS_DELETE_NOT_FOUND + id, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.notFound().build();
        }

        tagService.deleteTag(id);
        appEventPublisher.publishAuditEvent(TAGS_DELETE_OK + id, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<TagResponse>> getTagsByGroupType(TagGroupEnum type, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(TAGS_GET_BY_GROUP_TYPE_START + type, INFO, request, AUDIT_NAME, userId);

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN, ROLE_ORGANIZER)) {
            appEventPublisher.publishAuditEvent(TAGS_GET_BY_GROUP_TYPE_UNAUTHORIZED, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .build();
        }

        var tags = tagService.getTagsByGroupType(type);
        appEventPublisher.publishAuditEvent(TAGS_GET_BY_GROUP_TYPE_OK + type, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(tags);
    }
}
