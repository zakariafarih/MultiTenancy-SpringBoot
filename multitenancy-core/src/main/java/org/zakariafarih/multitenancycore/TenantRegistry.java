package org.zakariafarih.multitenancycore;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that loads tenants.yml at startup and
 * gives cheap lookup by tenantId.
 */
@Slf4j
public class TenantRegistry {

    @Getter
    private final Map<String, TenantProperties.TenantConfig> tenantMap = new ConcurrentHashMap<>();

    public TenantRegistry() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("tenants.yml")) {
            if (in == null) throw new IllegalStateException("tenants.yml not found on classpath");
            Yaml yaml = new Yaml();
            Map<?, ?> raw = yaml.load(in);
            TenantProperties props = yaml.loadAs(yaml.dump(raw), TenantProperties.class);

            props.getTenants().forEach(cfg -> {
                log.info("Registering tenant {}", cfg.getId());
                tenantMap.put(cfg.getId(), cfg);
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load tenants.yml", e);
        }
    }

    public TenantProperties.TenantConfig get(String tenantId) {
        TenantProperties.TenantConfig cfg = tenantMap.get(tenantId);
        if (cfg == null) throw new UnknownTenantException(tenantId);
        return cfg;
    }
}