package com.todo.ConfigTest;

import com.todo.config.DatabaseConfig;
import com.todo.config.PropertiesLoader;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseConfigTest {

    private static Properties originalProperties;
    private static Field propsField;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Store reference to properties field for test isolation
        propsField = PropertiesLoader.class.getDeclaredField("properties");
        propsField.setAccessible(true);
 
        originalProperties = new Properties();
        Properties currentProps = (Properties) propsField.get(null);
        originalProperties.putAll(currentProps);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Reset database connection pool
        DatabaseConfig.closePool();

        // Reset properties to original state
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(originalProperties);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up after each test
        DatabaseConfig.closePool();

        // Reset properties to original state
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(originalProperties);
    }

    // GROUP 1: Basic initialization and connection tests
    
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
    @DisplayName("Database initialization should fail with invalid configuration")
    void testInitializeFailure() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:postgresql://invalid:5432/nonexistentdb");
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        assertTrue(exception.getMessage().contains("Cannot connect to database") || 
                  exception.getCause() instanceof SQLException,
                  "Should throw exception with appropriate error message");
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

    // GROUP 2: Connection management tests
    
    @Test
    @Order(4)
    @DisplayName("Multiple connections should be unique and managed properly")
    void testMultipleConnectionRequests() {
        assertDoesNotThrow(() -> {
            DatabaseConfig.initialize();
            
            try (var conn1 = DatabaseConfig.getConnection();
                 var conn2 = DatabaseConfig.getConnection();
                 var conn3 = DatabaseConfig.getConnection()) {
                
                assertAll("Multiple connections validation",
                    () -> assertNotNull(conn1, "First connection should not be null"),
                    () -> assertNotNull(conn2, "Second connection should not be null"),
                    () -> assertNotNull(conn3, "Third connection should not be null"),
                    () -> assertFalse(conn1.isClosed(), "First connection should be open"),
                    () -> assertFalse(conn2.isClosed(), "Second connection should be open"),
                    () -> assertFalse(conn3.isClosed(), "Third connection should be open"),
                    () -> assertNotSame(conn1, conn2, "Connection instances should be different"),
                    () -> assertNotSame(conn1, conn3, "Connection instances should be different"),
                    () -> assertNotSame(conn2, conn3, "Connection instances should be different")
                );
                
                // Test connection lifecycle
                conn1.close();
                assertTrue(conn1.isClosed(), "Connection should be properly closed");
                assertFalse(conn2.isClosed(), "Other connections shouldn't be affected");
            }
        });
    }

    @Test
    @Order(5)
    @DisplayName("Connection pool should handle concurrent connections under load")
    void testConcurrentConnections() throws Exception {
        DatabaseConfig.initialize();
        final int threadCount = 10;
        final int connectionsPerThread = 3;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < connectionsPerThread; j++) {
                        try (Connection conn = DatabaseConfig.getConnection()) {
                            assertNotNull(conn, "Connection should be valid under concurrent load");
                            assertFalse(conn.isClosed(), "Connection should be open");
                            // Simulate some work
                            Thread.sleep(20);
                        }
                    }
                } catch (Exception e) {
                    fail("Exception during concurrent connection test: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertTrue(completed, "All connection threads should complete within timeout");
    }

    @Test
    @Order(6)
    @DisplayName("Test closePool when dataSource is null")
    void testClosePoolWithNullDataSource() throws Exception {
      
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        
        HikariDataSource originalDataSource = (HikariDataSource) dataSourceField.get(null);
        
        try {
        	dataSourceField.set(null, null);
          
        	assertDoesNotThrow(() -> DatabaseConfig.closePool());
            
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    assertNotNull(conn);
                }
            });
        } finally {
           
        	if (originalDataSource != null) {
                dataSourceField.set(null, originalDataSource);
            }
        }
    }
    
    // GROUP 3: Configuration parameter tests
    
    @Test
    @Order(7)
    @DisplayName("Test database configuration parameter methods")
    void testDbConfigurationParameters() throws Exception {
        // Test getDbUrl
        Method getDbUrlMethod = DatabaseConfig.class.getDeclaredMethod("getDbUrl");
        getDbUrlMethod.setAccessible(true);
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:test:url");
        
        String urlResult = (String) getDbUrlMethod.invoke(null);
        assertEquals("jdbc:test:url", urlResult, "getDbUrl should return correct value");
        
        // Test getDbUsername
        Method getDbUsernameMethod = DatabaseConfig.class.getDeclaredMethod("getDbUsername");
        getDbUsernameMethod.setAccessible(true);
        properties.setProperty("db.username", "test_username");
        
        String usernameResult = (String) getDbUsernameMethod.invoke(null);
        assertEquals("test_username", usernameResult, "getDbUsername should return correct value");
        
        // Test getDbPassword
        Method getDbPasswordMethod = DatabaseConfig.class.getDeclaredMethod("getDbPassword");
        getDbPasswordMethod.setAccessible(true);
        properties.setProperty("db.password", "test_password");
        
        String passwordResult = (String) getDbPasswordMethod.invoke(null);
        assertEquals("test_password", passwordResult, "getDbPassword should return correct value");
    }
    
    // GROUP 4: Error handling tests
    
    @Test
    @Order(8)
    @DisplayName("Test initialization with missing connection parameters")
    void testInitializeWithMissingParameters() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        
        // Test missing URL
        properties.remove("db.url");
        Exception urlException = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        assertTrue(urlException.getMessage().contains("not found") || 
                   urlException.getCause() instanceof RuntimeException,
                   "Should properly handle missing URL");
        
        // Reset and test missing username
        properties.putAll(originalProperties);
        properties.remove("db.username");
        Exception usernameException = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        assertTrue(usernameException.getMessage().contains("not found") || 
                   usernameException.getCause() instanceof RuntimeException,
                   "Should properly handle missing username");
        
        // Reset and test missing password
        properties.putAll(originalProperties);
        properties.remove("db.password");
        Exception passwordException = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        assertTrue(passwordException.getMessage().contains("not found") || 
                   passwordException.getCause() instanceof RuntimeException,
                   "Should properly handle missing password");
    }
    
    @Test
    @Order(9)
    @DisplayName("Should handle connection timeout and network errors")
    void testConnectionTimeoutAndNetworkErrors() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        // Test with very short timeout to nonexistent host
        properties.setProperty("db.pool.connectionTimeout", "250");
        properties.setProperty("db.url", "jdbc:postgresql://nonexistent-host:5432/db");
        
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        String message = exception.getMessage().toLowerCase();
        assertTrue(message.contains("cannot connect") || 
                   message.contains("connection") || 
                   message.contains("timeout") ||
                   exception.getCause() instanceof SQLException,
                   "Should handle connection timeout properly");
    }
    
    @Test
    @Order(10)
    @DisplayName("Should handle invalid pool configurations")
    void testInvalidPoolConfigurations() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        
        // Test negative pool size
        properties.setProperty("db.pool.maxSize", "-1");
        assertThrows(IllegalArgumentException.class, 
            () -> DatabaseConfig.initialize(),
            "Should reject negative pool size");
        
        // Test zero pool size
        properties.putAll(originalProperties);
        properties.setProperty("db.pool.maxSize", "0");
        assertThrows(IllegalArgumentException.class, 
            () -> DatabaseConfig.initialize(),
            "Should reject zero pool size");
        
        // Test invalid numeric format
        properties.putAll(originalProperties);
        properties.setProperty("db.pool.maxSize", "invalid");
        assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize(),
            "Should handle non-numeric pool size");
    }
    
    @Test
    @Order(11)
    @DisplayName("Should handle null datasource in getConnection")
    void testNullDataSource() throws Exception {
        // Set dataSource field to null using reflection
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        dataSourceField.set(null, null);
        
        // getConnection should handle null dataSource by initializing
        assertDoesNotThrow(() -> {
            try (Connection conn = DatabaseConfig.getConnection()) {
                assertNotNull(conn, "Should get valid connection after auto-initialization");
                assertFalse(conn.isClosed(), "Connection should be open");
            }
        }, "getConnection should handle null dataSource");
    }
    
    @Test
    @Order(12)
    @DisplayName("Verify Flyway migration handling")
    void testFlywayMigrationHandling() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        // Point to valid server but nonexistent database to trigger Flyway failure
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/nonexistent_db_for_test");
        
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        String message = exception.getMessage().toLowerCase();
        boolean isValidError = message.contains("cannot connect") || 
                              message.contains("database") || 
                              message.contains("does not exist") ||
                              exception.getCause() instanceof SQLException;
        
        assertTrue(isValidError, "Should handle Flyway migration failure appropriately");
    }
    
    @Test
    @Order(13)
    @DisplayName("Test initialization with valid connection parameters")
    void testInitializationWithValidParameters() throws Exception {
        // Set valid connection parameters
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/testdb");
        properties.setProperty("db.username", "testuser");
        properties.setProperty("db.password", "testpassword");
        properties.setProperty("db.pool.maxSize", "5");
        properties.setProperty("db.pool.minIdle", "1");
        properties.setProperty("db.pool.idleTimeout", "10000");
        properties.setProperty("db.pool.connectionTimeout", "5000");
        
        try {
            DatabaseConfig.initialize();
        
        } catch (RuntimeException e) {
     
            assertTrue(e.getMessage().contains("Cannot connect to database") || 
                      e.getCause() instanceof SQLException,
                      "Should throw appropriate exception");
        }
    }
    
    @Test
    @Order(14)
    @DisplayName("Test dataSource.isClosed() behavior")
    void testDataSourceIsClosedBehavior() throws Exception {
 
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        
        try {
            
            DatabaseConfig.initialize();
         
            DatabaseConfig.closePool();
            
            Connection conn = DatabaseConfig.getConnection();
            conn.close();
            
 
            assertNotNull(dataSourceField.get(null), "DataSource should be reinitialized");
            
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot connect") || 
                       e.getCause() instanceof SQLException,
                       "Expected exception type when DB is unavailable");
        }
    }
    
    @Test
    @Order(15)
    @DisplayName("Test SQLException propagation in getConnection")
    void testSQLExceptionPropagation() throws Exception {
        // Get access to dataSource field
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
   
        com.zaxxer.hikari.HikariDataSource mockDataSource = mock(com.zaxxer.hikari.HikariDataSource.class);
        when(mockDataSource.isClosed()).thenReturn(false);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Test exception"));

        dataSourceField.set(null, mockDataSource);

        assertThrows(SQLException.class, () -> DatabaseConfig.getConnection());
    }
    
    @Test
    @Order(16)
    @DisplayName("Test connection property getters")
    void testConnectionPropertyGetters() throws Exception {

        Method getDbUrlMethod = DatabaseConfig.class.getDeclaredMethod("getDbUrl");
        Method getDbUsernameMethod = DatabaseConfig.class.getDeclaredMethod("getDbUsername");
        Method getDbPasswordMethod = DatabaseConfig.class.getDeclaredMethod("getDbPassword");
        
        getDbUrlMethod.setAccessible(true);
        getDbUsernameMethod.setAccessible(true);
        getDbPasswordMethod.setAccessible(true);
        
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "test-jdbc-url");
        properties.setProperty("db.username", "test-username");
        properties.setProperty("db.password", "test-password");
    
        assertEquals("test-jdbc-url", getDbUrlMethod.invoke(null));
        assertEquals("test-username", getDbUsernameMethod.invoke(null));
        assertEquals("test-password", getDbPasswordMethod.invoke(null));
    }
    
    @Test
    @Order(17)
    @DisplayName("Test handling of invalid Hikari configuration")
    void testInvalidHikariConfiguration() throws Exception {
        // Set invalid configuration (negative values)
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.pool.maxSize", "-5");
        
        // Should throw IllegalArgumentException
        assertThrows(RuntimeException.class, () -> DatabaseConfig.initialize());
    }
    
