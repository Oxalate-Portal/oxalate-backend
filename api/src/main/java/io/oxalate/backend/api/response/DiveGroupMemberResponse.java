package io.oxalate.backend.api.response;

import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A single member of a dive group.
 */
@Schema(description = "A member of a dive group")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DiveGroupMemberResponse {

    @Schema(description = "Unique identifier of the user", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long userId;

    @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "In what capacity the user takes part in the event", example = "SCUBA_DIVER")
    private UserTypeEnum userType;

    @Schema(description = "Whether this member is the owner of the dive group", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean owner;

    @Schema(description = "Timestamp of when the user joined the dive group", example = "2023-10-05T14:48:00Z")
    private Instant joinedAt;
}
