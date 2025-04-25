package org.zakariafarih.cliniccore;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.zakariafarih.multitenancyrepo.TenantRepositoryFactory;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tenants/{tenantId}/patients")
public class PatientController {

    private final TenantRepositoryFactory repoFactory;

    @GetMapping
    public List<Patient> list(@PathVariable String tenantId) {
        return repo(tenantId).findAll();
    }

    @PostMapping
    @Transactional
    public Patient add(@PathVariable String tenantId,
                       @RequestBody Patient p) {
        return repo(tenantId).save(p);
    }

    private PatientRepository repo(String tenantId) {
        return repoFactory.getRepository(
                PatientRepository.class, tenantId, "clinic");
    }
}
