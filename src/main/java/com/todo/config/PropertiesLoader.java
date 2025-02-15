package com.todo.config;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    private static  Properties properties = new Properties();

    static {
        try (InputStream input = PropertiesLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("application.properties not found in classpath");
            }
            properties.load(input);
            // Resolve environment variables
            resolveEnvironmentVariables();
        } catch (Exception e) {
            throw new RuntimeException("Could not load application.properties", e);
        }
    }

    private static void resolveEnvironmentVariables() {
        Properties resolvedProps = new Properties();
        properties.forEach((key, value) -> {
            String stringValue = value.toString();
            if (stringValue.startsWith("${") && stringValue.endsWith("}")) {
                // Extract environment variable name and default value
                String envVarFull = stringValue.substring(2, stringValue.length() - 1);
                
                // Replace escaped colons (\:) with a placeholder
                String tempString = envVarFull.replace("\\:", "[COLON]");
                
                // Now split safely
                String[] parts = tempString.contains(":") ? tempString.split(":", 2) : new String[]{tempString};
                
                // Restore colons in the default value
                String envVar = parts[0].replace("[COLON]", ":");
                String defaultValue = parts.length > 1 ? parts[1].replace("[COLON]", ":") : null;
                
                // Get environment variable value or use default
                String envValue = System.getenv(envVar);
                if (envValue != null) {
                    resolvedProps.setProperty(key.toString(), envValue);
                } else if (defaultValue != null) {
                    resolvedProps.setProperty(key.toString(), defaultValue);
                } else {
                    // If no env var and no default, keep original value
                    resolvedProps.setProperty(key.toString(), stringValue);
                }
            } else {
                resolvedProps.setProperty(key.toString(), stringValue);
            }
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