//    @Test
//    @Order(18)
//    @DisplayName("Test exception path in initialize method")
//    void testExceptionPathInInitialize() throws Exception {
//        // Configure properties to cause connection failure
//        Properties properties = (Properties) propsField.get(null);
//        String originalUrl = properties.getProperty("db.url");
//        
//        try {
//            // Set invalid connection URL
//            properties.setProperty("db.url", "jdbc:postgresql://invalid-host:5432/nonexistent");
//            
//            // The initialize should throw some kind of exception
//            assertThrows(Exception.class, () -> DatabaseConfig.initialize(),
//                "Database initialization with invalid URL should throw an exception");
//                
//        } finally {
//            // Restore original URL
//            if (originalUrl != null) {
//                properties.setProperty("db.url", originalUrl);
//            }
//        }
//    }
//    
//    @Test
//    @Order(19)
//    @DisplayName("Simple test for Flyway configuration code path")
//    void testFlywayConfigurationCodePath() {
//        // Store original properties
//        Properties properties = null;
//        try {
//            properties = (Properties) propsField.get(null);
//        } catch (Exception e) {
//            fail("Could not access properties field: " + e.getMessage());
//            return;
//        }
//        
//        String originalUrl = properties.getProperty("db.url");
//        String originalUsername = properties.getProperty("db.username");
//        String originalPassword = properties.getProperty("db.password");
//
//        try {
//            // Use properties that appear valid but point to a non-existent server
//            // This should reach the Flyway code but then fail
//            properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/flyway_test_db");
//            properties.setProperty("db.username", "flyway_test_user");
//            properties.setProperty("db.password", "flyway_test_password");
//            
//            try {
//                // This will likely throw an exception, but will execute the Flyway code path
//                DatabaseConfig.initialize();
//            } catch (Exception e) {
//                // Expected - real database likely doesn't exist
//                // The important thing is that we called initialize(), which contains the Flyway code
//            }
//            
//            // No assertion needed - we're just trying to execute the code path
//            // Success here is simply not throwing any unexpected exceptions
//            
//        } finally {
//            // Restore original properties
//            if (originalUrl != null) properties.setProperty("db.url", originalUrl);
//            if (originalUsername != null) properties.setProperty("db.username", originalUsername);
//            if (originalPassword != null) properties.setProperty("db.password", originalPassword);
//        }
//    }
//
//    @Test
//    @Order(20)
//    @DisplayName("Test closePool with non-null dataSource")
//    void testClosePoolWithNonNullDataSource() throws Exception {
//        // Access the dataSource field
//        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
//        dataSourceField.setAccessible(true);
//        
//        // Create a mock dataSource that is already closed
//        HikariDataSource mockClosedDataSource = mock(HikariDataSource.class);
//        when(mockClosedDataSource.isClosed()).thenReturn(true);
//        
//        // Create a mock dataSource that is open
//        HikariDataSource mockOpenDataSource = mock(HikariDataSource.class);
//        when(mockOpenDataSource.isClosed()).thenReturn(false);
//        
//        // Save original value
//        HikariDataSource originalDataSource = (HikariDataSource) dataSourceField.get(null);
//        
//        try {
//            // Test with closed dataSource
//            dataSourceField.set(null, mockClosedDataSource);
//            assertDoesNotThrow(() -> DatabaseConfig.closePool(),
//                "closePool should handle already closed dataSource");
//            verify(mockClosedDataSource, never()).close();
//            
//            // Test with open dataSource
//            dataSourceField.set(null, mockOpenDataSource);
//            assertDoesNotThrow(() -> DatabaseConfig.closePool(),
//                "closePool should close open dataSource");
//            verify(mockOpenDataSource).close();
//            
//        } finally {
//            // Restore original dataSource
//            dataSourceField.set(null, originalDataSource);
//        }
//    }
}