package org.zakariafarih.multitenancycore;

/** Thrown when no tenant id is bound to the current request context. */
public class TenantNotResolvedException extends RuntimeException {
    public TenantNotResolvedException() {
        super("Tenant id is required but was not resolved from the request");
    }
}
