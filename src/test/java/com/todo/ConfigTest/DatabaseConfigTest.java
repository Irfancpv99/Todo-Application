package com.todo.ConfigTest;

import com.todo.config.DatabaseConfig;
import com.todo.config.PropertiesLoader;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
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
 
        originalProperties = new Properties();
        Properties currentProps = (Properties) propsField.get(null);
        originalProperties.putAll(currentProps);
    }

    @BeforeEach
    void setUp() throws Exception {
  
        DatabaseConfig.closePool();

        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(originalProperties);
    }

    @AfterEach
    void tearDown() throws Exception {
        DatabaseConfig.closePool();

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
            DatabaseConfig.closePool();
        }, "Closing pool multiple times should not throw exception");
    }
    
    @Test 
    @Order(7)
    @DisplayName("Should handle database URL retrieval")
    void testGetDbUrl() {
        String properties = PropertiesLoader.getProperty("db.url", "jdbc:postgresql://localhost:5432/testdb");
        assertNotNull(properties);
    }
    
    @Test
    @Order(8) 
    @DisplayName("Should handle database username retrieval")
    void testGetDbUsername() {
        String properties = PropertiesLoader.getProperty("db.username", "test_user");
        assertNotNull(properties);
    }
    
    @Test
    @Order(9)
    @DisplayName("Should handle database password retrieval")
    void testGetDbPassword() {
        String properties = PropertiesLoader.getProperty("db.password", "test_pass");
        assertNotNull(properties);
    }
    
    @Test
    @Order(10)
    @DisplayName("Should handle connection close")
    void testConnectionClose() throws SQLException {
        DatabaseConfig.initialize();
        Connection conn = DatabaseConfig.getConnection();
        conn.close();
        assertTrue(conn.isClosed());
    }
    
    @Test
    @Order(11)
    @DisplayName("Should handle initialization with invalid pool settings")
    void testInvalidPoolSettings() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.pool.maxSize", "invalid");
        
        assertThrows(RuntimeException.class, () -> DatabaseConfig.initialize());
    }

    @Test
    @Order(12)
    @DisplayName("Should handle connection timeout")
    void testConnectionTimeout() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.pool.connectionTimeout", "250"); // Minimum allowed timeout
        properties.setProperty("db.url", "jdbc:postgresql://invalid:5432/db");
        
        assertThrows(RuntimeException.class, () -> DatabaseConfig.initialize());
    }
    
    @Test
    @Order(13)
    @DisplayName("Should handle database initialization with invalid pool max size")
    void testInvalidPoolMaxSize() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.pool.maxSize", "-1");
        
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.initialize());
    }
    
    @Test
    @Order(14)
    @DisplayName("Should handle null datasource in getConnection")
    void testNullDataSource() throws Exception {
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        dataSourceField.set(null, null);
        
        assertDoesNotThrow(() -> DatabaseConfig.getConnection());
    }
    
    @Test
    @Order(15)
    @DisplayName("JOptionPane shown on connection failure")
    void testJOptionPaneOnConnectionFailure() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:postgresql://invalid:5432/nonexistentdb");
        
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize()
        );
        
        String actualMessage = exception.getMessage();
        assertTrue(
            actualMessage.contains("UnknownHostException") || 
            actualMessage.contains("The connection attempt failed") ||
            actualMessage.contains("Connection to") ||
            actualMessage.contains("Cannot connect to database"),
            "Exception message should indicate connection failure. Actual message: " + actualMessage
        );
    }
    
    @Test
    @Order(16)
    @DisplayName("Invalid Hikari parameters throw exceptions")
    void testInvalidHikariParameters() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.pool.maxSize", "0");
        
        assertThrows(IllegalArgumentException.class, DatabaseConfig::initialize);
    }
}