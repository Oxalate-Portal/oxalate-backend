package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Membership request")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MembershipRequest {
    @Schema(description = "membership ID", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("id")
    private long id;

    @Schema(description = "ID of the user for whom the membership is", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("userId")
    private long userId;

    @Schema(description = "Membership status, this is not used when creating a new membership.", example = "EXPIRED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("status")
    private MembershipStatusEnum status;

    @Schema(description = "Membership type, no type should be DISABLED as it signifies that the membership functionality is not in use, this is not used when creating a new membership.", example = "PERIODICAL", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("type")
    private MembershipTypeEnum type;

    @Schema(description = "Optional membership start date time in ISO-8601 format, needed in some cases such as allowing joining before the period begins",
            example = "2024-01-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("startDate")
    private LocalDate startDate;

    @Schema(description = "Optional membership end date time in ISO-8601 format, needed in some cases such as periodical memberships which is for future periods",
            example = "2024-12-31", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("endDate")
    private LocalDate endDate;
}
