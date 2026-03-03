package io.oxalate.backend.aspect;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.audit.AuditContext;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.exception.OxalateAuditException;
import io.oxalate.backend.exception.OxalateAuthenticationException;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect that intercepts controller methods annotated with {@link Audited}
 * and automatically publishes START, OK and FAIL audit events.
 * <p>
 * Mid-method audit events are handled via typed exceptions extending
 * {@link OxalateAuditException}: the aspect catches them, publishes the
 * exception's audit message, and returns the appropriate HTTP response.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AppEventPublisher appEventPublisher;

    @Around("@annotation(audited)")
    public Object auditAround(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        var source = resolveSource(joinPoint);
        var request = resolveRequest(joinPoint);
        var userId = resolveUserId();
        var traceId = UUID.randomUUID();

        AuditContext.setTraceId(traceId);

        try {
            // Publish START event
            appEventPublisher.publishAuditEvent(audited.startMessage(), audited.level(), request, source, userId, traceId);

            var result = joinPoint.proceed();

            // Publish OK event
            appEventPublisher.publishAuditEvent(audited.okMessage(), audited.level(), request, source, userId, traceId);

            return result;
        } catch (OxalateAuthenticationException ex) {
            // OxalateAuthenticationException carries its own source and userId
            appEventPublisher.publishAuditEvent(ex.getAuditMessage(), ex.getAuditLevel(), request, ex.getAuditSource(), ex.getUserId(), traceId);
            return ResponseEntity.status(ex.getHttpStatus())
                                 .body(ex.getResponseBody());
        } catch (OxalateAuditException ex) {
            // All other audit exceptions use the controller's source and current userId
            appEventPublisher.publishAuditEvent(ex.getAuditMessage(), ex.getAuditLevel(), request, source, userId, traceId);
            return ResponseEntity.status(ex.getHttpStatus())
                                 .body(ex.getResponseBody());
        } catch (Throwable ex) {
            // Unexpected exception — publish failMessage if defined
            if (!audited.failMessage()
                        .isEmpty()) {
                appEventPublisher.publishAuditEvent(audited.failMessage(), AuditLevelEnum.ERROR, request, source, userId, traceId);
            }
            throw ex;
        } finally {
            AuditContext.clear();
        }
    }

    private String resolveSource(ProceedingJoinPoint joinPoint) {
        var targetClass = joinPoint.getTarget()
                                   .getClass();
        var auditSource = targetClass.getAnnotation(AuditSource.class);

        if (auditSource != null) {
            return auditSource.value();
        }

        return targetClass.getSimpleName();
    }

    private HttpServletRequest resolveRequest(ProceedingJoinPoint joinPoint) {
        // First try to find HttpServletRequest in the method arguments
        for (var arg : joinPoint.getArgs()) {
            if (arg instanceof HttpServletRequest httpServletRequest) {
                return httpServletRequest;
            }
        }

        // Fall back to RequestContextHolder
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }

        return null;
    }

    private Long resolveUserId() {
        try {
            return AuthTools.getCurrentUserId();
        } catch (Exception e) {
            return -1L;
        }
    }
}

