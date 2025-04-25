package org.zakariafarih.multitenancycore;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Runs Flyway baseline + migrate inside each service schema
 * of a tenant database.
 */
@Slf4j
public class SchemaGenerator {

    public void migrate(DataSource tenantDs, String schema) {
        log.info("Migrating schema {}", schema);
        Flyway.configure()
                .dataSource(tenantDs)
                .schemas(schema)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}