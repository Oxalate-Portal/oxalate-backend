package io.oxalate.backend.api.response;

import io.oxalate.backend.api.PageStatusEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PageResponse {

    private long id;

    private PageStatusEnum status;

    private long pageGroupId;

    private List<PageVersionResponse> pageVersions;

    private Set<PageRoleAccessResponse> rolePermissions;

    @Min(0)
    private long creator;

    @NotNull
    private Instant createdAt;

    private Long modifier;

    private Instant modifiedAt;
}
