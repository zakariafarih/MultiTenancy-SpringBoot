package org.zakariafarih.multitenancyrepo;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.zakariafarih.multitenancyrouting.EntityManagerFactoryManager;
import org.zakariafarih.multitenancyrouting.TenantDataSourceManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Produces Spring-Data repository proxies bound to a concrete
 * (tenantId, schema) at runtime.
 *
 * Caches the RepositoryFactory only – not an EntityManager instance –
 * so each transaction still gets its own, thread-safe EM proxy.
 */
@RequiredArgsConstructor
public class TenantRepositoryFactory {

    private final TenantDataSourceManager     dsManager;
    private final EntityManagerFactoryManager emfManager;

    private final com.github.benmanes.caffeine.cache.LoadingCache<String, RepositoryFactorySupport> cache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(java.time.Duration.ofHours(4))
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

    /* ---------- helper ---------- */
    private RepositoryFactorySupport newFactory(String key) {
        String[] parts = key.split("\\|");
        String tenantId = parts[0];
        String schema   = parts[1];

        DataSource ds = dsManager.get(tenantId);
        EntityManagerFactory emf = emfManager.get(ds, tenantId, schema);
        return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
    }
}
