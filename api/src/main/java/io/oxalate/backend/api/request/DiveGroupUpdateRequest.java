package io.oxalate.backend.api.request;

import io.oxalate.backend.api.DiveGroupTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Request used when updating an existing dive group.
 */
@Schema(description = "Request to update an existing dive group")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DiveGroupUpdateRequest {

    @Schema(description = "New name of the dive group", example = "Team Sidemount", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 1, max = 255)
    private String name;

    @Schema(description = "New free-text description of the dive group. An empty or missing value clears the description. The maximum length is set by "
            + "the portal configuration frontend.dive-group-description-max-length.", example = "We dive the wreck first and then the reef")
    private String description;

    @Schema(description = "Optional new owner of the group. Only organizers and administrators may transfer the ownership.", example = "123")
    private Long ownerId;

    @Schema(description = "Optional new type of the dive group. When omitted, the current type is kept.", example = "PROJECT")
    private DiveGroupTypeEnum groupType;
}
