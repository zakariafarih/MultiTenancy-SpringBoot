package org.zakariafarih.multitenancycore;

/**
 * Thrown when a request refers to a tenant that is not present
 * in the registry.  Controllers may translate it to 404/400.
 */
public class UnknownTenantException extends RuntimeException {
    public UnknownTenantException(String tenantId) {
        super("Unknown tenant: " + tenantId);
    }
}
