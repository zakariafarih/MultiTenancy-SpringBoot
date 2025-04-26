package org.zakariafarih.multitenancyrouting;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;

/**
 * Binds basic HikariCP pool configuration metrics manually.
 */
@RequiredArgsConstructor
public class TenantHikariMetricsBinder implements MeterBinder {

    private final String tenantId;
    private final HikariDataSource dataSource;

    @Override
    public void bindTo(MeterRegistry registry) {
        Tags tags = Tags.of("tenant", tenantId, "pool", dataSource.getPoolName());

        registry.gauge("hikaricp.pool.max", tags, dataSource, HikariDataSource::getMaximumPoolSize);
        registry.gauge("hikaricp.pool.min", tags, dataSource, HikariDataSource::getMinimumIdle);
    }
}
