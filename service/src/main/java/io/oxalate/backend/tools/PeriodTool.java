package io.oxalate.backend.tools;

import io.oxalate.backend.model.PeriodResult;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

public class PeriodTool {
    public static PeriodResult calculatePeriod(Instant now, LocalDate startDate, ChronoUnit calendarUnit, int periodStart, int unitCount) {
        // Ensure valid input for the period start
        if (periodStart <= 0 || periodStart > getMaxUnitValue(calendarUnit)) {
            throw new IllegalArgumentException("Invalid period start value for the given calendar unit.");
        }

        // Convert `now` to LocalDate for easier manipulation
        LocalDate currentDate = now.atZone(ZoneId.systemDefault())
                                   .toLocalDate();

        // Align the first period's start date
        // Iterate forward in unitCount increments to find the correct period
        LocalDate periodStartDate = alignFirstPeriodStart(startDate, calendarUnit, periodStart);

        while (!isDateWithinPeriod(currentDate, periodStartDate, calendarUnit, unitCount)) {
            periodStartDate = periodStartDate.plus(unitCount, calendarUnit);
        }

        // Calculate the end date of the identified period
        LocalDate periodEndDate = calculatePeriodEnd(periodStartDate, calendarUnit, unitCount);

        return PeriodResult.builder()
                           .startDate(periodStartDate)
                           .endDate(periodEndDate)
                           .build();
    }

    private static LocalDate alignFirstPeriodStart(LocalDate startDate, ChronoUnit unit, int periodStart) {
        return switch (unit) {
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
    }

    private static boolean isDateWithinPeriod(LocalDate date, LocalDate periodStart, ChronoUnit unit, int unitCount) {
        LocalDate periodEnd = calculatePeriodEnd(periodStart, unit, unitCount);
        return (date.isEqual(periodStart) || date.isAfter(periodStart)) && date.isBefore(periodEnd.plusDays(1));
    }

    private static LocalDate calculatePeriodEnd(LocalDate periodStart, ChronoUnit unit, int unitCount) {
        return periodStart.plus(unitCount, unit);
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
