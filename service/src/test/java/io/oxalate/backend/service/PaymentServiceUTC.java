package io.oxalate.backend.service;

import io.oxalate.backend.tools.PeriodTool;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cases for PaymentService, at this point we mainly test the date calculations
 */

@Slf4j
@ExtendWith(MockitoExtension.class)
public class PaymentServiceUTC {
    @ParameterizedTest
    @CsvSource({
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Europe/Helsinki,  YEARS,   1, 2025-01-01, 1,  1, 1, 2026",
            "durational, 2025-01-31, 2025-01-31T01:00:00.000Z, Europe/Helsinki,  YEARS,   1, 2025-01-01, 1, 31, 1, 2026",
            "durational, 2024-02-29, 2024-02-29T01:00:00.000Z, Europe/Helsinki,  YEARS,   1, 2024-01-01, 1, 28, 2, 2025", // Leap year
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Europe/Helsinki,  MONTHS,  1, 2025-01-01, 1,  1, 2, 2025",
            "durational, 2025-01-31, 2025-01-31T01:00:00.000Z, Europe/Helsinki,  MONTHS,  1, 2025-01-01, 1, 28, 2, 2025", // Shorter month
            "durational, 2024-01-29, 2024-01-29T01:00:00.000Z, Europe/Helsinki,  MONTHS,  1, 2024-01-01, 2, 29, 2, 2024", // Leap year
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Europe/Helsinki,  DAYS,    1, 2025-01-01, 1,  2, 1, 2025",
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Europe/Helsinki,  DAYS,   30, 2025-01-01, 1, 31, 1, 2025",
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Europe/Helsinki,  DAYS,   60, 2025-01-01, 1,  2, 3, 2025",
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Australia/Hobart, YEARS,   1, 2025-01-01, 1,  1, 1, 2026",
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Australia/Hobart, MONTHS,  1, 2025-01-01, 1,  1, 2, 2025",
            "durational, 2025-01-01, 2025-01-01T01:00:00.000Z, Australia/Hobart, DAYS,    1, 2025-01-01, 1,  2, 1, 2025",
    })
    void getExpirationTimeOk(String expirationType, LocalDate localDate, Instant instant, String timezone, ChronoUnit chronoUnit, long unitCount,
            LocalDate startDate, long periodStartPoint, int expectedDay, int expectedMonth, int expectedYear) {

        var result = getExpirationTime(expirationType, localDate, instant, timezone, chronoUnit, unitCount, startDate, periodStartPoint);

        log.info("Expiration time: {}", result);
        assertTrue(result.isAfter(instant));
        assertEquals(expectedDay, result.atZone(ZoneId.of(timezone))
                                        .getDayOfMonth());
        assertEquals(expectedMonth, result.atZone(ZoneId.of(timezone))
                                          .getMonthValue());
        assertEquals(expectedYear, result.atZone(ZoneId.of(timezone))
                                         .getYear());
    }

    protected Instant getExpirationTime(String oneTimeExpirationType, LocalDate localDate, Instant instant, String timezone, ChronoUnit chronoUnit,
            long unitCounts, LocalDate startDate, long periodStartPoint) {
        var zoneId = ZoneId.of(timezone);

        switch (oneTimeExpirationType) {
        case "disabled", "perpetual" -> {
            return null;
        }
        case "periodical" -> {
            var periodResult = PeriodTool.calculatePeriod(instant, startDate, chronoUnit, periodStartPoint, unitCounts);
            return periodResult.getEndDate()
                               .atStartOfDay(zoneId)
                               .toInstant();
        }
        case "durational" -> {
            // This gets tricky because some chronoUnits can not be just added to the current time, so e.g. if we add one month to 31.01., we should get 28.02.
            // same as if it was 30.01. or 29.01. We need to cover all months that are not 31 days long

            if (chronoUnit == ChronoUnit.MONTHS) {
                // Get the current date day
                var currentDate = localDate;
                var startDateDay = currentDate.getDayOfMonth();
                // Get the current month number
                var startDateMonth = currentDate.getMonthValue();
                var endYear = currentDate.getYear();
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
                    // If end month length is less than the current day, we need to set the day to the last day of the month
                    if (startDateDay > endMonthLength) {
                        endDay = endMonthLength;
                    }
                }
                // Now we can assemble the end date
                var endDateString = String.format("%d-%02d-%02d", endYear, endMonth, endDay);
                var endDate = LocalDate.parse(endDateString);
                // And convert it to an instant
                return endDate.atStartOfDay(zoneId)
                              .toInstant();
            } else if (chronoUnit == ChronoUnit.YEARS) {
                // This is almost the same as for months, but we only need to consider the leap year February, otherwise we just increase the year
                // If the current date is 29.02. we need to set the end date to 28.02. of the next year
                var currentDate = localDate;

                var startDateDay = currentDate.getDayOfMonth();
                var startDateMonth = currentDate.getMonthValue();
                var endYear = currentDate.getYear() + (int) unitCounts;
                var endDate = startDateDay;

                if (startDateDay == 29 && startDateMonth == 2) {
                    endDate = 28;
                }

                return LocalDate.of(endYear, startDateMonth, endDate)
                                .atStartOfDay(zoneId)
                                .toInstant();
            } else {
                return localDate.plus(unitCounts, chronoUnit)
                                .atStartOfDay()
                                .atZone(zoneId)
                                .toInstant();
            }
        }
        }

        return null;
    }
}
