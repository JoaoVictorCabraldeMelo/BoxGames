package org.ada.com.adapters.out.persistence.h2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionProvider {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public ConnectionProvider(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to open database connection.", ex);
        }
    }
}

