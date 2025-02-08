 package com.todo.Integration;

import com.todo.model.User;
import com.todo.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceIntTest {
    private UserService userService;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
    	UserService.clearUsers();
        userService = new UserService();
    }

    @Test
    @DisplayName("Register and Login Flow")
    void testRegisterAndLoginFlow() {
    	
        // Register user
        
    	User registeredUser = userService.registerUser(TEST_USERNAME, TEST_PASSWORD);
        assertNotNull(registeredUser);
        assertEquals(TEST_USERNAME, registeredUser.getUsername());

        // Login with same credentials
        
        User loggedInUser = userService.login(TEST_USERNAME, TEST_PASSWORD);
        assertNotNull(loggedInUser);
        assertEquals(registeredUser.getUsername(), loggedInUser.getUsername());
    }

    @Test
    @DisplayName("Multiple User Registration and Login")
    void testMultipleUserFlow() {
        String user1 = "user1";
        String user2 = "user2";
        String password = "password123";

        // Multiple users register
        
        User registeredUser1 = userService.registerUser(user1, password);
        User registeredUser2 = userService.registerUser(user2, password);
        
        assertNotNull(registeredUser1);
        assertNotNull(registeredUser2);
        assertEquals(user1, registeredUser1.getUsername());
        assertEquals(user2, registeredUser2.getUsername());
        assertNotEquals(registeredUser1.getUserid(), registeredUser2.getUserid());

        // Verify both users can login
        
        User loggedInUser1 = userService.login(user1, password);
        User loggedInUser2 = userService.login(user2, password);

        assertNotNull(loggedInUser1);
        assertNotNull(loggedInUser2);
        assertEquals(user1, loggedInUser1.getUsername());
        assertEquals(user2, loggedInUser2.getUsername());
    }

    @Test
    @DisplayName("User Session Management")
    void testUserSessionFlow() {
    	
        // Register user
    	
        User registeredUser = userService.registerUser(TEST_USERNAME, TEST_PASSWORD);

        // Multiple successful login attempts should work
        
        User firstLogin = userService.login(TEST_USERNAME, TEST_PASSWORD);
        User secondLogin = userService.login(TEST_USERNAME, TEST_PASSWORD);

        assertNotNull(firstLogin);
        assertNotNull(secondLogin);
        assertEquals(registeredUser.getUsername(), firstLogin.getUsername());
        assertEquals(registeredUser.getUsername(), secondLogin.getUsername());
        
        // Verify login fails with wrong password
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.login(TEST_USERNAME, "wrongpassword"));
    }

    @Test
    @DisplayName("User Registration State Persistence")
    void testUserStatePersistence() {
    	
        // Register user with first service instance
        
    	userService.registerUser(TEST_USERNAME, TEST_PASSWORD);
        assertTrue(userService.isUsernameTaken(TEST_USERNAME));
        
        // Create new service instance and verify user still exists
        
        UserService newUserService = new UserService();
        assertTrue(newUserService.isUsernameTaken(TEST_USERNAME));
        
        // Verify can still login with new service instance
        
        User loggedInUser = newUserService.login(TEST_USERNAME, TEST_PASSWORD);
        assertNotNull(loggedInUser);
        assertEquals(TEST_USERNAME, loggedInUser.getUsername());
    }
}