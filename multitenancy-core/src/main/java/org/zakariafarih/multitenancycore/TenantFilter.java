package org.zakariafarih.multitenancycore;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;

/**
 * Stores the tenant id in TenantContext (ThreadLocal) and MDC.
 * Resolution logic is delegated to {@link TenantIdResolver}.
 */
@Slf4j
@Component @Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantFilter implements Filter {

    private static final List<String> SKIP = List.of(
            "/actuator/**", "/static/**", "/public/**");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final TenantIdResolver tenantIdResolver;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) req;
        String uri = http.getRequestURI();

        try {
            if (SKIP.stream().anyMatch(p -> matcher.match(p, uri))) {
                chain.doFilter(req, res);
                return;
            }

            String tenant = tenantIdResolver.resolveTenantId(http);
            if (tenant != null && !tenant.isBlank()) {
                TenantContext.set(tenant);
                MDC.put("tenant", tenant);
            }

            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            MDC.remove("tenant");
        }
    }
}
