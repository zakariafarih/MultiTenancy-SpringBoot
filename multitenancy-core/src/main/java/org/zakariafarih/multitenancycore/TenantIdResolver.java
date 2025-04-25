package org.zakariafarih.multitenancycore;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Strategy interface: find the tenant id for the current call.
 * Implementations can inspect headers, JWT claims, sub-domains, etc.
 */
@FunctionalInterface
public interface TenantIdResolver {
    /** @return tenant id or {@code null} if not present */
    String resolveTenantId(HttpServletRequest request);
}
