package io.oxalate.backend.controller;

import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Shared fixture for the REST tests of the paged list endpoints: a MockMvc with the real security filter chain, users
 * with roles that are removed again after each test, and a JWT cookie value for them. Not a test class itself, so the
 * {@code *RTC} suffix is deliberately absent.
 */
public abstract class PagedRestTestSupport extends AbstractIntegrationTest {

    protected MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext webApplicationContext;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RoleRepository roleRepository;
    @Autowired
    protected JwtUtils jwtUtils;

    private final List<User> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUpSupport() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                 .apply(springSecurity())
                                 .build();
        createdUsers.clear();
    }

    @AfterEach
    void tearDownSupport() {
        cleanUpFixtures();

        for (var user : createdUsers) {
            roleRepository.removeUserRoles(user.getId());
            userRepository.deleteById(user.getId());
        }

        createdUsers.clear();
    }

    /**
     * Hook for subclasses to delete rows that reference the users before the users themselves are removed.
     */
    protected void cleanUpFixtures() {
        // Nothing by default
    }

    /**
     * Unique marker for this test invocation, used in names and searched for so that rows left behind by other test
     * classes never influence the page totals.
     */
    protected static String marker() {
        return "Paged" + System.nanoTime();
    }

    protected User createUser(String firstName, String lastName, RoleEnum... roles) {
        var user = User.builder()
                       .username("paged." + System.nanoTime() + "@test.tld")
                       .password("password")
                       .firstName(firstName)
                       .lastName(lastName)
                       .status(UserStatusEnum.ACTIVE)
                       .phoneNumber("358401234567")
                       .privacy(false)
                       .nextOfKin("Kin")
                       .registered(Instant.now()
                                          .minus(30, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .healthStatementId(1L)
                       .language("en")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();

        var saved = userRepository.save(user);

        for (var role : roles) {
            roleRepository.findByName(role)
                          .ifPresent(r -> roleRepository.addUserRole(saved.getId(), r.getId()));
        }

        createdUsers.add(saved);
        return saved;
    }

    protected String jwtFor(User user, RoleEnum... roles) {
        var authorities = new ArrayList<SimpleGrantedAuthority>();

        for (var role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.name()));
        }

        var userDetails = new UserDetailsImpl(user.getId(), user.getUsername(), user.getPassword(), authorities, user.isApprovedTerms(),
                user.getHealthStatementId(), false, user.getLanguage());
        return jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));
    }
}
