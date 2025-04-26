package org.zakariafarih.multitenancycore;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class ApiKeyAuthFilter implements Filter {

    private static final String HEADER = "X-Api-Key";
    private static final List<String> PROTECTED = List.of("/tenants","/tenants/**","/actuator","/actuator/**");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final MultitenancySecurityProps props;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  http = (HttpServletRequest) req;
        HttpServletResponse rsp  = (HttpServletResponse) res;
        String path = http.getRequestURI();

        /* skip all non-admin paths */
        if (PROTECTED.stream().noneMatch(p -> matcher.match(p, path))) {
            chain.doFilter(req, res);
            return;
        }

        /* IP allow-list (skip for loop-back so tests & local dev work) */
        String clientIp = http.getRemoteAddr();
        boolean loopback = clientIp.equals("127.0.0.1") ||
                clientIp.equals("::1") ||
                clientIp.startsWith("0:0:0:0:0:0:0:1");
        if (!loopback && !props.getAllowlist().isEmpty() &&
                props.getAllowlist().stream().noneMatch(clientIp::startsWith)) {
            rsp.sendError(HttpStatus.FORBIDDEN.value(), "IP not allowed");
            return;
        }

        /* API-key check (skip when no key configured – used by tests) */
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            String key = http.getHeader(HEADER);
            if (!props.getApiKey().equals(key)) {
                rsp.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API key");
                return;
            }
        }

        /* mark request as authenticated with role ADMIN */
        var auth = new UsernamePasswordAuthenticationToken(
                "admin-api-key", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try { chain.doFilter(req, res); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
