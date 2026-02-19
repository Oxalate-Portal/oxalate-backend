package io.oxalate.backend.exception;

import io.oxalate.backend.api.AuditLevelEnum;
import org.springframework.http.HttpStatus;

/**
 * Thrown when request validation fails in a controller method.
 */
public class OxalateValidationException extends OxalateAuditException {

    public OxalateValidationException(String auditMessage) {
        super(AuditLevelEnum.ERROR, auditMessage, HttpStatus.BAD_REQUEST);
    }

    public OxalateValidationException(String auditMessage, HttpStatus httpStatus) {
        super(AuditLevelEnum.ERROR, auditMessage, httpStatus);
    }

    public OxalateValidationException(AuditLevelEnum auditLevel, String auditMessage, HttpStatus httpStatus) {
        super(auditLevel, auditMessage, httpStatus);
    }

    public OxalateValidationException(AuditLevelEnum auditLevel, String auditMessage, HttpStatus httpStatus, Object responseBody) {
        super(auditLevel, auditMessage, httpStatus, responseBody);
    }
}

