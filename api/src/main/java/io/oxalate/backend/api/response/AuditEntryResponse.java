package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AuditLevel;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntryResponse {
    @JsonProperty("id")
    private long id;
    @JsonProperty("traceId")
    private String traceId;
    @JsonProperty("source")
    private String source;
    @JsonProperty("level")
    private AuditLevel level;
    @JsonProperty("userId")
    private long userId;
    @JsonProperty("userName")
    private String userName;
    @JsonProperty("address")
    private String address;
    @JsonProperty("message")
    private String message;
    @JsonProperty("createdAt")
    private Instant createdAt;

}
