package org.zakariafarih.multitenancyautoconfig;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.zakariafarih.multitenancycore.ApiKeyAuthFilter;
import org.zakariafarih.multitenancycore.MultitenancySecurityProps;

@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(MultitenancySecurityProps.class)
@RequiredArgsConstructor
public class OnboardingSecurityAutoConfiguration {

    private final MultitenancySecurityProps props;

    @Bean
    @Order(0)
    SecurityFilterChain apiKeyChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/tenants", "/tenants/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .addFilterBefore(new ApiKeyAuthFilter(props), BasicAuthenticationFilter.class);

        return http.build();
    }
}
