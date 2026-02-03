package io.oxalate.backend.model;

import io.oxalate.backend.api.EmailNotificationDetailEnum;
import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.api.EmailStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_queue")
public class EmailQueueEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false)
    private EmailNotificationTypeEnum emailType;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_detail", nullable = false)
    private EmailNotificationDetailEnum emailDetail;

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatusEnum status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_send_timestamp", nullable = false)
    private Instant nextSendTimestamp;

    @Builder.Default
    @Column(name = "counter", nullable = false)
    private int counter = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        nextSendTimestamp = createdAt.toInstant(ZoneOffset.UTC);
    }
}
