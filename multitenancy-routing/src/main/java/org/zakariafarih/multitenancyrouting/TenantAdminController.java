package org.zakariafarih.multitenancyrouting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.zakariafarih.multitenancycore.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping(path = "/tenants", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class TenantAdminController {

    private final TenantRegistry             registry;
    private final TenantSchemaCheckerService checker;
    private final TenantDataSourceManager    dsm;
    private final TenantDatabaseCreator      dbCreator;
    private final SchemaGenerator            schemaGen;
    private final MultitenancyProps          props;

    /* ────── existing GET endpoints ───────────────────────────── */

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("tenants", registry.getTenantMap().keySet());
    }

    @GetMapping("/{tenantId}/diagnostics")
    public Map<String, Object> diag(@PathVariable String tenantId) throws SQLException {
        var res = checker.check(dsm.get(tenantId));
        return Map.of("tenant", tenantId,
                "up",      res.ok(),
                "missing", res.missing());
    }

    /* ────── NEW  POST /tenants – dynamic onboarding ───────── */

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    /* No transaction manager is bound here on purpose:
       onboard touches the admin DB *and* the brand-new tenant DB(s),
       so a single XA/JTA transaction is not possible.
       We keep the method non-transactional and rely on the per-step
       error handling + rollback already implemented below. */
    public Map<String, Object> onboard(@RequestBody TenantProperties.TenantConfig payload) {

        String id = payload.getId();
        if (registry.getTenantMap().containsKey(id)) {
            throw new IllegalArgumentException("Tenant '" + id + "' already exists");
        }

        try {
            registry.getTenantMap().put(id, payload);
            log.info("▶️  Registering tenant {}", id);

            dbCreator.createDatabaseAndSchemaIfNotExists(payload, props.getSchemas());

            DataSource tenantDs = dsm.get(id);
            for (String schema : props.getSchemas()) {
                schemaGen.migrate(tenantDs, schema);
            }

            log.info("✅  Tenant {} onboarded on-the-fly", id);
            return Map.of("tenant", id, "status", "CREATED");

        } catch (Exception ex) {
            registry.getTenantMap().remove(id);
            log.error("❌  Onboarding of {} failed – rolled back", id, ex);
            throw ex;
        }
    }
}
