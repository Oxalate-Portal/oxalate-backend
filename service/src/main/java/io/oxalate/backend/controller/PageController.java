package io.oxalate.backend.controller;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageResponse;
import io.oxalate.backend.api.response.PagedResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_BLOGS_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_BLOGS_START;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_NAVIGATION_ELEMENTS_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_NAVIGATION_ELEMENTS_START;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_START;
import static io.oxalate.backend.events.AppAuditMessages.PAGES_GET_PAGE_UNAUTHORIZED;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.rest.PageAPI;
import io.oxalate.backend.service.PageService;
import io.oxalate.backend.tools.AuthTools;
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
@AuditSource("PageController")
public class PageController implements PageAPI {

    private final PageService pageService;

    @Override
    @Audited(startMessage = PAGES_GET_NAVIGATION_ELEMENTS_START, okMessage = PAGES_GET_NAVIGATION_ELEMENTS_OK)
    public ResponseEntity<List<PageGroupResponse>> getNavigationElements(String language) {
        var userRoles = AuthTools.getUserRoles();
        log.debug("Called with language {}", language);
        var paths = pageService.getPageGroups(language, userRoles);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(paths);
    }

    @Override
    @Audited(startMessage = PAGES_GET_BLOGS_START, okMessage = PAGES_GET_BLOGS_OK)
    public ResponseEntity<PagedResponse<PageResponse>> getBlogArticles(PagedRequest pagedRequest) {
        var userRoles = AuthTools.getUserRoles();
        log.debug("Fetch blogs with request {}", pagedRequest);
        var pagedResponse = pageService.getBlogArticles(pagedRequest, userRoles);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(pagedResponse);
    }

    @Override
    @Audited(startMessage = PAGES_GET_PAGE_START, okMessage = PAGES_GET_PAGE_OK)
    public ResponseEntity<PageResponse> getPageById(long pageId, String language) {
        var userRoles = AuthTools.getUserRoles();
        var selectedLanguage = language == null ? AuthTools.getLanguage() : language;
        log.debug("Called with page ID {}", pageId);

        try {
            var pageResponse = pageService.getPage(pageId, userRoles, selectedLanguage);

            if (pageResponse == null) {
                throw new OxalateNotFoundException(PAGES_GET_PAGE_NOT_FOUND);
            }

            return ResponseEntity.status(HttpStatus.OK)
                                 .body(pageResponse);
        } catch (AccessDeniedException e) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.WARN, PAGES_GET_PAGE_UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }
}
