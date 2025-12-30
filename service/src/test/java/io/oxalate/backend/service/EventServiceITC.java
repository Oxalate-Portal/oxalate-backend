package io.oxalate.backend.service;

import io.oxalate.backend.AbstractIntegrationTest;
import static io.oxalate.backend.api.CommentConstants.COMMON_LEGACY_EVENT_COMMENT_ID;
import static io.oxalate.backend.api.CommentConstants.ROOT_EVENT_COMMENT_ID;
import io.oxalate.backend.api.DiveTypeEnum;
import static io.oxalate.backend.api.DiveTypeEnum.CAVE;
import io.oxalate.backend.api.EventStatusEnum;
import static io.oxalate.backend.api.EventStatusEnum.PUBLISHED;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIODICAL;
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
import io.oxalate.backend.api.request.EventRequest;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.commenting.Comment;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.commenting.CommentRepository;
import io.oxalate.backend.repository.commenting.EventCommentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventServiceITC extends AbstractIntegrationTest {

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
    private PortalConfigurationService portalConfigurationService;

    private User organizer;
    private User diver;
    private Event event;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private EventCommentRepository eventCommentRepository;

    @BeforeEach
    void setUp() {
        // Add users
        this.organizer = generateUser(ACTIVE, ROLE_ORGANIZER);
        this.diver = generateUser(ACTIVE, ROLE_USER);
        // Add event which starts in 10 days
        this.event = generateEvent(Instant.now()
                                          .plus(10, ChronoUnit.DAYS), CAVE, organizer.getId(), PUBLISHED);

        portalConfigurationService.setRuntimeValue(PAYMENT.group, SINGLE_PAYMENT_ENABLED.key, "true");
        portalConfigurationService.setRuntimeValue(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.reloadPortalConfigurations();
    }

    @AfterEach
    void tearDown() {
        // Remove event participants
        eventParticipantsRepository.deleteAll();
        // Remove all event-comments
        eventCommentRepository.deleteAll();
        // Remove all comments with ID above 4
        var comments = commentRepository.findAll();
        for (var comment : comments) {
            if (comment.getId() > COMMON_LEGACY_EVENT_COMMENT_ID) {
                commentRepository.deleteById(comment.getId());
            }
        }

        // Remove event
        eventRepository.deleteAll();
        // Remove user roles
        roleRepository.deleteAllUserRolesByUserId(organizer.getId());
        roleRepository.deleteAllUserRolesByUserId(diver.getId());
        // Remove payments
        paymentRepository.deleteAll();
        // Remove users
        userRepository.deleteById(diver.getId());
        userRepository.deleteById(organizer.getId());
    }

    @Test
    void updateEventWithUserOk() {
        var paymentRequest = PaymentRequest.builder()
                                           .userId(diver.getId())
                                           .paymentCount(4)
                                           .paymentType(ONE_TIME)
                                           .build();
        paymentService.savePayment(paymentRequest);

        // Create related eventCommentTopic
        var comment = Comment.builder()
                             .userId(diver.getId())
                             .parentCommentId(ROOT_EVENT_COMMENT_ID)
                             .build();

        var eventRequest = generateEventRequestFromEvent();

        var eventResponse = eventService.updateEvent(eventRequest);

        assertNotNull(eventResponse);
        assertNotNull(eventResponse.getParticipants());
        assertFalse(eventResponse.getParticipants()
                                 .isEmpty());
        assertEquals(1, eventResponse.getParticipants()
                                     .size());
        assertEquals(diver.getId(), eventResponse.getParticipants()
                                                 .getFirst()
                                                 .getId());
    }

    @Test
    void updateEventWithUserNoTopicCommentOk() {
        var paymentRequest = PaymentRequest.builder()
                                           .userId(diver.getId())
                                           .paymentCount(4)
                                           .paymentType(ONE_TIME)
                                           .build();
        paymentService.savePayment(paymentRequest);

        var eventRequest = generateEventRequestFromEvent();

        var eventResponse = eventService.updateEvent(eventRequest);

        assertNotNull(eventResponse);
        assertNotNull(eventResponse.getParticipants());
        assertFalse(eventResponse.getParticipants()
                                 .isEmpty());
        assertEquals(1, eventResponse.getParticipants()
                                     .size());
        assertEquals(diver.getId(), eventResponse.getParticipants()
                                                 .getFirst()
                                                 .getId());
    }

    @Test
    void updateEventWithUserMultiplePaymentOk() {
        var oneTimePaymentRequest = PaymentRequest.builder()
                                                  .userId(diver.getId())
                                                  .paymentCount(4)
                                                  .paymentType(ONE_TIME)
                                                  .build();
        var oneTimePaymentResponse = paymentService.savePayment(oneTimePaymentRequest);
        assertNotNull(oneTimePaymentResponse);
        assertNotNull(oneTimePaymentResponse.getStartDate());
        assertNotNull(oneTimePaymentResponse.getEndDate());

        var periodPaymentRequest = PaymentRequest.builder()
                                                 .userId(diver.getId())
                                                 .paymentCount(4)
                                                 .paymentType(ONE_TIME)
                                                 .build();
        var periodPaymentStatusResponse = paymentService.savePayment(periodPaymentRequest);
        assertNotNull(periodPaymentStatusResponse);

        var eventRequest = generateEventRequestFromEvent();

        var eventResponse = eventService.updateEvent(eventRequest);

        assertNotNull(eventResponse);
        assertNotNull(eventResponse.getParticipants());
        assertFalse(eventResponse.getParticipants()
                                 .isEmpty());
        assertEquals(1, eventResponse.getParticipants()
                                     .size());
        assertEquals(diver.getId(), eventResponse.getParticipants()
                                                 .getFirst()
                                                 .getId());
    }

    @Test
    void updateEventWithUserPaymentExpiredFail() {
        var oneTimePaymentStatusResponse = PaymentRequest.builder()
                                                         .userId(diver.getId())
                                                         .paymentCount(4)
                                                         .paymentType(ONE_TIME)
                                                         .startDate(LocalDate.parse("2020-01-01"))
                                                         .endDate(LocalDate.parse("2021-01-01"))
                                                         .build();
        assertNotNull(oneTimePaymentStatusResponse);
        var paymentResponse = paymentService.savePayment(oneTimePaymentStatusResponse);
        paymentRepository.findById(paymentResponse.getId());

        var eventRequest = generateEventRequestFromEvent();

        var eventResponse = eventService.updateEvent(eventRequest);
        log.info("Event response: {}", eventResponse);
        assertNotNull(eventResponse);
        assertNotNull(eventResponse.getParticipants());
        // The diver should not be a participant because he had an expired payment
        assertTrue(eventResponse.getParticipants()
                                .isEmpty());
    }

    @Test
    void updateEventWithUserMultiplePaymentExpiredFail() {
        var thisYear = 2022;

        var oneTimePaymentRequest = PaymentRequest.builder()
                                                  .userId(diver.getId())
                                                  .paymentCount(4)
                                                  .paymentType(ONE_TIME)
                                                  .startDate(LocalDate.parse(thisYear + "-01-01"))
                                                  .endDate(LocalDate.parse((thisYear + 1) + "-01-01"))
                                                  .build();
        var oneTimePaymentResponse = paymentService.savePayment(oneTimePaymentRequest);
        log.info("One-time payment response: {}", oneTimePaymentResponse);
        assertNotNull(oneTimePaymentResponse);
        assertNotNull(oneTimePaymentResponse.getStartDate());
        assertNotNull(oneTimePaymentResponse.getEndDate());

        var periodPaymentRequest = PaymentRequest.builder()
                                                 .userId(diver.getId())
                                                 .paymentCount(4)
                                                 .paymentType(PERIODICAL)
                                                 .startDate(LocalDate.parse(thisYear + "-01-01"))
                                                 .endDate(LocalDate.parse((thisYear + 1) + "-01-01"))
                                                 .build();
        var periodPaymentResponse = paymentService.savePayment(periodPaymentRequest);
        log.info("Period payment response: {}", periodPaymentResponse);
        assertNotNull(periodPaymentResponse);
        assertNotNull(periodPaymentResponse.getStartDate());
        assertNotNull(periodPaymentResponse.getEndDate());

        var eventRequest = generateEventRequestFromEvent();
        log.info("Generated event request: {}", eventRequest);
        var eventResponse = eventService.updateEvent(eventRequest);

        assertNotNull(eventResponse);
        assertNotNull(eventResponse.getParticipants());
        // The diver should not be a participant because he had an expired payment
        assertTrue(eventResponse.getParticipants()
                                .isEmpty());
    }

    private Event generateEvent(Instant start, DiveTypeEnum type, long organizerId, EventStatusEnum eventStatus) {
        var event = Event.builder()
                         .type(type)
                         .title("Test Event")
                         .description("A Test generated event for testing cases with test data")
                         .startTime(start)
                         .eventDuration(6)
                         .maxDuration(120)
                         .maxDepth(40)
                         .maxParticipants(12)
                         .organizerId(organizerId)
                         .status(eventStatus)
                         .build();

        return eventRepository.save(event);
    }

    private User generateUser(UserStatusEnum userStatusEnum, RoleEnum roleEnum) {
        var randomUsername = "test-" + Instant.now()
                                              .toEpochMilli() + "@test.tld";
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
                                          .minus(1000L, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .language("de")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();

        var newUser = userRepository.save(user);
        var optionalRole = roleRepository.findByName(roleEnum);
        assertFalse(optionalRole.isEmpty());
        var role = optionalRole.get();
        roleRepository.addUserRole(newUser.getId(), role.getId());
        return newUser;
    }

    private EventRequest generateEventRequestFromEvent() {
        return EventRequest.builder()
                           .id(event.getId())
                           .type(event.getType())
                           .title("Updated Event")
                           .description("A Test generated event for testing cases with test data that has been updated")
                           .startTime(event.getStartTime())
                           .eventDuration(event.getEventDuration())
                           .maxDuration(event.getMaxDuration())
                           .maxDepth(event.getMaxDepth())
                           .maxParticipants(event.getMaxParticipants())
                           .organizerId(event.getOrganizerId())
                           .status(event.getStatus())
                           .participants(Set.of(diver.getId()))
                           .build();
    }
}
