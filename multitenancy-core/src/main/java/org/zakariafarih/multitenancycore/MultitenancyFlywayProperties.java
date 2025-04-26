package org.zakariafarih.multitenancycore;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Flyway-related tunables (lock retries, etc.).
 */
@Data
@ConfigurationProperties(prefix = "multitenancy.flyway")
public class MultitenancyFlywayProperties {

    /** Retries to acquire Flyway history table lock (0 = Flyway default). */
    private int lockRetryCount = 5;
}
