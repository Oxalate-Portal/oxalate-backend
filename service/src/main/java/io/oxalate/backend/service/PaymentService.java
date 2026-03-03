package io.oxalate.backend.service;

import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIODICAL;
import io.oxalate.backend.api.PeriodicPaymentTypeEnum;
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
import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.PaymentStatusResponse;
import io.oxalate.backend.model.Payment;
import io.oxalate.backend.model.PeriodResult;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.tools.PeriodTool;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

        // Check first if one-time payment is enabled
        // Get the type of period from the portal configuration
        var periodTypeString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key);
        var periodType = PeriodicPaymentTypeEnum.valueOf(periodTypeString);

        if (periodType.equals(PeriodicPaymentTypeEnum.DISABLED)) {
            log.warn("One-time payment type is disabled in configuration, we do not proceed on creating the one-time payment");
            return null;
        }

        // If the period type is perpetual, we only ever have one active one-time payment per user, and if more are set to be created then we add the count
        // to the existing one-time payment
        if (periodType.equals(PeriodicPaymentTypeEnum.PERPETUAL)) {
            log.debug("One-time payment type is perpetual, checking period configuration");
            var existingOneTimePayment = paymentRepository.findAllByUserIdAndPaymentType(userId, ONE_TIME)
                                                          .stream()
                                                          .filter(payment -> payment.getEndDate() == null)
                                                          .toList();

            if (!existingOneTimePayment.isEmpty()) {
                log.debug("User {} already has an active perpetual one-time payment, updating the payment count", userId);
                var payment = existingOneTimePayment.getFirst();
                // First we add the new count to the existing one
                payment.setPaymentCount(payment.getPaymentCount() + paymentRequest.getPaymentCount());

                // Remove the first and check if there are any left, if then collect their counts too and close them. existingOneTimePayment is an immutable list
                // so we need to create a new one which we can modify
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
            } else {
                log.debug("User {} does not have an active perpetual one-time payment, creating a new one", userId);
                var payment = Payment.builder()
                                     .userId(paymentRequest.getUserId())
                                     .paymentType(ONE_TIME)
                                     .created(Instant.now())
                                     .startDate(now)
                                     .endDate(null)
                                     .paymentCount(paymentRequest.getPaymentCount())
                                     .build();
            }
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

                // Check if the current portal configuration for payments has the expiration enabled
                var oneTimeExpirationType = portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key);
                var endDate = getExpirationDate(oneTimeExpirationType);
                payment.setEndDate(endDate);

                var newPayment = paymentRepository.save(payment);

                return newPayment.toPaymentResponse();
            } else {
                log.warn("Could not find active one-time payment with ID: {}", paymentRequest.getId());
            }
        }

        var periodResult = getOneTimeResult(periodType, now);
        var requestedStartDate = paymentRequest.getStartDate() != null ? paymentRequest.getStartDate() : periodResult.getStartDate();
        var requestedEndDate = paymentRequest.getEndDate() != null ? paymentRequest.getEndDate() : periodResult.getEndDate();
        // Else find the latest non-expired one-time payment

        // Check that the user does not already have an overlapping payment, only one active periodical payment is allowed at a time
        var oneTimePayments = paymentRepository.findAllByUserIdAndPaymentType(userId, ONE_TIME);
        var overlappingOnePayments = oneTimePayments.stream()
                                                    .filter(payment -> payment.getPaymentType()
                                                                              .equals(ONE_TIME)
                                                            && payment.getEndDate()
                                                                      .isAfter(requestedStartDate))
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

        // Get the type of period from the portal configuration
        var periodTypeString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_TYPE.key);
        var periodType = PeriodicPaymentTypeEnum.valueOf(periodTypeString);

        if (periodType.equals(PeriodicPaymentTypeEnum.DISABLED)) {
            log.warn("Periodical payment type is disabled in configuration, we do not proceed on creating the periodical payment");
            return null;
        }

        var periodResult = getPeriodResult(periodType, now);
        var requestedStartDate = paymentRequest.getStartDate() != null ? paymentRequest.getStartDate() : periodResult.getStartDate();
        var requestedEndDate = paymentRequest.getEndDate() != null ? paymentRequest.getEndDate() : periodResult.getEndDate();

        // Check that the user does not already have an overlapping payment, only one active periodical payment is allowed at a time
        var periodicalPayments = paymentRepository.findAllByUserIdAndPaymentType(userId, PERIODICAL);
        var overlappingPeriodPayments = periodicalPayments.stream()
                                                          .filter(payment -> payment.getPaymentType()
                                                                                    .equals(PERIODICAL)
                                                                  && payment.getEndDate()
                                                                            .isAfter(requestedStartDate))
                                                          .toList();

        if (!overlappingPeriodPayments.isEmpty()) {
            log.warn("User already has an active periodical payment: {}", overlappingPeriodPayments.getFirst());
            return overlappingPeriodPayments.getFirst()
                                            .toPaymentResponse();
        }

        // Last thing: Check if the user has any one-time payments that would overlap with the new periodical payment, and if so, set their end date to the
        // start date of the new periodical payment
        var oneTimePayments = paymentRepository.findByUserIdAndAndPaymentType(userId, ONE_TIME.name())
                                               .stream()
                                               .filter(payment -> payment.getPaymentType()
                                                                         .equals(ONE_TIME)
                                                       && payment.getEndDate()
                                                                 .isAfter(requestedStartDate))
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

        if (oneTimePayment.isEmpty()) {
            var oneTimeExpirationType = portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_TYPE.key);
            var endDate = getExpirationDate(oneTimeExpirationType);
            var payment = Payment.builder()
                                 .userId(userId)
                                 .paymentType(ONE_TIME)
                                 .created(Instant.now())
                                 .endDate(endDate)
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
        var timezone = portalConfigurationService.getStringConfiguration(GENERAL.group, TIMEZONE.key);
        var zoneId = ZoneId.of(timezone);
        var chronoUnit = ChronoUnit.valueOf(portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_UNIT.key));
        var unitCounts = portalConfigurationService.getNumericConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_LENGTH.key);
        var startDate = LocalDate.parse(portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key));
        var periodStartPoint = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key);
        var localDate = LocalDate.now(zoneId);

        // When this switch is modified, it has to be copied also to PaymentServiceUTC for testing purposes
        // >8 copy from here
        switch (oneTimeExpirationType) {
        case "disabled", "perpetual" -> {
            return null;
        }
        case "periodical" -> {
            var periodResult = PeriodTool.calculatePeriod(LocalDate.now(), startDate, chronoUnit, periodStartPoint, unitCounts);
            return periodResult.getEndDate();
        }
        case "durational" -> {
            // This gets tricky because some chronoUnits can not be just added to the current time, so e.g. if we add one month to 31.01., we should get 28.02.
            // same as if it was 30.01. or 29.01. We need to cover all months that are not 31 days long

            if (chronoUnit == ChronoUnit.MONTHS) {
                // Get the current date day
                var startDateDay = localDate.getDayOfMonth();
                // Get the current month number
                var startDateMonth = localDate.getMonthValue();
                var endYear = localDate.getYear();
                // Calculate the end month number
                int endMonth = startDateMonth + (int) unitCounts;
                int endDay = startDateDay;

                while (endMonth > 12) {
                    endMonth = endMonth - 12;
                    endYear++;
                }

                // At this point we have sorted out the year and the month of the end date. Next we need to figure out what the day should be.
                // We can just raise the month number with the unit counts if the current month day is 28 or less.
                // Else we need to do some magic to get the correct day.
                if (startDateDay > 28) {
                    // Get the length of the end month of the end year (keep in mind that it could be a leap year February
                    var endMonthLength = LocalDate.of(endYear, endMonth, 1)
                                                  .lengthOfMonth();
                    log.info("End month length for year {} month {} is {} when start day is {}", endYear, endMonth, endMonthLength, startDateDay);
                    // If end month length is less than the current day, we need to set the day to the last day of the month
                    if (endMonthLength < startDateDay) {
                        endDay = endMonthLength;
                    }
                }
                // Now we can assemble the end date
                var endDateString = String.format("%d-%02d-%02d", endYear, endMonth, endDay);
                log.info("Calculated end date string: {}", endDateString);
                return LocalDate.parse(endDateString);
            } else if (chronoUnit == ChronoUnit.YEARS) {
                // This is almost the same as for months, but we only need to consider the leap year February, otherwise we just increase the year
                // If the current date is 29.02. we need to set the end date to 28.02. of the next year
                var startDateDay = localDate.getDayOfMonth();
                var startDateMonth = localDate.getMonthValue();
                var endYear = localDate.getYear() + (int) unitCounts;
                var endDate = startDateDay;

                if (startDateDay == 29 && startDateMonth == 2) {
                    endDate = 28;
                }

                return LocalDate.of(endYear, startDateMonth, endDate);
            } else {
                return localDate.plus(unitCounts, chronoUnit);
            }
        }
        }
        // 8< to here

        return null;
    }

    private PeriodResult getPeriodResult(PeriodicPaymentTypeEnum periodicPaymentType, LocalDate localDateNow) {
        var periodResult = new PeriodResult();
        var periodUnitString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, PERIODICAL_PAYMENT_METHOD_UNIT.key);
        var periodUnit = ChronoUnit.valueOf(periodUnitString);
        var periodLength = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_LENGTH.key);

        if (periodicPaymentType.equals(PeriodicPaymentTypeEnum.PERIODICAL)) {
            var periodStartPoint = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key);
            var calculationStart = portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key);
            var calculationStartDate = LocalDate.parse(calculationStart);
            periodResult = PeriodTool.calculatePeriod(localDateNow, calculationStartDate, periodUnit, periodStartPoint,
                    periodLength);
        } else {
            periodResult.setStartDate(localDateNow);
            periodResult.setEndDate(localDateNow.plus(periodLength, periodUnit));
        }
        return periodResult;
    }

    private PeriodResult getOneTimeResult(PeriodicPaymentTypeEnum periodicPaymentType, LocalDate localDateNow) {
        var periodResult = new PeriodResult();
        var periodUnitString = portalConfigurationService.getEnumConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_UNIT.key);
        var periodUnit = ChronoUnit.valueOf(periodUnitString);
        var periodLength = portalConfigurationService.getNumericConfiguration(PAYMENT.group, ONE_TIME_PAYMENT_EXPIRATION_LENGTH.key);

        if (periodicPaymentType.equals(PeriodicPaymentTypeEnum.PERIODICAL)) {
            var periodStartPoint = portalConfigurationService.getNumericConfiguration(PAYMENT.group, PAYMENT_PERIOD_START_POINT.key);
            var calculationStart = portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key);
            var calculationStartDate = LocalDate.parse(calculationStart);
            periodResult = PeriodTool.calculatePeriod(localDateNow, calculationStartDate, periodUnit, periodStartPoint,
                    periodLength);
        } else {
            periodResult.setStartDate(localDateNow);
            periodResult.setEndDate(localDateNow.plus(periodLength, periodUnit));
        }
        return periodResult;
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
