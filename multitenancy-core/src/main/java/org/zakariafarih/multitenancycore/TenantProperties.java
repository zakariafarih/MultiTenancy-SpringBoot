package org.zakariafarih.multitenancycore;

import lombok.Data;

import java.util.List;

/**
 * POJO mapped from tenants.yml
 */
@Data
public class TenantProperties {
    private List<TenantConfig> tenants;

    @Data
    public static class TenantConfig {
        private String id;
        private String dbName;
        private String dbUser;
        private String dbPassword;
        private String host = "localhost";
        private int    port = 5432;
        private int maxPool;    // Optional per-tenant override
        private long idleTimeoutMs;     // optional
    }
}