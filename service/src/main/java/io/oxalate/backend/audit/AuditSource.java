package io.oxalate.backend.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation that declares the audit source name for a controller.
 * Replaces the {@code private static final String AUDIT_NAME} pattern.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditSource {
    String value();
}

