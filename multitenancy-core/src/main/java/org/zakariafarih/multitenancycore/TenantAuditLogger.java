package org.zakariafarih.multitenancycore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Best-effort logger for onboarding events.
 * Any failure is swallowed so that the functional flow is never blocked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantAuditLogger {

    private final TenantAuditLogRepository repo;

    @Transactional(value = "auditTxManager", propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(String tenantId,
                           TenantAuditLog.EventType event) {
        saveSafe(TenantAuditLog.builder()
                .tenantId(tenantId)
                .eventType(event)
                .status(TenantAuditLog.Status.SUCCESS)
                .build());
    }

    @Transactional(value = "auditTxManager", propagation = Propagation.REQUIRES_NEW)
    public void logFailure(String tenantId,
                           TenantAuditLog.EventType event,
                           String err) {
        saveSafe(TenantAuditLog.builder()
                .tenantId(tenantId)
                .eventType(event)
                .status(TenantAuditLog.Status.FAILURE)
                .errorMessage(err)
                .build());
    }

    /* ——— internal ——— */

    private void saveSafe(TenantAuditLog entry) {
        try {
            repo.save(entry);
        } catch (Exception ex) {
            log.warn("Audit-log write failed (ignored): {}", ex.getMessage(), ex);
        }
    }
}
