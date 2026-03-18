package io.oxalate.backend.service;

import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIODICAL;
import io.oxalate.backend.api.PeriodicPaymentTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_LENGTH;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.ONE_TIME_PAYMENT_EXPIRATION_UNIT;
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
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.tools.PeriodTools;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {
    private final PortalConfigurationService portalConfigurationService;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

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

        var paymentList = paymentRepository.findAllCurrentAndFuturePaymentByPaymentType(paymentType.name());

        for (var payment : paymentList) {
            var paymentStatusResponse = PaymentStatusResponse.builder()
                                                             .userId(payment.getUserId())
                                                             .status(UpdateStatusEnum.OK)
                                                             .payments(List.of(payment.toPaymentResponse()))
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

    public List<PaymentResponse> getActivePaymentResponsesByUser(long userId) {
        var payments = getActivePaymentsByUser(userId);
        var paymentResponses = new ArrayList<PaymentResponse>();

        for (Payment payment : payments) {
            var paymentResponse = payment.toPaymentResponse();

            if (payment.getPaymentType()
                       .equals(ONE_TIME)) {
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
                           .equals(PaymentTypeEnum.PERIODICAL)) {
                    return Optional.of(PERIODICAL);
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

    public List<Payment> getActivePaymentsByUser(long userId) {
        return paymentRepository.findAllCurrentPaymentsByUserId(userId);
    }

    public List<Payment> findAllByUserId(long userId) {
        return paymentRepository.findAllByUserIdOrderByStartDateDesc(userId);
    }

    @Transactional
    public PaymentResponse savePayment(PaymentRequest paymentRequest) {
        var paymentResponse = PaymentResponse.builder()
                                             .id(0L)
                                             .userId(0L)
                                             .paymentType(null)
                                             .paymentCount(0)
                                             .created(null)
                                             .startDate(null)
                                             .endDate(null)
                                             .build();

        switch (paymentRequest.getPaymentType()) {
        case ONE_TIME -> paymentResponse = saveOneTimePayment(paymentRequest);
        case PERIODICAL -> paymentResponse = savePeriodPayment(paymentRequest);
        default -> log.error("Unknown payment type: {}", paymentRequest.getPaymentType());
        }

        return paymentResponse;
    }

    @Transactional
    public PaymentResponse saveOneTimePayment(PaymentRequest paymentRequest) {
        var now = LocalDate.now();
        var userId = paymentRequest.getUserId();
        var effectivePaymentMode = getEffectivePaymentMode();

        if (effectivePaymentMode.equals(PeriodicPaymentTypeEnum.DISABLED)) {
            log.warn("Payment creation/update is disabled by configuration, skipping one-time payment handling");
            return null;
        }

        // If the effective mode is perpetual, only one active one-time payment should exist and all duplicates are consolidated.
        if (effectivePaymentMode.equals(PeriodicPaymentTypeEnum.PERPETUAL)) {
            log.debug("Effective payment mode is perpetual, checking existing one-time entries");
            var existingOneTimePayment = paymentRepository.findAllByUserIdAndPaymentType(userId, ONE_TIME)
                                                          .stream()
                                                          .filter(payment -> payment.getEndDate() == null)
                                                          .toList();

            if (!existingOneTimePayment.isEmpty()) {
                log.debug("User {} already has an active perpetual one-time payment, updating the payment count", userId);
                var payment = existingOneTimePayment.getFirst();
                payment.setPaymentCount(payment.getPaymentCount() + paymentRequest.getPaymentCount());

                var mutableList = new ArrayList<>(existingOneTimePayment);
                mutableList.removeFirst();

                for (var oldPayment : mutableList) {
                    log.debug("Found additional perpetual one-time payment ID {}, adding its count {} to the main payment and closing it",
                            oldPayment.getId(), oldPayment.getPaymentCount());
                    payment.setPaymentCount(payment.getPaymentCount() + oldPayment.getPaymentCount());
                    oldPayment.setEndDate(now);
                    oldPayment.setPaymentCount(0);
                    paymentRepository.save(oldPayment);
                }

                var newPayment = paymentRepository.save(payment);
                return newPayment.toPaymentResponse();
            }

            log.debug("User {} does not have an active perpetual one-time payment, creating a new one", userId);
            var payment = Payment.builder()
                                 .userId(paymentRequest.getUserId())
                                 .paymentType(ONE_TIME)
                                 .created(Instant.now())
                                 .startDate(now)
                                 .endDate(null)
                                 .paymentCount(paymentRequest.getPaymentCount())
                                 .build();
            var newPayment = paymentRepository.save(payment);
            return newPayment.toPaymentResponse();
        }

        // First check if the given request points to an active one-time payment
        if (paymentRequest.getId() > 0L) {
            log.debug("Updating existing one-time payment ID: {}", paymentRequest.getId());
            var oldPayment = paymentRepository.findById(paymentRequest.getId());

            if (oldPayment.isPresent() && (oldPayment.get()
                                                     .getEndDate() == null || oldPayment.get()
                                                                                        .getEndDate()
                                                                                        .isAfter(LocalDate.now()))) {
                var payment = oldPayment.get();
                payment.setPaymentCount(paymentRequest.getPaymentCount());
                payment.setEndDate(calculateEndDateForMode(PaymentTypeEnum.ONE_TIME, effectivePaymentMode, now));

                var newPayment = paymentRepository.save(payment);
                return newPayment.toPaymentResponse();
            }

            log.warn("Could not find active one-time payment with ID: {}", paymentRequest.getId());
        }

        var requestedStartDate = paymentRequest.getStartDate() != null ? paymentRequest.getStartDate() : now;
        var requestedEndDate = resolveRequestedOrCalculatedEndDate(paymentRequest, PaymentTypeEnum.ONE_TIME, effectivePaymentMode, requestedStartDate);

        var oneTimePayments = paymentRepository.findAllByUserIdAndPaymentType(userId, ONE_TIME);
        var overlappingOnePayments = oneTimePayments.stream()
                                                    .filter(payment -> payment.getPaymentType()
                                                                              .equals(ONE_TIME)
                                                            && isActiveOnDate(payment, now))
                                                    .toList();

        if (!overlappingOnePayments.isEmpty()) {
            log.warn("User already has an active one-time payment: {}", overlappingOnePayments.getFirst());
            return overlappingOnePayments.getFirst()
                                         .toPaymentResponse();
        }

        var payment = Payment.builder()
                             .userId(paymentRequest.getUserId())
                             .paymentType(ONE_TIME)
                             .created(Instant.now())
                             .startDate(requestedStartDate)
                             .endDate(requestedEndDate)
                             .paymentCount(paymentRequest.getPaymentCount())
                             .build();

        var newPayment = paymentRepository.save(payment);

        return newPayment.toPaymentResponse();
    }

    /**
     * Saves a period payment to the database, the payment will be saved for the current period
     *
     * @param paymentRequest Payment request
     * @return Saved payment response
     */

    @Transactional
    public PaymentResponse savePeriodPayment(PaymentRequest paymentRequest) {
        var now = LocalDate.now();
        var userId = paymentRequest.getUserId();
        var effectivePaymentMode = getEffectivePaymentMode();

        if (effectivePaymentMode.equals(PeriodicPaymentTypeEnum.DISABLED)) {
            log.warn("Payment creation/update is disabled by configuration, skipping periodical payment handling");
            return null;
        }

        var requestedStartDate = paymentRequest.getStartDate() != null ? paymentRequest.getStartDate() : now;
        var requestedEndDate = resolveRequestedOrCalculatedEndDate(paymentRequest, PaymentTypeEnum.PERIODICAL, effectivePaymentMode, requestedStartDate);

        var periodicalPayments = paymentRepository.findAllByUserIdAndPaymentType(userId, PERIODICAL);
        var overlappingPeriodPayments = periodicalPayments.stream()
                                                          .filter(payment -> payment.getPaymentType()
                                                                                    .equals(PERIODICAL)
                                                                  && isActiveOnDate(payment, now))
                                                          .toList();

        if (!overlappingPeriodPayments.isEmpty()) {
            log.warn("User already has an active periodical payment: {}", overlappingPeriodPayments.getFirst());
            return overlappingPeriodPayments.getFirst()
                                            .toPaymentResponse();
        }

        var oneTimePayments = paymentRepository.findByUserIdAndAndPaymentType(userId, ONE_TIME.name())
                                               .stream()
                                               .filter(payment -> payment.getPaymentType()
                                                                         .equals(ONE_TIME)
                                                       && isActiveOnDate(payment, now))
                                               .toList();

        if (!oneTimePayments.isEmpty()) {
            for (var oneTimePayment : oneTimePayments) {
                oneTimePayment.setEndDate(requestedStartDate.minusDays(1));
                paymentRepository.save(oneTimePayment);
            }
        }

        var payment = Payment.builder()
                             .userId(userId)
                             .paymentType(PERIODICAL)
                             .created(Instant.now())
                             .startDate(requestedStartDate)
                             .endDate(requestedEndDate)
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
        var effectivePaymentMode = getEffectivePaymentMode();

        if (effectivePaymentMode.equals(PeriodicPaymentTypeEnum.DISABLED)) {
            log.warn("Payment creation/update is disabled by configuration, skipping one-time payment increment");
            return null;
        }

        if (oneTimePayment.isEmpty()) {
            var payment = Payment.builder()
                                 .userId(userId)
                                 .paymentType(ONE_TIME)
                                 .created(Instant.now())
                                 .startDate(LocalDate.now())
                                 .endDate(calculateEndDateForMode(PaymentTypeEnum.ONE_TIME, effectivePaymentMode, LocalDate.now()))
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

    protected LocalDate getExpirationDate(String oneTimeExpirationType) {
        var normalizedType = normalizePeriodicType(oneTimeExpirationType);

        if (normalizedType.equals(PeriodicPaymentTypeEnum.DISABLED) || normalizedType.equals(PeriodicPaymentTypeEnum.PERPETUAL)) {
            return null;
        }

        return calculateEndDateForMode(PaymentTypeEnum.ONE_TIME, normalizedType, LocalDate.now());
    }

    private PeriodicPaymentTypeEnum getEffectivePaymentMode() {
        var oneTimeExpirationType = normalizePeriodicType(
                portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key));
        var periodicalMethodType = normalizePeriodicType(
                portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key));

        if (oneTimeExpirationType.equals(PeriodicPaymentTypeEnum.DISABLED)
                || periodicalMethodType.equals(PeriodicPaymentTypeEnum.DISABLED)) {
            return PeriodicPaymentTypeEnum.DISABLED;
        }

        if (oneTimeExpirationType.equals(PeriodicPaymentTypeEnum.PERPETUAL)
                || periodicalMethodType.equals(PeriodicPaymentTypeEnum.PERPETUAL)) {
            return PeriodicPaymentTypeEnum.PERPETUAL;
        }

        return oneTimeExpirationType;
    }

    private LocalDate calculateEndDateForMode(PaymentTypeEnum paymentType, PeriodicPaymentTypeEnum paymentMode, LocalDate calculationDate) {
        if (paymentMode.equals(PeriodicPaymentTypeEnum.PERPETUAL)) {
            return null;
        }

        // PERIODICAL mode uses the shared payment period configuration for both one-time and periodical entries.
        var useSharedPeriodConfig = paymentMode.equals(PeriodicPaymentTypeEnum.PERIODICAL);

        var chronoUnit = switch (paymentType) {
            case ONE_TIME -> useSharedPeriodConfig
                    ? ChronoUnit.valueOf(portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_UNIT.key))
                    : ChronoUnit.valueOf(portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_UNIT.key));
            case PERIODICAL -> ChronoUnit.valueOf(portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_UNIT.key));
            default -> throw new IllegalArgumentException("Unsupported payment type for end date calculation: " + paymentType);
        };

        var unitCount = switch (paymentType) {
            case ONE_TIME -> useSharedPeriodConfig
                    ? portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_LENGTH.key)
                    : portalConfigurationService.getNumericConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_LENGTH.key);
            case PERIODICAL -> portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_LENGTH.key);
            default -> throw new IllegalArgumentException("Unsupported payment type for end date calculation: " + paymentType);
        };

        var periodStart = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key);
        var periodAnchor = LocalDate.parse(portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key));

        return PeriodTools.calculatePeriod(calculationDate, periodAnchor, chronoUnit, periodStart, unitCount)
                         .getEndDate();
    }

    private LocalDate resolveRequestedOrCalculatedEndDate(PaymentRequest paymentRequest,
            PaymentTypeEnum paymentType,
            PeriodicPaymentTypeEnum paymentMode,
            LocalDate requestedStartDate) {
        var requestedEndDate = paymentRequest.getEndDate();

        // Preserve explicitly provided historical ranges used by integrations/imports.
        if (paymentRequest.getStartDate() != null
                && requestedEndDate != null
                && !requestedEndDate.isAfter(LocalDate.now())) {
            return requestedEndDate;
        }

        return calculateEndDateForMode(paymentType, paymentMode, requestedStartDate);
    }

    private PeriodicPaymentTypeEnum normalizePeriodicType(String type) {
        return PeriodicPaymentTypeEnum.valueOf(type.toUpperCase(Locale.ROOT));
    }

    private boolean isActiveOnDate(Payment payment, LocalDate date) {
        var started = payment.getStartDate() == null || !payment.getStartDate()
                                                                .isAfter(date);
        var notEnded = payment.getEndDate() == null || !payment.getEndDate()
                                                               .isBefore(date);
        return started && notEnded;
    }

    /**
     * Enriches the given list of PaymentStatusResponse objects with the user's full name.
     *
     * @param responseList List of payment status responses to enrich
     */
    public void appendNameToPaymentStatusResponse(List<PaymentStatusResponse> responseList) {
        for (PaymentStatusResponse response : responseList) {
            var optionalUser = userRepository.findById(response.getUserId());

            if (optionalUser.isEmpty()) {
                log.error("User ID {} from payments could not be found", response.getUserId());
                continue;
            }

            var user = optionalUser.get();
            response.setName(user.getLastName() + " " + user.getFirstName());
        }
    }
}
