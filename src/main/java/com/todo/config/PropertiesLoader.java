package com.todo.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    private static Properties properties = new Properties();
    
    static {
        loadProperties("application.properties");
    }
    
    public static void loadProperties(String filename) {
        try (InputStream input = getResourceAsStream(filename)) {
            loadProperties(input, filename);
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + filename, e);
        }
    }

    public static void loadProperties(InputStream input, String filename) {
        if (input == null) {
            throw new RuntimeException(filename + " not found");
        }
        try {
            properties.load(input);
            resolveEnvironmentVariables();
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + filename, e);
        }
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
    private static InputStream getResourceAsStream(String filename) {
        return PropertiesLoader.class.getClassLoader().getResourceAsStream(filename);
    }
}