package io.oxalate.backend.service;

import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIODICAL;
import io.oxalate.backend.api.PeriodicPaymentTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_LENGTH;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_UNIT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_ENABLED;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_LENGTH;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_START;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_START_POINT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_UNIT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.SINGLE_PAYMENT_ENABLED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_USER;
import io.oxalate.backend.api.UserStatusEnum;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.model.Payment;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentServiceITC extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PortalConfigurationService portalConfigurationService;

    private static final List<String> MUTATED_PAYMENT_CONFIG_KEYS = List.of(
            SINGLE_PAYMENT_ENABLED.key,
            PAYMENT_ENABLED.key,
            ONE_TIME_PAYMENT_EXPIRATION_TYPE.key,
            ONE_TIME_PAYMENT_EXPIRATION_UNIT.key,
            ONE_TIME_PAYMENT_EXPIRATION_LENGTH.key,
            PERIODICAL_PAYMENT_METHOD_TYPE.key,
            PERIODICAL_PAYMENT_METHOD_UNIT.key,
            PAYMENT_PERIOD_LENGTH.key,
            PAYMENT_PERIOD_START_POINT.key,
            PAYMENT_PERIOD_START.key
    );

    private User diver;

    @BeforeEach
    void setUp() {
        this.diver = generateUser(ACTIVE, ROLE_USER);
        applyDeterministicOneTimePaymentConfig();
    }

    @AfterEach
    void tearDown() {
        // Avoid leaking runtime config state to other tests using the shared integration DB.
        for (var key : MUTATED_PAYMENT_CONFIG_KEYS) {
            portalConfigurationService.setRuntimeValue(PAYMENT.group, key, null);
        }
        portalConfigurationService.reloadPortalConfigurations();
    }

    @Test
    void shouldSaveAndRetrievePayment() {
        // Given
        var now = LocalDate.now();
        var paymentRequest = PaymentRequest.builder()
                                           .userId(diver.getId())
                                           .paymentCount(4)
                                           .paymentType(PaymentTypeEnum.ONE_TIME)
                                           .startDate(now)
                                           .endDate(now.plusYears(1))
                                           .build();

        // When
        var savedPayment = paymentService.savePayment(paymentRequest);
        assertNotNull(savedPayment);
        log.info("Saved payment: {}", savedPayment);
        // Then, because everything in the previous call happens in one transaction, we need to re-fetch
        var newPayments = paymentService.getActivePaymentsByUser(diver.getId());
        assertNotNull(newPayments);
        assertFalse(newPayments.isEmpty());
        assertEquals(1, newPayments.size());
        var payment = newPayments.getFirst();
        var retrievedPayment = paymentRepository.findById(payment.getId());
        assertTrue(retrievedPayment.isPresent());
    }

    @Test
    void shouldReturnNullWhenPaymentModeDisabled() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.DISABLED, PeriodicPaymentTypeEnum.PERIODICAL);

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentCount(1)
                                                                .paymentType(ONE_TIME)
                                                                .startDate(LocalDate.now())
                                                                .build());

        assertNull(response);
    }

    @Test
    void shouldMergePerpetualOneTimePayments() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERPETUAL, PeriodicPaymentTypeEnum.PERIODICAL);

        createPayment(diver.getId(), ONE_TIME, 2, LocalDate.now()
                                                           .minusDays(4), null);
        createPayment(diver.getId(), ONE_TIME, 3, LocalDate.now()
                                                           .minusDays(2), null);

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentCount(5)
                                                                .paymentType(ONE_TIME)
                                                                .build());

        assertNotNull(response);
        var allOneTime = paymentRepository.findAllByUserIdAndPaymentType(diver.getId(), ONE_TIME);
        var activePerpetual = allOneTime.stream()
                                        .filter(p -> p.getEndDate() == null)
                                        .toList();
        assertEquals(1, activePerpetual.size());
        assertEquals(10, activePerpetual.getFirst()
                                        .getPaymentCount());

        var closedEntries = allOneTime.stream()
                                      .filter(p -> p.getEndDate() != null)
                                      .toList();
        assertFalse(closedEntries.isEmpty());
        assertEquals(0, closedEntries.getFirst()
                                     .getPaymentCount());
    }

    @Test
    void shouldUpdateExistingOneTimeById() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var existing = createPayment(diver.getId(), ONE_TIME, 1, LocalDate.now()
                                                                          .minusDays(1), LocalDate.now()
                                                                                                  .plusDays(10));

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .id(existing.getId())
                                                                .userId(diver.getId())
                                                                .paymentCount(7)
                                                                .paymentType(ONE_TIME)
                                                                .build());

        assertNotNull(response);
        assertEquals(7, response.getPaymentCount());
        assertEquals(existing.getId(), response.getId());
    }

    @Test
    void shouldReturnExistingOverlappingOneTimePayment() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var existing = createPayment(diver.getId(), ONE_TIME, 2, LocalDate.now()
                                                                          .minusDays(1), LocalDate.now()
                                                                                                  .plusDays(20));

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentCount(5)
                                                                .paymentType(ONE_TIME)
                                                                .startDate(LocalDate.now())
                                                                .build());

        assertNotNull(response);
        assertEquals(existing.getId(), response.getId());
    }

    @Test
    void shouldIgnoreExpiredOneTimeEvenWhenRequestStartDateIsHistorical() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        createPayment(diver.getId(), ONE_TIME, 1, LocalDate.now()
                                                           .minusYears(7), LocalDate.now()
                                                                                    .minusDays(1));

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentCount(2)
                                                                .paymentType(ONE_TIME)
                                                                .startDate(LocalDate.now()
                                                                                    .minusYears(7))
                                                                .build());

        assertNotNull(response);
        assertTrue(response.getId() > 0);
        assertEquals(2, response.getPaymentCount());
        assertTrue(response.getStartDate() != null);
    }

    @Test
    void shouldKeepHistoricalRangeWhenProvided() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var start = LocalDate.now()
                             .minusYears(3);
        var end = LocalDate.now()
                           .minusYears(2);

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentCount(1)
                                                                .paymentType(ONE_TIME)
                                                                .startDate(start)
                                                                .endDate(end)
                                                                .build());

        assertNotNull(response);
        assertEquals(end, response.getEndDate());
    }

    @Test
    void shouldSavePeriodicalAndCloseOverlappingOneTime() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var oneTime = createPayment(diver.getId(), ONE_TIME, 4, LocalDate.now()
                                                                         .minusDays(3), LocalDate.now()
                                                                                                 .plusDays(40));
        var periodStart = LocalDate.now()
                                   .plusDays(2);

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentType(PERIODICAL)
                                                                .startDate(periodStart)
                                                                .build());

        assertNotNull(response);
        assertEquals(PERIODICAL, response.getPaymentType());

        var closedOneTime = paymentRepository.findById(oneTime.getId())
                                             .orElseThrow();
        assertEquals(periodStart.minusDays(1), closedOneTime.getEndDate());
    }

    @Test
    void shouldReturnExistingOverlappingPeriodical() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var existing = createPayment(diver.getId(), PERIODICAL, 0, LocalDate.now()
                                                                            .minusDays(1), LocalDate.now()
                                                                                                    .plusDays(20));

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentType(PERIODICAL)
                                                                .startDate(LocalDate.now())
                                                                .build());

        assertNotNull(response);
        assertEquals(existing.getId(), response.getId());
    }

    @Test
    void shouldIgnoreExpiredPeriodicalEvenWhenRequestStartDateIsHistorical() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        createPayment(diver.getId(), PERIODICAL, 0, LocalDate.now()
                                                             .minusYears(7), LocalDate.now()
                                                                                      .minusDays(1));

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentType(PERIODICAL)
                                                                .startDate(LocalDate.now()
                                                                                    .minusYears(7))
                                                                .build());

        assertNotNull(response);
        assertEquals(PERIODICAL, response.getPaymentType());
        assertTrue(response.getId() > 0);
    }

    @Test
    void shouldIncreaseAndDecreaseOneTimeCounters() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var increased = paymentService.increaseOneTimePayment(diver.getId(), 2);
        assertNotNull(increased);
        var afterCreate = paymentService.getActivePaymentsByUser(diver.getId())
                                        .getFirst();
        assertEquals(2, afterCreate.getPaymentCount());

        var increasedAgain = paymentService.increaseOneTimePayment(diver.getId(), 3);
        assertNotNull(increasedAgain);
        var afterIncrease = paymentService.getActivePaymentsByUser(diver.getId())
                                          .getFirst();
        assertEquals(5, afterIncrease.getPaymentCount());

        var decreased = paymentService.decreaseOneTimePayment(diver.getId());
        assertNotNull(decreased);
        var afterDecrease = paymentService.getActivePaymentsByUser(diver.getId())
                                          .getFirst();
        assertEquals(4, afterDecrease.getPaymentCount());

        var missingUserResult = paymentService.decreaseOneTimePayment(999_999L);
        assertNull(missingUserResult);
    }

    @Test
    void shouldResolveBestAvailablePaymentTypeForDifferentCases() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var periodUser = generateUser(ACTIVE, ROLE_USER);
        createPayment(periodUser.getId(), PERIODICAL, 0, LocalDate.now()
                                                                  .minusDays(1), LocalDate.now()
                                                                                          .plusDays(30));
        assertEquals(Optional.of(PERIODICAL), paymentService.getBestAvailablePaymentType(periodUser.getId()));

        var oneTimeUser = generateUser(ACTIVE, ROLE_USER);
        createPayment(oneTimeUser.getId(), ONE_TIME, 2, LocalDate.now()
                                                                 .minusDays(1), LocalDate.now()
                                                                                         .plusDays(30));
        assertEquals(Optional.of(ONE_TIME), paymentService.getBestAvailablePaymentType(oneTimeUser.getId()));

        var futureOneTimeUser = generateUser(ACTIVE, ROLE_USER);
        createPayment(futureOneTimeUser.getId(), ONE_TIME, 0, LocalDate.now()
                                                                       .plusDays(2), LocalDate.now()
                                                                                              .plusDays(30));
        assertEquals(Optional.of(ONE_TIME), paymentService.getBestAvailablePaymentType(futureOneTimeUser.getId()));

        var noPaymentUser = generateUser(ACTIVE, ROLE_USER);
        assertTrue(paymentService.getBestAvailablePaymentType(noPaymentUser.getId())
                                 .isEmpty());
    }

    @Test
    void shouldReturnStatusesByTypeAndAppendNames() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var userOne = generateUser(ACTIVE, ROLE_USER);
        var userTwo = generateUser(ACTIVE, ROLE_USER);
        createPayment(userOne.getId(), ONE_TIME, 1, LocalDate.now()
                                                             .minusDays(1), LocalDate.now()
                                                                                     .plusDays(15));
        createPayment(userTwo.getId(), PERIODICAL, 0, LocalDate.now()
                                                               .minusDays(1), LocalDate.now()
                                                                                       .plusDays(15));

        var activeStatuses = paymentService.getAllActivePaymentStatus();
        assertFalse(activeStatuses.isEmpty());

        var oneTimeStatuses = paymentService.getAllActivePaymentByType(ONE_TIME);
        assertFalse(oneTimeStatuses.isEmpty());
        assertEquals(ONE_TIME, oneTimeStatuses.getFirst()
                                              .getPayments()
                                              .getFirst()
                                              .getPaymentType());

        paymentService.appendNameToPaymentStatusResponse(activeStatuses);
        assertTrue(activeStatuses.stream()
                                 .allMatch(status -> status.getName() != null && !status.getName()
                                                                                        .isBlank()));
    }

    @Test
    void shouldResetPaymentsByType() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        createPayment(diver.getId(), ONE_TIME, 3, LocalDate.now()
                                                           .minusDays(1), LocalDate.now()
                                                                                   .plusDays(60));
        var beforeReset = paymentService.getAllActivePaymentByType(ONE_TIME);
        assertFalse(beforeReset.isEmpty());

        var resetOk = paymentService.resetAllPayments(ONE_TIME);
        assertTrue(resetOk);

        var afterReset = paymentService.getAllActivePaymentByType(ONE_TIME);
        assertTrue(afterReset.isEmpty());
    }

    @Test
    void shouldCalculateExpirationDateByMode() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var disabledExpiration = paymentService.getExpirationDate(PeriodicPaymentTypeEnum.DISABLED.name());
        assertNull(disabledExpiration);

        var perpetualExpiration = paymentService.getExpirationDate(PeriodicPaymentTypeEnum.PERPETUAL.name());
        assertNull(perpetualExpiration);

        var periodicalExpiration = paymentService.getExpirationDate(PeriodicPaymentTypeEnum.PERIODICAL.name());
        assertNotNull(periodicalExpiration);
        assertNotEquals(LocalDate.now(), periodicalExpiration);
    }

    @Test
    void shouldReturnFalseWhenResetAllPaymentsTypeIsNull() {
        var resetOk = paymentService.resetAllPayments(null);
        assertFalse(resetOk);
    }

    @Test
    void shouldIgnoreMissingUserWhenAppendingNames() {
        var responseList = List.of(
                io.oxalate.backend.api.response.PaymentStatusResponse.builder()
                                                                     .userId(999_999L)
                                                                     .status(io.oxalate.backend.api.UpdateStatusEnum.OK)
                                                                     .payments(List.of())
                                                                     .build()
        );

        paymentService.appendNameToPaymentStatusResponse(responseList);
        assertNull(responseList.getFirst()
                               .getName());
    }

    @Test
    void shouldReturnNullWhenIncreaseOneTimeDisabled() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.DISABLED, PeriodicPaymentTypeEnum.DISABLED);

        var response = paymentService.increaseOneTimePayment(diver.getId(), 2);
        assertNull(response);
    }

    @Test
    void shouldCreateNewOneTimeWhenUpdateIdNotFound() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .id(999_999L)
                                                                .userId(diver.getId())
                                                                .paymentCount(6)
                                                                .paymentType(ONE_TIME)
                                                                .startDate(LocalDate.now()
                                                                                    .plusDays(1))
                                                                .build());

        assertNotNull(response);
        assertEquals(6, response.getPaymentCount());
        assertTrue(response.getId() > 0);
    }

    @Test
    void shouldSavePeriodicalViaSavePaymentSwitch() {
        applyPaymentModeConfig(PeriodicPaymentTypeEnum.PERIODICAL, PeriodicPaymentTypeEnum.PERIODICAL);

        var response = paymentService.savePayment(PaymentRequest.builder()
                                                                .userId(diver.getId())
                                                                .paymentType(PERIODICAL)
                                                                .startDate(LocalDate.now())
                                                                .build());

        assertNotNull(response);
        assertEquals(PERIODICAL, response.getPaymentType());
    }

    @Test
    void shouldThrowWhenSavePaymentTypeIsNull() {
        assertThrows(NullPointerException.class,
                () -> paymentService.savePayment(PaymentRequest.builder()
                                                               .userId(diver.getId())
                                                               .paymentType(null)
                                                               .build()));
    }

    @Test
    void shouldFindAllPaymentsOrderedByStartDateDesc() {
        createPayment(diver.getId(), ONE_TIME, 1, LocalDate.now()
                                                           .minusDays(10), LocalDate.now()
                                                                                    .plusDays(1));
        createPayment(diver.getId(), PERIODICAL, 0, LocalDate.now()
                                                             .minusDays(2), LocalDate.now()
                                                                                     .plusDays(20));

        var payments = paymentService.findAllByUserId(diver.getId());

        assertTrue(payments.size() >= 2);
        assertTrue(!payments.get(0)
                            .getStartDate()
                            .isBefore(payments.get(1)
                                              .getStartDate()));
    }

    private void applyDeterministicOneTimePaymentConfig() {
        portalConfigurationService.setRuntimeValue(PAYMENT.group, SINGLE_PAYMENT_ENABLED.key, "true");
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PAYMENT_ENABLED.key, "true");

        // One-time expiration and periodical method type are both considered in current PaymentService mode resolution.
        portalConfigurationService.setRuntimeValue(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_UNIT.key, ChronoUnit.YEARS.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_LENGTH.key, "1");

        portalConfigurationService.setRuntimeValue(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key, PeriodicPaymentTypeEnum.PERIODICAL.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_UNIT.key, ChronoUnit.YEARS.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PAYMENT_PERIOD_LENGTH.key, "1");
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key, "1");
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PAYMENT_PERIOD_START.key, LocalDate.now()
                                                                                                     .minusYears(1)
                                                                                                     .toString());

        portalConfigurationService.reloadPortalConfigurations();

        assertEquals("true", portalConfigurationService.getStringConfiguration(PAYMENT.group, SINGLE_PAYMENT_ENABLED.key));
        assertEquals(PeriodicPaymentTypeEnum.PERIODICAL.name(),
                portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key));
        assertEquals(PeriodicPaymentTypeEnum.PERIODICAL.name(),
                portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key));
    }

    private void applyPaymentModeConfig(PeriodicPaymentTypeEnum oneTimeType, PeriodicPaymentTypeEnum periodicalType) {
        portalConfigurationService.setRuntimeValue(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key, oneTimeType.name());
        portalConfigurationService.setRuntimeValue(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key, periodicalType.name());
        portalConfigurationService.reloadPortalConfigurations();
    }

    private Payment createPayment(long userId,
            PaymentTypeEnum type,
            int count,
            LocalDate startDate,
            LocalDate endDate) {
        return paymentRepository.save(Payment.builder()
                                             .userId(userId)
                                             .paymentType(type)
                                             .paymentCount(count)
                                             .created(Instant.now())
                                             .startDate(startDate)
                                             .endDate(endDate)
                                             .build());
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
}
