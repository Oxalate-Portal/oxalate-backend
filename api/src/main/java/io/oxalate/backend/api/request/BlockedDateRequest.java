package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedDateRequest {
    @JsonProperty(value = "blockedDate", required = true)
    private Date blockedDate;
    @JsonProperty(value = "reason", required = true)
    private String reason;
}
