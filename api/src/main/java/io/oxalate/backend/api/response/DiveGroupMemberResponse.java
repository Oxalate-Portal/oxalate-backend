package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single member of a dive group.
 */
@Schema(description = "A member of a dive group")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiveGroupMemberResponse {

    @Schema(description = "Unique identifier of the user", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("userId")
    private long userId;

    @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    private String name;

    @Schema(description = "In what capacity the user takes part in the event", example = "SCUBA_DIVER")
    @JsonProperty("userType")
    private UserTypeEnum userType;

    @Schema(description = "Whether this member is the owner of the dive group", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("owner")
    private boolean owner;

    @Schema(description = "Timestamp of when the user joined the dive group", example = "2023-10-05T14:48:00Z")
    @JsonProperty("joinedAt")
    private Instant joinedAt;
}
