package org.zakariafarih.multitenancyrouting;

import org.springframework.jdbc.datasource.AbstractDataSource;
import org.zakariafarih.multitenancycore.TenantContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Delegates every call to the DataSource that belongs to the tenant
 * currently stored in {@link TenantContext}.  Because the delegate is
 * resolved on every call, new tenants added after boot are supported
 * without rebuilding bean definitions.
 */
public class DynamicRoutingDataSource extends AbstractDataSource {

    private final TenantDataSourceManager dsManager;

    public DynamicRoutingDataSource(TenantDataSourceManager dsManager) {
        this.dsManager = dsManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return lookup().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return lookup().getConnection(username, password);
    }

    private DataSource lookup() {
        return dsManager.get(TenantContext.getRequired());
    }
}
