package org.zakariafarih.multitenancyrouting;

import com.github.benmanes.caffeine.cache.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.zakariafarih.multitenancycore.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lazily builds and caches one HikariCP pool per tenant.
 * Pools are evicted LRU after {@code idleEviction}.
 *
 * **FIX 2025-04-26**
 * Removed the `connectionInitSql` that executed
 * <code>SET SCHEMA&nbsp;clinic</code> _before_ the “clinic” schema was
 * created.
 * That init SQL caused Hikari to fail while establishing the very first
 * connection, breaking pool initialisation and – in turn – every test that
 * expected 200 tenants to be routable.
 *
 * We now simply rely on the fully-qualified schema names in all entity
 * mappings (e.g. <code>@Table(schema = "clinic")</code>) and on Hibernate’s
 * <code>hibernate.default_schema</code> property where needed.
 */
@Slf4j
public class TenantDataSourceManager {

    private final TenantRegistry                   registry;
    private final MultitenancyPoolProperties       poolProps;
    private final MultitenancyMonitoringProperties monitorProps;
    private final String[]                         requiredSchemas;
    private final SchemaGenerator                  schemaGen;

    private final LoadingCache<String, DataSource> cache;

    public TenantDataSourceManager(TenantRegistry                   registry,
                                   MultitenancyPoolProperties       poolProps,
                                   MultitenancyProps                appProps,
                                   MultitenancyMonitoringProperties monitorProps,
                                   SchemaGenerator                  schemaGen) {

        this.registry     = registry;
        this.poolProps    = poolProps;
        this.monitorProps = monitorProps;
        this.schemaGen    = schemaGen;

        /* Fall back to monitor.expected-schemas when the application uses
           the hard-coded default ["public"] (meaning “not configured”). */
        boolean onlyPublic = appProps.getSchemas().length == 1
                && "public".equalsIgnoreCase(appProps.getSchemas()[0]);
        this.requiredSchemas = (onlyPublic)
                ? monitorProps.getExpectedSchemas().toArray(String[]::new)
                : appProps.getSchemas();

        this.cache = Caffeine.newBuilder()
                .maximumSize(poolProps.getMaxTenantPools())
                .expireAfterAccess(poolProps.getIdleEviction())
                .evictionListener(this::closePool)
                .build(this::create);
    }

    /* ───────────────────────── public API ───────────────────────── */

    public DataSource get(String tenantId) { return cache.get(tenantId); }

    /* ───────────────────── cache helpers ────────────────────────── */

    private void closePool(String tenant, DataSource ds, RemovalCause cause) {
        if (ds instanceof HikariDataSource hds) hds.close();
        log.info("Closed pool for tenant {} (cause: {})", tenant, cause);
    }

    private DataSource create(String tenantId) {
        TenantProperties.TenantConfig cfg = registry.get(tenantId);

        String db  = (cfg.getDbName() != null && !cfg.getDbName().isBlank())
                ? cfg.getDbName() : tenantId;
        boolean h2 = (cfg.getHost() == null || cfg.getHost().isBlank());

        HikariConfig hc = new HikariConfig();
        hc.setPoolName("tenant-" + tenantId + "-pool");
        hc.setJdbcUrl(h2
                ? "jdbc:h2:mem:%s;MODE=PostgreSQL;DB_CLOSE_DELAY=-1".formatted(db)
                : "jdbc:postgresql://%s:%d/%s".formatted(cfg.getHost(), cfg.getPort(), db));
        hc.setUsername(h2 ? "sa"
                : (cfg.getDbUser()     != null ? cfg.getDbUser()     : "postgres"));
        hc.setPassword(h2 ? ""
                : (cfg.getDbPassword() != null ? cfg.getDbPassword() : ""));
        hc.setMinimumIdle(0);
        hc.setMaximumPoolSize(cfg.getMaxPool() > 0
                ? cfg.getMaxPool() : poolProps.getDefaultMaxPoolSize());
        hc.setIdleTimeout(cfg.getIdleTimeoutMs() > 0
                ? cfg.getIdleTimeoutMs() : poolProps.getDefaultIdleTimeout().toMillis());
        hc.setValidationTimeout(poolProps.getValidationTimeout().toMillis());

        /* ⚠️  No `connectionInitSql` here – see class-level javadoc. */

        HikariDataSource ds = new HikariDataSource(hc);

        ensureSchemas(ds);

        // run migrations once per pool
        for (String schema : requiredSchemas) {
            schemaGen.migrate(ds, schema);
        }

        log.info("Created datasource for tenant {}", tenantId);
        return ds;
    }

    /* ───────────────────── schema helper ────────────────────────── */

    private void ensureSchemas(DataSource ds) {
        try (Connection c = ds.getConnection();
             Statement  s = c.createStatement()) {

            for (String schema : requiredSchemas) {
                s.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            }
        } catch (SQLException e) {
            log.warn("Schema init failed – ignored", e);
        }
    }
}
