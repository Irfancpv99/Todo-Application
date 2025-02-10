package com.todo.ConfigTest;

import com.todo.config.PropertiesLoader;
import org.junit.jupiter.api.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertiesLoaderTest {
    
    private static Properties originalProperties;
    private static Field propsField;
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    
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

        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(originalProperties);
 
        System.setOut(new PrintStream(outputStreamCaptor));

        System.setProperty("TEST_DB_URL", "jdbc:postgresql://testhost:5432/testdb");
        System.setProperty("TEST_DB_USER", "testuser");
        System.setProperty("TEST_DB_PASSWORD", "testpass");
    }
    
    @AfterEach
    void tearDown() throws Exception {

        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(originalProperties);

        System.setOut(standardOut);
    
        System.clearProperty("TEST_DB_URL");
        System.clearProperty("TEST_DB_USER");
        System.clearProperty("TEST_DB_PASSWORD");
    }

    @Test
    @Order(2)
    @DisplayName("Should use default values when environment variables are missing")
    void testEnvironmentVariableDefaults() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.missing", "${NONEXISTENT_VAR:default_value}");

        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);

        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);

        assertEquals("default_value", PropertiesLoader.getProperty("test.missing"));
    }

    @Test
    @Order(3)
    @DisplayName("Should handle properties without environment variables")
    void testPlainProperties() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.plain", "plain_value");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        assertEquals("plain_value", PropertiesLoader.getProperty("test.plain"));
    }

    @Test
    @Order(4)
    @DisplayName("Should get integer property with default value")
    void testGetIntegerProperty() {
        int defaultValue = 42;
        int value = PropertiesLoader.getIntProperty("test.integer", defaultValue);
        assertEquals(defaultValue, value);
  
        Properties properties;
        try {
            properties = (Properties) propsField.get(null);
            properties.setProperty("test.integer", "100");
            assertEquals(100, PropertiesLoader.getIntProperty("test.integer", defaultValue));
        } catch (Exception e) {
            fail("Failed to set test property");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should handle invalid integer properties")
    void testInvalidIntegerProperty() {
        Properties properties;
        try {
            properties = (Properties) propsField.get(null);
            properties.setProperty("test.invalid.int", "not_a_number");
            
            assertThrows(NumberFormatException.class, () -> 
                PropertiesLoader.getIntProperty("test.invalid.int", 42)
            );
        } catch (Exception e) {
            fail("Failed to set test property");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should print properties correctly")
    void testPrintProperties() {
        Properties properties;
        try {
            properties = (Properties) propsField.get(null);
            properties.setProperty("test.print", "print_value");
            
            PropertiesLoader.printProperties();
            
            String printedOutput = outputStreamCaptor.toString();
            assertTrue(printedOutput.contains("Loaded Properties:"));
            assertTrue(printedOutput.contains("test.print = print_value"));
        } catch (Exception e) {
            fail("Failed to set test property");
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should throw exception for missing required properties")
    void testMissingRequiredProperties() {
        Exception exception = assertThrows(RuntimeException.class, 
            () -> PropertiesLoader.getProperty("non.existent.property")
        );
        
        assertTrue(exception.getMessage().contains("not found in application.properties"));
    }

    @Test
    @Order(8)
    @DisplayName("Should handle missing properties with defaults")
    void testMissingPropertiesWithDefaults() {
        String defaultValue = "default";
        assertEquals(defaultValue, 
            PropertiesLoader.getProperty("non.existent.property", defaultValue)
        );
    }

    @Test
    @Order(9)
    @DisplayName("Should handle environment variables without defaults")
    void testEnvironmentVariablesWithoutDefaults() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.nodefault", "${NONEXISTENT_VAR}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        assertEquals("${NONEXISTENT_VAR}", PropertiesLoader.getProperty("test.nodefault"));
    }
    
    @Test
    @Order(10)
    @DisplayName("Should handle escaped colons in environment variables")
    void testEscapedColons() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.escaped", "${TEST_VAR:value\\:with\\:colons}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        
        
        properties.putAll(testProps);

        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);

        assertEquals("value:with:colons", PropertiesLoader.getProperty("test.escaped"));
    }
    
    @Test
    @Order(11)
    @DisplayName("Should handle environment variables with empty default values")
    void testEmptyDefaultValues() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.empty.default", "${NONEXISTENT_VAR:}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);

        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);

        assertEquals("", PropertiesLoader.getProperty("test.empty.default"));
    }

    @Test
    @Order(12)
    @DisplayName("Should handle properties file not found")
    void testPropertiesFileNotFound() {
        assertThrows(RuntimeException.class, () -> {
            try (InputStream input = PropertiesLoader.class.getClassLoader()
                    .getResourceAsStream("nonexistent.properties")) {
                Properties props = new Properties();
                props.load(input);
            }
        });
    }
    
    @Test
    @Order(13)
    @DisplayName("Should handle malformed environment variable syntax")
    void testMalformedEnvironmentVariable() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.malformed", "${TEST_VAR");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("${TEST_VAR", PropertiesLoader.getProperty("test.malformed"));
    }
    
    @Test
    @Order(14)
    @DisplayName("Should handle single environment variable with value")
    void testSingleEnvironmentVariable() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.env", "${TEST_ENV_VAR:default}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("default", PropertiesLoader.getProperty("test.env"));
    }
}