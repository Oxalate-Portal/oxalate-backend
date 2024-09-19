package io.oxalate.backend.model.filetransfer;

import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.api.response.filetransfer.DocumentFileResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "document_files", uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_files_file_name", columnNames = {"file_name"})
})
public class DocumentFile extends AbstractFile {

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatusEnum status;

    public DocumentFileResponse toResponse() {
        return DocumentFileResponse.builder()
                                   .id(this.id)
                                   .filename(this.fileName)
                                   .mimetype(this.mimeType)
                                   .filesize(this.fileSize)
                                   .filechecksum(this.fileChecksum)
                                   .creator(this.creator.getLastName() + ", " + this.creator.getFirstName())
                                   .createdAt(this.createdAt)
                                   .status(this.status)
                                   .build();
    }
}
