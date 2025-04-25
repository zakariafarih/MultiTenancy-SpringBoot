package org.zakariafarih.multitenancycore;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global multitenancy settings resolved from
 * <pre>multitenancy.*</pre> properties.
 */
@Data
@ConfigurationProperties(prefix = "multitenancy")
public class MultitenancyProps {

    /** Base packages containing @Entity classes (comma separated). */
    private String[] packages = { "com.example" };

    /** Schemas owned by this micro-service (comma separated). */
    private String[] schemas  = { "public" };
}
