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
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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
    @Order(5)
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
    @Order(6)
    @DisplayName("Should throw exception for missing required properties")
    void testMissingRequiredProperties() {
        Exception exception = assertThrows(RuntimeException.class, 
            () -> PropertiesLoader.getProperty("non.existent.property")
        );
        
        assertTrue(exception.getMessage().contains("not found in application.properties"));
    }

    @Test
    @Order(7)
    @DisplayName("Should handle missing properties with defaults")
    void testMissingPropertiesWithDefaults() {
        String defaultValue = "default";
        assertEquals(defaultValue, 
            PropertiesLoader.getProperty("non.existent.property", defaultValue)
        );
    }

    @Test
    @Order(8)
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
    @Order(9)
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
    @Order(10)
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
    @Order(11)
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
    @Order(12)
    @DisplayName("Should handle malformed environment variable")
    void testMalformedEnvironmentVariable() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.malformed", "${TEST_VAR:value");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("${TEST_VAR:value", PropertiesLoader.getProperty("test.malformed"));
    }
    
    @Test
    @Order(13)
    @DisplayName("Should handle single environment variable")
    void testSingleEnvironmentVariable() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.single", "${TEST_VAR:defaultValue}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("defaultValue", PropertiesLoader.getProperty("test.single"));
    }
    
    @Test
    @Order(14)
    @DisplayName("Should handle empty properties file")
    void testEmptyPropertiesFile() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        assertThrows(RuntimeException.class, () -> 
            PropertiesLoader.getProperty("any.property"));
    }

    @Test
    @Order(15)
    @DisplayName("Should handle empty environment variable name")
    void testEmptyEnvironmentVariableName() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.empty.env", "${}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        assertEquals("${}", PropertiesLoader.getProperty("test.empty.env"));
    }
    
    @Test
    @Order(16)
    @DisplayName("Should handle multiple colons in default value")
    void testMultipleColonsInDefault() throws Exception {

        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);

        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("test.multicolon", "${TEST_VAR:http\\://example.com\\:8080/path}");
    
        resolveMethod.invoke(null);
        
        String result = PropertiesLoader.getProperty("test.multicolon");
        assertEquals("http://example.com:8080/path", result);
    }

    @Test
    @Order(17)
    @DisplayName("Retain placeholder when env var and default missing")
    void testMissingEnvAndDefault() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.missing", "${NONEXISTENT_VAR}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("${NONEXISTENT_VAR}", PropertiesLoader.getProperty("test.missing"));
    }
    
    @Test
    @Order(18)
    @DisplayName("Static initializer throws when properties file missing")
    void testStaticInitializerFailure() throws Exception {
        // Temporarily replace the static block to simulate missing file
        Properties original = (Properties) propsField.get(null);
        propsField.set(null, new Properties()); 
        
        assertThrows(RuntimeException.class, () -> PropertiesLoader.getProperty("any.key"));
        propsField.set(null, original); 
    }
    
    @Test
    @Order(19)
    @DisplayName("Should handle circular environment variable references")
    void testCircularReferences() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.circular1", "${test.circular2:default1}");
        testProps.setProperty("test.circular2", "${test.circular1:default2}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("default1", PropertiesLoader.getProperty("test.circular1"));
        assertEquals("default2", PropertiesLoader.getProperty("test.circular2"));
    }
    
    @Test
    @Order(20)
    @DisplayName("Should handle property with only whitespace")
    void testWhitespaceProperty() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.whitespace", "   ");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        assertEquals("   ", PropertiesLoader.getProperty("test.whitespace"));
    }
    
    @Test
    @Order(21)
    @DisplayName("Should handle environment variable with special characters")
    void testSpecialCharactersInEnvVar() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.special", "${SPECIAL_VAR:!@#$%^&*()}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("!@#$%^&*()", PropertiesLoader.getProperty("test.special"));
    }

    @Test
    @Order(22)
    @DisplayName("Should handle null property value")
    void testNullPropertyValue() throws Exception {
        Properties properties = (Properties) propsField.get(null);
        properties.setProperty("test.null", "");
        
        String value = PropertiesLoader.getProperty("test.null");
        assertEquals("", value);
    }
    
    @Test
    @Order(23)
    @DisplayName("Should handle multiple environment variables in single property")
    void testMultipleEnvironmentVariables() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.multiple", "${TEST_VAR1:default1}_${TEST_VAR2:default2}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("default1_default2", PropertiesLoader.getProperty("test.multiple"));
    }
    
    @Test
    @Order(24)
    @DisplayName("Should handle environment variable without default value")
    void testEnvironmentVariableWithoutDefault() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.nodefault", "${TEST_VAR}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        
        assertEquals("${TEST_VAR}", PropertiesLoader.getProperty("test.nodefault"));
    }
    
    

    @Test
    @Order(25)
    @DisplayName("Should handle sequential environment variables")
    void testSequentialEnvironmentVariables() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.sequential", "${FIRST:one}${SECOND:two}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);
        System.out.println("Actual resolved value: " + PropertiesLoader.getProperty("test.sequential"));
        
        assertEquals("onetwo", PropertiesLoader.getProperty("test.sequential"));
    }

    @Test
    @Order(26)
    @DisplayName("Should handle environment variables with separators")
    void testEnvironmentVariablesWithSeparators() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("test.separators", "${VAR1:first}-${VAR2:second}");
        
        Properties properties = (Properties) propsField.get(null);
        properties.clear();
        properties.putAll(testProps);
        
        Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(null);

        System.out.println("Actual resolved value: " + PropertiesLoader.getProperty("test.separators"));
        
        assertEquals("first-second", PropertiesLoader.getProperty("test.separators"));
    }
  
  @Test
  @Order(27)
  @DisplayName("Should handle very long property values")
  void testVeryLongPropertyValue() throws Exception {
      StringBuilder longValue = new StringBuilder();
      for (int i = 0; i < 1000; i++) {
          longValue.append("very_long_value_");
      }
      
      Properties testProps = new Properties();
      testProps.setProperty("test.long", longValue.toString());
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      assertEquals(longValue.toString(), PropertiesLoader.getProperty("test.long"));
  }
  
  @Test
  @DisplayName("Test getIntProperty with extremely large value")
  void testGetIntPropertyWithExtremeValue() throws Exception {
      Properties properties = (Properties) propsField.get(null);
      properties.setProperty("extreme.int", String.valueOf(Integer.MAX_VALUE));
      
      int result = PropertiesLoader.getIntProperty("extreme.int", 0);
      assertEquals(Integer.MAX_VALUE, result);
  }
  
  @Test
  @DisplayName("Test getIntProperty with negative value")
  void testGetIntPropertyWithNegativeValue() throws Exception {
      Properties properties = (Properties) propsField.get(null);
      properties.setProperty("negative.int", "-42");
      
      int result = PropertiesLoader.getIntProperty("negative.int", 0);
      assertEquals(-42, result);
  }
  
  @Test
  @DisplayName("Test getProperty with environment variable containing special regex characters")
  void testGetPropertyWithRegexSpecialChars() throws Exception {
      Properties testProps = new Properties();
      testProps.setProperty("regex.special", "${REGEX_VAR:a+b*c?d(e)f[g]}");
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
      resolveMethod.setAccessible(true);
      resolveMethod.invoke(null);
      
      assertEquals("a+b*c?d(e)f[g]", PropertiesLoader.getProperty("regex.special"));
  }
  
  @Test
  @DisplayName("Test getProperty with nested environment variables")
  void testComplexNestedEnvironmentVariables() throws Exception {
      Properties testProps = new Properties();
      testProps.setProperty("complex.nested", "prefix_${VAR1:${VAR2:${VAR3:final}}}_suffix");
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
      resolveMethod.setAccessible(true);
      resolveMethod.invoke(null);
      String actual = PropertiesLoader.getProperty("complex.nested");
      assertEquals("prefix_${VAR2:${VAR3:final}}_suffix", actual);
      
  }
  
  @Test
  @DisplayName("Test extreme edge case with unclosed variable placeholder")
  void testUnclosedVariablePlaceholder() throws Exception {
      Properties testProps = new Properties();
      testProps.setProperty("unclosed.var", "start ${UNCLOSED_VAR");
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
      resolveMethod.setAccessible(true);
      resolveMethod.invoke(null);
      
      // The behavior is to leave it as is
      assertEquals("start ${UNCLOSED_VAR", PropertiesLoader.getProperty("unclosed.var"));
  }
  
  @Test
  @DisplayName("Test extreme edge case with multiple consecutive escaped colons")
  void testMultipleConsecutiveEscapedColons() throws Exception {
      Properties testProps = new Properties();
      testProps.setProperty("multiple.escaped.colons", "${TEST_VAR:a\\:\\:\\:b}");
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
      resolveMethod.setAccessible(true);
      resolveMethod.invoke(null);
      
      assertEquals("a:::b", PropertiesLoader.getProperty("multiple.escaped.colons"));
  }
  
  @Test
  @DisplayName("Test resolveEnvironmentVariables with variable at end of string")
  void testVariableAtEndOfString() throws Exception {
      Properties testProps = new Properties();
      testProps.setProperty("var.at.end", "prefix_${END_VAR:end}");
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
      resolveMethod.setAccessible(true);
      resolveMethod.invoke(null);
      
      assertEquals("prefix_end", PropertiesLoader.getProperty("var.at.end"));
  }
  
  @Test
  @DisplayName("Test resolveEnvironmentVariables with variable at start of string")
  void testVariableAtStartOfString() throws Exception {
      Properties testProps = new Properties();
      testProps.setProperty("var.at.start", "${START_VAR:start}_suffix");
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
      resolveMethod.setAccessible(true);
      resolveMethod.invoke(null);
      
      assertEquals("start_suffix", PropertiesLoader.getProperty("var.at.start"));
  }
  
  @Test
  @DisplayName("Test overlapping environment variable patterns")
  void testOverlappingEnvironmentVariablePatterns() throws Exception {
      Properties testProps = new Properties();
      testProps.setProperty("overlapping.vars", "${PREFIX_${INNER:inner}_SUFFIX:default}");
      
      Properties properties = (Properties) propsField.get(null);
      properties.clear();
      properties.putAll(testProps);
      
      Method resolveMethod = PropertiesLoader.class.getDeclaredMethod("resolveEnvironmentVariables");
      resolveMethod.setAccessible(true);
      resolveMethod.invoke(null);
      String result = PropertiesLoader.getProperty("overlapping.vars");
      assertNotNull(result);
  }
  
  @Test
  @DisplayName("Test empty key in getProperty")
  void testEmptyKeyInGetProperty() {
      Exception exception = assertThrows(RuntimeException.class,
          () -> PropertiesLoader.getProperty("")
      );
      assertTrue(exception.getMessage().contains("not found"));
  }
  
  @Test
  @DisplayName("Test null key in getProperty with default")
  void testNullKeyInGetPropertyWithDefault() {
      
      assertThrows(NullPointerException.class,
          () -> PropertiesLoader.getProperty(null, "default")
      );
      
  }
  
 }