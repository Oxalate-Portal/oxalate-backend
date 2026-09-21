package io.oxalate.backend.api.request;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Schema(description = "Membership request")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MembershipRequest {
    @Schema(description = "membership ID", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private long id;

    @Schema(description = "ID of the user for whom the membership is", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private long userId;

    @Schema(description = "Membership status, this is not used when creating a new membership.", example = "EXPIRED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MembershipStatusEnum status;

    @Schema(description = "Membership type, no type should be DISABLED as it signifies that the membership functionality is not in use, this is not used when creating a new membership.", example = "PERIODICAL", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MembershipTypeEnum type;

    @Schema(description = "Optional membership start date time in ISO-8601 format, needed in some cases such as allowing joining before the period begins",
            example = "2024-01-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate startDate;

    @Schema(description = "Optional membership end date time in ISO-8601 format, needed in some cases such as periodical memberships which is for future periods",
            example = "2024-12-31", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate endDate;
}
