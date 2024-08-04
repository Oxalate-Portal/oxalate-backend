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
public class PageGroupRequest {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    private PageStatusEnum status;

    private Set<PageGroupVersionRequest> pageGroupVersions;
}
