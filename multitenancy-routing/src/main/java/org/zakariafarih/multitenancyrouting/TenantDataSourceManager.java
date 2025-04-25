package org.zakariafarih.multitenancyrouting;

import com.github.benmanes.caffeine.cache.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zakariafarih.multitenancycore.TenantProperties;
import org.zakariafarih.multitenancycore.TenantRegistry;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * Creates at most {@code maxPools} HikariCP pools and evicts the
 * least-recently used ones after {@code idleEviction}.
 */
@RequiredArgsConstructor
@Slf4j
public class TenantDataSourceManager {

    private final TenantRegistry registry;

    /* --- tunables (could be @ConfigurationProperties later) --- */
    private final int      maxPools     = 200;
    private final Duration idleEviction = Duration.ofHours(2);

    private final LoadingCache<String, DataSource> cache =
            Caffeine.newBuilder()
                    .maximumSize(maxPools)
                    .expireAfterAccess(idleEviction)
                    .evictionListener(this::closePool)
                    .build(this::create);

    public DataSource get(String tenantId) {
        return cache.get(tenantId);
    }

    /* ---------- helpers ---------- */

    private void closePool(String tenant, DataSource ds, RemovalCause cause) {
        if (ds instanceof HikariDataSource hds) {
            hds.close();
            log.info("Closed pool for tenant {} (cause: {})", tenant, cause);
        }
    }

    private DataSource create(String tenantId) {
        TenantProperties.TenantConfig cfg = registry.get(tenantId);

        HikariConfig hc = new HikariConfig();
        hc.setPoolName("tenant-" + tenantId + "-pool");
        hc.setJdbcUrl("jdbc:postgresql://%s:%d/%s"
                .formatted(cfg.getHost(), cfg.getPort(), cfg.getDbName()));
        hc.setUsername(cfg.getDbUser());
        hc.setPassword(cfg.getDbPassword());

        /* sensible defaults but overridable through tenants.yml */
        hc.setMinimumIdle(0);
        hc.setMaximumPoolSize(cfg.getMaxPool() > 0 ? cfg.getMaxPool() : 10);
        hc.setIdleTimeout(cfg.getIdleTimeoutMs() > 0
                ? cfg.getIdleTimeoutMs()
                : Duration.ofMinutes(10).toMillis());
        hc.setValidationTimeout(Duration.ofSeconds(3).toMillis());

        HikariDataSource ds = new HikariDataSource(hc);
        bindMetricsIfPossible(ds);
        log.info("Created datasource for tenant {}", tenantId);
        return ds;
    }

    /** Register pool metrics only if micrometer-hikaricp is on the classpath. */
    private void bindMetricsIfPossible(HikariDataSource ds) {
        try {
            Class.forName("io.micrometer.core.instrument.binder.db.HikariCPCollector");
            // class is present – bind via reflection to avoid hard dependency
            var ctor = Class
                    .forName("io.micrometer.core.instrument.binder.db.HikariCPCollector")
                    .getConstructor(javax.sql.DataSource.class);
            Object binder = ctor.newInstance(ds);

            var bindTo = binder.getClass().getMethod("bindTo", MeterRegistry.class);
            bindTo.invoke(binder, io.micrometer.core.instrument.Metrics.globalRegistry);

            log.debug("Micrometer Hikari metrics bound for pool {}", ds.getPoolName());
        } catch (ClassNotFoundException e) {
            // micrometer-hikaricp not on classpath – silently skip
        } catch (Exception ex) {
            log.warn("Failed to bind Hikari metrics", ex);
        }
    }
}
