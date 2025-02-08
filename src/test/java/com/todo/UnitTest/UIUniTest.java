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
}