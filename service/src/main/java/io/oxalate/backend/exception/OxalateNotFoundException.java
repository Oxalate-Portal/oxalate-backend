package io.oxalate.backend.exception;

import io.oxalate.backend.api.AuditLevelEnum;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource is not found.
 */
public class OxalateNotFoundException extends OxalateAuditException {

    public OxalateNotFoundException(String auditMessage) {
        super(AuditLevelEnum.WARN, auditMessage, HttpStatus.NOT_FOUND);
    }

    public OxalateNotFoundException(String auditMessage, HttpStatus httpStatus) {
        super(AuditLevelEnum.WARN, auditMessage, httpStatus);
    }

    public OxalateNotFoundException(AuditLevelEnum auditLevel, String auditMessage, HttpStatus httpStatus) {
        super(auditLevel, auditMessage, httpStatus);
    }
}

