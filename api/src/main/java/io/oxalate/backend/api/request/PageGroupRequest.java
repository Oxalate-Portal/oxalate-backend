package io.oxalate.backend.api.request;

import io.oxalate.backend.api.PageStatusEnum;
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
public class PageGroupRequest {

    private Long id;

    private PageStatusEnum status;

    private Set<PageGroupVersionRequest> pageGroupVersions;
}
