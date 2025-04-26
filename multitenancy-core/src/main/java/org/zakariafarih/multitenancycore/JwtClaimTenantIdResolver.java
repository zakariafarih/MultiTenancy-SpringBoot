package org.zakariafarih.multitenancycore;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Extracts tenant id from JWT claim "tenant".
 * Active only if Spring-Security & resource-server jars are present.
 */
@Component
@ConditionalOnClass(JwtAuthenticationToken.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class JwtClaimTenantIdResolver implements TenantIdResolver {

    private static final String CLAIM = "tenant";

    @Override
    public String resolveTenantId(HttpServletRequest req) {
        var principal = req.getUserPrincipal();
        if (principal instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            Object claim = jwt.getClaim(CLAIM);
            return claim != null ? claim.toString() : null;
        }
        return null;
    }
}
