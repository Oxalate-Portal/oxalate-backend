package io.oxalate.backend.model;

import io.oxalate.backend.api.PageStatusEnum;
import io.oxalate.backend.api.request.PageGroupRequest;
import io.oxalate.backend.api.response.PageGroupResponse;
import io.oxalate.backend.api.response.PageGroupVersionResponse;
import io.oxalate.backend.api.response.PageResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "page_groups")
public class PageGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PageStatusEnum status;

    @Transient
    private List<PageGroupVersion> groupVersions;

    @Transient
    private List<Page> pages;

    public static PageGroup of(PageGroupRequest pageGroupRequest) {
        var pathVersions = new ArrayList<PageGroupVersion>();

        if (pageGroupRequest.getPageGroupVersions() != null) {
            for (var pageGroupVersionRequest : pageGroupRequest.getPageGroupVersions())
                pathVersions.add(PageGroupVersion.of(pageGroupVersionRequest));
        }

        return PageGroup.builder()
                        .id(null)
                        .status(pageGroupRequest.getStatus())
                        .groupVersions(pathVersions)
                        .build();
    }

    public PageGroupResponse toResponse() {
        var pageGroupVersionResponses = new ArrayList<PageGroupVersionResponse>();

        if (this.getGroupVersions() != null) {
            for (var pathVersion : this.getGroupVersions())
                pageGroupVersionResponses.add(pathVersion.toResponse());
        }

        var pageResponses = new ArrayList<PageResponse>();

        if (this.getPages() != null) {
            for (var page : this.getPages())
                pageResponses.add(page.toResponse());
        }

        return PageGroupResponse.builder()
                                .id(this.getId())
                                .status(this.getStatus())
                                .pages(pageResponses)
                                .pageGroupVersions(pageGroupVersionResponses)
                                .build();
    }
}
