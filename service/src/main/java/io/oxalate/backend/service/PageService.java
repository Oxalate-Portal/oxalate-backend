package io.oxalate.backend.service;

import io.oxalate.backend.api.PageStatusEnum;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.request.PageGroupRequest;
import io.oxalate.backend.api.request.PageRequest;
import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageResponse;
import io.oxalate.backend.api.response.PageRoleAccessResponse;
import io.oxalate.backend.model.Page;
import io.oxalate.backend.model.PageGroup;
import io.oxalate.backend.model.PageGroupVersion;
import io.oxalate.backend.model.PageRoleAccess;
import io.oxalate.backend.model.PageVersion;
import io.oxalate.backend.repository.PageGroupRepository;
import io.oxalate.backend.repository.PageGroupVersionRepository;
import io.oxalate.backend.repository.PageRepository;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.PageVersionRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PageService {

    private final PageRepository pageRepository;
    private final PageGroupRepository pageGroupRepository;
    private final PageGroupVersionRepository pageGroupVersionRepository;
    private final PageRoleAccessRepository pageRoleAccessRepository;
    private final PageVersionRepository pageVersionRepository;

    private final long RESERVED_PAGE_GROUP_ID = 1;

    public PageResponse getPage(long pageId, Set<RoleEnum> roles, String language) {
        // Check if the page exists
        var optionalPage = pageRepository.findById(pageId);

        if (optionalPage.isEmpty()) {
            return null;
        }

        if (!optionalPage.get()
                         .getStatus()
                         .equals(PageStatusEnum.PUBLIC)) {
            log.info("Attempted to fetch page with ID {} which was not public {}", pageId, optionalPage.get()
                                                                                                       .getStatus());
            return null;
        }

        // Check if the language version exists
        var optionalPageVersion = pageVersionRepository.findByPageIdAndLanguage(pageId, language);

        if (optionalPageVersion.isEmpty()) {
            return null;
        }

        var page = optionalPage.get();
        var pageVersion = optionalPageVersion.get();

        // Check if the user has access to the page
        var permissionList = pageRoleAccessRepository.findByPageIdAndRoleIn(page.getId(), roles);

        if (permissionList.isEmpty()) {
            log.debug("User did not have any of the required roles {} to access page ID {}", roles, pageId);
            throw new AccessDeniedException("Access denied");
        }

        var permissionResponses = new HashSet<PageRoleAccessResponse>();
        for (var permission : permissionList) {
            // Add the role permissions to the page
            permissionResponses.add(permission.toResponse());
        }

        // With the language defined, we return only that language version of the page
        var pageResponse = page.toResponse();
        pageResponse.setPageVersions(List.of(pageVersion.toResponse()));
        pageResponse.setRolePermissions(permissionResponses);

        return pageResponse;
    }

    public List<PageGroupResponse> getPageGroups(String language, Set<RoleEnum> roles) {
        var pageGroupResponses = new ArrayList<PageGroupResponse>();

        var pageGroups = pageGroupRepository.findAllExceptId(RESERVED_PAGE_GROUP_ID);

        log.debug("Got list of page groups before filtering with lang {}: {}", language, pageGroups);

        for (var pageGroup : pageGroups) {
            populatePath(pageGroup, language);

            // Check that the user has access to the pages
            var pageListFiltered = new ArrayList<Page>();
            for (var page : pageGroup.getPages()) {
                var permissionList = pageRoleAccessRepository.findByPageIdAndRoleIn(page.getId(), roles);

                // In addition to access, the page must be public
                if (!permissionList.isEmpty() && page.getStatus()
                                                     .equals(PageStatusEnum.PUBLIC)) {
                    pageListFiltered.add(page);
                }
            }

            // If there are no pages, then we don't add the path
            if (pageListFiltered.isEmpty()) {
                continue;
            }

            // Update the page set with the filtered pages
            pageGroup.setPages(pageListFiltered);
            pageGroupResponses.add(pageGroup.toResponse());
        }

        log.debug("List of page groups after filtering: {}", pageGroupResponses);

        return pageGroupResponses;
    }

    // Methods used by the page management
    // Paths
    public List<PageGroupResponse> getAllPageGroups() {
        var pageGroupResponses = new ArrayList<PageGroupResponse>();

        var pageGroups = pageGroupRepository.findAll();

        for (var pageGroup : pageGroups) {
            // Add all the page group versions
            populatePath(pageGroup, null);
            pageGroupResponses.add(pageGroup.toResponse());
        }

        log.debug("List of navigationElements: {}", pageGroupResponses);
        return pageGroupResponses;
    }

    public PageGroupResponse getPageGroup(long pageGroupId) {
        var optionalPageGroup = pageGroupRepository.findById(pageGroupId);

        if (optionalPageGroup.isEmpty()) {
            return null;
        }

        var pageGroup = optionalPageGroup.get();

        // Populate the page group
        populatePath(pageGroup, null);

        return pageGroup.toResponse();
    }

    @Transactional
    public PageGroupResponse createPath(PageGroupRequest pathRequests) {
        if (!verifyPageGroupRequests(pathRequests)) {
            return null;
        }

        var pageGroup = PageGroup.of(pathRequests);
        var newPageGroup = pageGroupRepository.save(pageGroup);
        // Save all PageGroupVersions
        for (var pageGroupVersionRequest : pathRequests.getPageGroupVersions()) {
            var pageGroupVersion = PageGroupVersion.of(pageGroupVersionRequest);
            pageGroupVersion.setPageGroupId(newPageGroup.getId());
            pageGroupVersionRepository.save(pageGroupVersion);
        }

        populatePath(newPageGroup, null);

        return newPageGroup.toResponse();
    }

    @Transactional
    public PageGroupResponse updatePath(PageGroupRequest pageGroupRequest) {
        if (!verifyPageGroupRequests(pageGroupRequest)) {
            return null;
        }

        var optionalPageGroup = pageGroupRepository.findById(pageGroupRequest.getId());

        if (optionalPageGroup.isEmpty()) {
            log.warn("Requested to update non-existing page group: {}", pageGroupRequest);
            return null;
        }

        var pageGroup = optionalPageGroup.get();

        var existingPageGroupVersions = pageGroupVersionRepository.findAllByPageGroupIdOrderByLanguageAsc(pageGroupRequest.getId());
        // We update the ones we have
        for (var existingPageGroupVersion : existingPageGroupVersions) {
            // Find the page group in the list of request paths
            var newPageGroupVersionRequest = pageGroupRequest.getPageGroupVersions()
                                                             .stream()
                                                             .filter(path -> Objects.equals(path.getId(), existingPageGroupVersion.getId()))
                                                             .findFirst();

            if (newPageGroupVersionRequest.isPresent()) {
                // Update the page group
                existingPageGroupVersion.setTitle(newPageGroupVersionRequest.get()
                                                                            .getTitle());
                existingPageGroupVersion.setLanguage(newPageGroupVersionRequest.get()
                                                                               .getLanguage());
                pageGroupVersionRepository.save(existingPageGroupVersion);

                // Remove the request from the list
                pageGroupRequest.getPageGroupVersions()
                                .remove(newPageGroupVersionRequest.get());
            } else {
                // Delete the obsolete page group version
                pageGroupVersionRepository.delete(existingPageGroupVersion);
            }
        }

        // And if there is a new language entry, then we add it
        for (var pageGroupVersionRequest : pageGroupRequest.getPageGroupVersions()) {
            var newPageGroupVersion = PageGroupVersion.of(pageGroupVersionRequest);
            pageGroupVersionRepository.save(newPageGroupVersion);
        }

        var newPageGroup = pageGroupRepository.save(pageGroup);
        populatePath(newPageGroup, null);

        return newPageGroup.toResponse();
    }

    @Transactional
    public boolean deletePageGroup(long pageGroupId) {

        var optionalPageGroup = pageGroupRepository.findById(pageGroupId);

        if (optionalPageGroup.isEmpty()) {
            return false;
        }

        // Delete by the path, the cascade in pages and page_versions, but we need to clean up page_role_access manually
        var pages = pageRepository.findAllByPageGroupIdOrderByIdAsc(pageGroupId);

        for (var page : pages) {
            pageRoleAccessRepository.deleteAllByPageId(page.getId());
        }

        pageGroupRepository.deleteById(pageGroupId);

        return true;
    }

    // Pages
    public List<PageResponse> getPagesByPageGroupId(long pageGroupId) {
        var pageResponses = new ArrayList<PageResponse>();

        // First check that the path exists
        var pageGroups = pageGroupRepository.findAllById(pageGroupId);

        if (pageGroups.isEmpty()) {
            return pageResponses;
        }

        // Get the pages for the list of paths

        var pages = pageRepository.findAllByIdInOrderByIdAsc(pageGroups.stream()
                                                                       .map(PageGroup::getId)
                                                                       .toList());

        for (var page : pages) {
            pageResponses.add(page.toResponse());
        }

        return pageResponses;
    }

    public PageResponse getPageById(long pageId) {
        var optionalPage = pageRepository.findById(pageId);

        if (optionalPage.isEmpty()) {
            return null;
        }

        var page = optionalPage.get();
        populatePage(page, null);

        return page.toResponse();

    }

    @Transactional
    public PageResponse createPage(PageRequest pageRequest, long userId) {
        if (pageRequest == null || pageRequest.getPageVersions()
                                              .isEmpty()) {
            log.warn("PageRequest was null or empty");
            return null;
        }

        if (nonExistingPageGroup(pageRequest.getPageGroupId())) {
            return null;
        }

        var page = Page.of(pageRequest, userId);
        // Add page
        var newPage = pageRepository.save(page);

        for (var pageVersionRequest : pageRequest.getPageVersions()) {
            pageVersionRequest.setPageId(newPage.getId());
            var pageVersion = PageVersion.of(pageVersionRequest);
            pageVersionRepository.save(pageVersion);
        }

        for (var role : pageRequest.getRolePermissions()) {
            var pageRoleAccess = PageRoleAccess.builder()
                                               .role(role.getRole())
                                               .pageId(newPage.getId())
                                               .readPermission(role.isReadPermission())
                                               .writePermission(role.isWritePermission())
                                               .build();
            pageRoleAccessRepository.save(pageRoleAccess);
        }

        populatePage(newPage, null);
        return newPage.toResponse();
    }

    @Transactional
    public PageResponse updatePage(PageRequest pageRequest, Set<RoleEnum> roles, long userId) {

        if (pageRequest == null || pageRequest.getPageVersions()
                                              .isEmpty()) {
            log.error("PageRequest was null or empty");
            return null;
        }

        if (nonExistingPageGroup(pageRequest.getPageGroupId())) {
            return null;
        }

        var optionalPage = pageRepository.findById(pageRequest.getId());
        if (optionalPage.isEmpty()) {
            log.error("Page with ID {} was not found", pageRequest.getId());
            return null;
        }

        var page = optionalPage.get();

        // Check whether the user has write access role to the existing page
        var pageRoleAccessList = pageRoleAccessRepository.findByPageIdAndRoleInAndWritePermission(pageRequest.getId(), roles, true);
        log.debug("PageRoleAccess list: {}", pageRoleAccessList);

        // If the user does not have write permission, then we don't allow them to update the page
        if (pageRoleAccessList.isEmpty()) {
            log.debug("User did not have access to all the pages");
            throw new AccessDeniedException("Access denied");
        }

        // Get the list of all page versions, including those that will be deleted, from the database
        var existingPageVersions = pageVersionRepository.findAllByPageIdOrderByLanguage(pageRequest.getId());

        // Go through the existing page versions and either update them, or delete them
        for (var existingPageVersion : existingPageVersions) {
            // Find the page version in the list of request page versions
            var pageVersionRequest = pageRequest.getPageVersions()
                                                .stream()
                                                .filter(filterPage -> Objects.equals(filterPage.getId(), existingPageVersion.getId()))
                                                .findFirst();

            if (pageVersionRequest.isPresent()) {
                // Update the page version
                existingPageVersion.setTitle(pageVersionRequest.get()
                                                               .getTitle());
                existingPageVersion.setIngress(pageVersionRequest.get()
                                                                 .getIngress());
                existingPageVersion.setBody(pageVersionRequest.get()
                                                              .getBody());
                pageVersionRepository.save(existingPageVersion);
                // Remove the request from the list
                pageRequest.getPageVersions()
                           .remove(pageVersionRequest.get());
            } else {
                // Delete the obsolete page version
                pageVersionRepository.delete(existingPageVersion);
            }
        }

        // Then create the remaining page versions

        for (var pageVersionRequestToAdd : pageRequest.getPageVersions()) {
            var pageVersion = PageVersion.of(pageVersionRequestToAdd);
            pageVersionRepository.save(pageVersion);
        }

        // Update the page role accesses, this is done simply by removing the existing ones, and then adding the new ones
        pageRoleAccessRepository.deleteAllByPageId(pageRequest.getId());
        // Check that the assigned page role accesses are removed
        var pageRoleAccessListAfterDelete = pageRoleAccessRepository.findAllByPageId(pageRequest.getId());

        if (!pageRoleAccessListAfterDelete.isEmpty()) {
            log.error("PageRoleAccesses were not deleted for page ID {}", pageRequest.getId());
            throw new RuntimeException();
        }

        for (var permission : pageRequest.getRolePermissions()) {
            var newPageRoleAccess = PageRoleAccess.builder()
                                                  .role(permission.getRole())
                                                  .pageId(pageRequest.getId())
                                                  .readPermission(permission.isReadPermission())
                                                  .writePermission(permission.isWritePermission())
                                                  .build();
            pageRoleAccessRepository.save(newPageRoleAccess);
        }

        // Finally update the page
        page.setStatus(pageRequest.getStatus());
        page.setPageGroupId(pageRequest.getPageGroupId());
        page.setModifier(userId);
        page.setModifiedAt(Instant.now());
        page.setPageGroupId(pageRequest.getPageGroupId());
        var newPage = pageRepository.save(page);
        populatePage(newPage, null);
        return newPage.toResponse();
    }

    private boolean nonExistingPageGroup(long pageGroupId) {
        // Check that the page request is using an existing page group ID
        if (pageGroupId < 0) {
            log.error("PageRequest did not have a proper page group ID: {}", pageGroupId);
            return true;
        }

        var optionalPageGroup = pageGroupRepository.findById(pageGroupId);

        if (optionalPageGroup.isEmpty()) {
            log.error("PageGroup with ID {} was not found", pageGroupId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deletePage(long pageId) {
        // Check if page exists
        var optionalPage = pageRepository.findById(pageId);

        if (optionalPage.isEmpty()) {
            return false;
        }

        pageRepository.deleteAllById(pageId);

        return true;
    }

    // Private methods

    /**
     * Takes in a PageGroupRequests object and makes sure there are no duplicate languages
     *
     * @param pathRequests Request send by user
     * @return boolean if the request is correct
     */
    private boolean verifyPageGroupRequests(PageGroupRequest pathRequests) {
        var languageSet = new HashSet<String>();

        for (var path : pathRequests.getPageGroupVersions()) {
            if (languageSet.contains(path.getLanguage())) {
                return false;
            }

            languageSet.add(path.getLanguage());
        }

        return true;
    }

    /**
     * Takes in a language and a path ID and returns a list of pages with the specific language that belongs to that path
     *
     * @param language    Language of the page
     * @param pageGroupId ID of the path
     * @param roles       Roles of the user, used to check that the user has correct role to the page
     * @return List of pages
     */

    private List<Page> getPagesByPageGroupIdAndLanguage(String language, long pageGroupId, Set<RoleEnum> roles) {
        log.debug("Getting pages for path ID {} and language {}", pageGroupId, language);

        var pageListFiltered = new ArrayList<Page>();

        var pageList = pageRepository.findAllByPageGroupIdOrderByIdAsc(pageGroupId);

        log.debug("Got list of pages for path ID ({}): {}", pageGroupId, pageList);

        if (pageList.isEmpty()) {
            return pageListFiltered;
        }

        for (var page : pageList) {
            // First check if the language version is present
            log.debug("Searching for page version with page ID {} and language {}", page.getId(), language);
            var optionalPageVersion = pageVersionRepository.findByPageIdAndLanguage(page.getId(), language);

            if (optionalPageVersion.isPresent()) {
                // Then check that the user has access to the page
                log.debug("Searching for page role access with page ID {} and roles {}", page.getId(), roles);
                var permissionList = pageRoleAccessRepository.findByPageIdAndRoleIn(page.getId(), roles);

                if (!permissionList.isEmpty()) {
                    // We (re)populate the page versions list with the language version
                    page.setPageVersions(List.of(optionalPageVersion.get()));
                    pageListFiltered.add(page);
                }
            }
        }

        return pageListFiltered;
    }

    /**
     * Takes in a page and populates the page versions list with all the language versions as well as roles
     *
     * @param page Page to be populated
     */
    private void populatePage(Page page, String language) {
        // If language is defined, then only fetch that language version, if the language does not exist, then return without populating
        if (language != null) {
            var pageVersion = pageVersionRepository.findByPageIdAndLanguage(page.getId(), language);

            if (pageVersion.isEmpty()) {
                log.warn("PageVersion with language {} was not found for page ID {}", language, page.getId());
                return;
            }

            page.setPageVersions(List.of(pageVersion.get()));
        } else {
            // Get all the page versions
            var pageVersions = pageVersionRepository.findAllByPageIdOrderByLanguage(page.getId());
            page.setPageVersions(pageVersions);
        }

        // Get all the page role accesses
        var pageRoleAccesses = pageRoleAccessRepository.findAllByPageId(page.getId());
        page.setRolePermissions(pageRoleAccesses);
    }

    /**
     * Takes in a PageGroup and populates the path versions list with all the language versions as well as pages
     *
     * @param pageGroup PageGroup to be populated
     */
    private void populatePath(PageGroup pageGroup, String language) {
        // If language is defined, then only fetch that language version, if the language does not exist, then return without populating
        if (language != null) {
            var pageGroupVersion = pageGroupVersionRepository.findByPageGroupIdAndLanguage(pageGroup.getId(), language);

            if (pageGroupVersion.isEmpty()) {
                log.warn("PageGroupVersion with language {} was not found for path ID {}", language, pageGroup.getId());
                return;
            }

            pageGroup.setGroupVersions(List.of(pageGroupVersion.get()));
        } else {
            // Get all the path versions
            var pathVersions = pageGroupVersionRepository.findAllByPageGroupIdOrderByLanguageAsc(pageGroup.getId());
            pageGroup.setGroupVersions(pathVersions);
        }

        // Get all the pages
        var pages = pageRepository.findAllByPageGroupIdOrderByIdAsc(pageGroup.getId());
        // Populate the pages
        for (var page : pages) {
            populatePage(page, language);
        }

        pageGroup.setPages(pages);
    }
}
