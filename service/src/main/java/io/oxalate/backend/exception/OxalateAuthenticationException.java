package io.oxalate.backend.exception;

import io.oxalate.backend.api.AuditLevelEnum;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception for authentication-related audit events. Extends {@link OxalateAuditException}
 * and adds the {@code auditSource} and {@code userId} fields used by {@code AuthService}.
 */
@Getter
public class OxalateAuthenticationException extends OxalateAuditException {
    private final String auditSource;
    private final long userId;

    public OxalateAuthenticationException(AuditLevelEnum auditLevel, String auditMessage, String auditSource, long userId, HttpStatus httpErrorStatus) {
        super(auditLevel, auditMessage, httpErrorStatus);
        this.auditSource = auditSource;
        this.userId = userId;
    }

    /**
     * Backward-compatible alias for {@link #getHttpStatus()}.
     */
    public HttpStatus getHttpErrorStatus() {
        return getHttpStatus();
    }
}
