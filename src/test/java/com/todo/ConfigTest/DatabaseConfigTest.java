package com.todo.ConfigTest;

import com.todo.config.DatabaseConfig;
import com.todo.config.PropertiesLoader;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;


import java.lang.reflect.Constructor;
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
        Method getDbUrlMethod = DatabaseConfig.class.getDeclaredMethod("getDbUrl");
        Method getDbUsernameMethod = DatabaseConfig.class.getDeclaredMethod("getDbUsername");
        Method getDbPasswordMethod = DatabaseConfig.class.getDeclaredMethod("getDbPassword");
        
        getDbUrlMethod.setAccessible(true);
        getDbUsernameMethod.setAccessible(true);
        getDbPasswordMethod.setAccessible(true);
        
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:test:url");
        properties.setProperty("db.username", "test_username");
        properties.setProperty("db.password", "test_password");
        
        assertEquals("jdbc:test:url", getDbUrlMethod.invoke(null), "getDbUrl should return correct value");
        assertEquals("test_username", getDbUsernameMethod.invoke(null), "getDbUsername should return correct value");
        assertEquals("test_password", getDbPasswordMethod.invoke(null), "getDbPassword should return correct value");
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
        properties.setProperty("db.pool.connectionTimeout", "250");
        properties.setProperty("db.url", "jdbc:postgresql://nonexistent-host:5432/db");
        
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        assertTrue(exception.getMessage().contains("Cannot connect to database") ||
                  exception.getCause() instanceof SQLException,
                  "Should handle connection timeout appropriately");
    }

    @Test
    @Order(10)
    @DisplayName("Test initialize with missing DB URL")
    void testInitializeWithMissingDbUrl() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.remove("db.url");

        Exception exception = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.initialize());

        String errorMessage = exception.getMessage();
        if (exception.getCause() != null) {
            errorMessage = exception.getCause().getMessage();
        }
        
        assertTrue(
            errorMessage.contains("db.url") || 
            errorMessage.contains("not found") || 
            errorMessage.contains("Cannot connect to database"),
            "Should fail with appropriate error message. Got: " + errorMessage
        );

        assertTrue(
            exception instanceof RuntimeException || 
            (exception.getCause() != null && exception.getCause() instanceof RuntimeException),
            "Should throw RuntimeException for missing configuration"
        );
    }

    @Test
    @Order(11)
    @DisplayName("Should handle null datasource in getConnection")
    void testNullDataSource() throws Exception {
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        dataSourceField.set(null, null);
        
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
    @DisplayName("Test SQLException propagation in getConnection")
    void testSQLExceptionPropagation() throws Exception {
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
   
        HikariDataSource mockDataSource = mock(HikariDataSource.class);
        when(mockDataSource.isClosed()).thenReturn(false);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Test exception"));

        dataSourceField.set(null, mockDataSource);
        assertThrows(SQLException.class, () -> DatabaseConfig.getConnection());
    }
    
    @Test
    @Order(14)
    @DisplayName("Test handling of invalid Hikari configuration")
    void testInvalidHikariConfiguration() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.pool.maxSize", "-5");
        
        assertThrows(RuntimeException.class, () -> DatabaseConfig.initialize(),
            "Should handle invalid Hikari configuration appropriately");
    }
    
    @Test
    @Order(15)
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
    @DisplayName("Test database configuration getters")
    void testDbConfigGetters() throws Exception {
        // Access the private methods using reflection
        Method getDbUrlMethod = DatabaseConfig.class.getDeclaredMethod("getDbUrl");
        Method getDbUsernameMethod = DatabaseConfig.class.getDeclaredMethod("getDbUsername");
        Method getDbPasswordMethod = DatabaseConfig.class.getDeclaredMethod("getDbPassword");
        
        getDbUrlMethod.setAccessible(true);
        getDbUsernameMethod.setAccessible(true);
        getDbPasswordMethod.setAccessible(true);
        
        // Set the properties for testing
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "test_url");
        properties.setProperty("db.username", "test_user");
        properties.setProperty("db.password", "test_pass");
        
        // Execute the methods and verify results
        assertEquals("test_url", getDbUrlMethod.invoke(null), "getDbUrl should return correct value");
        assertEquals("test_user", getDbUsernameMethod.invoke(null), "getDbUsername should return correct value");
        assertEquals("test_pass", getDbPasswordMethod.invoke(null), "getDbPassword should return correct value");
    }
    
    // Test closed datasource in getConnection
    @Test
    @DisplayName("Test getConnection with closed dataSource")
    void testGetConnectionWithClosedDataSource() throws Exception {
        // Initialize first
        DatabaseConfig.initialize();
        
        // Get the dataSource field
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        
        // Create a mock dataSource
        HikariDataSource mockDataSource = mock(HikariDataSource.class);
        when(mockDataSource.isClosed()).thenReturn(true);
        when(mockDataSource.getConnection()).thenReturn(mock(Connection.class));
        
        // Save original and replace with mock
        HikariDataSource originalDataSource = (HikariDataSource) dataSourceField.get(null);
        dataSourceField.set(null, mockDataSource);
        
        try {
            // This should reinitialize since the mock datasource is "closed"
            Connection conn = DatabaseConfig.getConnection();
            assertNotNull(conn, "Should get a connection");
            
            // Verify isClosed was called
            verify(mockDataSource).isClosed();
        } finally {
            // Restore original
            dataSourceField.set(null, originalDataSource);
        }
    }
    
    // Test with null dataSource
    @Test
    @DisplayName("Test getConnection with null dataSource")
    void testGetConnectionWithNullDataSource() throws Exception {
        // Get the dataSource field
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        
        // Save original and set to null
        HikariDataSource originalDataSource = (HikariDataSource) dataSourceField.get(null);
        dataSourceField.set(null, null);
        
        try {
            // This should initialize since dataSource is null
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    assertNotNull(conn, "Should get a connection after initialization");
                }
            });
        } finally {
            // Close any new pool created
            DatabaseConfig.closePool();
            
            // Restore original
            dataSourceField.set(null, originalDataSource);
        }
    }
    
    // Test all branches of closePool
    @Test
    @DisplayName("Test all branches of closePool")
    void testAllClosePoolBranches() throws Exception {
        // Test with null dataSource
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        
        // Save original
        HikariDataSource originalDataSource = (HikariDataSource) dataSourceField.get(null);
        
        // Test null datasource
        dataSourceField.set(null, null);
        assertDoesNotThrow(() -> DatabaseConfig.closePool(), 
            "closePool should handle null dataSource");
        
        // Test closed datasource
        HikariDataSource mockClosedDataSource = mock(HikariDataSource.class);
        when(mockClosedDataSource.isClosed()).thenReturn(true);
        dataSourceField.set(null, mockClosedDataSource);
        
        assertDoesNotThrow(() -> DatabaseConfig.closePool(), 
            "closePool should handle already closed dataSource");
        verify(mockClosedDataSource).isClosed();
        verify(mockClosedDataSource, never()).close();
        
        // Test open datasource
        HikariDataSource mockOpenDataSource = mock(HikariDataSource.class);
        when(mockOpenDataSource.isClosed()).thenReturn(false);
        dataSourceField.set(null, mockOpenDataSource);
        
        assertDoesNotThrow(() -> DatabaseConfig.closePool(), 
            "closePool should handle open dataSource");
        verify(mockOpenDataSource).isClosed();
        verify(mockOpenDataSource).close();
        
        // Restore original
        dataSourceField.set(null, originalDataSource);
    }
    
    // Test database initialization failure
    @Test
    @DisplayName("Test database initialization failure")
    void testDatabaseInitializationFailure() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("db.url", "jdbc:postgresql://invalid:5432/nonexistentdb");
        
        // This should throw an exception
        Exception exception = assertThrows(RuntimeException.class, 
            () -> DatabaseConfig.initialize());
        
        // Just assert that we got an exception with some message
        assertNotNull(exception.getMessage(), "Exception should have a message");
    }
    
    // Test SQLException in getConnection
    @Test
    @DisplayName("Test SQLException in getConnection")
    void testSQLExceptionInGetConnection() throws Exception {
        // Get the dataSource field
        Field dataSourceField = DatabaseConfig.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        
        // Create a mock dataSource that throws SQLException
        HikariDataSource mockDataSource = mock(HikariDataSource.class);
        when(mockDataSource.isClosed()).thenReturn(false);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Test exception"));
        
        // Save original and replace with mock
        HikariDataSource originalDataSource = (HikariDataSource) dataSourceField.get(null);
        dataSourceField.set(null, mockDataSource);
        
        try {
            // This should throw SQLException
            assertThrows(SQLException.class, 
                () -> DatabaseConfig.getConnection());
            
            // Verify getConnection was called
            verify(mockDataSource).getConnection();
        } finally {
            // Restore original
            dataSourceField.set(null, originalDataSource);
        }
    }
    
 
    @Test
    @DisplayName("Test private constructor for coverage")
    void testPrivateConstructor() throws Exception {
        Constructor<DatabaseConfig> constructor = DatabaseConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        DatabaseConfig instance = constructor.newInstance();
        assertNotNull(instance, "Should create instance");
    }
}