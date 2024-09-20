package io.oxalate.backend.model.filetransfer;

import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.api.response.filetransfer.PageFileResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "page_files")
public class PageFile extends AbstractFile {

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "page_id", nullable = false)
    private long pageId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatusEnum status;

    public PageFileResponse toResponse() {
        return PageFileResponse.builder()
                               .id(this.id)
                               .filename(this.fileName)
                               .mimetype(this.mimeType)
                               .filesize(this.fileSize)
                               .filechecksum(this.fileChecksum)
                               .creator(this.creator.getLastName() + ", " + this.creator.getFirstName())
                               .createdAt(this.createdAt)
                               .language(this.language)
                               .pageId(this.pageId)
                               .status(this.status)
                               .build();
    }
}
