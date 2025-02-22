package com.todo.ConfigTest;

import java.sql.Connection;
import java.sql.SQLException;

import com.todo.config.DatabaseConfig;

public class TestDatabaseConfig {
    private static boolean useRealDatabase = Boolean.parseBoolean(
        System.getProperty("useRealDatabase", "false"));
    
    public static Connection getConnection() throws SQLException {
        if (useRealDatabase) {
            return DatabaseConfig.getConnection();
        }
        
        // Return mock connection for tests
        throw new SQLException("Test exception");
    }
    
    public static void initialize() {
        // Do nothing for unit tests
    }
    
    public static void closePool() {
        // Do nothing for unit tests  
    }
}