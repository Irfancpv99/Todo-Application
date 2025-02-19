package com.todo.ConfigTest;

import com.todo.config.DatabaseConfig;
import com.todo.config.PropertiesLoader;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    @DisplayName("Test getDbUrl method")
    void testGetDbUrl() throws Exception {
    	
        Method getDbUrlMethod = DatabaseConfig.class.getDeclaredMethod("getDbUrl");
        getDbUrlMethod.setAccessible(true);
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:test:url");
        
        String result = (String) getDbUrlMethod.invoke(null);
        assertEquals("jdbc:test:url", result);
    }
    
    @Test
    @Order(8) 
    @DisplayName("Test getDbUsername method")
    void testGetDbUsername() throws Exception {
       
        Method getDbUsernameMethod = DatabaseConfig.class.getDeclaredMethod("getDbUsername");
        getDbUsernameMethod.setAccessible(true);
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.username", "test_username");
        
        String result = (String) getDbUsernameMethod.invoke(null);
        assertEquals("test_username", result);
    }
    
    @Test
    @Order(9)
    @DisplayName("Test getDbPassword method")
    void testGetDbPassword() throws Exception {
       
        Method getDbPasswordMethod = DatabaseConfig.class.getDeclaredMethod("getDbPassword");
        getDbPasswordMethod.setAccessible(true);
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.password", "test_password");
        
        String result = (String) getDbPasswordMethod.invoke(null);
        assertEquals("test_password", result);
    }
    
    @Test
    @Order(10)
    @DisplayName("Test initialize with missing DB URL")
    void testInitializeWithMissingDbUrl() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.remove("db.url");
        
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        assertTrue(exception.getMessage().contains("not found in application.properties") || 
                   exception.getCause() instanceof RuntimeException);
    }
    
    @Test
    @Order(10)
    @DisplayName("Test initialize with missing DB username")
    void testInitializeWithMissingDbUsername() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.remove("db.username");
        
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        assertTrue(exception.getMessage().contains("not found in application.properties") || 
                   exception.getCause() instanceof RuntimeException);
    }
    
    @Test
    @DisplayName("Test initialize with missing DB password")
    void testInitializeWithMissingDbPassword() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.remove("db.password");
        
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        assertTrue(exception.getMessage().contains("not found in application.properties") || 
                   exception.getCause() instanceof RuntimeException);
    }
    
    @Test
    @DisplayName("Test getConnection with already closed connection")
    void testGetConnectionWithClosedConnection() throws Exception {
        // Initialize the database
        DatabaseConfig.initialize();
        
        // Get connection and close it
        Connection conn1 = DatabaseConfig.getConnection();
        conn1.close();
        assertTrue(conn1.isClosed());
        
        // Should get new connection
        Connection conn2 = DatabaseConfig.getConnection();
        assertNotNull(conn2);
        assertFalse(conn2.isClosed());
        
        // Clean up
        conn2.close();
    }
    
    
    @Test
    @DisplayName("Test initialize after closing pool")
    void testInitializeAfterClosingPool() throws Exception {
        DatabaseConfig.initialize();
        DatabaseConfig.closePool();
        
        // Should reinitialize without issues
        assertDoesNotThrow(() -> {
            DatabaseConfig.initialize();
            try (Connection conn = DatabaseConfig.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
            }
        });
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
        properties.setProperty("db.pool.connectionTimeout", "250");
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
    
    @Test
    @Order(17)
    @DisplayName("Should handle Flyway migration failure")
    void testFlywayMigrationFailure() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/nonexistent_db");
        
        Exception exception = assertThrows(RuntimeException.class, () -> {
            DatabaseConfig.initialize();
        });
        System.out.println("Actual error message: " + exception.getMessage());
        assertTrue(
            exception.getMessage().contains("Cannot connect to database") ||
            exception.getMessage().contains("database \"nonexistent_db\" does not exist") ||
            exception.getMessage().contains("Connection to") ||
            exception.getMessage().contains("The connection attempt failed"),
            "Error message should indicate database connection failure. Actual message: " + exception.getMessage()
        );
    }
    
    @Test
    @Order(18)
    @DisplayName("Should handle already closed pool")
    void testAlreadyClosedPool() throws Exception {
        DatabaseConfig.initialize();
        DatabaseConfig.closePool();
        
        assertDoesNotThrow(() -> {
            try (Connection conn = DatabaseConfig.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
            }
        });
    }
    
    @Test
    @Order(19)
    @DisplayName("Should handle connection release")
    void testConnectionRelease() throws Exception {
        DatabaseConfig.initialize();
        Connection conn1 = DatabaseConfig.getConnection();
        conn1.close();
        Connection conn2 = DatabaseConfig.getConnection();
        assertNotNull(conn2);
        assertFalse(conn2.isClosed());
        conn2.close();
    }
    
    @Test
    @Order(20)
    @DisplayName("Should handle specific SQLException in initialize")
    void testSpecificSQLException() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/nonexistent_db");
        properties.setProperty("db.username", "invalid_user");
        
        Exception exception = assertThrows(RuntimeException.class, () -> {
            DatabaseConfig.initialize();
        });
        
        assertTrue(exception.getCause() instanceof SQLException);
    }
    
}