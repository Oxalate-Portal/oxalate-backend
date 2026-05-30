package io.oxalate.backend.service;

import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.DiveTypeEnum;
import static io.oxalate.backend.api.EventStatusEnum.PUBLISHED;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import io.oxalate.backend.api.PeriodicPaymentTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.SINGLE_PAYMENT_ENABLED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import static io.oxalate.backend.api.RoleEnum.ROLE_USER;
import io.oxalate.backend.api.UserStatusEnum;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.EventSubscribeRequest;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.MessageRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WaitingListServiceITC extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private EventParticipantsRepository eventParticipantsRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventService eventService;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PortalConfigurationService portalConfigurationService;

    private User organizer;
    private User diverA;
    private User diverB;
    private User diverC;
    private Event event;

    @BeforeEach
    void setUp() {
        organizer = generateUser(ACTIVE, ROLE_ORGANIZER);
        diverA = generateUser(ACTIVE, ROLE_USER);
        diverB = generateUser(ACTIVE, ROLE_USER);
        diverC = generateUser(ACTIVE, ROLE_USER);

        event = Event.builder()
                     .type(DiveTypeEnum.CAVE)
                     .title("Waiting List Test Event")
                     .description("Event used in waiting-list integration tests")
                     .startTime(Instant.now()
                                       .plus(2, ChronoUnit.DAYS))
                     .eventDuration(6)
                     .maxDuration(120)
                     .maxDepth(30)
                     .maxParticipants(1)
                     .organizerId(organizer.getId())
                     .status(PUBLISHED)
                     .build();
        event = eventService.save(event);

        portalConfigurationService.setRuntimeValue(PAYMENT.group, SINGLE_PAYMENT_ENABLED.key, "true");
        portalConfigurationService.setRuntimeValue(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.reloadPortalConfigurations();

        paymentService.savePayment(PaymentRequest.builder()
                                                 .userId(diverA.getId())
                                                 .paymentCount(3)
                                                 .paymentType(ONE_TIME)
                                                 .build());
        paymentService.savePayment(PaymentRequest.builder()
                                                 .userId(diverB.getId())
                                                 .paymentCount(3)
                                                 .paymentType(ONE_TIME)
                                                 .build());
        paymentService.savePayment(PaymentRequest.builder()
                                                 .userId(diverC.getId())
                                                 .paymentCount(3)
                                                 .paymentType(ONE_TIME)
                                                 .build());
    }

    @AfterEach
    void tearDown() {
        eventParticipantsRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM message_receivers");
        messageRepository.deleteAll();
        eventRepository.deleteAll();
        paymentRepository.deleteAll();

        roleRepository.deleteAllUserRolesByUserId(organizer.getId());
        roleRepository.deleteAllUserRolesByUserId(diverA.getId());
        roleRepository.deleteAllUserRolesByUserId(diverB.getId());
        roleRepository.deleteAllUserRolesByUserId(diverC.getId());

        userRepository.deleteById(diverC.getId());
        userRepository.deleteById(diverB.getId());
        userRepository.deleteById(diverA.getId());
        userRepository.deleteById(organizer.getId());
    }

    @Test
    void joinWaitingListValidOk() {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        var subscribed = eventService.addUserToEvent(diverA, subscribeRequest);
        assertNotNull(subscribed);

        var response = eventService.joinWaitingList(diverB, event.getId());
        assertNotNull(response);
        assertEquals(1, response.getParticipants()
                                .size());
        assertEquals(1, response.getWaitingList()
                                .size());
        assertEquals(diverB.getId(), response.getWaitingList()
                                             .getFirst()
                                             .getId());
    }

    @Test
    void joinWaitingListEventNotFullFail() {
        var response = eventService.joinWaitingList(diverA, event.getId());
        assertNull(response);
    }

    @Test
    void leaveWaitingListValidOk() {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        assertNotNull(eventService.addUserToEvent(diverA, subscribeRequest));
        assertNotNull(eventService.joinWaitingList(diverB, event.getId()));

        var response = eventService.leaveWaitingList(diverB, event.getId());
        assertNotNull(response);
        assertTrue(response.getWaitingList()
                           .isEmpty());
    }

    @Test
    void waitingListCountExcludesFromParticipantCountOk() {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        assertNotNull(eventService.addUserToEvent(diverA, subscribeRequest));
        assertNotNull(eventService.joinWaitingList(diverB, event.getId()));

        var response = eventService.findById(event.getId());
        assertNotNull(response);
        assertEquals(1, response.getParticipants()
                                .size());
        assertEquals(1, response.getWaitingList()
                                .size());
    }

    @Test
    void cancelParticipationWithWaitingListOk() {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        assertNotNull(eventService.addUserToEvent(diverA, subscribeRequest));
        assertNotNull(eventService.joinWaitingList(diverB, event.getId()));

        var response = eventService.removeUserFromEvent(diverA, event.getId());
        assertNotNull(response);
        assertEquals(1, response.getParticipants()
                                .size());
        assertEquals(diverB.getId(), response.getParticipants()
                                             .getFirst()
                                             .getId());
        assertTrue(response.getWaitingList()
                           .isEmpty());

        var waitingEntry = eventParticipantsRepository.findWaitingListEntryByEventIdAndUserId(event.getId(), diverB.getId());
        assertTrue(waitingEntry.isEmpty());

        var unreadMessages = messageRepository.findUnreadUserMessages(diverB.getId());
        assertEquals(1, unreadMessages.size());
    }

    @Test
    void cancelParticipationWithoutWaitingListOk() {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        assertNotNull(eventService.addUserToEvent(diverA, subscribeRequest));

        var response = eventService.removeUserFromEvent(diverA, event.getId());
        assertNotNull(response);
        assertTrue(response.getParticipants()
                           .isEmpty());
        assertTrue(response.getWaitingList()
                           .isEmpty());

        var unreadMessages = messageRepository.findUnreadUserMessages(diverA.getId());
        assertTrue(unreadMessages.isEmpty());
    }

    @Test
    void cancelParticipationWithMultipleWaitingListOk() {
        var subscribeRequest = EventSubscribeRequest.builder()
                                                    .diveEventId(event.getId())
                                                    .userType(UserTypeEnum.SCUBA_DIVER)
                                                    .build();
        assertNotNull(eventService.addUserToEvent(diverA, subscribeRequest));
        assertNotNull(eventService.joinWaitingList(diverB, event.getId()));
        assertNotNull(eventService.joinWaitingList(diverC, event.getId()));

        var response = eventService.removeUserFromEvent(diverA, event.getId());
        assertNotNull(response);
        assertEquals(1, response.getParticipants()
                                .size());
        assertEquals(diverB.getId(), response.getParticipants()
                                             .getFirst()
                                             .getId());
        assertEquals(1, response.getWaitingList()
                                .size());
        assertEquals(diverC.getId(), response.getWaitingList()
                                             .getFirst()
                                             .getId());

        var unreadMessagesB = messageRepository.findUnreadUserMessages(diverB.getId());
        var unreadMessagesC = messageRepository.findUnreadUserMessages(diverC.getId());
        assertEquals(1, unreadMessagesB.size());
        assertTrue(unreadMessagesC.isEmpty());
    }

    private User generateUser(UserStatusEnum userStatusEnum, RoleEnum roleEnum) {
        var randomUsername = "waiting-" + Instant.now()
                                                 .toEpochMilli() + "-" + Math.abs((int) (Math.random() * 10000)) + "@test.tld";
        var user = User.builder()
                       .username(randomUsername)
                       .password("password")
                       .firstName("Max")
                       .lastName("Mustermann")
                       .status(userStatusEnum)
                       .phoneNumber("123456789")
                       .privacy(false)
                       .nextOfKin("Maxine Mustermann")
                       .registered(Instant.now()
                                          .minus(10L, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .language("en")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();

        var newUser = userRepository.save(user);
        var optionalRole = roleRepository.findByName(roleEnum);
        assertFalse(optionalRole.isEmpty());
        roleRepository.addUserRole(newUser.getId(), optionalRole.get()
                                                                .getId());
        return newUser;
    }
}

