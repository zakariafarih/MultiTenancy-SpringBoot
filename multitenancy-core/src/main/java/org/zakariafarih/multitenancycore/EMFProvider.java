package org.zakariafarih.multitenancycore;

import com.github.benmanes.caffeine.cache.*;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class EMFProvider {

    private final LoadingCache<String, EntityManagerFactory> cache =
            Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterAccess(Duration.ofHours(4))
                    .removalListener(this::closeEmf)
                    .build(key -> null);

    public EntityManagerFactory get(DataSource ds,
                                    String tenantId,
                                    String schema,
                                    String[] packages) {
        String key = tenantId + '|' + schema;
        return cache.get(key, __ -> build(ds, schema, packages));
    }

    /* ---------- helpers ---------- */

    private void closeEmf(String key, EntityManagerFactory emf, RemovalCause cause) {
        if (emf != null) emf.close();
        log.info("Closed EMF {} (cause: {})", key, cause);
    }

    private EntityManagerFactory build(DataSource ds,
                                       String schema,
                                       String[] packages) {

        log.info("Building EMF for schema {}", schema);
        LocalContainerEntityManagerFactoryBean fb = new LocalContainerEntityManagerFactoryBean();
        fb.setDataSource(ds);
        fb.setPackagesToScan(packages);
        fb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        fb.getJpaPropertyMap().put("hibernate.default_schema", schema);
        fb.afterPropertiesSet();
        return fb.getObject();
    }
}
