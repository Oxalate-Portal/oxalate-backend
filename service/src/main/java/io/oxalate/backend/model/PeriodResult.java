package io.oxalate.backend.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@Builder
@AllArgsConstructor
public class PeriodResult {
    private final LocalDate startDate;
    private final LocalDate endDate;
}
