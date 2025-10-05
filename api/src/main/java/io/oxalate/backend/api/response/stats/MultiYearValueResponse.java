package io.oxalate.backend.api.response.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiYearValueResponse {
    private long year;
    private long value;
    private String type;
}
