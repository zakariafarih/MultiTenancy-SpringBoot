package org.zakariafarih.multitenancycore;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Resolves tenant from a query param <pre>?tenant={id}</pre>.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class QueryParamTenantIdResolver implements TenantIdResolver {

    private static final String PARAM = "tenant";

    @Override
    public String resolveTenantId(HttpServletRequest req) {
        return req.getParameter(PARAM);
    }
}
