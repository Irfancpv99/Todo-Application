package com.todo.UnitTest;

import com.todo.config.DatabaseConfig;
import org.junit.jupiter.api.*;

import java.sql.Connection;
//import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @BeforeEach
    void setUp() {
        // Clean setup - no mocking needed
        DatabaseConfig.closePool();
    }

    @AfterEach
    void tearDown() {
        DatabaseConfig.closePool();
    }

    @Test
    @DisplayName("Database initialization should succeed")
    void testSuccessfulInitialization() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.initialize();
            try (var conn = DatabaseConfig.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
            }
        }, "Database initialization should not throw any exceptions");
    }

    @Test
    @DisplayName("Connection pool should reinitialize after closure")
    void testConnectionPoolReinitialization() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.initialize();
            DatabaseConfig.closePool();

            try (var conn = DatabaseConfig.getConnection()) {
                assertNotNull(conn, "Should get new connection after reinitialization");
                assertFalse(conn.isClosed(), "New connection should be open");
            }
        }, "Pool reinitialization should not throw any exceptions");
    }

    @Test
    @DisplayName("Multiple connections should be unique")
    void testMultipleConnectionRequests() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.initialize();
            
            try (var conn1 = DatabaseConfig.getConnection();
                 var conn2 = DatabaseConfig.getConnection()) {
                
                assertAll("Multiple connections validation",
                    () -> assertNotNull(conn1, "First connection should not be null"),
                    () -> assertNotNull(conn2, "Second connection should not be null"),
                    () -> assertFalse(conn1.isClosed(), "First connection should be open"),
                    () -> assertFalse(conn2.isClosed(), "Second connection should be open"),
                    () -> assertNotSame(conn1, conn2, "Connections should be different instances")
                );
            }
        });
    }

    @Test
    @DisplayName("Connection pool should handle concurrent connections")
    void testConcurrentConnections() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.initialize();
            var connections = new Connection[5];
            
            // Get multiple connections concurrently
            for (int i = 0; i < connections.length; i++) {
                connections[i] = DatabaseConfig.getConnection();
            }
            
            // Verify all connections
            for (var conn : connections) {
                assertNotNull(conn, "Connection should be valid");
                assertFalse(conn.isClosed(), "Connection should be open");
                conn.close();
            }
        }, "Should handle multiple concurrent connections");
    }

    @Test
    @DisplayName("Close pool should handle null data source")
    void testClosePoolWithNullDataSource() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.closePool();
            DatabaseConfig.closePool(); // Second call should not throw exception
        }, "Closing pool multiple times should not throw exception");
    }
}