package org.zakariafarih.multitenancycore;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Resolves from
 *  • HTTP header "X-Tenant"
 *  • URI pattern /tenants/{id}/…
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // try first
public class HeaderPathTenantIdResolver implements TenantIdResolver {

    private static final String TENANT_HEADER = "X-Tenant";

    @Override
    public String resolveTenantId(HttpServletRequest req) {
        String id = req.getHeader(TENANT_HEADER);
        if (id != null && !id.isBlank()) return id;

        String[] segments = req.getRequestURI().split("/");
        if (segments.length > 2 && "tenants".equals(segments[1])) {
            return segments[2];
        }
        return null;
    }
}
