package org.zakariafarih.multitenancyrouting;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.zakariafarih.multitenancycore.TenantContext;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delegates to a cached {@link JpaTransactionManager} for the *current* tenant.
 * The first schema registered for the service becomes the default; entities
 * may still declare a @Table(schema=…).
 */
@RequiredArgsConstructor
public class RoutingJpaTransactionManager implements PlatformTransactionManager {

    private final TenantDataSourceManager     dsm;
    private final EntityManagerFactoryManager emfm;
    private final String[]                    serviceSchemas;   // injected from properties

    private final Map<String, JpaTransactionManager> cache = new ConcurrentHashMap<>();

    private JpaTransactionManager delegate() {
        String tenant = TenantContext.getRequired();

        return cache.computeIfAbsent(tenant, id -> {
            DataSource ds  = dsm.get(id);
            /* Choose first service schema as 'default'; it only affects
               hibernate.default_schema, not @Table-explicit entities. */
            String defaultSchema = serviceSchemas[0];
            EntityManagerFactory emf = emfm.get(ds, id, defaultSchema);
            return new JpaTransactionManager(emf);
        });
    }

    @Override public TransactionStatus getTransaction(TransactionDefinition d)
            throws TransactionException { return delegate().getTransaction(d); }

    @Override public void commit(TransactionStatus s)
            throws TransactionException { delegate().commit(s); }

    @Override public void rollback(TransactionStatus s)
            throws TransactionException { delegate().rollback(s); }
}
