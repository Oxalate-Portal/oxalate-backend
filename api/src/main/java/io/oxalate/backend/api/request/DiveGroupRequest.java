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
 * Request used when creating a new dive group for a specific dive event.
 */
@Schema(description = "Request to create a new dive group for a dive event")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiveGroupRequest {

    @Schema(description = "ID of the dive event the group belongs to", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @JsonProperty("eventId")
    private Long eventId;

    @Schema(description = "Name of the dive group", example = "Team Sidemount", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Size(min = 1, max = 255)
    @JsonProperty("name")
    private String name;

    @Schema(description = "Optional owner of the group. Only organizers and administrators may assign an owner other than themselves. "
            + "When omitted, the calling user becomes the owner.", example = "123")
    @JsonProperty("ownerId")
    private Long ownerId;
}
