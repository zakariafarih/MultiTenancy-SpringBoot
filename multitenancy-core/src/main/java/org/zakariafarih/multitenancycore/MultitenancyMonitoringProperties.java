package org.zakariafarih.multitenancycore;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Tunables for per-tenant health & metrics. */
@Data
@ConfigurationProperties(prefix = "multitenancy.monitor")
public class MultitenancyMonitoringProperties {

    /** Schemas every tenant DB **must** contain. */
    private List<String> expectedSchemas = List.of("clinic", "audit");

    /** Critical tables per schema — empty list ⇒ only schema presence is validated. */
    private Map<String, List<String>> criticalTables = Map.of(
            "clinic", List.of("patient")
    );

    /** Caffeine-cache TTL for expensive row-count queries. */
    private Duration cacheTtl = Duration.ofMinutes(5);

    /** Toggle live row-count gauges (off by default). */
    private boolean exportRowCounts = false;
}
