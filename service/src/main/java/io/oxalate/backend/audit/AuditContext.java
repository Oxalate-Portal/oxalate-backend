package io.oxalate.backend.audit;

import java.util.UUID;

/**
 * Thread-local holder for the audit trace ID. Set by {@link io.oxalate.backend.aspect.AuditAspect}
 * before controller method execution and cleared after completion.
 * <p>
 * Service-layer code can call {@link #getTraceId()} to obtain the current
 * trace ID for correlated audit events without needing it passed as a parameter.
 */
public final class AuditContext {

    private static final ThreadLocal<UUID> TRACE_ID = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void setTraceId(UUID traceId) {
        TRACE_ID.set(traceId);
    }

    public static UUID getTraceId() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}

