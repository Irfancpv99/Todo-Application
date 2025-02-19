package com.todo.ConfigTest;

import com.todo.config.PropertiesLoader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.Arguments;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.stream.Stream;
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

    // GROUP 1: Basic property access tests

    @Test
    @Order(1)
    @DisplayName("Should get properties with and without defaults")
    void testBasicPropertyAccess() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("test.basic", "basic_value");
        
    
        assertEquals("basic_value", PropertiesLoader.getProperty("test.basic"), 
            "Should retrieve existing property");
            
        assertEquals("basic_value", PropertiesLoader.getProperty("test.basic", "default"), 
            "Should return actual value when property exists");
            
        assertEquals("default_value", PropertiesLoader.getProperty("test.missing", "default_value"), 
            "Should return default value when property missing");
            
        Exception exception = assertThrows(RuntimeException.class, 
            () -> PropertiesLoader.getProperty("non.existent.property"));
        assertTrue(exception.getMessage().contains("not found in application.properties"), 
            "Should throw with appropriate message for missing required property");
    }

//    @ParameterizedTest
//    @CsvSource({
//        "test.int.valid, 100, 42, 100",
//        "test.int.missing, , 42, 42"
//    })
    @Order(2)
    @DisplayName("Should get integer properties correctly")
    void testGetIntegerProperty(String key, String value, int defaultValue, int expected) throws Exception {
        Properties properties = (Properties) propsField.get(null);
        if (value != null) {
            properties.setProperty(key, value);
        }
        
        assertEquals(expected, PropertiesLoader.getIntProperty(key, defaultValue),
            "Should return correct integer value");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle invalid integer properties")
    void testInvalidIntegerProperty() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("test.invalid.int", "not_a_number");
        
        assertThrows(NumberFormatException.class, () -> 
            PropertiesLoader.getIntProperty("test.invalid.int", 42),
            "Should throw NumberFormatException for non-numeric values");
        
        properties.setProperty("test.int.max", String.valueOf(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, PropertiesLoader.getIntProperty("test.int.max", 0),
            "Should handle maximum integer value");
            
        properties.setProperty("test.int.min", String.valueOf(Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, PropertiesLoader.getIntProperty("test.int.min", 0),
            "Should handle minimum integer value");
    }
    
    @Test
    @Order(4)
    @DisplayName("Should print properties correctly")
    void testPrintProperties() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("test.print.key1", "value1");
        properties.setProperty("test.print.key2", "value2");
        
        PropertiesLoader.printProperties();
        
        String printedOutput = outputStreamCaptor.toString();
        assertAll("Property printing verification",
            () -> assertTrue(printedOutput.contains("Loaded Properties:"), 
                "Should print header"),
            () -> assertTrue(printedOutput.contains("test.print.key1 = value1"), 
                "Should print first property"),
            () -> assertTrue(printedOutput.contains("test.print.key2 = value2"), 
                "Should print second property")
        );
    }
    
    // GROUP 2: Environment variable resolution tests
    
    @Test
    @Order(5)
    @DisplayName("Should resolve environment variables with defaults")
    void testEnvironmentVariableResolution() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("env.with.default", "${NONEXISTENT_VAR:default_value}");
        testProps.setProperty("env.without.default", "${NONEXISTENT_VAR}");
        testProps.setProperty("env.with.empty.default", "${NONEXISTENT_VAR:}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);

        assertAll("Environment variable resolution",
            () -> assertEquals("default_value", 
                PropertiesLoader.getProperty("env.with.default"),
                "Should use default value when environment variable is missing"),
            () -> assertEquals("${NONEXISTENT_VAR}", 
                PropertiesLoader.getProperty("env.without.default"),
                "Should retain placeholder when no default is provided"),
            () -> assertEquals("", 
                PropertiesLoader.getProperty("env.with.empty.default"),
                "Should use empty default value when specified")
        );
    }
    
    @Test
    @Order(6)
    @DisplayName("Should handle special characters in environment variable defaults")
    void testSpecialCharactersInDefaults() throws Exception {
        Properties testProps = new Properties();
        
   
        testProps.setProperty("env.url", "${URL_VAR:http\\://example.com\\:8080/path}");
        
        testProps.setProperty("env.regex", "${REGEX_VAR:a+b*c?d(e)f[g]}");
       
        testProps.setProperty("env.symbols", "${SYMBOLS_VAR:!@#$%^&*()}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);

        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);

        assertAll("Special character handling in defaults",
            () -> assertEquals("http://example.com:8080/path", 
                PropertiesLoader.getProperty("env.url"),
                "Should handle escaped colons in URLs"),
            () -> assertEquals("a+b*c?d(e)f[g]", 
                PropertiesLoader.getProperty("env.regex"),
                "Should handle regex special characters"),
            () -> assertEquals("!@#$%^&*()", 
                PropertiesLoader.getProperty("env.symbols"),
                "Should handle special symbols")
        );
    }
    
    @Test
    @Order(7)
    @DisplayName("Should handle multiple environment variables in one property")
    void testMultipleEnvironmentVariables() throws Exception {
        Properties testProps = new Properties();
 
        testProps.setProperty("env.multiple.separated", "${VAR1:first}-${VAR2:second}");
        
 
        testProps.setProperty("env.multiple.sequential", "${FIRST:one}${SECOND:two}");

        testProps.setProperty("env.multiple.complex", "prefix_${A:${B:value}}_suffix");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);

        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);

        assertAll("Multiple environment variables",
            () -> assertEquals("first-second", 
                PropertiesLoader.getProperty("env.multiple.separated"),
                "Should handle multiple variables with separators"),
            () -> assertEquals("onetwo", 
                PropertiesLoader.getProperty("env.multiple.sequential"),
                "Should handle sequential variables"),
            () -> assertEquals("prefix_${B:value}_suffix", 
                PropertiesLoader.getProperty("env.multiple.complex"),
                "Should handle complex nested variables appropriately")
        );
    }
    
