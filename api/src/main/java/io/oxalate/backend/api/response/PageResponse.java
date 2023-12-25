package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {

    @JsonProperty("id")
    private long id;

    @JsonProperty("status")
    private PageStatusEnum status;

    @JsonProperty("pageGroupId")
    private long pageGroupId;

    @JsonProperty("pageVersions")
    private List<PageVersionResponse> pageVersions;

    @JsonProperty("rolePermissions")
    private Set<PageRoleAccessResponse> rolePermissions;

    @Min(0)
    @JsonProperty("creator")
    private long creator;

    @NotNull
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("modifier")
    private Long modifier;

    @JsonProperty("modifiedAt")
    private Instant modifiedAt;
}
