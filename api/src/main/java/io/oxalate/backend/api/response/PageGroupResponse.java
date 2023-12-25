package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("pageGroupVersions")
    private List<PageGroupVersionResponse> pageGroupVersions;

    @JsonProperty("pages")
    private List<PageResponse> pages;
}
