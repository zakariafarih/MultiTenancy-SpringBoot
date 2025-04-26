package org.zakariafarih.multitenancyrouting;

import com.github.benmanes.caffeine.cache.*;
import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.zakariafarih.multitenancycore.MultitenancyMonitoringProperties;
import org.zakariafarih.multitenancycore.TenantRegistry;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@ConditionalOnBean(MeterRegistry.class)
@Component
@Slf4j
public class TenantMetricsExporter {

    private final TenantRegistry                   registry;
    private final TenantDataSourceManager          dsm;
    private final MultitenancyMonitoringProperties props;
    private final MeterRegistry                    meters;
    private final Cache<String, Map<String, Long>> cache;

    public TenantMetricsExporter(TenantRegistry                   registry,
                                 TenantDataSourceManager          dsm,
                                 MultitenancyMonitoringProperties props,
                                 MeterRegistry                    meters) {

        this.registry = registry;
        this.dsm      = dsm;
        this.props    = props;
        this.meters   = meters;

        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(props.getCacheTtl())
                .build();
    }

    /* ───────────────────────────── */

    @PostConstruct
    void init() {
        registry.getTenantMap().keySet().forEach(this::registerGauges);
    }

    /* ────────── gauge registration ────────── */

    private void registerGauges(String tenant) {

        Gauge.builder("tenant_schema_count",
                        () -> cacheGet(tenant).size())
                .tag("tenant", tenant)
                .description("Number of schemas in tenant DB")
                .register(meters);

        /* table count per schema */
        props.getExpectedSchemas().forEach(schema ->
                Gauge.builder("tenant_table_count",
                                () -> cacheGet(tenant).getOrDefault(schema, 0L))
                        .tag("tenant", tenant)
                        .tag("schema", schema)
                        .register(meters));

        /* optional row-counts */
        if (props.isExportRowCounts()) {
            props.getCriticalTables().forEach((schema, tables) ->
                    tables.forEach(table ->
                            Gauge.builder("tenant_table_rowcount",
                                            () -> rowCount(tenant, schema, table))
                                    .strongReference(true)
                                    .tag("tenant", tenant).tag("schema", schema).tag("table", table)
                                    .register(meters)));
        }
    }

    /* ────────── heavy queries (cached) ────────── */

    private Map<String, Long> cacheGet(String tenant) {
        return cache.get(tenant, this::schemaTableCounts);
    }

    private Map<String, Long> schemaTableCounts(String tenant) {
        try (Connection c = dsm.get(tenant).getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT table_schema, COUNT(*)
                     FROM information_schema.tables
                     WHERE table_type='BASE TABLE'
                     GROUP BY table_schema
                 """);
             ResultSet rs = ps.executeQuery()) {

            Map<String, Long> m = new HashMap<>();
            while (rs.next()) m.put(rs.getString(1), rs.getLong(2));
            return m;

        } catch (SQLException e) {
            throw new IllegalStateException("metrics query failed", e);
        }
    }

    private long rowCount(String tenant, String schema, String table) {
        String sql = """
                SELECT reltuples
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ? AND c.relname = ?
            """;
        try (Connection c = dsm.get(tenant).getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return (rs.next()) ? Math.round(rs.getDouble(1)) : -1;
            }
        } catch (SQLException e) {
            return -1;
        }
    }
}
