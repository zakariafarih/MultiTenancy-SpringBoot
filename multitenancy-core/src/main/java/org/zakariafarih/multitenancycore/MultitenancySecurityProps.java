package org.zakariafarih.multitenancycore;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "multitenancy.security")
public class MultitenancySecurityProps {
    /** Shared admin API key value (e.g., in Vault/Env). */
    private String apiKey;

    /** CIDR or single IPs allowed for onboarding; empty = allow all. */
    private List<String> allowlist = List.of();
}
