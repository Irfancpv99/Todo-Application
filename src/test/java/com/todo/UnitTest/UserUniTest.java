package com.todo.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todo.model.User;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

class UserUniTest {
	private static final Set<String> registeredUsernames = new HashSet<>();

	@Test
    @DisplayName("User registration should succeed with valid username and password")
    void testRegistrationSuccess() {

        String username = "validuser";
        String password = "validPassword123";

        User user = new User(1,username, password);
        boolean isRegistered = registeredUsernames.add(username);

        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(password, user.getPassword());
        assertFalse(isRegistered);
    }
	
	@Test
    @DisplayName("User registration should fail with an empty username")
    void testRegistrationFailsWithEmptyUsername() {
		
        String username = "";
        String password = "validPassword123";

        User user = new User(1,username, password);
        
        assertFalse(user.isValid());
    }
	
	@Test
    @DisplayName("User registration should fail with a null username")
    void testRegistrationFailsWithNullUsername() {
	 
        String username = null;
        String password = "validPassword123";
      
        User user = new User(1,username, password);
       
        assertFalse(user.isValid());
    }

	
	 @Test
	    @DisplayName("User registration should fail with an empty password")
	    void testRegistrationFailsWithEmptyPassword() {
	        // Arrange
	        String username = "validuser";
	        String password = "";
	        
	        User user = new User(1,username, password);

	        assertFalse(user.isValid());
	    }
	
	 
	    @Test
	    @DisplayName("User registration should fail with a null password")
	    void testRegistrationFailsWithNullPassword() {
	        
	        String username = "validuser";
	        String password = null;
	        
	        User user = new User(1,username, password);
	        
	        assertFalse(user.isValid());
	    }
	    
	    @Test
	    @DisplayName("User registration should fail if the username already exists")
	    void testRegistrationFailsWithDuplicateUsername() {
	        String username = "duplicateuser";
//	        String password = "validPassword123";
	        registeredUsernames.add(username);

	        boolean isRegistered = registeredUsernames.add(username);

	        assertFalse(isRegistered);
	    }
	    
	    @Test
	    @DisplayName("User registration should assign a unique ID to each user")
	    void testUniqueIdAssignment() {
	  
	        User user1 = new User(1,"user1", "password1");
	        User user2 = new User(2,"user2", "password2");

	        assertNotEquals(user1.getUserid(), user2.getUserid());
	    }

	    @Test
	    @DisplayName("User login should succeed with correct username and password")
	    void testLoginSuccess() {
	    	
	        String username = "validuser";
	        String password = "validPassword123";
	        User user = new User(1,username, password);
	        registeredUsernames.add(username);

	        assertTrue(user.login(username, password));
	    }
	    
	    @Test
	    @DisplayName("User login should fail with incorrect password")
	    void testLoginFailsWithIncorrectPassword() {
	        
	        String username = "validuser";
	        String password = "validPassword123";
	        User user = new User(1,username, password);
	        registeredUsernames.add(username);

	        assertFalse(user.login(username, "wrongPassword"));
	    }

	    @Test
	    @DisplayName("User login should fail with a non-existent username")
	    void testLoginFailsWithNonExistentUsername() {

	        User user = new User(1,"validuser", "validPassword123");

	        assertFalse(user.login("nonexistentuser", "validPassword123"));
	    }

	    @Test
	    @DisplayName("User login should fail with an empty username")
	    void testLoginFailsWithEmptyUsername() {

	        User user = new User(1,"validuser", "validPassword123");

	        assertFalse(user.login("", "validPassword123"));
	    }

	    @Test
	    @DisplayName("User login should fail with an empty password")
	    void testLoginFailsWithEmptyPassword() {

	        User user = new User(1,"validuser", "validPassword123");

	        assertFalse(user.login("validuser", ""));
	    }

	    @Test
	    @DisplayName("User IDs should increment correctly for each new registration")
	    void testUserIdIncrement() {
	        // Arrange
	        User user1 = new User(1,"user1", "password1");
	        User user2 = new User(2,"user2", "password2");

	        // Assert
	        assertTrue(user2.getUserid() > user1.getUserid());
	    }
	    
}