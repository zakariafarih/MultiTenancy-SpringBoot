package org.zakariafarih.multitenancyrepo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MultitenancyRepoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultitenancyRepoApplication.class, args);
    }

}
