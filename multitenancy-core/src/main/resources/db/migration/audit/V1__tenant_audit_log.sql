CREATE TABLE IF NOT EXISTS tenant_audit_log (
                                                id            BIGSERIAL PRIMARY KEY,
                                                tenant_id     VARCHAR(100)      NOT NULL,
    event_type    VARCHAR(50)       NOT NULL,
    ts_utc        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                status        VARCHAR(20)       NOT NULL,
    error_message TEXT
    );

CREATE INDEX IF NOT EXISTS idx_tenant_audit_log_tenant
    ON tenant_audit_log (tenant_id);

CREATE INDEX IF NOT EXISTS idx_tenant_audit_log_event
    ON tenant_audit_log (event_type);