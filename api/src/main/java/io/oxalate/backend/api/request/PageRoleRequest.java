package io.oxalate.backend.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRoleRequest {

    @JsonProperty("role")
    private RoleEnum role;

    @JsonProperty("readPermission")
    private boolean readPermission;

    @JsonProperty("writePermission")
    private boolean writePermission;
}
