package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request used when updating an existing dive group.
 */
@Schema(description = "Request to update an existing dive group")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiveGroupUpdateRequest {

    @Schema(description = "New name of the dive group", example = "Team Sidemount", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 1, max = 255)
    @JsonProperty("name")
    private String name;

    @Schema(description = "Optional new owner of the group. Only organizers and administrators may transfer the ownership.", example = "123")
    @JsonProperty("ownerId")
    private Long ownerId;
}
