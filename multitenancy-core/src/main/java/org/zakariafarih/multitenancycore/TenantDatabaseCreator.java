package org.zakariafarih.multitenancycore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Uses an "admin" DataSource + credentials to create databases and schemas idempotently.
 */
@Slf4j
@RequiredArgsConstructor
public class TenantDatabaseCreator {

    private final DataSource adminDataSource;
    private final String       adminUsername;
    private final String       adminPassword;

    /**
     * Creates the tenant database if missing, then creates the schemas inside it
     * using the admin credentials.
     */
    public void createDatabaseAndSchemaIfNotExists(
            TenantProperties.TenantConfig cfg,
            String[] schemas
    ) {
        String dbName = cfg.getDbName();

        // 1) CREATE DATABASE … via adminDataSource
        try (Connection adminConn = adminDataSource.getConnection();
             Statement adminStmt   = adminConn.createStatement()) {

            log.info("Ensuring database {} exists", dbName);
            /* Many managed Postgres services (RDS, Cloud SQL) forbid CREATE DATABASE.
                Try; if we get a permission error, log & continue. */
            try {
                adminStmt.executeUpdate("CREATE DATABASE " + dbName);
                log.info("Database {} created", dbName);
            }   catch (SQLException ex) {
                if ("42501".equals(ex.getSQLState())) {          // insufficient privilege
                    log.warn("CREATE DATABASE forbidden. Assuming {} already exists.", dbName);
                } else if (!"42P04".equals(ex.getSQLState())) {   // 42P04 = exists
                    throw ex;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure database exists: " + dbName, e);
        }

        // 2) CREATE SCHEMA … via adminUsername/adminPassword
        String url = String.format(
                "jdbc:postgresql://%s:%d/%s",
                cfg.getHost(),
                cfg.getPort(),
                dbName
        );

        try (Connection tenantConn = DriverManager.getConnection(url, adminUsername, adminPassword);
             Statement  schemaStmt  = tenantConn.createStatement()) {

            for (String schema : schemas) {
                String sql = "CREATE SCHEMA IF NOT EXISTS " + schema +
                        " AUTHORIZATION " + cfg.getDbUser();
                schemaStmt.executeUpdate(sql);
                log.info("Ensured schema {} exists in {}", schema, dbName);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure schemas in: " + dbName, e);
        }
    }
}
