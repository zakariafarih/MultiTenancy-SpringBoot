package org.zakariafarih.multitenancyautoconfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.zakariafarih.multitenancycore.*;

import javax.sql.DataSource;

@AutoConfiguration
@EnableConfigurationProperties({
        MultitenancyProps.class,
        MultitenancyPoolProperties.class,
        MultitenancyFlywayProperties.class,
        MultitenancySecurityProps.class,
        MultitenancyMonitoringProperties.class
})
@RequiredArgsConstructor
@ConfigurationPropertiesScan("org.zakariafarih")   // scans tenants.yml POJOs too
public class MultitenancyAutoConfiguration {

    private final Environment env;

    /* ──────────────── Admin DS ──────────────── */

    @Bean
    @ConditionalOnProperty(prefix = "multitenancy.admin", name = "url")
    public DataSource adminDataSource() {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(env.getProperty("multitenancy.admin.url"));
        hc.setUsername(env.getProperty("multitenancy.admin.username"));
        hc.setPassword(env.getProperty("multitenancy.admin.password"));
        return new HikariDataSource(hc);
    }

    /* ─────────── Core beans ─────────── */

    @Bean public TenantRegistry tenantRegistry() { return new TenantRegistry(); }

    @Bean @ConditionalOnMissingBean
    public SchemaGenerator schemaGenerator(MultitenancyFlywayProperties fp) {
        return new SchemaGenerator(fp);
    }

    @Bean @ConditionalOnMissingBean public EMFProvider emfProvider() { return new EMFProvider(); }

    /* ─────────── Bootstrapper (runs at start-up) ─────────── */

    @Bean @ConditionalOnMissingBean
    public TenantDatabaseCreator tenantDatabaseCreator(
            @Qualifier("adminDataSource") DataSource adminDataSource,
            MultitenancyPoolProperties poolProps) {

        return new TenantDatabaseCreator(
                adminDataSource,
                env.getProperty("multitenancy.admin.username", ""),
                env.getProperty("multitenancy.admin.password", ""),
                poolProps
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "multitenancy.bootstrap",
            name   = "enabled",
            matchIfMissing = true)
    public TenantBootstrapper tenantBootstrapper(
            TenantRegistry registry,
            TenantDatabaseCreator dbCreator,
            MultitenancyProps props,
            SchemaGenerator schemaGen,
            TenantAuditLogger auditLogger) {

        return new TenantBootstrapper(
                registry,
                dbCreator,
                tenantId -> { throw new UnsupportedOperationException("DS lookup not available in autoconfig"); },
                schemaGen,
                props.getSchemas(),
                auditLogger
        );
    }

    /* ─────────── Thread-local propagation ─────────── */

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
