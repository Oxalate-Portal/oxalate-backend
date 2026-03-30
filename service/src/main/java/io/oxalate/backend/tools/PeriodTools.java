package io.oxalate.backend.tools;

import io.oxalate.backend.model.PeriodResult;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PeriodTools {
    public static PeriodResult calculatePeriod(LocalDate currentDate, LocalDate startDate, ChronoUnit calendarUnit, long periodStart, long unitCount) {
        log.debug("calculatePeriod called with currentDate={}, startDate={}, calendarUnit={}, periodStart={}, unitCount={}",
                currentDate, startDate, calendarUnit, periodStart, unitCount);

        // Ensure valid input for the period start
        if (periodStart <= 0 || periodStart > getMaxUnitValue(calendarUnit)) {
            throw new IllegalArgumentException("Invalid period start value for the given calendar unit.");
        }

        if (unitCount <= 0) {
            throw new IllegalArgumentException("Unit count must be greater than zero.");
        }

        // Align the first period's start date
        var periodStartDate = alignFirstPeriodStart(startDate, calendarUnit, (int) periodStart);
        log.debug("Aligned initial period start date to {}", periodStartDate);

        // Move backwards when querying dates earlier than the configured anchor to avoid forward overflow.
        var backwardsIterations = 0;
        while (currentDate.isBefore(periodStartDate)) {
            var previousPeriodStartDate = periodStartDate;
            periodStartDate = periodStartDate.minus(unitCount, calendarUnit);
            backwardsIterations++;
            log.debug("Moved period start backwards from {} to {} (iteration={})", previousPeriodStartDate, periodStartDate, backwardsIterations);
        }

        // Iterate forward in unitCount increments to find the matching period.
        var forwardIterations = 0;
        while (!isDateWithinPeriod(currentDate, periodStartDate, calendarUnit, (int) unitCount)) {
            var previousPeriodStartDate = periodStartDate;
            periodStartDate = periodStartDate.plus(unitCount, calendarUnit);
            forwardIterations++;
            log.debug("Moved period start forward from {} to {} (iteration={})", previousPeriodStartDate, periodStartDate, forwardIterations);
        }

        // Calculate the end date of the identified period
        var periodEndDate = calculatePeriodEnd(periodStartDate, calendarUnit, (int) unitCount);
        log.debug("Resolved period result startDate={}, endDate={}, backwardsIterations={}, forwardIterations={}",
                periodStartDate, periodEndDate, backwardsIterations, forwardIterations);

        return PeriodResult.builder()
                           .startDate(periodStartDate)
                           .endDate(periodEndDate)
                           .build();
    }

    private static LocalDate alignFirstPeriodStart(LocalDate startDate, ChronoUnit unit, int periodStart) {
        var alignedDate = switch (unit) {
            case YEARS, MONTHS -> startDate.withMonth(periodStart)
                                           .withDayOfMonth(1);
            case WEEKS ->
                // Align to the start of the specified week of the year
                    LocalDate.of(startDate.getYear(), 1, 1)
                             .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, periodStart)
                             .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case DAYS -> startDate;
            default -> throw new IllegalArgumentException("Unsupported calendar unit: " + unit);
        };

        log.debug("alignFirstPeriodStart resolved startDate={}, unit={}, periodStart={} to alignedDate={}",
                startDate, unit, periodStart, alignedDate);

        return alignedDate;
    }

    private static boolean isDateWithinPeriod(LocalDate date, LocalDate periodStart, ChronoUnit unit, int unitCount) {
        var periodEnd = calculatePeriodEnd(periodStart, unit, unitCount);
        // Use an exclusive end boundary so exact boundary dates belong to the next period.
        var withinPeriod = (date.isEqual(periodStart) || date.isAfter(periodStart)) && date.isBefore(periodEnd);
        log.debug("isDateWithinPeriod evaluated date={}, periodStart={}, periodEnd={}, unit={}, unitCount={}, withinPeriod={}",
                date, periodStart, periodEnd, unit, unitCount, withinPeriod);
        return withinPeriod;
    }

    private static LocalDate calculatePeriodEnd(LocalDate periodStart, ChronoUnit unit, int unitCount) {
        var periodEnd = periodStart.plus(unitCount, unit);
        log.debug("calculatePeriodEnd resolved periodStart={}, unit={}, unitCount={} to periodEnd={}",
                periodStart, unit, unitCount, periodEnd);
        return periodEnd;
    }

    private static int getMaxUnitValue(ChronoUnit unit) {
        return switch (unit) {
            case YEARS -> 12; // Months in a year
            case MONTHS -> 12; // Same as years
            case WEEKS -> 53;  // Weeks in a year
            case DAYS -> 31;  // Days in a month
            default -> throw new IllegalArgumentException("Unsupported calendar unit: " + unit);
        };
    }
}
