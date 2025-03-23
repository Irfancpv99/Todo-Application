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
        
        User registeredUser1 = userService.registerUser(user1, password);
        User registeredUser2 = userService.registerUser(user2, password);
        
        assertNotNull(registeredUser1);
        assertNotNull(registeredUser2);
        assertEquals(user1, registeredUser1.getUsername());
        assertEquals(user2, registeredUser2.getUsername());
        assertNotEquals(registeredUser1.getUserid(), registeredUser2.getUserid());

        
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
    	
    	
        User registeredUser = userService.registerUser(TEST_USERNAME, TEST_PASSWORD);

        
        User firstLogin = userService.login(TEST_USERNAME, TEST_PASSWORD);
        User secondLogin = userService.login(TEST_USERNAME, TEST_PASSWORD);

        assertNotNull(firstLogin);
        assertNotNull(secondLogin);
        assertEquals(registeredUser.getUsername(), firstLogin.getUsername());
        assertEquals(registeredUser.getUsername(), secondLogin.getUsername());
        
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.login(TEST_USERNAME, "wrongpassword"));
    }

    @Test
    @DisplayName("User Registration State Persistence")
    void testUserStatePersistence() {
    	
        
    	userService.registerUser(TEST_USERNAME, TEST_PASSWORD);
        assertTrue(userService.isUsernameTaken(TEST_USERNAME));
                
        UserService newUserService = new UserService();
        assertTrue(newUserService.isUsernameTaken(TEST_USERNAME));
        
        
        User loggedInUser = newUserService.login(TEST_USERNAME, TEST_PASSWORD);
        assertNotNull(loggedInUser);
        assertEquals(TEST_USERNAME, loggedInUser.getUsername());
    }
    
    @Test
    @DisplayName("Database State After Failed Registration")
    void testDatabaseStateAfterFailedRegistration() {
       
    	User user = userService.registerUser("testuser", "password123");
        assertNotNull(user);
        
        assertThrows(IllegalArgumentException.class, () -> 
            userService.registerUser("testuser", "newpassword")
        );
        
        assertTrue(userService.isUsernameTaken("testuser"));
        
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
        
        for (int i = 0; i < numThreads; i++) {
            User user = userService.login("user" + i, "password" + i);
            assertNotNull(user);
        }
    }
    
    @Test
    @DisplayName("Test Concurrent User Login")
    void testConcurrentUserLogin() throws InterruptedException {
      
    	userService.registerUser("concurrentuser", "userpass");
        
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successfulLogins = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    User loggedInUser = userService.login("concurrentuser", "userpass");
                    if (loggedInUser != null) {
                        successfulLogins.incrementAndGet();
                    }
                } catch (Exception e) {
        
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(numThreads, successfulLogins.get(), 
            "All concurrent logins should succeed");
    }

    
    @Test
    @DisplayName("Username Uniqueness Constraint Test")
    void testUsernameUniquenessConstraint() {
        
    	User firstUser = userService.registerUser("uniqueuser", "password123");
        assertNotNull(firstUser);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("uniqueuser", "differentpassword");
        });
        assertEquals("User already exists.", exception.getMessage());
        
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
    @DisplayName("Test User Password Update Flow")
    void testUserPasswordUpdateFlow() {
      
    	User user = userService.registerUser("passworduser", "oldpassword");
        assertNotNull(user);
        
        User loggedInUser = userService.login("passworduser", "oldpassword");
        assertNotNull(loggedInUser);
        
        assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser("passworduser", "differentpassword")
        );
        
        loggedInUser = userService.login("passworduser", "oldpassword");
        assertNotNull(loggedInUser);
        assertEquals(user.getUserid(), loggedInUser.getUserid());
    }
    
    @Test
    @DisplayName("Test User Session Persistence")
    void testUserSessionPersistence() {
       
       User user = userService.registerUser("sessionuser", "sessionpass");
        assertNotNull(user);
        
        UserService newUserService = new UserService();
        
        assertTrue(newUserService.isUsernameTaken("sessionuser"));
        
        User loggedInUser = newUserService.login("sessionuser", "sessionpass");
        assertNotNull(loggedInUser);
        assertEquals(user.getUserid(), loggedInUser.getUserid());
    }

    @Test
    @DisplayName("Test Database Constraints")
    void testDatabaseConstraints() {
        User user = userService.registerUser("constraintuser", "password123");
        assertNotNull(user);

        assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser("constraintuser", "different_password")
        );

        User loggedInUser = userService.login("constraintuser", "password123");
        assertNotNull(loggedInUser);
        assertEquals(user.getUserid(), loggedInUser.getUserid());
    }

    @Test
    @DisplayName("Test User Authentication Flow")
    void testUserAuthenticationFlow() {
       
    	User user = userService.registerUser("authuser", "password123");
        assertNotNull(user);

        User loggedInUser = userService.login("authuser", "password123");
        assertNotNull(loggedInUser);
        assertEquals(user.getUserid(), loggedInUser.getUserid());

        assertThrows(IllegalArgumentException.class, () ->
            userService.login("authuser", "wrongpassword")
        );

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