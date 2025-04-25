package org.zakariafarih.multitenancycore;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Stores the tenant id in TenantContext (ThreadLocal) and MDC.
 * Resolution logic is delegated to {@link TenantIdResolver}.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantFilter implements Filter {

    private final TenantIdResolver tenantIdResolver;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws java.io.IOException, ServletException {

        try {
            String tenant = tenantIdResolver.resolveTenantId((HttpServletRequest) req);

            if (tenant != null && !tenant.isBlank()) {
                TenantContext.set(tenant);
                MDC.put("tenant", tenant);
            }
            chain.doFilter(req, res);
        }
        finally {
            TenantContext.clear();
            MDC.remove("tenant");
        }
    }
}
