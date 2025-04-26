package org.zakariafarih.multitenancycore;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Iterates over all available {@link TenantIdResolver} beans
 * (respecting {@link org.springframework.core.annotation.Order})
 * and returns the first non-blank tenant id.
 *
 * Marked {@code @Primary} so every injection point receives the chain
 * while individual resolvers remain independently testable / overridable.
 */
@Slf4j
@Component
@org.springframework.context.annotation.Primary
public class ChainedTenantIdResolver implements TenantIdResolver {

    private final List<TenantIdResolver> delegates;

    public ChainedTenantIdResolver(List<TenantIdResolver> resolvers) {
        // keep deterministic order, exclude self
        this.delegates = resolvers.stream()
                .filter(r -> !(r instanceof ChainedTenantIdResolver))
                .sorted(OrderComparator.INSTANCE)
                .toList();

        log.info("Tenant-resolver chain: {}", delegates);
    }

    @Override
    public String resolveTenantId(HttpServletRequest request) {
        for (TenantIdResolver r : delegates) {
            String id = r.resolveTenantId(request);
            if (id != null && !id.isBlank()) {
                return id.trim();
            }
        }
        return null;
    }
}
