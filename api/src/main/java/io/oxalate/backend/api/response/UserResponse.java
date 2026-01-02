package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.AbstractUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse extends AbstractUser {

    @Schema(description = "Count of dives associated with the user", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("diveCount")
    private long diveCount;

    @Schema(description = "List of payments made by the user", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("payments")
    private List<PaymentResponse> payments;

    @Schema(description = "List of memberships of the user", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("memberships")
    private List<MembershipResponse> memberships;

    @Schema(description = "List of tags associated with the user")
    @JsonProperty("tags")
    private Set<TagResponse> tags;
}
