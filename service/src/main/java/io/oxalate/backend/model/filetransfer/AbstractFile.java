package io.oxalate.backend.model.filetransfer;

import io.oxalate.backend.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class AbstractFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    protected Long id;

    @Column(name = "file_name", nullable = false)
    protected String fileName;

    @Column(name = "mime_type", nullable = false)
    protected String mimeType;

    @Column(name = "file_size", nullable = false)
    protected long fileSize;

    @Column(name = "file_checksum", nullable = false)
    protected String fileChecksum;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_upload_files_users_id"))
    protected User creator;

    @Column(name = "created_at", nullable = false)
    protected Instant createdAt;
}
