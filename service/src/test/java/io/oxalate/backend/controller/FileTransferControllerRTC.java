package io.oxalate.backend.controller;

import io.oxalate.backend.AbstractIntegrationTest;
import static io.oxalate.backend.api.PortalConfigEnum.FILES;
import static io.oxalate.backend.api.PortalConfigEnum.FileConfigEnum.DOCUMENTS_SUPPORTED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.DocumentFile;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.DocumentFileRepository;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import io.oxalate.backend.service.PortalConfigurationService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileTransferControllerRTC extends AbstractIntegrationTest {

    private static final String DOCUMENT_FILES_ENDPOINT = "/api/files/documents";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private DocumentFileRepository documentFileRepository;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PortalConfigurationService portalConfigurationService;

    private User adminUser;
    private User nonAdminUser;
    private User otherUser;
    private String adminJwt;
    private String nonAdminJwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        documentFileRepository.deleteAll();
        portalConfigurationService.setRuntimeValue(FILES.group, DOCUMENTS_SUPPORTED.key, "true");
        portalConfigurationService.reloadPortalConfigurations();

        adminUser = createUserWithRoles(Set.of(RoleEnum.ROLE_ADMIN));
        nonAdminUser = createUserWithRoles(Set.of(RoleEnum.ROLE_USER));
        otherUser = createUserWithRoles(Set.of(RoleEnum.ROLE_USER));

        adminJwt = generateJwtTokenForUser(adminUser, Set.of(RoleEnum.ROLE_ADMIN));
        nonAdminJwt = generateJwtTokenForUser(nonAdminUser, Set.of(RoleEnum.ROLE_USER));
    }

    @Test
    void findAllDocumentFiles_adminWithCreatorIdSelf_returnsOnlyOwnDocuments() throws Exception {
        var adminDocument = createDocumentFile(adminUser, "admin-self.pdf");
        createDocumentFile(otherUser, "admin-other.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT)
                       .queryParam("creatorId", String.valueOf(adminUser.getId()))
                       .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value(adminDocument.getId()))
               .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findAllDocumentFiles_adminWithCreatorIdOther_returnsOnlyRequestedCreatorDocuments() throws Exception {
        createDocumentFile(adminUser, "admin.pdf");
        var otherDocument = createDocumentFile(otherUser, "other.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT)
                       .queryParam("creatorId", String.valueOf(otherUser.getId()))
                       .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value(otherDocument.getId()))
               .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findAllDocumentFiles_nonAdminWithCreatorIdSelf_returnsOnlyOwnDocuments() throws Exception {
        var ownDocument = createDocumentFile(nonAdminUser, "self.pdf");
        createDocumentFile(otherUser, "someone-else.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT)
                       .queryParam("creatorId", String.valueOf(nonAdminUser.getId()))
                       .cookie(new Cookie(JWT_TOKEN, nonAdminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value(ownDocument.getId()))
               .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findAllDocumentFiles_nonAdminWithCreatorIdOther_forbidden() throws Exception {
        createDocumentFile(nonAdminUser, "self.pdf");
        createDocumentFile(otherUser, "other.pdf");

        mockMvc.perform(get(DOCUMENT_FILES_ENDPOINT)
                       .queryParam("creatorId", String.valueOf(otherUser.getId()))
                       .cookie(new Cookie(JWT_TOKEN, nonAdminJwt)))
               .andExpect(status().isForbidden());
    }

    private DocumentFile createDocumentFile(User creator, String filename) {
        return documentFileRepository.save(DocumentFile.builder()
                                                       .fileName(filename + "-" + Instant.now()
                                                                                         .toEpochMilli())
                                                       .mimeType("application/pdf")
                                                       .fileSize(1024)
                                                       .fileChecksum("checksum-" + filename)
                                                       .creator(creator)
                                                       .createdAt(Instant.now())
                                                       .status(UploadStatusEnum.UPLOADED)
                                                       .build());
    }

    private User createUserWithRoles(Set<RoleEnum> roles) {
        var user = User.builder()
                       .username("file-test-" + Instant.now()
                                                       .toEpochMilli() + "-" + Math.random() + "@test.tld")
                       .password("password")
                       .firstName("Test")
                       .lastName("User")
                       .status(UserStatusEnum.ACTIVE)
                       .phoneNumber("123456789")
                       .privacy(false)
                       .nextOfKin("N/A")
                       .registered(Instant.now()
                                          .minus(1000L, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .language("en")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();

        var savedUser = userRepository.save(user);
        for (var roleEnum : roles) {
            roleRepository.findByName(roleEnum)
                          .ifPresent(role -> roleRepository.addUserRole(savedUser.getId(), role.getId()));
        }

        return savedUser;
    }

    private String generateJwtTokenForUser(User user, Set<RoleEnum> roles) {
        var authorities = roles.stream()
                               .map(role -> new SimpleGrantedAuthority(role.name()))
                               .toList();

        var userDetails = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                user.isApprovedTerms(),
                user.getHealthStatementId(),
                false,
                user.getLanguage()
        );

        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        return jwtUtils.generateJwtToken(authentication);
    }
}

