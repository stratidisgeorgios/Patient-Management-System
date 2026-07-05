package com.patientsystem.treatmentservice.multitenancy;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class MultitenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    public MultitenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection(String tenantId) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.createStatement().execute("SET search_path TO " + tenantId);
        return connection;
    }

    @Override
    public void releaseConnection(String tenantId, Connection connection) throws SQLException {
        connection.createStatement().execute("SET search_path TO public");
        connection.close();
    }

    @Override public Connection getAnyConnection() throws SQLException { return dataSource.getConnection(); }
    @Override public void releaseAnyConnection(Connection connection) throws SQLException { connection.close(); }
    @Override public boolean supportsAggressiveRelease() { return false; }
    @Override public boolean isUnwrappableAs(Class<?> unwrapType) { return false; }
    @Override public <T> T unwrap(Class<T> unwrapType) { return null; }
}
