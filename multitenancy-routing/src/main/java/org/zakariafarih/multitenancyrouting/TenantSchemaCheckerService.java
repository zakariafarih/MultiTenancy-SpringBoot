package org.zakariafarih.multitenancyrouting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zakariafarih.multitenancycore.MultitenancyMonitoringProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantSchemaCheckerService {

    private final MultitenancyMonitoringProperties props;

    public CheckResult check(DataSource ds) throws SQLException {

        /* ↓ normalise to lower-case so H2/PG quirks don’t matter */
        Set<String> presentSchemas = listSchemas(ds).stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Map<String, List<String>> missing = new LinkedHashMap<>();

        /* ── validate schemas & tables ───────────────────────────── */
        for (String schema : props.getExpectedSchemas()) {
            if (!presentSchemas.contains(schema.toLowerCase(Locale.ROOT))) {
                missing.put(schema, List.of());
                continue;
            }
            var critical = props.getCriticalTables().getOrDefault(schema, List.of());
            List<String> absentTables = new ArrayList<>();
            for (String t : critical) {
                if (!tableExists(ds, schema, t)) absentTables.add(t);
            }
            if (!absentTables.isEmpty()) missing.put(schema, absentTables);
        }

        return new CheckResult(missing.isEmpty(), missing);
    }

    /* ═════ small helpers ═════ */

    private Set<String> listSchemas(DataSource ds) {
        try (Connection c = ds.getConnection();
             ResultSet rs = c.getMetaData().getSchemas()) {
            Set<String> s = new HashSet<>();
            while (rs.next()) s.add(rs.getString("TABLE_SCHEM"));
            return s;
        } catch (SQLException e) {
            throw new IllegalStateException("schema query failed", e);
        }
    }

    private boolean tableExists(DataSource ds, String schema, String table)
            throws SQLException {

        /* query *all* tables and filter in-memory so we are immune to
            DB-specific case-sensitivity quirks (H2 vs PostgreSQL). */
            try (Connection c = ds.getConnection();
            ResultSet rs = c.getMetaData().getTables(
                null, null, null, new String[]{"TABLE"})) {

            while (rs.next()) {
                String s = rs.getString("TABLE_SCHEM");
                String t = rs.getString("TABLE_NAME");
                if (schema.equalsIgnoreCase(s) && table.equalsIgnoreCase(t)) {
                    return true;
                }
            }
            return false;
        }
    }


    public record CheckResult(boolean ok, Map<String, List<String>> missing) {}
}
