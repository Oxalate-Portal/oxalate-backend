package io.oxalate.backend.exception;

import io.oxalate.backend.api.AuditLevelEnum;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all audit-aware exceptions. When thrown from a controller method
 * annotated with {@link io.oxalate.backend.audit.Audited}, the
 * {@link io.oxalate.backend.aspect.AuditAspect} catches it, publishes the audit event,
 * and returns the appropriate HTTP response.
 */
@Getter
public class OxalateAuditException extends RuntimeException {
    private final AuditLevelEnum auditLevel;
    private final String auditMessage;
    private final HttpStatus httpStatus;
    private final Object responseBody;

    public OxalateAuditException(AuditLevelEnum auditLevel, String auditMessage, HttpStatus httpStatus) {
        this(auditLevel, auditMessage, httpStatus, null);
    }

    public OxalateAuditException(AuditLevelEnum auditLevel, String auditMessage, HttpStatus httpStatus, Object responseBody) {
        super(auditMessage);
        this.auditLevel = auditLevel;
        this.auditMessage = auditMessage;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }
}

