package io.oxalate.backend.controller;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.request.PageGroupRequest;
import io.oxalate.backend.api.request.PageRequest;
import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
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
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.rest.PageManagementAPI;
import io.oxalate.backend.service.PageService;
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
@AuditSource("PageManagementController")
public class PageManagementController implements PageManagementAPI {

    private final PageService pageService;

    // Paths
    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_START, okMessage = MGMNT_PAGES_GET_NAVIGATION_ELEMENTS_OK)
    public ResponseEntity<List<PageGroupResponse>> getPageGroups() {
        var navigationElements = pageService.getAllPageGroups();
        return ResponseEntity.status(HttpStatus.OK)
                             .body(navigationElements);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_GET_PAGE_GROUP_START, okMessage = MGMNT_PAGES_GET_PAGE_GROUP_OK)
    public ResponseEntity<PageGroupResponse> getPageGroup(long pageGroupId) {
        var navigationElements = pageService.getPageGroup(pageGroupId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(navigationElements);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_CREATE_GROUP_START, okMessage = MGMNT_PAGES_CREATE_GROUP_OK)
    public ResponseEntity<PageGroupResponse> createPageGroup(PageGroupRequest pathRequests) {
        var response = pageService.createPath(pathRequests);

        if (response == null) {
            throw new OxalateValidationException(AuditLevelEnum.ERROR, MGMNT_PAGES_CREATE_GROUP_NONE_CREATED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_UPDATE_PAGE_GROUP_START, okMessage = MGMNT_PAGES_UPDATE_PAGE_GROUP_OK)
    public ResponseEntity<PageGroupResponse> updatePageGroup(PageGroupRequest pathRequests) {
        var navigationElementResponses = pageService.updatePageGroup(pathRequests);

        if (navigationElementResponses == null) {
            throw new OxalateValidationException(AuditLevelEnum.ERROR, MGMNT_PAGES_UPDATE_PAGE_GROUP_NONE_UPDATED + pathRequests,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(navigationElementResponses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_CLOSE_PAGE_GROUP_START, okMessage = MGMNT_PAGES_CLOSE_PAGE_GROUP_OK)
    public ResponseEntity<HttpStatus> closePageGroup(long pageGroupId) {
        if (!pageService.closePageGroup(pageGroupId)) {
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, MGMNT_PAGES_CLOSE_PAGE_GROUP_NOT_FOUND + pageGroupId, HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(null);
    }

    // Pages
    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_GET_PAGES_START, okMessage = MGMNT_PAGES_GET_PAGES_OK)
    public ResponseEntity<List<PageResponse>> getPagesByPageGroupId(long pageGroupId) {
        var pages = pageService.getPagesByPageGroupId(pageGroupId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(pages);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_GET_PAGE_START, okMessage = MGMNT_PAGES_GET_PAGE_OK)
    public ResponseEntity<PageResponse> getPageById(long pageId) {
        var pageResponse = pageService.getPageById(pageId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(pageResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_CREATE_PAGE_START, okMessage = MGMNT_PAGES_CREATE_PAGE_OK)
    public ResponseEntity<PageResponse> createPage(PageRequest pageRequest) {
        var userId = AuthTools.getCurrentUserId();
        var newPages = pageService.createPage(pageRequest, userId);

        if (newPages == null) {
            throw new OxalateValidationException(AuditLevelEnum.ERROR, MGMNT_PAGES_CREATE_PAGE_NONE_CREATED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(newPages);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_UPDATE_PAGE_START, okMessage = MGMNT_PAGES_UPDATE_PAGE_OK)
    public ResponseEntity<PageResponse> updatePage(PageRequest pageRequest) {
        var userId = AuthTools.getCurrentUserId();
        var userRoles = AuthTools.getUserRoles();
        var newPageResponse = pageService.updatePage(pageRequest, userRoles, userId);

        if (newPageResponse == null) {
            throw new OxalateValidationException(AuditLevelEnum.ERROR, MGMNT_PAGES_CREATE_PAGE_NONE_UPDATED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.status(HttpStatus.OK)
                             .body(newPageResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Audited(startMessage = MGMNT_PAGES_CLOSE_PAGE_START, okMessage = MGMNT_PAGES_CLOSE_PAGE_OK)
    public ResponseEntity<HttpStatus> closePage(long pageId) {
        if (!pageService.closePage(pageId)) {
            throw new OxalateNotFoundException(AuditLevelEnum.ERROR, MGMNT_PAGES_CLOSE_PAGE_NOT_FOUND + pageId, HttpStatus.BAD_REQUEST);
        }

        return null;
    }
}
