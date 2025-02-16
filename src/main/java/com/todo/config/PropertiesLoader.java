package com.todo.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String resolvedValue = stringValue;
            
            // Pattern to match environment variables with optional default values
            Pattern pattern = Pattern.compile("\\$\\{([^}]*?)(?::([^}]*))?}");
            Matcher matcher = pattern.matcher(stringValue);
            StringBuffer sb = new StringBuffer();
            
            while (matcher.find()) {
                String envVarName = matcher.group(1);
                String defaultValue = matcher.group(2);
                
                // Get environment variable value or use default
                String envValue = System.getenv(envVarName);
                String replacement = envValue != null ? envValue : 
                                   defaultValue != null ? defaultValue : 
                                   matcher.group(0); // Keep original if no env var and no default
                
                // Escape special regex characters in replacement
                replacement = replacement.replace("$", "\\$");
                replacement = replacement.replace("\\:", ":");
                
                matcher.appendReplacement(sb, replacement);
            }
            matcher.appendTail(sb);
            resolvedValue = sb.toString();
            
            resolvedProps.setProperty(key.toString(), resolvedValue);
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