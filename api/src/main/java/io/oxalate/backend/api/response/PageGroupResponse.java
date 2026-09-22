package io.oxalate.backend.api.response;

import io.oxalate.backend.api.PageStatusEnum;
import java.util.List;
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
public class PageGroupResponse {

    private long id;

    private PageStatusEnum status;

    private List<PageGroupVersionResponse> pageGroupVersions;

    private List<PageResponse> pages;
}
