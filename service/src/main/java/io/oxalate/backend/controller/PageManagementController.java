package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.ERROR;
import static io.oxalate.backend.api.AuditLevel.INFO;
import io.oxalate.backend.api.request.PageGroupRequest;
import io.oxalate.backend.api.request.PageRequest;
import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageResponse;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CLOSE_PAGE_GROUP_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CLOSE_PAGE_GROUP_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CLOSE_PAGE_GROUP_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CLOSE_PAGE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CLOSE_PAGE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CLOSE_PAGE_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CREATE_GROUP_NONE_CREATED;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CREATE_GROUP_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CREATE_GROUP_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CREATE_PAGE_NONE_CREATED;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CREATE_PAGE_NONE_UPDATED;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CREATE_PAGE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_CREATE_PAGE_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_PAGES_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_PAGES_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_PAGE_GROUP_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_PAGE_GROUP_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_PAGE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_GET_PAGE_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_UPDATE_PAGE_GROUP_NONE_UPDATED;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_UPDATE_PAGE_GROUP_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_UPDATE_PAGE_GROUP_START;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_UPDATE_PAGE_OK;
import static io.oxalate.backend.events.AppAuditMessages.MGMNT_PAGES_UPDATE_PAGE_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.PageManagementAPI;
import io.oxalate.backend.service.PageService;
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
public class PageManagementController implements PageManagementAPI {

    private static final String AUDIT_NAME = "PageManagementController";
    private final PageService pageService;
    private final AppEventPublisher appEventPublisher;

    // Paths
    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<PageGroupResponse>> getPageGroups(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_START, INFO, request, AUDIT_NAME, userId);

        var navigationElements = pageService.getAllPageGroups();
        appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(navigationElements);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<PageGroupResponse> getPageGroup(long pageGroupId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_PAGE_GROUP_START, INFO, request, AUDIT_NAME, userId);

        var navigationElements = pageService.getPageGroup(pageGroupId);
        appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_PAGE_GROUP_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(navigationElements);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PageGroupResponse> createPageGroup(PageGroupRequest pathRequests, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_CREATE_GROUP_START, INFO, request, AUDIT_NAME, userId);

        var response = pageService.createPath(pathRequests);

        if (response == null) {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_CREATE_GROUP_NONE_CREATED, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(MGMNT_PAGES_CREATE_GROUP_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PageGroupResponse> updatePageGroup(PageGroupRequest pathRequests, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_UPDATE_PAGE_GROUP_START, INFO, request, AUDIT_NAME, userId);

        var navigationElementResponses = pageService.updatePageGroup(pathRequests);

        if (navigationElementResponses == null) {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_UPDATE_PAGE_GROUP_NONE_UPDATED + pathRequests, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(MGMNT_PAGES_UPDATE_PAGE_GROUP_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(navigationElementResponses);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<HttpStatus> closePageGroup(long pageGroupId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_CLOSE_PAGE_GROUP_START, INFO, request, AUDIT_NAME, userId);

        if (pageService.closePageGroup(pageGroupId)) {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_CLOSE_PAGE_GROUP_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(null);
        } else {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_CLOSE_PAGE_GROUP_NOT_FOUND + pageGroupId, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }
    }

    // Pages
    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<PageResponse>> getPagesByPageGroupId(long pageGroupId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_PAGES_START, INFO, request, AUDIT_NAME, userId);

        var pages = pageService.getPagesByPageGroupId(pageGroupId);
        appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_PAGES_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(pages);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<PageResponse> getPageById(long pageId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_PAGE_START, INFO, request, AUDIT_NAME, userId);

        var pageResponse = pageService.getPageById(pageId);
        appEventPublisher.publishAuditEvent(MGMNT_PAGES_GET_PAGE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(pageResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<PageResponse> createPage(PageRequest pageRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_CREATE_PAGE_START, INFO, request, AUDIT_NAME, userId);

        var newPages = pageService.createPage(pageRequest, userId);

        if (newPages == null) {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_CREATE_PAGE_NONE_CREATED, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(MGMNT_PAGES_CREATE_PAGE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(newPages);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<PageResponse> updatePage(PageRequest pageRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var userRoles = AuthTools.getUserRoles();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_UPDATE_PAGE_START, INFO, request, AUDIT_NAME, userId);

        var newPageResponse = pageService.updatePage(pageRequest, userRoles, userId);

        if (newPageResponse == null) {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_CREATE_PAGE_NONE_UPDATED, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }

        appEventPublisher.publishAuditEvent(MGMNT_PAGES_UPDATE_PAGE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(newPageResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<HttpStatus> closePage(long pageId, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(MGMNT_PAGES_CLOSE_PAGE_START, INFO, request, AUDIT_NAME, userId);

        if (pageService.closePage(pageId)) {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_CLOSE_PAGE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
            return null;

        } else {
            appEventPublisher.publishAuditEvent(MGMNT_PAGES_CLOSE_PAGE_NOT_FOUND + pageId, ERROR, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null);
        }
    }
}
