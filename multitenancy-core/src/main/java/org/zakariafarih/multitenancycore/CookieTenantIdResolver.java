package org.zakariafarih.multitenancycore;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Resolves tenant id from an HTTP cookie "X-Tenant".
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CookieTenantIdResolver implements TenantIdResolver {

    private static final String COOKIE = "X-Tenant";

    @Override
    public String resolveTenantId(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if (COOKIE.equalsIgnoreCase(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
