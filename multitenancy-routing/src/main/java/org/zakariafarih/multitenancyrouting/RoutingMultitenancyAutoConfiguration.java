package org.zakariafarih.multitenancyrouting;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.zakariafarih.multitenancycore.*;

import javax.sql.DataSource;
import java.util.HashMap;

/**
 * Auto-configuration that wires all routing / multi-tenant infrastructure.
 */
@AutoConfiguration(
        before = org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
)
@RequiredArgsConstructor
public class RoutingMultitenancyAutoConfiguration {

    private final TenantRegistry             registry;
    private final MultitenancyPoolProperties pool;
    private final EMFProvider                emfProvider;
    private final MultitenancyProps          props;

    /* ───────── per-tenant datasource cache ───────── */
    @Bean
    public TenantDataSourceManager tenantDataSourceManager(
            TenantRegistry registry,
            MultitenancyPoolProperties pool,
            MultitenancyProps props,
            MultitenancyMonitoringProperties monitor,
            SchemaGenerator schemaGen) {

        return new TenantDataSourceManager(registry, pool, props, monitor, schemaGen);
    }

    /* ───────── EMF cache helper ───────── */
    @Bean
    public EntityManagerFactoryManager entityManagerFactoryManager() {
        return new EntityManagerFactoryManager(emfProvider, props.getPackages());
    }

    /* ───────── routing DataSource (lazy) ───────── */
    @Bean
    public DataSource routingDataSource(TenantDataSourceManager dsm) {
        return new DynamicRoutingDataSource(dsm);
    }

    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(TenantDataSourceManager dsm) {
        return new org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy(
                new DynamicRoutingDataSource(dsm));
    }

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            JpaProperties jpa) {

        var fb = new LocalContainerEntityManagerFactoryBean();
        fb.setDataSource(dataSource);
        fb.setPackagesToScan(props.getPackages());
        fb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        fb.setJpaPropertyMap(new HashMap<>(jpa.getProperties()));
        fb.getJpaPropertyMap().put("hibernate.default_schema", props.getSchemas()[0]);
        return fb;
    }

    /* ───────── routing-aware Tx manager ───────── */
    @Bean
    @Primary
    public PlatformTransactionManager routingTxManager(
            TenantDataSourceManager dsm,
            EntityManagerFactoryManager emfm) {

        return new RoutingJpaTransactionManager(
                dsm, emfm, props.getSchemas(), pool);
    }

    /* ───────── TenantRepositoryFactory ───────── */
    @Bean
    @ConditionalOnMissingBean
    public TenantRepositoryFactory tenantRepositoryFactory(
            TenantDataSourceManager dsm,
            EntityManagerFactoryManager emfm) {

        return new TenantRepositoryFactory(dsm, emfm);
    }
}
