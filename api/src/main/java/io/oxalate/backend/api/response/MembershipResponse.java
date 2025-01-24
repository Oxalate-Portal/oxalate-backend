package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Membership request")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MembershipResponse {
    @Schema(description = "membership ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    private long id;

    @Schema(description = "ID of the user for whom the membership is", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("userId")
    private long userId;

    @Schema(description = "Clear text name of the user for whom the membership is", example = "Charlie Brown", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("username")
    private String username;

    @Schema(description = "Membership status", example = "EXPIRED", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    private MembershipStatusEnum status;

    @Schema(description = "Membership type, no type should be DISABLED as it signifies that the membership functionality is not in use", example = "PERIODICAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("type")
    private MembershipTypeEnum type;

    @Schema(description = "When was the periodic payment done", example = "2023-04-12T11:22:33.542Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("createdAt")
    private LocalDate createdAt;

    @Schema(description = "When will the periodic payment expire", example = "2023-04-12T11:22:33.542Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("expiresAt")
    private LocalDate expiresAt;
}
