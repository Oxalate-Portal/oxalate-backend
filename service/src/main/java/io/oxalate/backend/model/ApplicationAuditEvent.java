package io.oxalate.backend.model;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.response.AuditEntryResponse;
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
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "application_audit_event")
public class ApplicationAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "level", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditLevelEnum level;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditEntryResponse toAuditEntryResponse() {
        return AuditEntryResponse.builder()
                                 .id(id)
                                 .traceId(traceId)
                                 .source(source)
                                 .level(level)
                                 .userId((userId == null) ? 0 : userId)
                                 .address(ipAddress)
                                 .message(message)
                                 .createdAt(createdAt)
                                 .build();
    }
}
