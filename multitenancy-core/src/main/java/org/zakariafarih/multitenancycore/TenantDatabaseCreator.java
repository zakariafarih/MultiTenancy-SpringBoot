package org.zakariafarih.multitenancycore;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Idempotently creates (a) the tenant database and (b) every service schema.
 * Uses a one-shot Hikari pool (maxPool = 1) for DDL work.
 */
@Slf4j
@RequiredArgsConstructor
public class TenantDatabaseCreator {

    private final DataSource                 adminDataSource;   // points to postgres/postgresql
    private final String                     adminUsername;
    private final String                     adminPassword;
    private final MultitenancyPoolProperties poolProps;

    public void createDatabaseAndSchemaIfNotExists(
            TenantProperties.TenantConfig cfg,
            String[] schemas) {

        String dbName = cfg.getDbName();

        /* 1️⃣ ensure database exists – Postgres only */
        try (Connection c = adminDataSource.getConnection();
             Statement  s = c.createStatement()) {

            boolean postgres = c.getMetaData().getDatabaseProductName()
                    .toLowerCase().contains("postgres");
            if (postgres) {
                log.info("Ensuring database {} exists (PostgreSQL)", dbName);
                try {
                    s.executeUpdate("CREATE DATABASE " + dbName);
                    log.info("Database {} created", dbName);
                } catch (SQLException ex) {
                    // 42P04 = already exists | 42501 = insufficient privilege
                    if (!"42P04".equals(ex.getSQLState()) &&
                            !"42501".equals(ex.getSQLState())) throw ex;
                    log.debug("CREATE DATABASE skipped: {}", ex.getMessage());
                }
            } else {
                log.info("Non-Postgres database → skipping CREATE DATABASE");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed while ensuring database " + dbName, e);
        }

        /* 2️⃣ ensure service schemas exist (tiny throw-away pool) */
        boolean inMemory = cfg.getHost() == null || cfg.getHost().isBlank();

        String url = inMemory
                ? "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                : "jdbc:postgresql://%s:%d/%s".formatted(cfg.getHost(), cfg.getPort(), dbName);

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);
        hc.setUsername(inMemory ? "sa" : adminUsername);
        hc.setPassword(inMemory ? ""   : adminPassword);
        hc.setPoolName("ddl-" + dbName);
        hc.setMaximumPoolSize(poolProps.getDdlPoolSize());
        hc.setMinimumIdle(0);
        hc.setIdleTimeout(poolProps.getDefaultIdleTimeout().toMillis());
        hc.setValidationTimeout(poolProps.getValidationTimeout().toMillis());

        try (HikariDataSource ddlDs = new HikariDataSource(hc);
             Connection conn       = ddlDs.getConnection();
             Statement  stmt       = conn.createStatement()) {

            for (String schema : schemas) {
                /* H2 cannot handle “… AUTHORIZATION user” unless that role exists */
                String ddl;
                if (inMemory || cfg.getDbUser() == null || cfg.getDbUser().isBlank()) {
                    ddl = "CREATE SCHEMA IF NOT EXISTS %s";
                    stmt.executeUpdate(ddl.formatted(schema));
                } else {
                    ddl = "CREATE SCHEMA IF NOT EXISTS %s AUTHORIZATION %s";
                    stmt.executeUpdate(ddl.formatted(schema, cfg.getDbUser()));
                }
                log.info("Ensured schema {} exists in {}", schema, dbName);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure schemas in " + dbName, e);
        }
    }
}
