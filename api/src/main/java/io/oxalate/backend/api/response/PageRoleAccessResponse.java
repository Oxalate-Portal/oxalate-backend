package io.oxalate.backend.api.response;

import io.oxalate.backend.api.RoleEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class PageRoleAccessResponse {
    private long id;

    private long pageId;

    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    private boolean readPermission;

    private boolean writePermission;
}
