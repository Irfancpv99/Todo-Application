 package com.todo.Integration;

import com.todo.model.User;
import com.todo.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    
    @Test
    @DisplayName("Database State After Failed Registration")
    void testDatabaseStateAfterFailedRegistration() {
        // Create initial user
        User user = userService.registerUser("testuser", "password123");
        assertNotNull(user);
        
        // Attempt to create user with same username
        assertThrows(IllegalArgumentException.class, () -> 
            userService.registerUser("testuser", "newpassword")
        );
        
        // Verify database state
        assertTrue(userService.isUsernameTaken("testuser"));
        
        // Verify original user can still login
        User loggedInUser = userService.login("testuser", "password123");
        assertNotNull(loggedInUser);
        assertEquals(user.getUserid(), loggedInUser.getUserid());
    }
    
    @Test
    @DisplayName("Concurrent User Registration")
    void testConcurrentUserRegistration() throws InterruptedException {
        int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    userService.registerUser("user" + index, "password" + index);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(numThreads, successCount.get());
        assertEquals(0, failCount.get());
        
        // Verify all users were created and can login
        for (int i = 0; i < numThreads; i++) {
            User user = userService.login("user" + i, "password" + i);
            assertNotNull(user);
        }
    }
    
    @Test
    @DisplayName("Username Uniqueness Constraint Test")
    void testUsernameUniquenessConstraint() {
        // First registration should succeed
        User firstUser = userService.registerUser("uniqueuser", "password123");
        assertNotNull(firstUser);
        
        // Second registration with same username should fail
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("uniqueuser", "differentpassword");
        });
        assertEquals("User already exists.", exception.getMessage());
        
        // Original user should still be able to login
        User loggedInUser = userService.login("uniqueuser", "password123");
        assertNotNull(loggedInUser);
        assertEquals(firstUser.getUserid(), loggedInUser.getUserid());
    }
    
    @Test
    @DisplayName("Test Registration Input Validation")
    void testRegistrationInputValidation() {
        // Test null username
        assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser(null, "password123")
        );

        // Test empty username
        assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser("", "password123")
        );

        // Test null password
        assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser("testuser", null)
        );

        // Test empty password
        assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser("testuser", "")
        );
    }

    @Test
    @DisplayName("Test Login Input Validation")
    void testLoginInputValidation() {
        // Test null username
        assertThrows(IllegalArgumentException.class, () ->
            userService.login(null, "password123")
        );

        // Test empty username
        assertThrows(IllegalArgumentException.class, () ->
            userService.login("", "password123")
        );

        // Test null password
        assertThrows(IllegalArgumentException.class, () ->
            userService.login("testuser", null)
        );

        // Test empty password
        assertThrows(IllegalArgumentException.class, () ->
            userService.login("testuser", "")
        );
    }

    @Test
    @DisplayName("Test Database Constraints")
    void testDatabaseConstraints() {
        // Register initial user
        User user = userService.registerUser("constraintuser", "password123");
        assertNotNull(user);

        // Attempt to register same username (should trigger unique constraint)
        assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser("constraintuser", "different_password")
        );

        // Verify original user still exists and can login
        User loggedInUser = userService.login("constraintuser", "password123");
        assertNotNull(loggedInUser);
        assertEquals(user.getUserid(), loggedInUser.getUserid());
    }

    @Test
    @DisplayName("Test User Authentication Flow")
    void testUserAuthenticationFlow() {
        // Register user
        User user = userService.registerUser("authuser", "password123");
        assertNotNull(user);

        // Test successful login
        User loggedInUser = userService.login("authuser", "password123");
        assertNotNull(loggedInUser);
        assertEquals(user.getUserid(), loggedInUser.getUserid());

        // Test login with wrong password
        assertThrows(IllegalArgumentException.class, () ->
            userService.login("authuser", "wrongpassword")
        );

        // Test login with non-existent user
        assertThrows(IllegalArgumentException.class, () ->
            userService.login("nonexistentuser", "password123")
        );
    }

    @Test
    @DisplayName("Test Username Availability Check")
    void testUsernameAvailabilityCheck() {
        String username = "availabilityuser";
        
       
        assertFalse(userService.isUsernameTaken(username));

       
        User user = userService.registerUser(username, "password123");
        assertNotNull(user);

       
        assertTrue(userService.isUsernameTaken(username));

        
        UserService newUserService = new UserService();
        assertTrue(newUserService.isUsernameTaken(username));
    }
    
    @Test
    @DisplayName("Test SQL Exception in Username Check")
    void testSQLExceptionInUsernameCheck() {
        assertThrows(RuntimeException.class, () -> 
            userService.isUsernameTaken(null)
        );
    }
    
    @Test
    @DisplayName("Test Generated Keys Failure")
    void testGeneratedKeysFailure() {
        assertThrows(RuntimeException.class, () ->
            userService.registerUser("a".repeat(300), "pass")
        );
    }
}