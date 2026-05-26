package io.oxalate.backend.controller;

import io.oxalate.backend.AbstractIntegrationTest;
import static io.oxalate.backend.api.EventStatusEnum.PUBLISHED;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import io.oxalate.backend.api.PeriodicPaymentTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.SINGLE_PAYMENT_ENABLED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.SecurityConstants.JWT_TOKEN;
import io.oxalate.backend.api.UserStatusEnum;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.EventSubscribeRequest;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsImpl;
import io.oxalate.backend.service.EventService;
import io.oxalate.backend.service.PaymentService;
import io.oxalate.backend.service.PortalConfigurationService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventControllerWaitingListRTC extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventParticipantsRepository eventParticipantsRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private EventService eventService;
    @Autowired
    private PortalConfigurationService portalConfigurationService;

    private MockMvc mockMvc;

    private User organizer;
    private User activeUser;
    private User participant;
    private Event event;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                 .apply(springSecurity())
                                 .build();

        organizer = createUser(ACTIVE, RoleEnum.ROLE_ORGANIZER);
        activeUser = createUser(ACTIVE, RoleEnum.ROLE_USER);
        participant = createUser(ACTIVE, RoleEnum.ROLE_USER);

        var authorities = List.of(new SimpleGrantedAuthority(RoleEnum.ROLE_USER.name()));
        var userDetails = new UserDetailsImpl(activeUser.getId(), activeUser.getUsername(), activeUser.getPassword(), authorities,
                activeUser.isApprovedTerms(), activeUser.getHealthStatementId(), false, activeUser.getLanguage());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        jwtToken = jwtUtils.generateJwtToken(authentication);

        portalConfigurationService.setRuntimeValue(PAYMENT.group, SINGLE_PAYMENT_ENABLED.key, "true");
        portalConfigurationService.setRuntimeValue(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.reloadPortalConfigurations();

        event = Event.builder()
                     .title("RTC waiting list event")
                     .description("RTC waiting list event description")
                     .type(io.oxalate.backend.api.DiveTypeEnum.CAVE)
                     .startTime(Instant.now()
                                       .plus(1, ChronoUnit.DAYS))
                     .eventDuration(3)
                     .maxDuration(120)
                     .maxDepth(25)
                     .maxParticipants(1)
                     .status(PUBLISHED)
                     .organizerId(organizer.getId())
                     .build();
        event = eventService.save(event);

        paymentService.savePayment(PaymentRequest.builder()
                                                 .userId(activeUser.getId())
                                                 .paymentCount(2)
                                                 .paymentType(ONE_TIME)
                                                 .build());
        paymentService.savePayment(PaymentRequest.builder()
                                                 .userId(participant.getId())
                                                 .paymentCount(2)
                                                 .paymentType(ONE_TIME)
                                                 .build());
    }

    @AfterEach
    void tearDown() {
        eventParticipantsRepository.deleteAll();
        eventRepository.deleteAll();
        paymentRepository.deleteAll();

        roleRepository.deleteAllUserRolesByUserId(organizer.getId());
        roleRepository.deleteAllUserRolesByUserId(activeUser.getId());
        roleRepository.deleteAllUserRolesByUserId(participant.getId());

        userRepository.deleteById(participant.getId());
        userRepository.deleteById(activeUser.getId());
        userRepository.deleteById(organizer.getId());
    }

    @Test
    void joinWaitingListValidOk() throws Exception {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        eventService.addUserToEvent(participant, subscribeRequest);

        mockMvc.perform(post("/api/events/{eventId}/waiting-list/join", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, jwtToken)))
               .andExpect(status().isOk());
    }

    @Test
    void joinWaitingListEventNotFoundFail() throws Exception {
        mockMvc.perform(post("/api/events/{eventId}/waiting-list/join", 999999L)
                       .cookie(new Cookie(JWT_TOKEN, jwtToken)))
               .andExpect(status().isNotFound());
    }

    @Test
    void leaveWaitingListValidOk() throws Exception {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        eventService.addUserToEvent(participant, subscribeRequest);
        eventService.joinWaitingList(activeUser, event.getId());

        mockMvc.perform(post("/api/events/{eventId}/waiting-list/leave", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, jwtToken)))
               .andExpect(status().isOk());
    }

    @Test
    void leaveWaitingListNotInListFail() throws Exception {
        mockMvc.perform(post("/api/events/{eventId}/waiting-list/leave", event.getId())
                       .cookie(new Cookie(JWT_TOKEN, jwtToken)))
               .andExpect(status().isBadRequest());
    }

    private User createUser(UserStatusEnum status, RoleEnum roleEnum) {
        var username = "rtc-" + Instant.now()
                                       .toEpochMilli() + "-" + Math.abs((int) (Math.random() * 10000)) + "@example.tld";
        var user = User.builder()
                       .username(username)
                       .password("password")
                       .firstName("RTC")
                       .lastName("User")
                       .status(status)
                       .approvedTerms(true)
                       .healthStatementId(1L)
                       .phoneNumber("123456")
                       .privacy(false)
                       .nextOfKin("Kin")
                       .registered(Instant.now()
                                          .minus(5, ChronoUnit.DAYS))
                       .language("en")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();

        var saved = userRepository.save(user);
        var roleOpt = roleRepository.findByName(roleEnum);
        assertFalse(roleOpt.isEmpty());
        roleRepository.addUserRole(saved.getId(), roleOpt.get()
                                                         .getId());
        return saved;
    }
}



