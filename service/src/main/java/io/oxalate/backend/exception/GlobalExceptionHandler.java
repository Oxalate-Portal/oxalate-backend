package io.oxalate.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Central error handling for the REST API.
 * <p>
 * OWASP A10:2025 (Mishandling of Exceptional Conditions) and A09:2025 (Logging Failures):
 * <ul>
 *     <li>No unhandled exception is allowed to reach the container, where it would render a stack trace or an
 *     implementation-revealing default error page.</li>
 *     <li>Clients receive an RFC 9457 {@link ProblemDetail} with a generic message and a correlation id.
 *     The details, including the stack trace, stay in the server log next to the same correlation id.</li>
 *     <li>Security exceptions are deliberately re-thrown so that Spring Security's
 *     {@code ExceptionTranslationFilter} keeps producing 401 for anonymous callers and 403 for authenticated
 *     ones. Handling them here would collapse that distinction.</li>
 * </ul>
 * Controllers annotated with {@code @Audited} still have their {@link OxalateAuditException}s converted by
 * {@code AuditAspect}; the handler below is the safety net for everything that escapes it.
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE = "The request could not be processed";

    /**
     * Lets Spring Security translate authentication and authorization failures itself.
     *
     * @param e the security exception
     * @throws RuntimeException always, re-throwing the original exception
     */
    @ExceptionHandler({ AccessDeniedException.class, AuthenticationException.class })
    public void handleSecurityException(RuntimeException e) {
        throw e;
    }

    @ExceptionHandler(OxalateAuditException.class)
    public ResponseEntity<Object> handleOxalateAuditException(OxalateAuditException e, HttpServletRequest request) {
        log.warn("{} {} rejected: {}", request.getMethod(), request.getRequestURI(), e.getAuditMessage());

        if (e.getResponseBody() != null) {
            return ResponseEntity.status(e.getHttpStatus())
                                 .body(e.getResponseBody());
        }

        return ResponseEntity.status(e.getHttpStatus())
                             .body(problemDetail(e.getHttpStatus(), e.getAuditMessage(), request));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("Rejecting malformed request {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity.badRequest()
                             .body(problemDetail(HttpStatus.BAD_REQUEST, "The request was malformed or failed validation", request));
    }

    /**
     * A unique constraint or foreign key violation must not surface the SQL statement or the schema.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        var correlationId = logUnexpected(e, request);
        var problem = problemDetail(HttpStatus.CONFLICT, "The request conflicts with the current state of the resource", request);
        problem.setProperty("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception e, HttpServletRequest request) {
        var correlationId = logUnexpected(e, request);
        var problem = problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_ERROR_MESSAGE, request);
        problem.setProperty("correlationId", correlationId);
        return ResponseEntity.internalServerError()
                             .body(problem);
    }

    /**
     * Logs the full exception server side and returns the identifier that is echoed to the client, so an
     * incident can be correlated without leaking internals.
     */
    private String logUnexpected(Exception e, HttpServletRequest request) {
        var correlationId = UUID.randomUUID()
                                .toString();
        log.error("Unhandled exception [{}] while processing {} {}", correlationId, request.getMethod(), request.getRequestURI(), e);
        return correlationId;
    }

    private ProblemDetail problemDetail(HttpStatus status, String detail, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }
}
