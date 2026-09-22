package io.oxalate.backend.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Request used when setting the order of the dive groups of a single dive event. The list must contain every dive
 * group of the dive event exactly once; the position in the list defines the new order of the group.
 */
@Schema(description = "Request to set the order of the dive groups of a dive event")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DiveGroupOrderRequest {

    @Schema(description = "IDs of every dive group of the dive event in the wanted order", example = "[3, 1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private List<Long> diveGroupIds;
}
