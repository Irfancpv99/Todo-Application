package com.todo.UnitTest;

import com.todo.model.User;
import com.todo.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

class UserServiceUniTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        UserService.clearUsers();
        userService = new UserService();
    }

    @Test
    @DisplayName("User registration should succeed with valid credentials")
    void testRegisterUserSuccess() {
        String username = "newuser";
        String password = "password123";

        User registeredUser = userService.registerUser(username, password);

        assertNotNull(registeredUser);
        assertEquals(username, registeredUser.getUsername());
        assertTrue(userService.isUsernameTaken(username));
    }

    @Test
    @DisplayName("User registration should fail with duplicate username")
    void testRegisterUserFailsWithDuplicateUsername() {
        String username = "duplicateuser";
        String password = "password123";
        userService.registerUser(username, password);

        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser(username, "differentpassword"));
    }

    @Test
    @DisplayName("User registration should fail with invalid credentials")
    void testRegisterUserFailsWithInvalidCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser("", "password123"));

        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser(null, "password123"));

        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser("username", ""));

        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser("username", null));
    }

    @Test
    @DisplayName("User login should succeed with valid credentials")
    void testLoginSuccess() {
        String username = "loginuser";
        String password = "password123";
        userService.registerUser(username, password);

        User loggedInUser = userService.login(username, password);

        assertNotNull(loggedInUser);
        assertEquals(username, loggedInUser.getUsername());
    }

    @Test
    @DisplayName("User login should fail with incorrect credentials")
    void testLoginFailure() {
        String username = "newuser";
        String password = "password124";
        userService.registerUser(username, password);
        
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            userService.login(username, "wrongpassword");
        });
        assertEquals("Invalid username or password", exception1.getMessage());
        
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            userService.login("nonexistentuser", password);
        });
        assertEquals("Invalid username or password", exception2.getMessage());
    }
    
    @Test
    @DisplayName("Clear users should remove all existing users")
    void testClearUsers() {
        userService.registerUser("user1", "pass1");
        userService.registerUser("user2", "pass2");

        assertTrue(userService.isUsernameTaken("user1"));
        assertTrue(userService.isUsernameTaken("user2"));        
        
        UserService.clearUsers();
        
        assertFalse(userService.isUsernameTaken("user1"));
        assertFalse(userService.isUsernameTaken("user2"));
    }
    
    @Test
    @DisplayName("Login should fail with empty credentials")
    void testLoginWithEmptyCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> userService.login("", "password123"));
            
        assertThrows(IllegalArgumentException.class,
            () -> userService.login("username", ""));
            
        assertThrows(IllegalArgumentException.class,
            () -> userService.login(null, "password123"));
            
        assertThrows(IllegalArgumentException.class,
            () -> userService.login("username", null));
    }

    @Test
    @DisplayName("Database error handling in user operations")
    void testDatabaseErrorHandling() {
        
        userService.registerUser("user1", "pass1");
        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser("user1", "pass2"));
    }
    
    @Test
    @DisplayName("Test Database Error in Username Check")
    void testDatabaseErrorInUsernameCheck() {
        userService.registerUser("testuser", "password");
        assertThrows(RuntimeException.class, () -> 
            userService.isUsernameTaken(null)
        );
        assertTrue(userService.isUsernameTaken("testuser"));
    }
    
    @Test
    @DisplayName("Test Clear Users with Test Exception")
    void testClearUsersWithTestException() {
        userService.registerUser("testuser1", "password");
        userService.registerUser("testuser2", "password");
        
        assertTrue(userService.isUsernameTaken("testuser1"));
        assertTrue(userService.isUsernameTaken("testuser2"));
        
        UserService.clearUsers();
        
         assertFalse(userService.isUsernameTaken("testuser1"));
        assertFalse(userService.isUsernameTaken("testuser2"));
    }
    
    @Test
    @DisplayName("Test Generated Keys Failure in User Registration")
    void testGeneratedKeysFailure() {
         String longUsername = "a".repeat(300);
        assertThrows(RuntimeException.class, () ->
            userService.registerUser(longUsername, "password")
        );
    }
    
    @Test
    @DisplayName("Test Database Error Recovery in Username Check")
    void testDatabaseErrorRecoveryInUsernameCheck() {
        userService.registerUser("user1", "password");
        assertTrue(userService.isUsernameTaken("user1"));
        
         assertThrows(RuntimeException.class, () -> 
            userService.isUsernameTaken(null)
        );
        
        userService.registerUser("user2", "password");
        assertTrue(userService.isUsernameTaken("user2"));
    }
}


