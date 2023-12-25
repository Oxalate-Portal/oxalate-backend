package io.oxalate.backend.model;

import io.oxalate.backend.api.PageStatusEnum;
import io.oxalate.backend.api.request.PageRequest;
import io.oxalate.backend.api.response.PageResponse;
import io.oxalate.backend.api.response.PageRoleAccessResponse;
import io.oxalate.backend.api.response.PageVersionResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
@Table(name = "pages")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Min(1)
    @Column(name = "page_group_id")
    private long pageGroupId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PageStatusEnum status;

    @Min(1)
    @Column(name = "creator")
    private long creator;

    @NotNull
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "modifier")
    private Long modifier;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Transient
    private List<PageVersion> pageVersions;

    @Transient
    private Set<PageRoleAccess> rolePermissions;

    public static Page of(PageRequest pageRequest, long creator) {
        var pageVersions = new ArrayList<PageVersion>();

        for (var pageVersionRequest : pageRequest.getPageVersions()) {
            pageVersions.add(PageVersion.of(pageVersionRequest));
        }

        return Page.builder()
                   .id((pageRequest.getId() == 0L ? null : pageRequest.getId()))
                   .pageGroupId(pageRequest.getPageGroupId())
                   .status(pageRequest.getStatus())
                   .creator(creator)
                   .createdAt(Instant.now())
                   .modifier(null)
                   .modifiedAt(null)
                   .pageVersions(pageVersions)
                   .build();
    }

    public PageResponse toResponse() {
        var pageVersionResponses = new ArrayList<PageVersionResponse>();

        if (this.getPageVersions() != null) {
            for (var pageVersion : this.getPageVersions()) {
                pageVersionResponses.add(pageVersion.toResponse());
            }
        }

        var rolePermissionResponses = new HashSet<PageRoleAccessResponse>();

        if (this.getRolePermissions() != null) {
            for (var rolePermission : this.getRolePermissions()) {
                rolePermissionResponses.add(rolePermission.toResponse());
            }
        }

        return PageResponse.builder()
                           .id(this.getId())
                           .pageGroupId(this.getPageGroupId())
                           .status(this.getStatus())
                           .creator(this.getCreator())
                           .createdAt(this.getCreatedAt())
                           .modifier(this.getModifier())
                           .modifiedAt(this.getModifiedAt())
                           .pageVersions(pageVersionResponses)
                           .rolePermissions(rolePermissionResponses)
                           .build();
    }
}
