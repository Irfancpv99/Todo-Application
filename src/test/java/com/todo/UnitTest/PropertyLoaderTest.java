package com.todo.UnitTest;

import com.todo.config.PropertiesLoader;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PropertiesLoaderTest {
    
    @BeforeEach
    void setUp() {
        // Set known environment variables for testing
        System.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/todo_db");
        System.setProperty("DB_USER", "postgres");
        System.setProperty("DB_PASSWORD", "postgres");
    }

    @AfterEach
    void tearDown() {
        // Clear test environment variables
        System.clearProperty("DB_URL");
        System.clearProperty("DB_USER");
        System.clearProperty("DB_PASSWORD");
    }

    @Test
    @DisplayName("Should get database URL property")
    void testGetDatabaseUrl() {
        String dbUrl = PropertiesLoader.getProperty("db.url");
        assertNotNull(dbUrl, "Database URL should not be null");
        assertTrue(dbUrl.contains("postgresql"), "Database URL should be a PostgreSQL URL");
    }

    @Test
    @DisplayName("Should get database username property")
    void testGetDatabaseUsername() {
        String username = PropertiesLoader.getProperty("db.username");
        assertNotNull(username, "Database username should not be null");
        assertFalse(username.isEmpty(), "Database username should not be empty");
    }

    @Test
    @DisplayName("Should get database password property")
    void testGetDatabasePassword() {
        String password = PropertiesLoader.getProperty("db.password");
        assertNotNull(password, "Database password should not be null");
        assertFalse(password.isEmpty(), "Database password should not be empty");
    }

    @Test
    @DisplayName("Should get integer property with default value")
    void testGetIntegerProperty() {
        int maxSize = PropertiesLoader.getIntProperty("db.pool.maxSize", 10);
        int minIdle = PropertiesLoader.getIntProperty("db.pool.minIdle", 2);
        int idleTimeout = PropertiesLoader.getIntProperty("db.pool.idleTimeout", 300000);
        
        assertAll("Pool configuration properties",
            () -> assertTrue(maxSize > 0, "Max pool size should be positive"),
            () -> assertTrue(minIdle > 0, "Min idle connections should be positive"),
            () -> assertTrue(idleTimeout > 0, "Idle timeout should be positive")
        );
    }

    @Test
    @DisplayName("Should handle missing properties with default values")
    void testMissingPropertiesWithDefaults() {
        String defaultValue = "default";
        String value = PropertiesLoader.getProperty("non.existent.property", defaultValue);
        assertEquals(defaultValue, value, "Should return default value for missing property");
    }

    @Test
    @DisplayName("Should throw exception for missing required properties")
    void testMissingRequiredProperties() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            PropertiesLoader.getProperty("non.existent.property");
        });
        
        assertTrue(exception.getMessage().contains("not found"),
            "Exception message should indicate property not found");
    }

    @Test
    @DisplayName("Should print properties without exception")
    void testPrintProperties() {
        assertDoesNotThrow(() -> {
            PropertiesLoader.printProperties();
        }, "Printing properties should not throw exception");
    }

    @Test
    @DisplayName("Should handle invalid integer properties")
    void testInvalidIntegerProperty() {
        int defaultValue = 42;
        int value = PropertiesLoader.getIntProperty("non.existent.int", defaultValue);
        assertEquals(defaultValue, value, "Should return default value for missing integer property");
    }
}