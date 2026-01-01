package io.oxalate.backend.tools;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
public class PeriodToolUTC {
    @CsvSource({
            "2024-02-06, 2023-04-22, YEARS,   2, 1, 2024-02-01, 2025-02-01", // Easy case
            "2024-02-06, 2023-04-22, YEARS,   3, 1, 2023-03-01, 2024-03-01", // Handle leap year
            "2024-02-06, 2023-04-22, YEARS,   3, 6, 2023-03-01, 2029-03-01", // Handle multiple years
            "2024-02-06, 2023-04-22, MONTHS,  3, 1, 2024-02-01, 2024-03-01",
            "2024-08-06, 2023-04-22, MONTHS,  2, 5, 2024-05-01, 2024-10-01",
            "2024-08-06, 2024-04-22, WEEKS,   1, 1, 2024-08-05, 2024-08-12",
            "2024-08-06, 2024-04-22, WEEKS,  26, 4, 2024-07-22, 2024-08-19"
    })
    @ParameterizedTest
    void periodResultV2Ok(LocalDate nowInstant, LocalDate localDate, ChronoUnit calendarUnit, long periodStart, long unitCount, LocalDate startDate,
            LocalDate endDate) {
        var periodResult = PeriodTool.calculatePeriod(nowInstant, localDate, calendarUnit, periodStart, unitCount);
        assertEquals(startDate, periodResult.getStartDate());
        assertEquals(endDate, periodResult.getEndDate());
    }

    @Test
    void futurePeriodCalculation() {
        var currentYear = LocalDate.now(ZoneId.systemDefault())
                                   .getYear();
        var nowInstant = LocalDate.now()
                                  .plusDays(5 * 366);
        var localDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        var calendarUnit = ChronoUnit.YEARS;
        var periodStart = 1L;
        var unitCount = 1L;

        var periodResult = PeriodTool.calculatePeriod(nowInstant, localDate, calendarUnit, periodStart, unitCount);
        assertEquals(LocalDate.of(currentYear + 5, 1, 1), periodResult.getStartDate());
        assertEquals(LocalDate.of(currentYear + 6, 1, 1), periodResult.getEndDate());
    }
}
