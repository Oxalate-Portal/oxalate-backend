package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A dive group belonging to a specific dive event, including its current members.
 */
@Schema(description = "A dive group of a dive event")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiveGroupResponse {

    @Schema(description = "Unique identifier of the dive group", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    private long id;

    @Schema(description = "ID of the dive event the group belongs to", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("eventId")
    private long eventId;

    @Schema(description = "Name of the dive group", example = "Team Sidemount", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    private String name;

    @Schema(description = "User ID of the owner of the dive group", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("ownerId")
    private long ownerId;

    @Schema(description = "Full name of the owner of the dive group", example = "John Doe")
    @JsonProperty("ownerName")
    private String ownerName;

    @Schema(description = "Timestamp of when the dive group was created", example = "2023-10-05T14:48:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(description = "Timestamp of when the dive group was last updated", example = "2023-10-06T14:48:00Z")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @Schema(description = "Members of the dive group")
    @JsonProperty("members")
    private List<DiveGroupMemberResponse> members;
}
