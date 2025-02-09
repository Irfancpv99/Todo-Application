package com.todo.UnitTest;

import com.todo.model.User;
import com.todo.service.UserService;
import com.todo.ui.UI;
import com.todo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UIUniTest {
    private UI ui;
    private UserService userService;
    private TodoService todoService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        todoService = mock(TodoService.class);
        ui = new UI(userService, todoService);
        UserService.clearUsers();
        }

    @Test
    @DisplayName("Initial State of User Page")
    void testInitialState() {
        assertTrue(ui.getUsernameField().getText().isEmpty());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
        assertNotNull(ui.getRegisterButton());
        assertNotNull(ui.getLoginButton());
    }

    @Test
    @DisplayName("Register User Successful")
    void testSuccessfulRegistration() {
        String username = "testuser";
        String password = "testpass123";
        User mockUser = new User(1,username, password);

        when(userService.registerUser(username, password)).thenReturn(mockUser);

        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getRegisterButton().doClick();

        verify(userService).registerUser(username, password);
        assertTrue(ui.getUsernameField().getText().isEmpty());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }
    
    @Test
    @DisplayName("Register User Failed")
    void testFailedRegistration() {
        String username = "testuser";
        String password = "testpass123";
        
        when(userService.registerUser(username, password))
            .thenThrow(new IllegalArgumentException("Username already exists"));

        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getRegisterButton().doClick();

        verify(userService).registerUser(username, password);
        assertEquals(username, ui.getUsernameField().getText());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }

    @Test
    @DisplayName("User login Successful")
    void testSuccessfulLogin() {
        String username = "testuser";
        String password = "testpass123";
        User mockUser = new User(1,username, password);

        when(userService.login(username, password)).thenReturn(mockUser);

        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getLoginButton().doClick();

        verify(userService).login(username, password);
        assertTrue(ui.getUsernameField().getText().isEmpty());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }

    @Test
    @DisplayName("User login Failed")
    void testFailedLogin() {
        String username = "wronguser";
        String password = "wrongpass123";
        
        when(userService.login(username, password))
            .thenThrow(new IllegalArgumentException("Invalid username or password"));

        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getLoginButton().doClick();

        verify(userService).login(username, password);
        assertEquals(username, ui.getUsernameField().getText());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }
    
    @Test
    @DisplayName("Verify TodoUI Launch After Login")
    void testTodoUILaunch() {
        String username = "testuser";
        String password = "testpass123";
        User mockUser = new User(1,username, password);

        when(userService.login(username, password)).thenReturn(mockUser);

        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getLoginButton().doClick();

        verify(userService).login(username, password);
        assertFalse(ui.isVisible());
    }
    
    @Test
    @DisplayName("Should Clear Fields After Successful Registration")
    void testClearFieldsAfterRegistration() {
        var testUser = new User(1, "testuser", "password123");
        when(userService.registerUser("testuser", "password123")).thenReturn(testUser);

        ui.getUsernameField().setText("testuser");
        ui.getPasswordField().setText("password123");
        ui.getRegisterButton().doClick();

        assertTrue(ui.getUsernameField().getText().isEmpty());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }
    
    @Test
    @DisplayName("Should Show Error On Invalid Password")
    void testInvalidPasswordError() {
        ui.getUsernameField().setText("testuser");
        ui.getPasswordField().setText("short");
        ui.getRegisterButton().doClick();

        assertEquals("testuser", ui.getUsernameField().getText());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }
    
    @Test
    @DisplayName("Should Handle Null User Login Response")
    void testNullUserLoginResponse() {
        when(userService.login("testuser", "password123")).thenReturn(null);

        ui.getUsernameField().setText("testuser");
        ui.getPasswordField().setText("password123");
        ui.getLoginButton().doClick();

        assertEquals("testuser", ui.getUsernameField().getText());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }
    
    @Test
    @DisplayName("Should Handle Login With Special Characters")
    void testLoginWithSpecialCharacters() {
        String username = "test@user#$";
        String password = "pass@123!";
        User mockUser = new User(1, username, password);
        
        when(userService.login(username, password)).thenReturn(mockUser);

        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getLoginButton().doClick();

        verify(userService).login(username, password);
    }
    
//    @Test
//    @DisplayName("Should Handle Password With Spaces")
//    void testPasswordWithSpaces() {
//        ui.getUsernameField().setText("testuser");
//        ui.getPasswordField().setText("pass word 123");
//        ui.getRegisterButton().doClick();
//
//        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
//    }
    @Test
    @DisplayName("Should Handle Username With Only Spaces")
    void testUsernameWithOnlySpaces() {
        ui.getUsernameField().setText("   ");
        ui.getPasswordField().setText("password123");
        ui.getRegisterButton().doClick();

        assertEquals("   ", ui.getUsernameField().getText());
        assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
    }
    
    @Test
    @DisplayName("Should Handle Very Long Username")
    void testVeryLongUsername() {
        String longUsername = "a".repeat(100);
        String password = "password123";
        User mockUser = new User(1, longUsername, password);
        
        when(userService.registerUser(longUsername, password)).thenReturn(mockUser);

        ui.getUsernameField().setText(longUsername);
        ui.getPasswordField().setText(password);
        ui.getRegisterButton().doClick();

        verify(userService).registerUser(longUsername, password);
    }
    
    @Test
    @DisplayName("Should Handle Very Long Password")
    void testVeryLongPassword() {
        String username = "testuser";
        String longPassword = "a".repeat(100);
        User mockUser = new User(1, username, longPassword);
        
        when(userService.registerUser(username, longPassword)).thenReturn(mockUser);

        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(longPassword);
        ui.getRegisterButton().doClick();

        verify(userService).registerUser(username, longPassword);
    }
    
    @Test
    @DisplayName("Should Handle Rapid Multiple Login Attempts")
    void testRapidLoginAttempts() {
        String username = "testuser";
        String password = "password123";
        User mockUser = new User(1, username, password);
        
        when(userService.login(username, password))
            .thenReturn(mockUser)
            .thenThrow(new IllegalArgumentException("Too many attempts"));

        // First attempt
        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getLoginButton().doClick();

        // Second rapid attempt
        ui.getUsernameField().setText(username);
        ui.getPasswordField().setText(password);
        ui.getLoginButton().doClick();

        verify(userService, times(2)).login(username, password);
    }
    
}