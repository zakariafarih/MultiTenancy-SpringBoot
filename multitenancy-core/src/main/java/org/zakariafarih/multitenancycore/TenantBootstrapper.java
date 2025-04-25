package org.zakariafarih.multitenancycore;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Spring component that runs at startup and ensures:
 * 1) databases exist
 * 2) service schemas exist
 * 3) Flyway migrations are applied
 */
@Slf4j
@RequiredArgsConstructor
public class TenantBootstrapper {

    private final TenantRegistry registry;
    private final TenantDatabaseCreator databaseCreator;
    private final Function<String, DataSource> dataSourceFactory;
    private final SchemaGenerator       schemaGen;

    private final String[] schemasForThisService;

    @PostConstruct
    public void init() {
        registry.getTenantMap().values().parallelStream().forEach(cfg -> {
                    databaseCreator.createDatabaseAndSchemaIfNotExists(cfg, schemasForThisService);
                    DataSource tenantDs = dataSourceFactory.apply(cfg.getId());
                    for (String schema : schemasForThisService) {
                        schemaGen.migrate(tenantDs, schema);
                    }
        });
        log.info("Tenant bootstrap completed");
    }
}