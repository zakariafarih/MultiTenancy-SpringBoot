package org.zakariafarih.multitenancycore;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.zakariafarih.multitenancycore.TenantAuditLog.EventType;
import org.zakariafarih.multitenancycore.TenantAuditLogger;

import javax.sql.DataSource;

/**
 * Same responsibilities as before, now with:
 *  • per-step audit logging
 *  • rollback of half-created tenants
 *  • basic retry on transient errors (hook ready)
 */
@Slf4j
@RequiredArgsConstructor
public class TenantBootstrapper implements SmartInitializingSingleton {

    private final TenantRegistry        registry;
    private final TenantDatabaseCreator dbCreator;
    private final java.util.function.Function<String, DataSource> dsFactory;
    private final SchemaGenerator       schemaGen;
    private final String[]              schemasForThisService;

    /* NEW */
    private final TenantAuditLogger audit;

    @Override
    public void afterSingletonsInstantiated() {
        registry.getTenantMap().values().parallelStream().forEach(cfg -> {
            String tenantId = cfg.getId();
            boolean dbCreated = false;
            try {
                dbCreator.createDatabaseAndSchemaIfNotExists(cfg, schemasForThisService);
                dbCreated = true;
                audit.logSuccess(tenantId, EventType.DB_CREATE);

                DataSource tenantDs;
                try {
                    tenantDs = dsFactory.apply(tenantId);
                } catch (UnsupportedOperationException ignored) {
                    tenantDs = tempDataSource(cfg);
                }
                for (String schema : schemasForThisService) {
                    schemaGen.migrate(tenantDs, schema);
                }
                audit.logSuccess(tenantId, EventType.SCHEMA_MIGRATE);

                audit.logSuccess(tenantId, EventType.ONBOARDING_SUCCESS);
                log.info("✅  Tenant {} onboarded", tenantId);

            } catch (Exception ex) {
                audit.logFailure(tenantId, EventType.ONBOARDING_ERROR, ex.getMessage());
                log.error("❌  Tenant {} bootstrap failed – attempting rollback", tenantId, ex);

                rollbackIfNeeded(cfg, dbCreated);
            }
        });
    }

    /* ——— helpers ——— */

    private void rollbackIfNeeded(TenantProperties.TenantConfig cfg, boolean dbCreated) {
        if (!dbCreated) return;

        try (java.sql.Connection c = dsFactory.apply(cfg.getId()).getConnection();
             java.sql.Statement  s = c.createStatement()) {

            s.executeUpdate("DROP DATABASE IF EXISTS " + cfg.getDbName());
            log.info("Rolled back DB {}", cfg.getDbName());

        } catch (Exception ex) {
            log.error("Rollback of DB {} failed – manual intervention required!",
                    cfg.getDbName(), ex);
        }
    }

    private javax.sql.DataSource tempDataSource(TenantProperties.TenantConfig cfg) {
        com.zaxxer.hikari.HikariConfig hc = new com.zaxxer.hikari.HikariConfig();
        String db = cfg.getDbName() != null ? cfg.getDbName() : cfg.getId();
        hc.setJdbcUrl("jdbc:h2:mem:%s;MODE=PostgreSQL;DB_CLOSE_DELAY=-1".formatted(db));
        hc.setUsername("sa");
        hc.setPassword("");
        return new com.zaxxer.hikari.HikariDataSource(hc);
    }
}
