package io.oxalate.backend.exception;

import io.oxalate.backend.api.AuditLevelEnum;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an authorization check fails in a controller method.
 */
public class OxalateUnauthorizedException extends OxalateAuditException {

    public OxalateUnauthorizedException(String auditMessage) {
        super(AuditLevelEnum.ERROR, auditMessage, HttpStatus.UNAUTHORIZED);
    }

    public OxalateUnauthorizedException(String auditMessage, HttpStatus httpStatus) {
        super(AuditLevelEnum.ERROR, auditMessage, httpStatus);
    }

    public OxalateUnauthorizedException(AuditLevelEnum auditLevel, String auditMessage, HttpStatus httpStatus) {
        super(auditLevel, auditMessage, httpStatus);
    }
}

