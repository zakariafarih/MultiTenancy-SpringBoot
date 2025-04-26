package org.zakariafarih.multitenancycore;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads every tenants*.yml found on the class-path and provides
 * fast lookup by tenant id.
 */
@Slf4j
public class TenantRegistry {

    @Getter
    private final Map<String, TenantProperties.TenantConfig> tenantMap = new ConcurrentHashMap<>();

    public TenantRegistry() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();

            // first look for any tenants-*.yml (e.g. tenants-big.yml)
            Resource[] dashResources = resolver.getResources("classpath*:**/tenants-*.yml");
            Resource[] toLoad = dashResources.length > 0
                    // if we found any, use only those
                    ? dashResources
                    // otherwise fall back to the classic tenants.yml
                    : resolver.getResources("classpath*:**/tenants.yml");

            // prefer external files (so test’s tenants-big.yml on disk wins over jars)
            var external = Arrays.stream(toLoad)
                    .filter(r -> {
                        try {
                            return "file".equals(r.getURL().getProtocol());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            var resources = external.isEmpty() ? List.of(toLoad) : external;

            Yaml yaml = new Yaml();
            for (var res : resources) {
                try (InputStream in = res.getInputStream()) {
                    var props = yaml.loadAs(in, TenantProperties.class);
                    props.getTenants().forEach(cfg -> {
                        log.info("Registering tenant {} (from {})", cfg.getId(), res.getFilename());
                        tenantMap.put(cfg.getId(), cfg);
                    });
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load tenant descriptors", e);
        }
    }

    public TenantProperties.TenantConfig get(String tenantId) {
        TenantProperties.TenantConfig cfg = tenantMap.get(tenantId);
        if (cfg == null) throw new UnknownTenantException(tenantId);
        return cfg;
    }
}
