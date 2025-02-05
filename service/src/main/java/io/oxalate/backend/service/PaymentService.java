package io.oxalate.backend.service;

import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIOD;
import static io.oxalate.backend.api.PortalConfigEnum.GENERAL;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.TIMEZONE;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_LENGTH;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_UNIT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_LENGTH;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_START;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_START_POINT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_UNIT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.SINGLE_PAYMENT_ENABLED;
import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.PaymentStatusResponse;
import io.oxalate.backend.model.Payment;
import io.oxalate.backend.model.PeriodResult;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.tools.PeriodTool;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PortalConfigurationService portalConfigurationService;
    private final EventParticipantsRepository eventParticipantsRepository;

    public List<PaymentStatusResponse> getAllActivePaymentStatus() {
        var paymentStatusResponses = new ArrayList<PaymentStatusResponse>();

        var usersWithActivePayments = paymentRepository.findAllUserIdWithActivePayments();

        for (Long userId : usersWithActivePayments) {
            paymentStatusResponses.add(getPaymentStatusForUser(userId));
        }

        return paymentStatusResponses;
    }

    public List<PaymentStatusResponse> getAllActivePaymentByType(PaymentTypeEnum paymentType) {
        var paymentStatusResponses = new ArrayList<PaymentStatusResponse>();

        var paymentList = paymentRepository.findAllPaymentsWithActivePaymentsAndPaymentType(paymentType.name());

        for (var payment : paymentList) {
            var paymentStatusResponse = PaymentStatusResponse.builder()
                                                             .userId(payment.getUserId())
                                                             .status(UpdateStatusEnum.OK)
                                                             .payments(Set.of(payment.toPaymentResponse()))
                                                             .build();
            paymentStatusResponses.add(paymentStatusResponse);
        }

        return paymentStatusResponses;
    }

    public PaymentStatusResponse getPaymentStatusForUser(long userId) {
        var paymentResponses = getActivePaymentResponsesByUser(userId);
        // Populate the list of one-time payments used in future events
        var futureEventList = eventParticipantsRepository.findOneTimeFutureEventParticipantsByUserId(userId);
        for (var paymentResponse : paymentResponses) {
            if (paymentResponse.getPaymentType()
                               .equals(ONE_TIME)) {
                paymentResponse.setBoundEvents(futureEventList);
            }
        }

        var paymentStatusResponse = PaymentStatusResponse.builder()
                                                         .userId(userId)
                                                         .status(UpdateStatusEnum.OK)
                                                         .payments(paymentResponses)
                                                         .build();

        log.debug("Payment status response for user ID {}: {}", userId, paymentStatusResponse);
        return paymentStatusResponse;
    }

    public Set<PaymentResponse> getActivePaymentResponsesByUser(long userId) {
        var payments = getActivePaymentsByUser(userId);
        var paymentResponses = new HashSet<PaymentResponse>();

        for (Payment payment : payments) {
            var paymentResponse = payment.toPaymentResponse();

            if (payment.getPaymentType().equals(ONE_TIME)) {
                var futureEventList = eventParticipantsRepository.findOneTimeFutureEventParticipantsByUserId(userId);
                paymentResponse.setBoundEvents(futureEventList);
            }

            paymentResponses.add(paymentResponse);
        }

        log.debug("Found active payment responds for user ID {}: {}", userId, paymentResponses);
        return paymentResponses;
    }

    public Optional<PaymentTypeEnum> getBestAvailablePaymentType(long userId) {
        // Check whether the user has a period payment, if not, mark up as a one time participation
        var payments = getActivePaymentsByUser(userId);

        if (!payments.isEmpty()) {
            for (var payment : payments) {
                if (payment.getPaymentType()
                           .equals(PaymentTypeEnum.PERIOD)) {
                    return Optional.of(PERIOD);
                }
            }

            // No period was found, the entry has to be a one-time payment which has a count > 0
            return Optional.of(ONE_TIME);
        }

        // This gets interesting, because what remains is the possibility that the user has a one-time payment with a count of 0 but which is still active
        var emptyActiveOneTimePayment = paymentRepository.findActiveOneTimeByUserId(userId);

        if (!emptyActiveOneTimePayment.isEmpty()) {
            return Optional.of(ONE_TIME);
        }

        return Optional.empty();
    }

    public Set<Payment> getActivePaymentsByUser(long userId) {
        // var payments = paymentRepository.findAllByUserId(userId);
        var activePayments = paymentRepository.findAllActiveByUserId(userId);

        if (activePayments.size() > 2) {
            log.error("User {} has more than 2 active payment entries", userId);
        }

        return activePayments;
    }

    @Transactional
    public PaymentStatusResponse savePayment(PaymentRequest paymentRequest) {
        PaymentResponse PaymentResponse = null;

        switch (paymentRequest.getPaymentType()) {
        case ONE_TIME -> PaymentResponse = saveOneTimePayment(paymentRequest);
        case PERIOD -> PaymentResponse = savePeriodPayment(paymentRequest.getUserId());
        default -> log.error("Unknown payment type: {}", paymentRequest.getPaymentType());
        }

        if (PaymentResponse == null) {
            return PaymentStatusResponse.builder()
                                        .userId(paymentRequest.getUserId())
                                        .status(UpdateStatusEnum.FAIL)
                                        .build();
        }

        return getPaymentStatusForUser(paymentRequest.getUserId());
    }

    @Transactional
    public PaymentResponse saveOneTimePayment(PaymentRequest paymentRequest) {
        // Check first if one-time payment is enabled
        var isOneTimeEnabled = portalConfigurationService.getBooleanConfiguration(PAYMENT.group, SINGLE_PAYMENT_ENABLED.key);
        if (!isOneTimeEnabled) {
            log.warn("One-time payment ({} {}) is disabled: {}", PAYMENT.group, SINGLE_PAYMENT_ENABLED.key, isOneTimeEnabled);
            return null;
        }

        log.debug("Saving one-time payment: {}", paymentRequest);
        // First check if the given request points to an active one-time payment
        if (paymentRequest.getId() > 0L) {
            log.debug("Updating existing one-time payment ID: {}", paymentRequest.getId());
            var oldPayment = paymentRepository.findById(paymentRequest.getId());

            if (oldPayment.isPresent()
                    && (oldPayment.get()
                                  .getExpiresAt() == null || oldPayment.get()
                                                                       .getExpiresAt()
                                                                       .isAfter(Instant.now()))) {
                var payment = oldPayment.get();
                payment.setPaymentCount(paymentRequest.getPaymentCount());

                // Check if the current portal configuration for payments has the expiration enabled
                var oneTimeType = portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key);

                var timezone = portalConfigurationService.getStringConfiguration(GENERAL.group, TIMEZONE.key);
                var zoneId = ZoneId.of(timezone);
                var chronoUnit = ChronoUnit.valueOf(portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_UNIT.key));
                var expirationUnits = portalConfigurationService.getNumericConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_LENGTH.key);
                var startDate = LocalDate.parse(portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key));
                var periodStartPoint = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key);

                switch (oneTimeType) {
                case "perpetual" -> payment.setExpiresAt(null);
                case "periodical" -> {
                    var periodResult = PeriodTool.calculatePeriod(Instant.now(), startDate, chronoUnit, periodStartPoint, expirationUnits);
                    payment.setExpiresAt(periodResult.getEndDate()
                                                     .atStartOfDay(zoneId)
                                                     .toInstant());
                }
                case "durational" -> payment.setExpiresAt(Instant.now()
                                                                 .plus(expirationUnits, chronoUnit)
                                                                 .atZone(zoneId)
                                                                 .toInstant());
                }

                var newPayment = paymentRepository.save(payment);

                return newPayment.toPaymentResponse();
            } else {
                log.warn("Could not find active one-time payment with ID: {}", paymentRequest.getId());
            }
        }

        // Else find the latest non-expired one-time payment
        var activeOnetimePaymentList = paymentRepository.findActiveOneTimeByUserId(paymentRequest.getUserId());

        if (activeOnetimePaymentList.isEmpty()) {
            // If the one-time expiration configuration is enabled, then we need to calculate the expiration date
            Instant expiresAt = null;
            if (!portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key)
                                           .equals("disabled")) {
                var timezone = portalConfigurationService.getStringConfiguration(GENERAL.group, TIMEZONE.key);
                var zoneId = ZoneId.of(timezone);
                var chronoUnit = ChronoUnit.valueOf(portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_UNIT.key));
                var expirationUnits = portalConfigurationService.getNumericConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_LENGTH.key);
                expiresAt = Instant.now()
                                   .plus(expirationUnits, chronoUnit)
                                   .atZone(zoneId)
                                   .toInstant();
            }

            var payment = Payment.builder()
                                 .userId(paymentRequest.getUserId())
                                 .paymentType(ONE_TIME)
                                 .createdAt(Instant.now())
                                 .expiresAt(expiresAt)
                                 .paymentCount(paymentRequest.getPaymentCount())
                                 .build();

            var newPayment = paymentRepository.save(payment);

            return newPayment.toPaymentResponse();
        } else {
            var payment = activeOnetimePaymentList.getFirst();
            payment.setPaymentCount(payment.getPaymentCount());
            var newPayment = paymentRepository.save(payment);

            return newPayment.toPaymentResponse();
        }
    }

    /**
     * Saves a period payment to the database, the payment will be saved for the current period
     *
     * @param userId User ID
     */

    @Transactional
    public PaymentResponse savePeriodPayment(long userId) {
        var now = Instant.now();

        // Get the type of period from the portal configuration
        var periodTypeString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key);

        if (periodTypeString == null) {
            log.error("Could not find period type for key: {}", PAYMENT_PERIOD_START_POINT.key);
            return null;
        }

        var periodResult = new PeriodResult();

        var periodUnitString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_UNIT.key);
        var periodUnit = ChronoUnit.valueOf(periodUnitString);
        var unitCount = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_LENGTH.key);

        if (periodTypeString.equals("periodical")) {
            var periodStart = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key);
            var calculationStart = portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key);
            var calculationStartDate = LocalDate.parse(calculationStart);
            periodResult = PeriodTool.calculatePeriod(now, calculationStartDate, periodUnit, periodStart, unitCount);
        } else {
            periodResult.setStartDate(LocalDate.now());
            periodResult.setEndDate(LocalDate.now()
                                             .plus(unitCount, periodUnit));
        }

        var timezone = portalConfigurationService.getStringConfiguration(GENERAL.group, TIMEZONE.key);
        var zoneId = ZoneId.of(timezone);
        var payment = Payment.builder()
                             .userId(userId)
                             .paymentType(PERIOD)
                             .createdAt(Instant.now())
                             .expiresAt(periodResult.getEndDate()
                                                    .atStartOfDay(zoneId)
                                                    .toInstant())
                             .build();

        var newPayment = paymentRepository.save(payment);
        return newPayment.toPaymentResponse();
    }

    /**
     * Decreases the payment counter for a user, returns null if there were no valid one-time payment entries
     *
     * @param userId User ID whose payment entry will be decreased
     * @return Updated PaymentStatusResponse
     */

    @Transactional
    public PaymentStatusResponse decreaseOneTimePayment(long userId) {
        var oneTimePayment = paymentRepository.findByUserIdAndAndPaymentType(userId, ONE_TIME.name());

        if (oneTimePayment.isEmpty() || oneTimePayment.get()
                                                      .getPaymentCount() < 1) {
            log.warn("User {} does not have any valid time payment entries", userId);
            return null;
        }

        var payment = oneTimePayment.get();
        payment.setPaymentCount(payment.getPaymentCount() - 1);
        paymentRepository.save(payment);

        return getPaymentStatusForUser(userId);
    }

    /**
     * Increases the payment counter for a user, returns null if there were no valid one-time payment entries. This should only be used
     * when the user is rewarded as it increases the counter on an existing payment entry
     *
     * @param userId User ID whose payment entry will be increased
     * @param count  Amount of payments to increase
     * @return Updated PaymentStatusResponse
     */
    @Transactional
    public PaymentStatusResponse increaseOneTimePayment(Long userId, int count) {
        var oneTimePayment = paymentRepository.findActiveOneTimeByUserId(userId);

        if (oneTimePayment.isEmpty()) {
            var payment = Payment.builder()
                                 .userId(userId)
                                 .paymentType(ONE_TIME)
                                 .createdAt(Instant.now())
                                 .paymentCount(count)
                                 .build();
            paymentRepository.save(payment);
            return getPaymentStatusForUser(userId);
        }

        var payment = oneTimePayment.getFirst();
        payment.setPaymentCount(payment.getPaymentCount() + count);
        paymentRepository.save(payment);

        return getPaymentStatusForUser(userId);
    }

    @Transactional
    public boolean resetAllPayments(PaymentTypeEnum paymentType) {
        try {
            paymentRepository.resetAllPayments(paymentType.name());
        } catch (Exception e) {
            log.error("Failed to reset all periodic payments", e);
            return false;
        }

        return true;
    }
}
