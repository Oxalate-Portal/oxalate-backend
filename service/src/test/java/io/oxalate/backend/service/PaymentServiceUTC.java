package io.oxalate.backend.service;

import io.oxalate.backend.tools.PeriodTool;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit test cases for PaymentService, at this point we mainly test the date calculations
 */

@Slf4j
public class PaymentServiceUTC {
    @ParameterizedTest
    @CsvSource({
            "durational, 2025-01-01, Europe/Helsinki,  YEARS,   1, 2025-01-01, 1,  1, 1, 2026",
            "durational, 2025-01-31, Europe/Helsinki,  YEARS,   1, 2025-01-01, 1, 31, 1, 2026",
            "durational, 2024-02-29, Europe/Helsinki,  YEARS,   1, 2024-01-01, 1, 28, 2, 2025", // Leap year
            "durational, 2025-01-01, Europe/Helsinki,  MONTHS,  1, 2025-01-01, 1,  1, 2, 2025",
            "durational, 2025-01-31, Europe/Helsinki,  MONTHS,  1, 2025-01-01, 1, 28, 2, 2025", // Shorter month
            "durational, 2024-01-29, Europe/Helsinki,  MONTHS,  1, 2024-01-01, 2, 29, 2, 2024", // Leap year
            "durational, 2025-01-01, Europe/Helsinki,  DAYS,    1, 2025-01-01, 1,  2, 1, 2025",
            "durational, 2025-01-01, Europe/Helsinki,  DAYS,   30, 2025-01-01, 1, 31, 1, 2025",
            "durational, 2025-01-01, Europe/Helsinki,  DAYS,   60, 2025-01-01, 1,  2, 3, 2025",
            "durational, 2025-01-01, Australia/Hobart, YEARS,   1, 2025-01-01, 1,  1, 1, 2026",
            "durational, 2025-01-01, Australia/Hobart, MONTHS,  1, 2025-01-01, 1,  1, 2, 2025",
            "durational, 2025-01-01, Australia/Hobart, DAYS,    1, 2025-01-01, 1,  2, 1, 2025",
    })
    void getExpirationDateOk(String expirationType, LocalDate localDate, String timezone, ChronoUnit chronoUnit, long unitCount,
            LocalDate startDate, long periodStartPoint, int expectedDay, int expectedMonth, int expectedYear) {

        var result = getExpirationDate(expirationType, localDate, timezone, chronoUnit, unitCount, startDate, periodStartPoint);

        log.info("Expiration time: {}", result);
        assertTrue(result.isAfter(localDate));
        assertEquals(expectedDay, result.getDayOfMonth());
        assertEquals(expectedMonth, result.getMonthValue());
        assertEquals(expectedYear, result.getYear());
    }

    protected LocalDate getExpirationDate(String oneTimeExpirationType, LocalDate localDate, String timezone, ChronoUnit chronoUnit,
            long unitCounts, LocalDate startDate, long periodStartPoint) {

        // This switch is copied from PaymentService.getExpirationDate()
        // >8 paste from here
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
}
