package io.oxalate.backend.model.filetransfer;

import io.oxalate.backend.api.response.filetransfer.CertificateFileResponse;
import io.oxalate.backend.model.Certificate;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "certificate_files")
public class CertificateFile extends AbstractFile {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    public CertificateFileResponse toResponse() {
        return CertificateFileResponse.builder()
                                      .id(this.id)
                                      .filename(this.fileName)
                                      .mimetype(this.mimeType)
                                      .filesize(this.fileSize)
                                      .filechecksum(this.fileChecksum)
                                      .creator(this.creator.getLastName() + ", " + this.creator.getFirstName())
                                      .createdAt(this.createdAt)
                                      .certificateId(this.certificate.getId())
                                      .build();
    }
}
