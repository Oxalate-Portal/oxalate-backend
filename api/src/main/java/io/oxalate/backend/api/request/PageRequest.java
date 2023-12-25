package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.PageStatusEnum;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    private PageStatusEnum status;

    @JsonProperty("pageGroupId")
    private long pageGroupId;

    @JsonProperty("rolePermissions")
    private Set<PageRoleRequest> rolePermissions;

    @JsonProperty("pageVersions")
    private Set<PageVersionRequest> pageVersions;
}
