package org.zakariafarih.multitenancycore;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;

@AutoConfiguration
@RequiredArgsConstructor
@EntityScan(basePackageClasses = TenantAuditLog.class)
@EnableJpaRepositories(
        basePackageClasses = TenantAuditLogRepository.class,
        entityManagerFactoryRef = "auditEntityManagerFactory",
        transactionManagerRef  = "auditTxManager")
@ConditionalOnBean(name = "adminDataSource")
public class AuditJpaAutoConfiguration {

    private final DataSource adminDataSource;
    private final JpaProperties jpaProperties;

    @Bean
    public LocalContainerEntityManagerFactoryBean auditEntityManagerFactory() {
        var fb = new LocalContainerEntityManagerFactoryBean();
        fb.setDataSource(adminDataSource);
        fb.setPackagesToScan(TenantAuditLog.class.getPackageName());
        fb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        /* propagate dialect & any other spring.jpa.* properties */
        fb.setJpaPropertyMap(new HashMap<>(jpaProperties.getProperties()));
        return fb;
    }

    @Bean("auditTxManager")
    public JpaTransactionManager auditTxManager(
            @Qualifier("auditEntityManagerFactory") EntityManagerFactory auditEntityManagerFactory) {
        return new JpaTransactionManager(auditEntityManagerFactory);
    }
}
