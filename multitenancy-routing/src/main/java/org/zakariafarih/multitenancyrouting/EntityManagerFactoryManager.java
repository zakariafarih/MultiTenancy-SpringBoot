package org.zakariafarih.multitenancyrouting;

import jakarta.persistence.EntityManagerFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.zakariafarih.multitenancycore.EMFProvider;

import javax.sql.DataSource;

@RequiredArgsConstructor
public class EntityManagerFactoryManager {

    private final EMFProvider emfProvider;
    private final @NonNull String[] packagesToScan;

    public EntityManagerFactory get(DataSource ds, String tenantId, String schema) {
        return emfProvider.get(ds, tenantId, schema, packagesToScan);
    }
}
