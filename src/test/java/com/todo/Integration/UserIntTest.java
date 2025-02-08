package com.todo.Integration;

import com.todo.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

class UserIntTest {
    private static final Set<String> existingUsernames = new HashSet<>();
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "testPass123";
    
    @BeforeEach
    void setUp() {
        existingUsernames.clear();
    }
    
    @Test
    @DisplayName("Complete User Lifecycle Test")
    void testUserLifecycle() {
        // Create new user
        User user = new User(1,TEST_USERNAME, TEST_PASSWORD);
        
        // Verify user creation
        assertNotNull(user);
        assertEquals(TEST_USERNAME, user.getUsername());
        assertTrue(user.isValid());
        
        // Test login functionality
        assertTrue(user.login(TEST_USERNAME, TEST_PASSWORD));
        
        // Verify user ID assignment
        assertTrue(user.getUserid() > 0);
    }
    
    @Test
    @DisplayName("User Authentication Flow Test")
    void testAuthenticationFlow() {
        User user = new User(1,TEST_USERNAME, TEST_PASSWORD);
        
        // Test successful login
        assertTrue(user.login(TEST_USERNAME, TEST_PASSWORD));
        
        // Test failed login attempts
        assertFalse(user.login(TEST_USERNAME, "wrongPassword"));
        assertFalse(user.login("wrongUsername", TEST_PASSWORD));
        assertFalse(user.login("wrongUsername", "wrongPassword"));
    }
    
    @Test
    @DisplayName("Multiple Users Creation and ID Assignment Test")
    void testMultipleUsersCreation() {
        User user1 = new User(1,"user1", "pass1");
        User user2 = new User(2,"user2", "pass2");
        User user3 = new User(3,"user3", "pass3");
        
        // Verify unique ID assignment
        
        assertNotEquals(user1.getUserid(), user2.getUserid());
        assertNotEquals(user2.getUserid(), user3.getUserid());
        assertNotEquals(user1.getUserid(), user3.getUserid());
        
        // Verify ID incrementation
        
        assertTrue(user2.getUserid() > user1.getUserid());
        assertTrue(user3.getUserid() > user2.getUserid());
    }
    
    @Test
    @DisplayName("User Validation States Test")
    void testUserValidationStates() {
        // Test valid user
        User validUser = new User(1,TEST_USERNAME, TEST_PASSWORD);
        assertTrue(validUser.isValid());
        
        // Test invalid states
        User nullUsername = new User(1,null, TEST_PASSWORD);
        User emptyUsername = new User(1,"", TEST_PASSWORD);
        User nullPassword = new User(1,TEST_USERNAME, null);
        User emptyPassword = new User(1,TEST_USERNAME, "");
        
        assertFalse(nullUsername.isValid());
        assertFalse(emptyUsername.isValid());
        assertFalse(nullPassword.isValid());
        assertFalse(emptyPassword.isValid());
    }
    
    @Test
    @DisplayName("User Data Persistence Test")
    void testUserDataPersistence() {
        User user = new User(1,TEST_USERNAME, TEST_PASSWORD);
        String newUsername = "newTestUser";
        
        // Test username modification
        user.setUsername(newUsername);
        assertEquals(newUsername, user.getUsername());
        
        // Verify login still works with new username
        assertTrue(user.login(newUsername, TEST_PASSWORD));
        assertFalse(user.login(TEST_USERNAME, TEST_PASSWORD));
    }
    
    
}