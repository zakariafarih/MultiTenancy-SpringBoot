package org.zakariafarih.multitenancyrouting;

import com.github.benmanes.caffeine.cache.*;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.*;
import org.zakariafarih.multitenancycore.*;

import javax.sql.DataSource;

/**
 * One {@link JpaTransactionManager} per tenant, cached with Caffeine.
 * Eviction closes the underlying EntityManagerFactory to prevent leaks.
 */
@Slf4j
public class RoutingJpaTransactionManager
        implements PlatformTransactionManager, DisposableBean {

    private final TenantDataSourceManager     dsm;
    private final EntityManagerFactoryManager emfm;
    private final String[]                    serviceSchemas;
    private final MultitenancyPoolProperties  poolProps;

    /* holder keeps a reference to close EMF on eviction */
    private record Holder(JpaTransactionManager tm, EntityManagerFactory emf) {}

    private final LoadingCache<String, Holder> cache;

    public RoutingJpaTransactionManager(TenantDataSourceManager dsm,
                                        EntityManagerFactoryManager emfm,
                                        String[] serviceSchemas,
                                        MultitenancyPoolProperties poolProps) {

        this.dsm            = dsm;
        this.emfm           = emfm;
        this.serviceSchemas = serviceSchemas;
        this.poolProps      = poolProps;

        this.cache = Caffeine.newBuilder()
                .maximumSize(poolProps.getMaxTenantPools())
                .expireAfterAccess(poolProps.getIdleEviction())
                .removalListener(this::closeOnEvict)
                .build(this::create);
    }

    /* ---------- PlatformTransactionManager ---------- */

    @Override
    public TransactionStatus getTransaction(TransactionDefinition def)
            throws TransactionException {
        return delegate().getTransaction(def);
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        delegate().commit(status);
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        delegate().rollback(status);
    }

    /* ---------- lifecycle ---------- */

    @Override
    public void destroy() {
        cache.invalidateAll();
        cache.cleanUp(); // forces removal listener -> EMF.close()
    }

    /* ---------- helpers ---------- */

    private Holder create(String tenantId) {
        DataSource ds = dsm.get(tenantId);
        String defaultSchema = serviceSchemas[0];
        EntityManagerFactory emf = emfm.get(ds, tenantId, defaultSchema);
        return new Holder(new JpaTransactionManager(emf), emf);
    }

    private void closeOnEvict(String tenant, Holder holder, RemovalCause cause) {
        if (holder != null && holder.emf().isOpen()) {
            holder.emf().close();
            log.info("Closed EMF for tenant {} (cause: {})", tenant, cause);
        }
    }

    private JpaTransactionManager delegate() {
        return cache.get(TenantContext.getRequired()).tm();
    }
}
