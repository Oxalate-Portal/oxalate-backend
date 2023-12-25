package io.oxalate.backend.model;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.request.PageRoleRequest;
import io.oxalate.backend.api.response.PageRoleAccessResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "page_role_access")
public class PageRoleAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "page_id")
    private long pageId;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    @Column(name = "read_permission")
    private boolean readPermission;

    @Column(name = "write_permission")
    private boolean writePermission;

    public static PageRoleAccess of(PageRoleRequest rolePermissionRequest, long pageId) {
        return PageRoleAccess.builder()
                             .pageId(pageId)
                             .role(rolePermissionRequest.getRole())
                             .readPermission(rolePermissionRequest.isReadPermission())
                             .writePermission(rolePermissionRequest.isWritePermission())
                             .build();
    }

    public PageRoleAccessResponse toResponse() {
        return PageRoleAccessResponse.builder()
                                     .id(this.id)
                                     .pageId(this.pageId)
                                     .role(this.role)
                                     .readPermission(this.readPermission)
                                     .writePermission(this.writePermission)
                                     .build();
    }
}
