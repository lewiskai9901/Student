package com.school.management.infrastructure.tenant;

/**
 * ThreadLocal holder for tenant context.
 * Set in JwtAuthenticationFilter, cleared in finally block.
 */
public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    public static void setContext(TenantContext context) {
        CONTEXT.set(context);
    }

    public static TenantContext getContext() {
        return CONTEXT.get();
    }

    public static Long getTenantId() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getTenantId() : 1L;
    }

    public static String getTenantCode() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getTenantCode() : "default";
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
