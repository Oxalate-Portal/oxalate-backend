package io.oxalate.backend.model;

import io.oxalate.backend.api.request.PageGroupVersionRequest;
import io.oxalate.backend.api.response.PageGroupVersionResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
@Table(name = "page_group_versions")
public class PageGroupVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Min(value = 1, message = "Path id must be at least 1")
    @Column(name = "page_group_id", nullable = false)
    private Long pageGroupId;

    @Size(min = 2, max = 256, message = "Title must be between 2 and 256 characters long")
    @Column(name = "title", nullable = false)
    private String title;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @Column(name = "language", nullable = false)
    private String language;

    public static PageGroupVersion of(PageGroupVersionRequest pageGroupVersionRequest) {
        return PageGroupVersion.builder()
                               .id(null)
                               .pageGroupId(pageGroupVersionRequest.getPageGroupId())
                               .title(pageGroupVersionRequest.getTitle())
                               .language(pageGroupVersionRequest.getLanguage())
                               .build();
    }

    public PageGroupVersionResponse toResponse() {
        return PageGroupVersionResponse.builder()
                                       .id(this.id)
                                       .pageGroupId(this.pageGroupId)
                                       .title(this.title)
                                       .language(this.language)
                                       .build();
    }
}
