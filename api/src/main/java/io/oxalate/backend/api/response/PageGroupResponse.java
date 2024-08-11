package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.PageStatusEnum;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageGroupResponse {

    @JsonProperty("id")
    private long id;

    @JsonProperty("status")
    private PageStatusEnum status;

    @JsonProperty("pageGroupVersions")
    private List<PageGroupVersionResponse> pageGroupVersions;

    @JsonProperty("pages")
    private List<PageResponse> pages;
}