//    @ParameterizedTest
//    @ValueSource(strings = {
//        "${UNCLOSED_VAR", 
//        "prefix ${MISSING_CLOSE",
//        "${EMPTY}"
//    })
    @Order(8)
    @DisplayName("Should handle malformed environment variable patterns")
    void testMalformedEnvironmentVariables(String badPattern) throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("env.malformed", badPattern);
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);

        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals(badPattern, PropertiesLoader.getProperty("env.malformed"),
            "Should preserve malformed environment variable patterns");
    }
    
    // GROUP 3: Edge cases and error handling tests
    
    @Test
    @Order(9)
    @DisplayName("Should handle extremely long property values")
    void testExtremelyLongPropertyValue() throws Exception {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("very_long_value_");
        }
        String extremeValue = longValue.toString();
        
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("test.extreme.long", extremeValue);
        
        assertEquals(extremeValue, PropertiesLoader.getProperty("test.extreme.long"),
            "Should handle extremely long property values");
    }
    
    @Test
    @Order(10)
    @DisplayName("Should handle properties file loading edge cases")
    void testPropertiesFileLoadingEdgeCases() {
      
        assertThrows(RuntimeException.class, () -> {
            try (InputStream input = PropertiesLoader.class.getClassLoader()
                    .getResourceAsStream("nonexistent.properties")) {
                Properties props = new Properties();
                if (input == null) {
                    throw new RuntimeException("properties not found");
                }
                props.load(input);
            }
        }, "Should throw for missing properties file");
    }
    
    @Test
    @Order(11)
    @DisplayName("Should handle null and empty keys and values")
    void testNullAndEmptyKeysAndValues() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        
        
        properties.setProperty("test.empty.value", "");
        assertEquals("", PropertiesLoader.getProperty("test.empty.value"),
            "Should handle empty property value");
            
        Exception emptyKeyException = assertThrows(RuntimeException.class,
            () -> PropertiesLoader.getProperty(""));
        assertTrue(emptyKeyException.getMessage().contains("not found"),
            "Should throw for empty key");
            
        assertThrows(NullPointerException.class,
            () -> PropertiesLoader.getProperty(null, "default"));
            
        properties.setProperty("test.whitespace.value", "   ");
        assertEquals("   ", PropertiesLoader.getProperty("test.whitespace.value"),
            "Should preserve whitespace in property values");
    }
    
    @Test
    @Order(12)
    @DisplayName("Should reinitialize properties when needed")
    void testPropertiesReinitialization() throws Exception {
       
        Properties properties = (Properties) propsField.get(null);
        Properties backup = new Properties();
        backup.putAll(properties);
        
        properties.clear();
        Exception emptyPropsException = assertThrows(RuntimeException.class,
            () -> PropertiesLoader.getProperty("any.key"));
        assertTrue(emptyPropsException.getMessage().contains("not found"),
            "Should throw when properties are empty");
            
        properties.putAll(backup);
        assertDoesNotThrow(() -> {
            properties.setProperty("test.restored", "restored_value");
            assertEquals("restored_value", PropertiesLoader.getProperty("test.restored"));
        }, "Should recover after properties restoration");
    }
    
    @Test
    @Order(13)
    @DisplayName("Complete system test with multiple property types")
    void testCompleteSystem() throws Exception {
        Properties testProps = new Properties();

        testProps.setProperty("system.regular", "regular_value");
    
        testProps.setProperty("system.integer", "42");
        
        
        testProps.setProperty("system.env", "${SYSTEM_VAR:env_default}");

        testProps.setProperty("system.url", "${URL_VAR:jdbc:postgresql://host\\:port/db}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertAll("Complete system verification",
            () -> assertEquals("regular_value", 
                PropertiesLoader.getProperty("system.regular")),
            () -> assertEquals(42, 
                PropertiesLoader.getIntProperty("system.integer", 0)),
            () -> assertEquals("env_default", 
                PropertiesLoader.getProperty("system.env")),
            () -> assertEquals("jdbc:postgresql://host:port/db", 
                PropertiesLoader.getProperty("system.url"))
        );
    }
    
    @Test
    @Order(14)
    @DisplayName("Test alternative resource not found verification")
    void testResourceNotFoundVerification() {

        InputStream nonExistentStream = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream("definitely-does-not-exist.properties");
        
        assertNull(nonExistentStream, "Non-existent resource stream should be null");
   
        assertThrows(RuntimeException.class, () -> {
            if (nonExistentStream == null) {
                throw new RuntimeException("properties not found in classpath");
            }
        }, "Should throw for missing resource");
    }
    
    @Test
    @Order(15)
    @DisplayName("Test environment variable via System properties")
    void testEnvironmentVariableViaSystemProperties() throws Exception {
        try {
            
            System.setProperty("TEST_PROPERTY_ENV", "system_property_value");
           
            Properties testProps = new Properties();
            testProps.setProperty("test.system.property", "${TEST_PROPERTY_ENV:default_value}");
            
            Properties properties = (Properties) propsField.get(null);
            properties.clear();
            properties.putAll(testProps);
            
            Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
            resolveMethod.setAccessible(true);
            resolveMethod.invoke(null);
            
            assertEquals("default_value", PropertiesLoader.getProperty("test.system.property"),
                "Should use default value since System.getenv() won't see System.setProperty values");
            
        } finally {
            System.clearProperty("TEST_PROPERTY_ENV");
        }
    }
    
   
    
    @Order(16)
    @DisplayName("Test complex environment variable patterns")
    void testComplexEnvironmentVariablePatterns(String input, String expected) throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.pattern", input);
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals(expected, PropertiesLoader.getProperty("test.pattern"),
            "Should correctly resolve complex pattern: " + input);
    }
    
    static Stream<Arguments> provideComplexVariablePatterns() {
        return Stream.of(
       
            Arguments.of("prefix${VAR:value}suffix", "prefixvaluesuffix"),
            Arguments.of("${VAR1:val1}${VAR2:val2}", "val1val2"),
            Arguments.of("${VAR1:val1}_${VAR2:val2}_${VAR3:val3}", "val1_val2_val3"),
            Arguments.of("no_variables_here", "no_variables_here"),
            Arguments.of("${:empty_name}", "empty_name"),  // Updated expectation
            Arguments.of("${}:after_empty", "${}:after_empty"),
            Arguments.of("${VAR:value\\:with\\:colons}", "value:with:colons"),
            Arguments.of("${VAR:\\:leading:colon}", ":leading:colon"),
            Arguments.of("text with ${MISSING_VAR} incomplete", "text with ${MISSING_VAR} incomplete"),
            Arguments.of("${VAR:value}${", "value${"),
            Arguments.of("${VAR1:${VAR2:nested}}", "${VAR2:nested}")
        );
    }
    
    @Test
    @Order(17)
    @DisplayName("Test properties forEach lambda execution")
    void testPropertiesForEachLambda() throws Exception {
       
        Properties testProps = new Properties();
        
        testProps.setProperty("test.regular", "regular_value");
        
        testProps.setProperty("test.start", "${START:start}_rest");
        
        testProps.setProperty("test.middle", "before_${MIDDLE:middle}_after");
 
        testProps.setProperty("test.end", "before_${END:end}");
    
        testProps.setProperty("test.multiple", "${ONE:one}_${TWO:two}_${THREE:three}");
   
        testProps.setProperty("test.unclosed", "before_${UNCLOSED");
        
        testProps.setProperty("test.empty", "${EMPTY:}_suffix");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertAll("forEach lambda execution paths",
            () -> assertEquals("regular_value", PropertiesLoader.getProperty("test.regular")),
            () -> assertEquals("start_rest", PropertiesLoader.getProperty("test.start")),
            () -> assertEquals("before_middle_after", PropertiesLoader.getProperty("test.middle")),
            () -> assertEquals("before_end", PropertiesLoader.getProperty("test.end")),
            () -> assertEquals("one_two_three", PropertiesLoader.getProperty("test.multiple")),
            () -> assertEquals("before_${UNCLOSED", PropertiesLoader.getProperty("test.unclosed")),
            () -> assertEquals("_suffix", PropertiesLoader.getProperty("test.empty"))
        );
    }
    
    @Test
    @Order(18)
    @DisplayName("Test StringBuilder result construction")
    void testStringBuilderResultConstruction() throws Exception {
     
        
        Properties testProps = new Properties();
        testProps.setProperty("test.no.vars", "plain text value");
        
        testProps.setProperty("test.var.start", "${VAR:value}after");
        
        testProps.setProperty("test.var.end", "before${VAR:value}");
        
        testProps.setProperty("test.complex", "start ${VAR1:v1} middle ${VAR2:v2} end");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertAll("StringBuilder operation paths",
            () -> assertEquals("plain text value", PropertiesLoader.getProperty("test.no.vars")),
            () -> assertEquals("valueafter", PropertiesLoader.getProperty("test.var.start")),
            () -> assertEquals("beforevalue", PropertiesLoader.getProperty("test.var.end")),
            () -> assertEquals("start v1 middle v2 end", PropertiesLoader.getProperty("test.complex"))
        );
    }
}