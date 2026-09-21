package io.oxalate.backend.controller;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.MEMBERSHIP;
import static io.oxalate.backend.api.PortalConfigEnum.MembershipConfigEnum.MEMBERSHIP_TYPE;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.service.PortalConfigurationService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paged active memberships, {@code GET /api/memberships}.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MembershipControllerRTC extends PagedRestTestSupport {

    private static final String MEMBERSHIPS_ENDPOINT = "/api/memberships";

    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private PortalConfigurationService portalConfigurationService;

    private final List<Membership> createdMemberships = new ArrayList<>();
    private String marker;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        marker = marker();
        setMembershipType("perpetual");
        var admin = createUser("Admin", "Administrator", RoleEnum.ROLE_ADMIN);
        adminJwt = jwtFor(admin, RoleEnum.ROLE_ADMIN);

        var memberA = createUser("Ann", marker + "A", RoleEnum.ROLE_USER);
        var memberB = createUser("Bob", marker + "B", RoleEnum.ROLE_USER);
        var memberC = createUser("Cid", marker + "C", RoleEnum.ROLE_USER);
        var expired = createUser("Eve", marker + "E", RoleEnum.ROLE_USER);
        var cancelled = createUser("Xer", marker + "X", RoleEnum.ROLE_USER);

        createMembership(memberA, MembershipStatusEnum.ACTIVE, null);
        createMembership(memberB, MembershipStatusEnum.ACTIVE, LocalDate.now());
        createMembership(memberC, MembershipStatusEnum.ACTIVE, LocalDate.now()
                                                                        .plusYears(1));
        // Neither an expired nor a cancelled membership is active
        createMembership(expired, MembershipStatusEnum.ACTIVE, LocalDate.now()
                                                                        .minusDays(1));
        createMembership(cancelled, MembershipStatusEnum.CANCELLED, null);
    }

    @Override
    protected void cleanUpFixtures() {
        for (var membership : createdMemberships) {
            membershipRepository.deleteById(membership.getId());
        }

        createdMemberships.clear();
        setMembershipType("disabled");
    }

    @Test
    void getAllActiveMembershipsPageShapeOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("size", "2")
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
               // Default order is user id ascending, i.e. creation order of the members
               .andExpect(jsonPath("$.content[0].username", is(marker + "A Ann")))
               .andExpect(jsonPath("$.content[1].username", is(marker + "B Bob")));
    }

    @Test
    void getAllActiveMembershipsLastPageOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("size", "2")
                                                 .queryParam("page", "1")
                                                 .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(1)))
               .andExpect(jsonPath("$.last", is(true)))
               .andExpect(jsonPath("$.content[0].username", is(marker + "C Cid")));
    }

    @Test
    void getAllActiveMembershipsSortByUsernameDescOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("sort_by", "username")
                                                 .queryParam("direction", "DESC")
                                                 .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].username", startsWith(marker + "C")))
               .andExpect(jsonPath("$.content[2].username", startsWith(marker + "A")));
    }

    @Test
    void getAllActiveMembershipsSearchByFirstNameOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS_ENDPOINT).queryParam("search", "bob")
                                                 .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content[0].username", is(marker + "B Bob")));
    }

    @Test
    void getAllActiveMembershipsForbiddenSortColumnFallsBackToDefaultOk() throws Exception {
        mockMvc.perform(get(MEMBERSHIPS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("sort_by", "user.password")
                                                 .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total_elements", is(3)))
               .andExpect(jsonPath("$.content[0].username", is(marker + "A Ann")));
    }

    @Test
    void getAllActiveMembershipsDisabledTypeReturnsEmptyPageOk() throws Exception {
        setMembershipType("disabled");

        mockMvc.perform(get(MEMBERSHIPS_ENDPOINT).queryParam("search", marker)
                                                 .queryParam("size", "7")
                                                 .cookie(new Cookie(JWT_TOKEN, adminJwt)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content", hasSize(0)))
               .andExpect(jsonPath("$.size", is(7)))
               .andExpect(jsonPath("$.total_elements", is(0)))
               .andExpect(jsonPath("$.total_pages", is(0)))
               .andExpect(jsonPath("$.empty", is(true)));
    }

    private void createMembership(User user, MembershipStatusEnum status, LocalDate endDate) {
        var membership = membershipRepository.save(Membership.builder()
                                                             .userId(user.getId())
                                                             .type(MembershipTypeEnum.PERPETUAL)
                                                             .status(status)
                                                             .startDate(LocalDate.now()
                                                                                 .minusMonths(1))
                                                             .endDate(endDate)
                                                             .created(Instant.now())
                                                             .build());
        createdMemberships.add(membership);
    }

    private void setMembershipType(String type) {
        portalConfigurationService.setRuntimeValue(MEMBERSHIP.group, MEMBERSHIP_TYPE.key, type);
        portalConfigurationService.reloadPortalConfigurations();
    }
}
