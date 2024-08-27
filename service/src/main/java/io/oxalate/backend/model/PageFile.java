package io.oxalate.backend.model;

import io.oxalate.backend.api.UploadStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "page_files")
public class PageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "page_id")
    private long pageId;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_checksum", nullable = false)
    private String fileChecksum;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatusEnum status;

    @Column(name = "creator", nullable = false)
    private long creator;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
