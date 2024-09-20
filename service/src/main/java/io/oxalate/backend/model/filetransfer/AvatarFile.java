package io.oxalate.backend.model.filetransfer;

import io.oxalate.backend.api.response.filetransfer.AvatarFileResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "avatar_files")
public class AvatarFile extends AbstractFile {

    public AvatarFileResponse toResponse() {
        return AvatarFileResponse.builder()
                                 .id(this.id)
                                 .filename(this.fileName)
                                 .mimetype(this.mimeType)
                                 .filesize(this.fileSize)
                                 .filechecksum(this.fileChecksum)
                                 .creator(this.creator.getLastName() + ", " + this.creator.getFirstName())
                                 .createdAt(this.createdAt)
                                 .build();
    }
}
