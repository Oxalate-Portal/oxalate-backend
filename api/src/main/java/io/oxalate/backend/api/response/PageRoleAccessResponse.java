package io.oxalate.backend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.oxalate.backend.api.RoleEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRoleAccessResponse {
    @JsonProperty("id")
    private long id;

    @JsonProperty("pageId")
    private long pageId;

    @JsonProperty("role")
    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    @JsonProperty("readPermission")
    private boolean readPermission;

    @JsonProperty("writePermission")
    private boolean writePermission;
}
