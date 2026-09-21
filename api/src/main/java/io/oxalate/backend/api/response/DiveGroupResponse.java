package io.oxalate.backend.api.response;

import io.oxalate.backend.api.DiveGroupTypeEnum;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A dive group belonging to a specific dive event, including its current members.
 */
@Schema(description = "A dive group of a dive event")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DiveGroupResponse {

    @Schema(description = "Unique identifier of the dive group", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long id;

    @Schema(description = "ID of the dive event the group belongs to", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private long eventId;

    @Schema(description = "Name of the dive group", example = "Team Sidemount", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Free-text description of the dive group, null when none has been given", example = "We dive the wreck first and then the reef")
    private String description;

    @Schema(description = "User ID of the owner of the dive group", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private long ownerId;

    @Schema(description = "Full name of the owner of the dive group", example = "John Doe")
    private String ownerName;

    @Schema(description = "Type of the dive group, either a normal dive or a special project dive", example = "NORMAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private DiveGroupTypeEnum groupType;

    @Schema(description = "Position of the dive group within the dive event, starting from 1", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private int groupOrder;

    @Schema(description = "Timestamp of when the dive group was created", example = "2023-10-05T14:48:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;

    @Schema(description = "Timestamp of when the dive group was last updated", example = "2023-10-06T14:48:00Z")
    private Instant updatedAt;

    @Schema(description = "Members of the dive group")
    private List<DiveGroupMemberResponse> members;

    @Schema(description = "Dive files uploaded by the members of the dive group")
    private List<DiveFileResponse> diveFiles;
}
