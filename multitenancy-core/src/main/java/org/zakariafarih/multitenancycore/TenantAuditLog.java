package org.zakariafarih.multitenancycore;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** Immutable audit trail for tenant-bootstrap actions. */
@Entity @Table(name = "tenant_audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantAuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tenantId;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private EventType eventType;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant tsUtc;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /* ——— simple enums keep SQL readable ——— */

    public enum Status { SUCCESS, FAILURE }

    public enum EventType {
        DB_CREATE,
        SCHEMA_MIGRATE,
        ONBOARDING_SUCCESS,
        ONBOARDING_ERROR
    }
}
