package io.oxalate.backend.api.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyEventResponse {
    private Instant eventDate;
    private String eventName;
}
