package io.oxalate.backend.audit;

import io.oxalate.backend.api.AuditLevelEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that declares the audit messages for the standard
 * START → OK / FAIL envelope around a controller method.
 * <p>
 * The {@link io.oxalate.backend.aspect.AuditAspect} intercepts methods annotated
 * with this annotation and automatically publishes the audit events.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    /**
     * Message published at method entry.
     */
    String startMessage();

    /**
     * Message published when the method completes successfully.
     */
    String okMessage();

    /**
     * Message published when the method throws an unexpected exception (not an OxalateAuditException). Defaults to empty (no event).
     */
    String failMessage() default "";

    /**
     * Audit level for the start and ok events. Defaults to INFO.
     */
    AuditLevelEnum level() default AuditLevelEnum.INFO;
}

