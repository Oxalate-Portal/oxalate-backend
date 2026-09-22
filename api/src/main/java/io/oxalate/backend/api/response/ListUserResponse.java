package io.oxalate.backend.api.response;

import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A minimum user response containing only the id and name which is the last and first name concatenated. Is not anonymized since this should only be used by
 * organizers and admins. Used for dropdown lists etc.
 */

@Schema(description = "")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ListUserResponse {
    @Schema(description = "Unique identifier of the user", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long id;

    @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    protected String name;

    @Schema(description = "Count of dives associated with the user", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long eventDiveCount;

    @Schema(description = "Timestamp of when the user was registered", example = "2023-10-05T14:48:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    protected Instant createdAt;

    @Schema(description = "List of payments made by the user", requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<PaymentResponse> payments;

    @Schema(description = "Indicates if the user has an active membership", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    protected boolean membershipActive;

    @Schema(description = "In what capacity the user usually takes part in events", example = "SCUBA_DIVER", requiredMode = Schema.RequiredMode.REQUIRED)
    protected UserTypeEnum userType;

    @Schema(description = "Most advanced certificate classification of the user")
    protected String certificateClassificationTitle;

    @Schema(description = "List of tags associated with the user")
    private Set<TagResponse> tags;
}
