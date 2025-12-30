package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * A minimum user response containing only the id and name which is the last and first name concatenated. Is not anonymized since this should only be used by
 * organizers and admins. Used for dropdown lists etc.
 */

@Schema(description = "")
@Builder
@Data
public class ListUserResponse {
    @Schema(description = "Unique identifier of the user", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    protected long id;

    @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    protected String name;

    @Schema(description = "Count of dives associated with the user", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("eventDiveCount")
    protected long eventDiveCount;

    @Schema(description = "Timestamp of when the user was registered", example = "2023-10-05T14:48:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("createdAt")
    protected Instant createdAt;

    @Schema(description = "List of payments made by the user", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("payments")
    protected List<PaymentResponse> payments;

    @Schema(description = "Indicates if the user has an active membership", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("membershipActive")
    protected boolean membershipActive;

    @Schema(description = "In what capacity the user usually takes part in events", example = "SCUBA_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("userType")
    protected UserTypeEnum userType;

    @Schema(description = "List of tags associated with the user")
    @JsonProperty("tags")
    private Set<TagResponse> tags;
}
