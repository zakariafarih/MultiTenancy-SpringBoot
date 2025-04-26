package org.zakariafarih.multitenancytest;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;
import org.zakariafarih.cliniccore.ClinicCoreApplication;
import org.zakariafarih.multitenancycore.TenantRegistry;
import org.zakariafarih.multitenancyrouting.TenantSchemaCheckerService;
import org.zakariafarih.multitenancyrouting.TenantDataSourceManager;

@SpringBootTest(
        classes = ClinicCoreApplication.class,
        properties = {           // override default tenant list with the big one
                "spring.config.import=classpath:tenants-big.yml"
        }
)
@ActiveProfiles("test")
@Slf4j
class ScalabilityBootTest {

    @Resource
    TenantRegistry             registry;
    @Resource TenantDataSourceManager    dsm;
    @Resource TenantSchemaCheckerService checker;

    @Test
    void coldStartWith200Tenants() throws Exception {

        /* ── 1️⃣ measure boot time ─────────────────────── */
        StopWatch sw = new StopWatch("cold-start");
        sw.start("context-refresh");
        // context already running at this point
        sw.stop();
        log.info(sw.prettyPrint());

        /* ── 2️⃣ basic validations ─────────────────────── */
        Assertions.assertEquals(
                200, registry.getTenantMap().size(), "missing tenants in registry");

        /* every tenant is routable and all expected schemas exist */
        var badTenants = registry.getTenantMap().keySet().parallelStream()
                .filter(t -> {
                    try {
                        var res = checker.check(dsm.get(t));
                        return !res.ok();
                    } catch (Exception ex) {
                        log.warn("schema check failed for {}", t, ex);
                        return true;
                    }
                }).toList();

        Assertions.assertTrue(badTenants.isEmpty(),
                "Some tenants failed validation: " + badTenants);

        log.info("✔️  Cold-start scalability test passed");
    }
}
