package org.zakariafarih.multitenancycore;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Default resolver: X-Tenant header OR /tenants/{id}/… path segment. */
@Component
public class HeaderPathTenantIdResolver implements TenantIdResolver {

    private static final String TENANT_HEADER = "X-Tenant";

    @Override
    public String resolveTenantId(HttpServletRequest req) {
        String id = req.getHeader(TENANT_HEADER);
        if (id != null && !id.isBlank()) return id;

        String[] parts = req.getRequestURI().split("/");
        if (parts.length > 2 && "tenants".equals(parts[1])) {
            return parts[2];
        }
        return null;
    }
}
