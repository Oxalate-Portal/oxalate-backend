package io.oxalate.backend.model;

import io.oxalate.backend.api.request.PageVersionRequest;
import io.oxalate.backend.api.response.PageVersionResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "page_versions")
public class PageVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @Column(name = "language", nullable = false)
    private String language;

    @Size(min = 2, max = 256, message = "Title must be between 2 and 256 characters long")
    @Column(name = "title", nullable = false)
    private String title;

    @Size(max = 512, message = "Ingress must be between 2 and 512 characters long")
    @Column(name = "ingress", nullable = false)
    private String ingress;

    @Size(min = 2, message = "Body must be at least 2 characters long")
    @Column(name = "body", nullable = false)
    private String body;

    public static PageVersion of(PageVersionRequest pageVersionRequest) {
        return PageVersion.builder()
                          .id(pageVersionRequest.getId() == 0L ? null : pageVersionRequest.getId())
                          .pageId((pageVersionRequest.getPageId() == 0L ? null : pageVersionRequest.getPageId()))
                          .language(pageVersionRequest.getLanguage())
                          .title(pageVersionRequest.getTitle())
                          .ingress(pageVersionRequest.getIngress())
                          .body(pageVersionRequest.getBody())
                          .build();
    }

    public PageVersionResponse toResponse() {
        return PageVersionResponse.builder()
                                  .id(this.getId())
                                  .pageId(this.getPageId())
                                  .language(this.getLanguage())
                                  .title(this.getTitle())
                                  .ingress(this.getIngress())
                                  .body(this.getBody())
                                  .build();
    }
}
