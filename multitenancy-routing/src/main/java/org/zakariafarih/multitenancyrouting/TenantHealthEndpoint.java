package org.zakariafarih.multitenancyrouting;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

@RestController
@RequestMapping("/actuator/tenants/{tenantId}/health")
@RequiredArgsConstructor
@Slf4j
public class TenantHealthEndpoint {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private final TenantDataSourceManager dsm;
    private final TenantSchemaCheckerService checker;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Health health(@PathVariable String tenantId) {

        try (Connection ignored = ds(tenantId).getConnection()) {

            var res = checker.check(ds(tenantId));

            return res.ok()
                    ? Health.up().withDetail("tenant", tenantId).build()
                    : Health.down().withDetail("tenant", tenantId)
                    .withDetail("missing", res.missing()).build();

        } catch (Exception ex) {
            log.warn("Tenant {} health DOWN: {}", tenantId, ex.getMessage());
            return Health.down(ex).withDetail("tenant", tenantId).build();
        }
    }

    /* helper */
    private DataSource ds(String tenantId) {
        DataSource ds = dsm.get(tenantId);
        if (ds instanceof HikariDataSource hds) {
            hds.setConnectionTimeout(TIMEOUT.toMillis());
        }
        return ds;
    }
}

