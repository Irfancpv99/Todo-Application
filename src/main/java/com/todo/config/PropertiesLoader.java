package com.todo.config;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    private static Properties properties = new Properties();
    
    static {
        String propertiesFile = isTestEnvironment() ? 
            "application-test.properties" : "application.properties";
        
        try (InputStream input = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream(propertiesFile)) {
            if (input == null) {
                throw new RuntimeException(propertiesFile + " not found in classpath");
            }
            properties.load(input);
            resolveEnvironmentVariables();
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + propertiesFile, e);
        }
    }
    
    private static boolean isTestEnvironment() {
        return "test".equals(System.getProperty("spring.profiles.active")) ||
               Boolean.parseBoolean(System.getProperty("test", "false"));
    }
    
    

    private static void resolveEnvironmentVariables() {
        Properties resolvedProps = new Properties();
        properties.forEach((key, value) -> {
            String stringValue = value.toString();
            StringBuilder result = new StringBuilder();
            int startIndex = 0;
            
            while (true) {
                int varStart = stringValue.indexOf("${", startIndex);
                if (varStart == -1) {
                    result.append(stringValue.substring(startIndex));
                    break;
                }
                
                result.append(stringValue.substring(startIndex, varStart));
                int varEnd = stringValue.indexOf("}", varStart);
                if (varEnd == -1) {
                    result.append(stringValue.substring(varStart));
                    break;
                }
                
                String var = stringValue.substring(varStart + 2, varEnd);
                String[] parts = var.split(":", 2);
                String envVar = parts[0];
                String defaultValue = parts.length > 1 ? parts[1].replace("\\:", ":") : null;
                
                String envValue = System.getenv(envVar);
                if (envValue != null) {
                    result.append(envValue);
                } else if (defaultValue != null) {
                    result.append(defaultValue);
                } else {
                    result.append("${").append(var).append("}");
                }
                
                startIndex = varEnd + 1;
            }
            
            resolvedProps.setProperty(key.toString(), result.toString());
        });
        properties.clear();
        properties.putAll(resolvedProps);
    }

    public static String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Property " + key + " not found in application.properties");
        }
        return value;
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static void printProperties() {
        System.out.println("Loaded Properties:");
        properties.forEach((key, value) -> 
            System.out.println(key + " = " + value));
    }
}