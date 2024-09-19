package io.oxalate.backend.model.filetransfer;

import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.api.response.filetransfer.DiveFileResponse;
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
@Table(name = "dive_files", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dive_files_file_name_event_id_dive_group_id", columnNames = {"file_name", "event_id", "dive_group_id"})
})
public class DiveFile extends AbstractFile {

    @Column(name = "event_id", nullable = false)
    private long eventId;

    @Column(name = "dive_group_id", nullable = false)
    private long diveGroupId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatusEnum status;

    public DiveFileResponse toResponse() {
        return DiveFileResponse.builder()
                               .id(this.id)
                               .filename(this.fileName)
                               .mimetype(this.mimeType)
                               .filesize(this.fileSize)
                               .filechecksum(this.fileChecksum)
                               .creator(this.creator.getLastName() + ", " + this.creator.getFirstName())
                               .createdAt(this.createdAt)
                               .eventId(this.eventId)
                               .diveGroupId(this.diveGroupId)
                               .status(this.status)
                               .build();
    }
}
