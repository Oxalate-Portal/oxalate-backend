package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedDateResponse {
    @JsonProperty("id")
    private long id;
    @JsonProperty("blockedDate")
    private Date blockedDate;
    @JsonProperty("createdAt")
    private Instant createdAt;
    @JsonProperty("creator")
    private long creator;
}
