package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import static io.oxalate.backend.api.AuditLevelEnum.WARN;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageResponse;
import io.oxalate.backend.api.response.PagedResponse;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_BLOGS_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_BLOGS_START;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_NAVIGATION_ELEMENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_NAVIGATION_ELEMENTS_START;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_START;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_UNAUTHORIZED;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.PageAPI;
import io.oxalate.backend.service.PageService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PageController implements PageAPI {

    private static final String AUDIT_NAME = "PageController";
    private final PageService pageService;
    private final AppEventPublisher appEventPublisher;

    @Override
    public ResponseEntity<List<PageGroupResponse>> getNavigationElements(String language, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var userRoles = AuthTools.getUserRoles();
        var auditUuid = appEventPublisher.publishAuditEvent(PAGES_GET_NAVIGATION_ELEMENTS_START, INFO, request, AUDIT_NAME, userId);

        log.debug("Called with language {}", language);
        var paths = pageService.getPageGroups(language, userRoles);

        appEventPublisher.publishAuditEvent(PAGES_GET_NAVIGATION_ELEMENTS_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(paths);
    }

    @Override
    public ResponseEntity<PagedResponse<PageResponse>> getBlogArticles(PagedRequest pagedRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var userRoles = AuthTools.getUserRoles();
        var auditUuid = appEventPublisher.publishAuditEvent(PAGES_GET_BLOGS_START, INFO, request, AUDIT_NAME, userId);

        log.debug("Fetch blogs with request {}", pagedRequest);

        var pagedResponse = pageService.getBlogArticles(pagedRequest, userRoles);

        appEventPublisher.publishAuditEvent(PAGES_GET_BLOGS_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(pagedResponse);
    }

    @Override
    public ResponseEntity<PageResponse> getPageById(long pageId, String language, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var userRoles = AuthTools.getUserRoles();
        var selectedLanguage = language == null ? AuthTools.getLanguage() : language;
        var auditUuid = appEventPublisher.publishAuditEvent(PAGES_GET_PAGE_START + pageId, INFO, request, AUDIT_NAME, userId);

        log.debug("Called with page ID {}", pageId);

        try {
            var pageResponse = pageService.getPage(pageId, userRoles, selectedLanguage);

            if (pageResponse == null) {
                appEventPublisher.publishAuditEvent(PAGES_GET_PAGE_NOT_FOUND, WARN, request, AUDIT_NAME, userId, auditUuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(null);
            }

            appEventPublisher.publishAuditEvent(PAGES_GET_PAGE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.status(HttpStatus.OK)
                                 .body(pageResponse);
        } catch (AccessDeniedException e) {
            appEventPublisher.publishAuditEvent(PAGES_GET_PAGE_UNAUTHORIZED, WARN, request, AUDIT_NAME, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(null);
        }
    }
}
