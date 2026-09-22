package io.oxalate.backend.api.response;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
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
public class MembershipResponse {
    @Schema(description = "membership ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private long id;

    @Schema(description = "ID of the user for whom the membership is", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private long userId;

    @Schema(description = "Clear text name of the user for whom the membership is", example = "Charlie Brown", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Membership status", example = "EXPIRED", requiredMode = Schema.RequiredMode.REQUIRED)
    private MembershipStatusEnum status;

    @Schema(description = "Membership type, no type should be DISABLED as it signifies that the membership functionality is not in use", example = "PERIODICAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private MembershipTypeEnum type;

    @Schema(description = "When does the periodic membership begin", example = "2023-04-12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate startDate;

    @Schema(description = "When does the periodic membership end", example = "2023-04-12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate endDate;

    @Schema(description = "When was the membership created", example = "2023-04-12T12:23:41.000Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant created;
}
