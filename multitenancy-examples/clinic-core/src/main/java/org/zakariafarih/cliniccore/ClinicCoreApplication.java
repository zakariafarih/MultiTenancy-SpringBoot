package org.zakariafarih.cliniccore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        exclude = {DataSourceAutoConfiguration.class},
        scanBasePackages = "org.zakariafarih"
)
@EnableTransactionManagement
public class ClinicCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicCoreApplication.class, args);
    }

}
