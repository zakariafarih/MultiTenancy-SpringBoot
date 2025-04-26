package org.zakariafarih.multitenancycore;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Global, application-wide pool and cache parameters.
 *
 * Controlled via <pre>multitenancy.pool.*</pre> in YAML/props.
 */
@Data
@ConfigurationProperties(prefix = "multitenancy.pool")
public class MultitenancyPoolProperties {

    /** Maximum Hikari pools kept in memory concurrently. */
    private int maxTenantPools = 200;

    /** LRU eviction timeout for an idle tenant pool / EMF / TxManager. */
    private Duration idleEviction = Duration.ofHours(2);

    /** Default maximumPoolSize for tenant Hikari pools (overridable per tenant). */
    private int defaultMaxPoolSize = 10;

    /** Default idleTimeout for tenant Hikari pools. */
    private Duration defaultIdleTimeout = Duration.ofMinutes(10);

    /** Hikari validation timeout. */
    private Duration validationTimeout = Duration.ofSeconds(3);

    /** Pool size used by one-off DDL operations (TenantDatabaseCreator). */
    private int ddlPoolSize = 1;
}
