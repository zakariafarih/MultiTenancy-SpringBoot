package org.zakariafarih.multitenancyautoconfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.zakariafarih.multitenancycore.*;
import org.zakariafarih.multitenancyrepo.TenantRepositoryFactory;
import org.zakariafarih.multitenancyrouting.DynamicRoutingDataSource;
import org.zakariafarih.multitenancyrouting.EntityManagerFactoryManager;
import org.zakariafarih.multitenancyrouting.RoutingJpaTransactionManager;
import org.zakariafarih.multitenancyrouting.TenantDataSourceManager;

import javax.sql.DataSource;

@AutoConfiguration
@EnableConfigurationProperties(MultitenancyProps.class)
@RequiredArgsConstructor
@ConfigurationPropertiesScan("org.zakariafarih.multitenancycore")
public class MultitenancyAutoConfiguration {

    private final Environment env;

    // ────────────────  Admin DS  ────────────────
    @Bean
    @ConditionalOnProperty(prefix = "multitenancy.admin", name = "url")
    public DataSource adminDataSource() {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(env.getProperty("multitenancy.admin.url"));
        hc.setUsername(env.getProperty("multitenancy.admin.username"));
        hc.setPassword(env.getProperty("multitenancy.admin.password"));
        return new HikariDataSource(hc);
    }

    // ───────────  Core helpers & context  ───────────
    @Bean public TenantRegistry tenantRegistry() { return new TenantRegistry(); }
    @Bean @ConditionalOnMissingBean public SchemaGenerator schemaGenerator() { return new SchemaGenerator(); }
    @Bean @ConditionalOnMissingBean public EMFProvider      emfProvider()     { return new EMFProvider(); }

    // ───────────  DS + EMF managers  ───────────
    @Bean @ConditionalOnMissingBean
    public TenantDataSourceManager tenantDataSourceManager(TenantRegistry registry) {
        return new TenantDataSourceManager(registry);
    }

    @Bean @ConditionalOnMissingBean
    public EntityManagerFactoryManager entityManagerFactoryManager(
            EMFProvider provider,
            MultitenancyProps props) {
        return new EntityManagerFactoryManager(provider, props.getPackages());
    }

    // ────────────────  Dynamic routing DS  ────────────────
    @Bean
    public DataSource routingDataSource(TenantDataSourceManager dsm) {
        return new DynamicRoutingDataSource(dsm);
    }

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager multitenantTxManager(
            TenantDataSourceManager dsm,
            EntityManagerFactoryManager emfm,
            MultitenancyProps props) {
        return new RoutingJpaTransactionManager(dsm, emfm, props.getSchemas());
    }

    @Bean @ConditionalOnMissingBean
    public TenantIdResolver tenantIdResolver() {
        return new HeaderPathTenantIdResolver();
    }

    @Bean @ConditionalOnMissingBean
    public TransactionTemplate tenantTransactionTemplate(PlatformTransactionManager multitenantTxManager) {
        return new TransactionTemplate(multitenantTxManager);
    }

    // ─────────────  Repository factory  ─────────────
    @Bean @ConditionalOnMissingBean
    public TenantRepositoryFactory tenantRepositoryFactory(
            TenantDataSourceManager dsm,
            EntityManagerFactoryManager emfm) {
        return new TenantRepositoryFactory(dsm, emfm);
    }

    // ───────────  Bootstrapper  ───────────
    @Bean @ConditionalOnMissingBean
    public TenantDatabaseCreator tenantDatabaseCreator(DataSource adminDataSource) {
        return new TenantDatabaseCreator(
                adminDataSource,
                env.getProperty("multitenancy.admin.username", ""),
                env.getProperty("multitenancy.admin.password", "")
        );
    }

    @Bean @ConditionalOnMissingBean
    public TenantBootstrapper tenantBootstrapper(
            TenantRegistry registry,
            TenantDatabaseCreator dbCreator,
            TenantDataSourceManager dsm,
            SchemaGenerator schemaGen) {

        String[] schemas = env.getProperty("multitenancy.schemas", "clinic").split("\\s*,\\s*");
        return new TenantBootstrapper(
                registry,
                dbCreator,
                tenantId -> dsm.get(tenantId.trim()),
                schemaGen,
                schemas
        );
    }

    // ───────────  Thread-local propagation  ───────────
    @Bean
    public TaskDecorator tenantAwareTaskDecorator() {
        return runnable -> {
            String tenant = TenantContext.get();
            return () -> {
                try {
                    if (tenant != null) TenantContext.set(tenant);
                    runnable.run();
                } finally {
                    TenantContext.clear();
                }
            };
        };
    }
}
