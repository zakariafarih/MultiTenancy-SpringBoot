package org.zakariafarih.multitenancyautoconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MultitenancyAutoconfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultitenancyAutoconfigApplication.class, args);
    }

}
