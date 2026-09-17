package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request used by the members of a dive group when updating the name and description of their group. Ownership and
 * group type are deliberately not part of this request; those are managed through {@link DiveGroupUpdateRequest} by
 * the owner or the organizer of the dive event.
 */
@Schema(description = "Request to update the name and description of a dive group, available to the members of the group")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiveGroupDetailsRequest {

    @Schema(description = "New name of the dive group", example = "Team Sidemount", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Size(min = 1, max = 255)
    @JsonProperty("name")
    private String name;

    @Schema(description = "New free-text description of the dive group. An empty or missing value clears the description. The maximum length is set by "
            + "the portal configuration frontend.dive-group-description-max-length.", example = "We dive the wreck first and then the reef")
    @JsonProperty("description")
    private String description;
}
