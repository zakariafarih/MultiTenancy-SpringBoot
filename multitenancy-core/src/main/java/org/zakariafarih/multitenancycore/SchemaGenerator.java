package org.zakariafarih.multitenancycore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Runs Flyway baseline + migrate for the given service schema.
 */
@Slf4j
@RequiredArgsConstructor
public class SchemaGenerator {

    private final MultitenancyFlywayProperties props;

    public void migrate(DataSource tenantDs, String schema) {
        log.info("Migrating schema {}", schema);

        String location = "classpath:db/migration/" + schema;

        Flyway.configure()
                .dataSource(tenantDs)
                .schemas(schema)
                .locations(location)
                .baselineOnMigrate(true)
                .lockRetryCount(props.getLockRetryCount())
                .load()
                .migrate();
    }
}
