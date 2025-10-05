package io.oxalate.backend.exception;

import io.oxalate.backend.api.AuditLevelEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class OxalateAuthenticationException extends RuntimeException {
    private final AuditLevelEnum auditLevel;
    private final String auditMessage;
    private final String auditSource;
    private final long userId;
    private final HttpStatus httpErrorStatus;
}
