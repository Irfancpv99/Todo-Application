package com.todo.UnitTest;

import com.todo.config.DatabaseConfig;
import com.todo.config.PropertiesLoader;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseConfigTest {

    private static Properties originalProperties;
    private static Field propsField;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Store reference to properties field
        propsField = PropertiesLoader.class.getDeclaredField("properties");
        propsField.setAccessible(true);
        
        // Store original properties
        originalProperties = new Properties();
        Properties currentProps = (Properties) propsField.get(null);
        originalProperties.putAll(currentProps);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Reset database connection
        DatabaseConfig.closePool();
        
        // Reset properties to original state before each test
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(originalProperties);
    }

    @AfterEach
    void tearDown() throws Exception {
        DatabaseConfig.closePool();
        
        // Restore original properties
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(originalProperties);
    }

    @Test
    @Order(1)
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
    @Order(2)
    @DisplayName("Database Initialization failed")
    void testInitializeFailure() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:postgresql://invalid:5432/nonexistentdb");
        
        assertThrows(RuntimeException.class, () -> DatabaseConfig.initialize());
    }

    @Test
    @Order(3)
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
    @Order(4)
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
    @Order(5)
    @DisplayName("Connection pool should handle concurrent connections")
    void testConcurrentConnections() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.initialize();
            var connections = new Connection[5];
            
            try {
                for (int i = 0; i < connections.length; i++) {
                    connections[i] = DatabaseConfig.getConnection();
                }
                
                for (var conn : connections) {
                    assertNotNull(conn, "Connection should be valid");
                    assertFalse(conn.isClosed(), "Connection should be open");
                }
            } finally {
                // Ensure all connections are closed
                for (var conn : connections) {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                }
            }
        }, "Should handle multiple concurrent connections");
    }

    @Test
    @Order(6)
    @DisplayName("Close pool should handle null data source")
    void testClosePoolWithNullDataSource() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.closePool();
            DatabaseConfig.closePool(); // Second call should not throw exception
        }, "Closing pool multiple times should not throw exception");
    }
}