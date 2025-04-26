package org.zakariafarih.multitenancyrouting;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * Builds Spring-Data repository proxies bound to a specific
 *   (tenantId, schema) at runtime.
 *
 * Cached per (tenant | schema) pair – **not** per EntityManager – so every
 * transaction still gets its own thread-safe proxy.
 */
@RequiredArgsConstructor
public class TenantRepositoryFactory {

    private final TenantDataSourceManager     dsManager;
    private final EntityManagerFactoryManager emfManager;

    private final LoadingCache<String, RepositoryFactorySupport> cache =
            Caffeine.newBuilder()
                    .maximumSize(1_000)
                    .expireAfterAccess(Duration.ofHours(4))
                    .build(this::newFactory);

    @SuppressWarnings("unchecked")
    public <T, ID, R extends JpaRepository<T, ID>> R getRepository(
            Class<R> repoInterface,
            String tenantId,
            String schema) {

        String key = tenantId + '|' + schema;
        RepositoryFactorySupport factory = cache.get(key);
        return factory.getRepository(repoInterface);
    }

    /* ---------- helpers ---------- */

    private RepositoryFactorySupport newFactory(String key) {
        String[] split   = key.split("\\|");
        String   tenant  = split[0];
        String   schema  = split[1];

        DataSource            ds  = dsManager.get(tenant);
        EntityManagerFactory  emf = emfManager.get(ds, tenant, schema);

        return new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(emf));
    }
}
