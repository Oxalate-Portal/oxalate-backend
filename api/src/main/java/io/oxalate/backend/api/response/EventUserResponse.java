package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * A minimum user response containing only the id and name which is the last and first name concatenated. Is not anonymized since this should only be used by
 * organizers and admins. Used for dropdown lists etc.
 */

@Builder
@Data
public class EventUserResponse {
    @JsonProperty("id")
    protected long id;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("eventDiveCount")
    protected long eventDiveCount;

    @JsonProperty("createdAt")
    protected Instant createdAt;

    @JsonProperty("payments")
    protected Set<PaymentResponse> payments;
}
