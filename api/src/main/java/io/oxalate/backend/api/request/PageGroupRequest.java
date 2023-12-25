package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    private Set<PageGroupVersionRequest> pageGroupVersions;
}
