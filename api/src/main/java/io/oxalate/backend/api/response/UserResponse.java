package io.oxalate.backend.api.response;

import io.oxalate.backend.api.AbstractUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserResponse extends AbstractUser {

    @Schema(description = "Count of dives associated with the user", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private long diveCount;

    @Schema(description = "List of payments made by the user", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PaymentResponse> payments;

    @Schema(description = "List of memberships of the user", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MembershipResponse> memberships;

    @Schema(description = "List of tags associated with the user")
    private Set<TagResponse> tags;
}
