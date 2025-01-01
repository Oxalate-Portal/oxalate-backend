package io.oxalate.backend.service;

import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIOD;
import static io.oxalate.backend.api.PortalConfigEnum.GENERAL;
import static io.oxalate.backend.api.PortalConfigEnum.GeneralConfigEnum.TIMEZONE;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_LENGTH;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_START;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_START_POINT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PERIODICAL_PAYMENT_METHOD_UNIT;
import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.PaymentStatusResponse;
import io.oxalate.backend.model.Payment;
import io.oxalate.backend.model.PeriodResult;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.tools.PeriodTool;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PortalConfigurationService portalConfigurationService;

    public List<PaymentStatusResponse> getAllActivePaymentStatus() {
        var paymentStatusResponses = new ArrayList<PaymentStatusResponse>();

        var usersWithActivePayments = paymentRepository.findAllUserIdWithActivePayments();

        for (Long userId : usersWithActivePayments) {
            paymentStatusResponses.add(getPaymentStatusForUser(userId));
        }

        return paymentStatusResponses;
    }

    public PaymentStatusResponse getPaymentStatusForUser(long userId) {
        var paymentResponses = getActivePaymentRespondsByUser(userId);

        var paymentStatusResponse = PaymentStatusResponse.builder()
                .userId(userId)
                .status(UpdateStatusEnum.OK)
                .payments(paymentResponses)
                .build();

        log.debug("Payment status response for user ID {}: {}", userId, paymentStatusResponse);
        return paymentStatusResponse;
    }

    public Set<PaymentResponse> getActivePaymentRespondsByUser(long userId) {
        var payments = getActivePaymentsByUser(userId);
        var paymentResponses = new HashSet<PaymentResponse>();

        for (Payment payment : payments) {
            paymentResponses.add(payment.toPaymentResponse());
        }

        log.debug("Found active payment responds for user ID {}: {}", userId, paymentResponses);
        return paymentResponses;
    }

    public PaymentTypeEnum getBestAvailablePaymentType(long userId) {
        // Check whether the user has a period payment, if not, mark up as a one time participation
        var payments = getActivePaymentsByUser(userId);

        if (!payments.isEmpty()) {
            for (var payment : payments) {
                if (payment.getPaymentType()
                           .equals(PaymentTypeEnum.PERIOD)) {
                    return PERIOD;
                }
            }
        }

        return ONE_TIME;
    }

    public Set<Payment> getActivePaymentsByUser(long userId) {
        // var payments = paymentRepository.findAllByUserId(userId);
        var activePayments = paymentRepository.findAllActiveByUserId(userId);

        if (activePayments.size() > 2) {
            log.error("User {} has more than 2 active payment entries", userId);
        }

        return activePayments;
    }

    @org.springframework.transaction.annotation.Transactional
    public PaymentStatusResponse savePayment(long userId, PaymentRequest paymentRequest) {
        switch (paymentRequest.getPaymentType()) {
        case ONE_TIME -> saveOneTimePayment(userId, paymentRequest.getPaymentCount());
        case PERIOD -> savePeriodPayment(userId);
        default -> log.error("Unknown payment type: {}", paymentRequest.getPaymentType());
        }

        return getPaymentStatusForUser(userId);
    }

    @Transactional
    public void saveOneTimePayment(long userId, long count) {
        var payment = Payment.builder()
                .userId(userId)
                .paymentType(ONE_TIME)
                .createdAt(Instant.now())
                .paymentCount((int) count)
                .build();

        paymentRepository.save(payment);
    }

    /**
     * Saves a period payment to the database, the payment will be saved for the current period
     *
     * @param userId User ID
     */

    @Transactional
    public void savePeriodPayment(long userId) {
        var now = Instant.now();

        // Get the type of period from the portal configuration
        var periodTypeString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key);

        if (periodTypeString == null) {
            log.error("Could not find period type for key: {}", PAYMENT_PERIOD_START_POINT.key);
            return;
        }

        var periodResult = new PeriodResult();

        var periodUnitString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_UNIT.key);
        var periodUnit = ChronoUnit.valueOf(periodUnitString);
        var unitCount = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_LENGTH.key);

        if (periodTypeString.equals("periodical")) {
            var periodStart = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key);
            var calculationStart = portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key);
            var calculationStartDate = LocalDate.parse(calculationStart);
            periodResult = PeriodTool.calculatePeriod(now, calculationStartDate, periodUnit, (int) periodStart, (int) unitCount);
        } else {
            periodResult.setStartDate(LocalDate.now());
            periodResult.setEndDate(LocalDate.now().plus(unitCount, periodUnit));
        }

        var timezone = portalConfigurationService.getStringConfiguration(GENERAL.group, TIMEZONE.key);
        var zoneId = ZoneId.of(timezone);
        var payment = Payment.builder()
                .userId(userId)
                .paymentType(PERIOD)
                .createdAt(Instant.now())
                .expiresAt(periodResult.getEndDate().atStartOfDay(zoneId).toInstant())
                .build();

        paymentRepository.save(payment);
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

        if (oneTimePayment.isEmpty() || oneTimePayment.get().getPaymentCount() < 1) {
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
        var oneTimePayment = paymentRepository.findByUserIdAndAndPaymentType(userId, ONE_TIME.name());

        if (oneTimePayment.isEmpty() || oneTimePayment.get().getPaymentCount() < 1) {
            var payment = Payment.builder()
                    .userId(userId)
                    .paymentType(ONE_TIME)
                    .createdAt(Instant.now())
                    .paymentCount(count)
                    .build();
            paymentRepository.save(payment);
            return getPaymentStatusForUser(userId);
        }

        var payment = oneTimePayment.get();
        payment.setPaymentCount(payment.getPaymentCount() + count);
        paymentRepository.save(payment);

        return getPaymentStatusForUser(userId);
    }

    @Transactional
    public boolean resetAllPeriodicPayments() {
        try {
            paymentRepository.resetAllPeriodicPayments();
        } catch (Exception e) {
            log.error("Failed to reset all periodic payments", e);
            return false;
        }

        return true;
    }
}
