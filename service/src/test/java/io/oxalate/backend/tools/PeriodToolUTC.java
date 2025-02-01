package io.oxalate.backend.tools;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
public class PeriodToolUTC {
    @CsvSource({
            "2024-02-06T13:13:13.432Z, 2023-04-22, YEARS,   2, 1, 2024-02-01, 2025-02-01", // Easy case
            "2024-02-06T13:13:13.432Z, 2023-04-22, YEARS,   3, 1, 2023-03-01, 2024-03-01", // Handle leap year
            "2024-02-06T13:13:13.432Z, 2023-04-22, YEARS,   3, 6, 2023-03-01, 2029-03-01", // Handle multiple years
            "2024-02-06T13:13:13.432Z, 2023-04-22, MONTHS,  3, 1, 2024-02-01, 2024-03-01",
            "2024-08-06T13:13:13.432Z, 2023-04-22, MONTHS,  2, 5, 2024-05-01, 2024-10-01",
            "2024-08-06T13:13:13.432Z, 2024-04-22, WEEKS,   1, 1, 2024-08-05, 2024-08-12",
            "2024-08-06T13:13:13.432Z, 2024-04-22, WEEKS,  26, 4, 2024-07-22, 2024-08-19"
    })
    @ParameterizedTest
    void periodResultV2Ok(Instant nowInstant, LocalDate localDate, ChronoUnit calendarUnit, int periodStart, int unitCount, LocalDate startDate,
            LocalDate endDate) {
        var periodResult = PeriodTool.calculatePeriod(nowInstant, localDate, calendarUnit, periodStart, unitCount);
        assertEquals(startDate, periodResult.getStartDate());
        assertEquals(endDate, periodResult.getEndDate());
    }
}
