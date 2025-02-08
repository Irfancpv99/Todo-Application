package com.todo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    private static String getDbUrl() {
        return PropertiesLoader.getProperty("db.url");
    }

    private static String getDbUsername() {
        return PropertiesLoader.getProperty("db.username");
    }

    private static String getDbPassword() {
        return PropertiesLoader.getProperty("db.password");
    }

    public static void initialize() {
        try {
            HikariConfig config = new HikariConfig();
            
            // Use the new methods to get connection details
            config.setJdbcUrl(getDbUrl());
            config.setUsername(getDbUsername());
            config.setPassword(getDbPassword());
            
            config.setMaximumPoolSize(PropertiesLoader.getIntProperty("db.pool.maxSize", 10));
            config.setMinimumIdle(PropertiesLoader.getIntProperty("db.pool.minIdle", 2));
            config.setIdleTimeout(PropertiesLoader.getIntProperty("db.pool.idleTimeout", 300000));
            config.setConnectionTimeout(PropertiesLoader.getIntProperty("db.pool.connectionTimeout", 30000));
            config.setInitializationFailTimeout(PropertiesLoader.getIntProperty("db.pool.initializationFailTimeout", 1));

            dataSource = new HikariDataSource(config);
            
            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("Database connection successful");
                
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .validateMigrationNaming(true)
                    .load();
                flyway.migrate();
            }
        } catch (SQLException e) {
            String message = "Cannot connect to database. Please ensure PostgreSQL is running.\nError: " + e.getMessage();
            JOptionPane.showMessageDialog(null, message, "Database Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(message, e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}