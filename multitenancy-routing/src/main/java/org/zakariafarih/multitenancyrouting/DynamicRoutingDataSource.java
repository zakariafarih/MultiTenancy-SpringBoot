package org.zakariafarih.multitenancyrouting;

import org.springframework.jdbc.datasource.AbstractDataSource;
import org.zakariafarih.multitenancycore.TenantContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Wrapper;

/**
 * Delegates every call to the tenant-specific DataSource looked-up *lazily*
 * via {@link TenantDataSourceManager}.  Supports {@link Wrapper} contract,
 * enabling Hibernate & JdbcTemplate to unwrap to the vendor class.
 */
public class DynamicRoutingDataSource extends AbstractDataSource implements Wrapper {

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

    /* ---------- Wrapper contract ---------- */

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        DataSource delegate = lookup();
        if (delegate instanceof Wrapper w && w.isWrapperFor(iface)) {
            return w.unwrap(iface);
        }
        throw new SQLException("Cannot unwrap " + delegate + " to " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) ||
                (lookup() instanceof Wrapper w && w.isWrapperFor(iface));
    }

    /* ---------- internal ---------- */

    private DataSource lookup() {
        return dsManager.get(TenantContext.getRequired());
    }
}
