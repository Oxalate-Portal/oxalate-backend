package io.oxalate.backend.controller;

import io.oxalate.backend.api.PageStatusEnum;
import static io.oxalate.backend.api.PortalConfigEnum.FILES;
import static io.oxalate.backend.api.PortalConfigEnum.FileConfigEnum.DIVE_FILES_SUPPORTED;
import static io.oxalate.backend.api.PortalConfigEnum.FileConfigEnum.DOCUMENTS_SUPPORTED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.model.Certificate;
import io.oxalate.backend.model.Page;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.AvatarFile;
import io.oxalate.backend.model.filetransfer.CertificateFile;
import io.oxalate.backend.model.filetransfer.DiveFile;
import io.oxalate.backend.model.filetransfer.DocumentFile;
import io.oxalate.backend.model.filetransfer.PageFile;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.PageRepository;
import io.oxalate.backend.repository.filetransfer.AvatarFileRepository;
import io.oxalate.backend.repository.filetransfer.CertificateDocumentRepository;
import io.oxalate.backend.repository.filetransfer.DiveFileRepository;
import io.oxalate.backend.repository.filetransfer.DocumentFileRepository;
import io.oxalate.backend.repository.filetransfer.PageFileRepository;
import io.oxalate.backend.service.PortalConfigurationService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paged file listings under {@code GET /api/files/*}.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileTransferControllerRTC extends PagedRestTestSupport {

    private static final String AVATAR_FILES_ENDPOINT = "/api/files/avatars";
    private static final String CERTIFICATE_FILES_ENDPOINT = "/api/files/certificates";
    private static final String DIVE_FILES_ENDPOINT = "/api/files/dive-files";
    private static final String DOCUMENT_FILES_ENDPOINT = "/api/files/documents";
    private static final String PAGE_FILES_ENDPOINT = "/api/files/page-files";
    private static final long BLOG_PAGE_GROUP_ID = 3L;

    @Autowired
    private AvatarFileRepository avatarFileRepository;
    @Autowired
    private CertificateDocumentRepository certificateDocumentRepository;
    @Autowired
    private CertificateRepository certificateRepository;
    @Autowired
    private DiveFileRepository diveFileRepository;
    @Autowired
    private DocumentFileRepository documentFileRepository;
    @Autowired
    private PageFileRepository pageFileRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private PortalConfigurationService portalConfigurationService;

    private final List<Runnable> cleanups = new ArrayList<>();
    private String marker;
    private User adminUser;
    private User nonAdminUser;
    private User otherUser;
    private String adminJwt;
    private String nonAdminJwt;

    @BeforeEach
    void setUp() {
        marker = marker();
        setFilesConfiguration(DOCUMENTS_SUPPORTED.key, "true");
        setFilesConfiguration(DIVE_FILES_SUPPORTED.key, "true");

        adminUser = createUser("Admin", marker + "Admin", RoleEnum.ROLE_ADMIN);
        nonAdminUser = createUser("Nina", marker + "Member", RoleEnum.ROLE_USER);
        otherUser = createUser("Otto", marker + "Other", RoleEnum.ROLE_USER);

        adminJwt = jwtFor(adminUser, RoleEnum.ROLE_ADMIN);
        nonAdminJwt = jwtFor(nonAdminUser, RoleEnum.ROLE_USER);
    }

    @Override
    protected void cleanUpFixtures() {
        // Files reference users, certificates and pages, so undo in reverse creation order
        for (var i = cleanups.size() - 1; i >= 0; i--) {
            cleanups.get(i)
                    .run();
        }

        cleanups.clear();
    }

    // ------------------------------------------------------------------
    // Documents
    // ------------------------------------------------------------------

    @Test
    void findAllDocumentFilesAdminWithCreatorIdSelfReturnsOnlyOwnDocumentsOk() throws Exception {
        var adminDocument = createDocumentFile(adminUser, "admin-self.pdf");
        createDocumentFile(otherUser, "admin-other.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT).queryParam("creator_id", String.valueOf(adminUser.getId()))
                                                    .queryParam("search", marker)
                                                    .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].id", is(adminDocument.getId()
                                                                     .intValue())))
               .andExpect(jsonPath("$.content[0].url", endsWith("/api/files/documents/" + adminDocument.getId())));
    }

    @Test
    void findAllDocumentFilesAdminWithCreatorIdOtherReturnsOnlyRequestedCreatorDocumentsOk() throws Exception {
        createDocumentFile(adminUser, "admin.pdf");
        var otherDocument = createDocumentFile(otherUser, "other.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT).queryParam("creator_id", String.valueOf(otherUser.getId()))
                                                    .queryParam("search", marker)
                                                    .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.content[0].id", is(otherDocument.getId()
                                                                     .intValue())));
    }

    @Test
    void findAllDocumentFilesAdminWithoutCreatorIdPagesAllDocumentsOk() throws Exception {
        createDocumentFile(adminUser, "a.pdf");
        createDocumentFile(otherUser, "b.pdf");
        createDocumentFile(nonAdminUser, "c.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT).queryParam("search", marker)
                                                    .queryParam("size", "2")
                                                    .queryParam("sort_by", "filename")
                                                    .queryParam("direction", "ASC")
                                                    .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(2)))
               .andExpect(jsonPath("$.page", is(0)))
               .andExpect(jsonPath("$.size", is(2)))
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)))
               .andExpect(jsonPath("$.empty", is(false)))
               .andExpect(jsonPath("$.content[0].filename", is(marker + "-a.pdf")));
    }

    @Test
    void findAllDocumentFilesNonAdminWithCreatorIdSelfReturnsOnlyOwnDocumentsOk() throws Exception {
        var ownDocument = createDocumentFile(nonAdminUser, "self.pdf");
        createDocumentFile(otherUser, "someone-else.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT).queryParam("creator_id", String.valueOf(nonAdminUser.getId()))
                                                    .queryParam("search", marker)
                                                    .cookie(new Cookie(JWT_TOKEN, nonAdminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.content[0].id", is(ownDocument.getId()
                                                                   .intValue())));
    }

    @Test
    void findAllDocumentFilesNonAdminWithoutCreatorIdIsLimitedToOwnDocumentsOk() throws Exception {
        var ownDocument = createDocumentFile(nonAdminUser, "self.pdf");
        createDocumentFile(otherUser, "someone-else.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT).queryParam("search", marker)
                                                    .cookie(new Cookie(JWT_TOKEN, nonAdminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].id", is(ownDocument.getId()
                                                                   .intValue())));
    }

    @Test
    void findAllDocumentFilesNonAdminWithCreatorIdOtherFail() throws Exception {
        createDocumentFile(nonAdminUser, "self.pdf");
        createDocumentFile(otherUser, "other.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT).queryParam("creator_id", String.valueOf(otherUser.getId()))
                                                    .cookie(new Cookie(JWT_TOKEN, nonAdminJwt)))
               .andExpect(status().isForbidden());
    }

    @Test
    void findAllDocumentFilesDisabledReturnsEmptyPageOk() throws Exception {
        createDocumentFile(adminUser, "hidden.pdf");
        setFilesConfiguration(DOCUMENTS_SUPPORTED.key, "false");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT).queryParam("size", "4")
                                                    .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(0)))
               .andExpect(jsonPath("$.size", is(4)))
               .andExpect(jsonPath("$.total_elements", is(0)))
               .andExpect(jsonPath("$.empty", is(true)));
    }

    // ------------------------------------------------------------------
    // Avatars
    // ------------------------------------------------------------------

    @Test
    void findAllAvatarFilesPageShapeAndSortOk() throws Exception {
        createAvatarFile(adminUser, "a.png", Instant.now()
                                                    .minus(3, ChronoUnit.HOURS));
        createAvatarFile(nonAdminUser, "b.png", Instant.now()
                                                       .minus(2, ChronoUnit.HOURS));
        createAvatarFile(otherUser, "c.png", Instant.now()
                                                    .minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get(AVATAR_FILES_ENDPOINT).queryParam("search", marker)
                                                  .queryParam("size", "2")
                                                  .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(2)))
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)))
               // Newest first by default
               .andExpect(jsonPath("$.content[0].filename", is(marker + "-c.png")));

        mockMvc.perform(get(AVATAR_FILES_ENDPOINT).queryParam("search", marker)
                                                  .queryParam("sort_by", "creator")
                                                  .queryParam("direction", "ASC")
                                                  .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               // creator sorts by the creator's last name: ...Admin, ...Member, ...Other
               .andExpect(jsonPath("$.content[0].filename", is(marker + "-a.png")))
               .andExpect(jsonPath("$.content[2].filename", is(marker + "-c.png")));
    }

    @Test
    void findAllAvatarFilesSearchByCreatorNameAndForbiddenSortOk() throws Exception {
        createAvatarFile(adminUser, "a.png", Instant.now());
        createAvatarFile(nonAdminUser, "b.png", Instant.now());

        mockMvc.perform(get(AVATAR_FILES_ENDPOINT).queryParam("search", marker + "Member")
                                                  .queryParam("sort_by", "file_checksum")
                                                  .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(1)))
               .andExpect(jsonPath("$.content[0].filename", is(marker + "-b.png")));
    }

    // ------------------------------------------------------------------
    // Certificates
    // ------------------------------------------------------------------

    @Test
    void findAllCertificateFilesPageShapeOk() throws Exception {
        var first = createCertificateFile(nonAdminUser, "padi.jpg");
        var second = createCertificateFile(otherUser, "cmas.jpg");

        mockMvc.perform(get(CERTIFICATE_FILES_ENDPOINT).queryParam("search", marker)
                                                       .queryParam("size", "1")
                                                       .queryParam("sort_by", "certificate_id")
                                                       .queryParam("direction", "DESC")
                                                       .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.total_elements", is(2)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.last", is(false)))
               .andExpect(jsonPath("$.content[0].certificate_id", is((int) second.getCertificate()
                                                                                 .getId())))
               .andExpect(jsonPath("$.content[0].url", endsWith("/api/files/certificates/" + second.getCertificate()
                                                                                                    .getId())));

        mockMvc.perform(get(CERTIFICATE_FILES_ENDPOINT).queryParam("search", marker)
                                                       .queryParam("sort_by", "certificate.user_id")
                                                       .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(2)))
               // Default order is createdAt DESC, so the later upload comes first
               .andExpect(jsonPath("$.content[0].id", is(second.getId()
                                                              .intValue())))
               .andExpect(jsonPath("$.content[1].id", is(first.getId()
                                                             .intValue())));
    }

    // ------------------------------------------------------------------
    // Dive files
    // ------------------------------------------------------------------

    @Test
    void findAllDiveFilesFilterByEventAndPageShapeOk() throws Exception {
        createDiveFile(nonAdminUser, 1001L, "plan-a.pdf");
        createDiveFile(nonAdminUser, 1001L, "plan-b.pdf");
        createDiveFile(otherUser, 1002L, "plan-c.pdf");

        mockMvc.perform(get(DIVE_FILES_ENDPOINT).queryParam("search", marker)
                                                .queryParam("event_id", "1001")
                                                .queryParam("size", "1")
                                                .queryParam("sort_by", "filename")
                                                .queryParam("direction", "ASC")
                                                .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.total_elements", is(2)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)))
               .andExpect(jsonPath("$.content[0].filename", is(marker + "-plan-a.pdf")))
               .andExpect(jsonPath("$.content[0].event_id", is(1001)));

        mockMvc.perform(get(DIVE_FILES_ENDPOINT).queryParam("search", marker)
                                                .queryParam("sort_by", "dive_group.owner")
                                                .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)));
    }

    @Test
    void findAllDiveFilesDisabledReturnsEmptyPageOk() throws Exception {
        createDiveFile(nonAdminUser, 1001L, "plan-a.pdf");
        setFilesConfiguration(DIVE_FILES_SUPPORTED.key, "false");

        mockMvc.perform(get(DIVE_FILES_ENDPOINT).cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(0)))
               .andExpect(jsonPath("$.total_elements", is(0)))
               .andExpect(jsonPath("$.empty", is(true)));
    }

    // ------------------------------------------------------------------
    // Page files
    // ------------------------------------------------------------------

    @Test
    void findAllPageFilesPageShapeAndSortOk() throws Exception {
        var page = createPage();
        createPageFile(adminUser, page, "en", "one.png");
        createPageFile(adminUser, page, "fi", "two.png");
        createPageFile(adminUser, page, "sv", "three.png");

        mockMvc.perform(get(PAGE_FILES_ENDPOINT).queryParam("search", marker)
                                                .queryParam("size", "2")
                                                .queryParam("sort_by", "language")
                                                .queryParam("direction", "ASC")
                                                .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(2)))
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.total_pages", is(2)))
               .andExpect(jsonPath("$.first", is(true)))
               .andExpect(jsonPath("$.last", is(false)))
               .andExpect(jsonPath("$.content[0].language", is("en")))
               .andExpect(jsonPath("$.content[0].page_id", is(page.getId()
                                                                .intValue())))
               .andExpect(jsonPath("$.content[0].url", endsWith("/api/files/page-files/" + page.getId() + "/en/" + marker + "-one.png")));

        mockMvc.perform(get(PAGE_FILES_ENDPOINT).queryParam("search", marker)
                                                .queryParam("sort_by", "creator.password")
                                                .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)));
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private DocumentFile createDocumentFile(User creator, String filename) {
        var documentFile = documentFileRepository.save(DocumentFile.builder()
                                                                   .fileName(marker + "-" + filename)
                                                                   .mimeType("application/pdf")
                                                                   .fileSize(1024)
                                                                   .fileChecksum("checksum-" + filename)
                                                                   .creator(creator)
                                                                   .createdAt(Instant.now())
                                                                   .status(UploadStatusEnum.UPLOADED)
                                                                   .build());
        cleanups.add(() -> documentFileRepository.deleteById(documentFile.getId()));
        return documentFile;
    }

    private void createAvatarFile(User creator, String filename, Instant createdAt) {
        var avatarFile = avatarFileRepository.save(AvatarFile.builder()
                                                             .fileName(marker + "-" + filename)
                                                             .mimeType("image/png")
                                                             .fileSize(512)
                                                             .fileChecksum("checksum-" + filename)
                                                             .creator(creator)
                                                             .createdAt(createdAt)
                                                             .build());
        cleanups.add(() -> avatarFileRepository.deleteById(avatarFile.getId()));
    }

    private CertificateFile createCertificateFile(User creator, String filename) {
        var certificate = certificateRepository.save(Certificate.builder()
                                                                .userId(creator.getId())
                                                                .organization("Org " + marker)
                                                                .certificateName("Cert " + filename)
                                                                .certificateId("cert-" + System.nanoTime())
                                                                .build());
        cleanups.add(() -> certificateRepository.deleteById(certificate.getId()));
        var certificateFile = certificateDocumentRepository.save(CertificateFile.builder()
                                                                                .fileName(marker + "-" + filename)
                                                                                .mimeType("image/jpeg")
                                                                                .fileSize(2048)
                                                                                .fileChecksum("checksum-" + filename)
                                                                                .creator(creator)
                                                                                .createdAt(Instant.now())
                                                                                .certificate(certificate)
                                                                                .build());
        cleanups.add(() -> certificateDocumentRepository.deleteById(certificateFile.getId()));
        return certificateFile;
    }

    private void createDiveFile(User creator, long eventId, String filename) {
        var diveFile = diveFileRepository.save(DiveFile.builder()
                                                       .fileName(marker + "-" + filename)
                                                       .mimeType("application/pdf")
                                                       .fileSize(4096)
                                                       .fileChecksum("checksum-" + filename)
                                                       .creator(creator)
                                                       .createdAt(Instant.now())
                                                       .eventId(eventId)
                                                       .diveGroupId(1L)
                                                       .status(UploadStatusEnum.UPLOADED)
                                                       .build());
        cleanups.add(() -> diveFileRepository.deleteById(diveFile.getId()));
    }

    private Page createPage() {
        var page = pageRepository.save(Page.builder()
                                           .pageGroupId(BLOG_PAGE_GROUP_ID)
                                           .status(PageStatusEnum.PUBLISHED)
                                           .creator(adminUser.getId())
                                           .createdAt(Instant.now())
                                           .build());
        cleanups.add(() -> pageRepository.deleteById(page.getId()));
        return page;
    }

    private void createPageFile(User creator, Page page, String language, String filename) {
        var pageFile = pageFileRepository.save(PageFile.builder()
                                                       .fileName(marker + "-" + filename)
                                                       .mimeType("image/png")
                                                       .fileSize(256)
                                                       .fileChecksum("checksum-" + filename)
                                                       .creator(creator)
                                                       .createdAt(Instant.now())
                                                       .language(language)
                                                       .pageId(page.getId())
                                                       .status(UploadStatusEnum.PUBLISHED)
                                                       .build());
        cleanups.add(() -> pageFileRepository.deleteById(pageFile.getId()));
    }

    private void setFilesConfiguration(String key, String value) {
        portalConfigurationService.setRuntimeValue(FILES.group, key, value);
        portalConfigurationService.reloadPortalConfigurations();
    }
}
