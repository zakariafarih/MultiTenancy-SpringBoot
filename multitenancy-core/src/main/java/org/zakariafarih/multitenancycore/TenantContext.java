package org.zakariafarih.multitenancycore;

/**
 * Thread-local holder for the current tenant id.
 * Adds a getRequired() helper so callers can fail fast,
 * and is package-private to avoid misuse from outside the core package.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) { CURRENT.set(tenantId); }

    public static String get() { return CURRENT.get(); }

    /** @throws IllegalStateException if no tenant id is present */
    public static String getRequired() {
        String id = CURRENT.get();
        if (id == null) {
            throw new TenantNotResolvedException();
        }
        return id;
    }

    public static void clear() { CURRENT.remove(); }
}
