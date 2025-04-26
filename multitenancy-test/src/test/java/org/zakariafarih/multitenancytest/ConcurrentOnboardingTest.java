package org.zakariafarih.multitenancytest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;
import org.zakariafarih.cliniccore.ClinicCoreApplication;
import org.zakariafarih.multitenancycore.TenantRegistry;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@SpringBootTest(
        classes = ClinicCoreApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "multitenancy.security.api-key=itDoesntMatterInTests",
                "spring.main.allow-bean-definition-overriding=true"   /* ease context reload */
        }
)
@ActiveProfiles("test")
@Slf4j
class ConcurrentOnboardingTest {

    private static final int THREADS  = 20;
    private static final int TENANTS  = 50;

    @Autowired TestRestTemplate rest;
    @Autowired TenantRegistry   registry;
    private final ObjectMapper om = new ObjectMapper();

    private final ExecutorService pool =
            Executors.newFixedThreadPool(THREADS);

    @AfterEach
    void tearDown() { pool.shutdownNow(); }

    @Test
    void onboardFiftyTenantsInParallel() throws Exception {

        StopWatch sw = new StopWatch("onboarding-parallel");
        sw.start();

        List<Callable<ResponseEntity<String>>> tasks = IntStream.rangeClosed(1, TENANTS)
                .mapToObj(i -> (Callable<ResponseEntity<String>>) () -> {
                    String id = "load" + i;
                    Payload p = new Payload(id,
                            id + "_db", id + "_app", "pwd");
                    HttpHeaders h = new HttpHeaders();
                    h.setContentType(MediaType.APPLICATION_JSON);
                    h.set("X-Api-Key", "itDoesntMatterInTests");
                    HttpEntity<String> req = new HttpEntity<>(om.writeValueAsString(p), h);
                    return rest.postForEntity("/tenants", req, String.class);
                }).toList();

        List<Future<ResponseEntity<String>>> futures = pool.invokeAll(tasks);

        sw.stop();
        long avgMs = sw.getTotalTimeMillis() / TENANTS;
        long failures = futures.stream()
                .filter(f -> {
                    try { return f.get().getStatusCode() != HttpStatus.CREATED; }
                    catch (Exception e) { return true; }
                }).count();

        log.info("Total: {} ms, avg/tenant ≈ {} ms, failures={}",
                sw.getTotalTimeMillis(), avgMs, failures);

        Assertions.assertEquals(0, failures, "Onboarding failures detected");
        Assertions.assertEquals(
                TENANTS, registry.getTenantMap().keySet().stream()
                        .filter(t -> t.startsWith("load")).count(),
                "Some tenants missing in registry");
    }

    /* simple DTO for the request payload */
    @Data
    static class Payload {
        final String id, dbName, dbUser, dbPassword;
    }
}
