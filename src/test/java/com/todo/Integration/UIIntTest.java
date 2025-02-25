package com.todo.Integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todo.model.User;
import com.todo.service.*;
import com.todo.ui.TodoUI;
import com.todo.ui.*;

public class UIIntTest {

    private static final String TEST_USERNAME = "integrationtest";
    private static final String TEST_PASSWORD = "testpass123";
   

    private UI ui;
    private TodoUI todoUI;
    private UserService userServiceMock;
    private TodoService todoServiceMock;
    

    @BeforeEach
    void setUp() throws Exception {
        userServiceMock = mock(UserService.class);
        todoServiceMock = mock(TodoService.class); 
        SwingUtilities.invokeAndWait(() -> {
            ui = new UI(userServiceMock, todoServiceMock);
            ui.setVisible(true);
        });
        UserService.clearUsers();
    }
    
    @AfterEach
    void tearDown() {
        SwingUtilities.invokeLater(() -> {
            if (ui != null) ui.dispose();
            if (todoUI != null) todoUI.dispose();
        });
    }
    
    @Test
    @DisplayName("Register New User Test")
    void testRegisterNewUser() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            doReturn(new User(1,TEST_USERNAME, TEST_PASSWORD)).when(userServiceMock).registerUser(TEST_USERNAME, TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty(), "Username field should be empty after registration.");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password field should be empty after registration.");
        });
    }
    
    @Test
    @DisplayName("Login with Registered User Test")
    void testLoginWithRegisteredUser() throws Exception {
        // Setup mock behavior
        doReturn(new User(1,TEST_USERNAME, TEST_PASSWORD))
            .when(userServiceMock)
            .login(TEST_USERNAME, TEST_PASSWORD);

        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getLoginButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(ui.isVisible());

            boolean todoUIFound = false;
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof TodoUI && frame.isVisible()) {
                    todoUIFound = true;
                    break;
                }
            }
            assertTrue(todoUIFound, "TodoUI should be visible after login");
        });
    }

    @Test
    @DisplayName("Login with Invalid Credentials Test")
    void testLoginWithInvalidCredentials() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText("wrongpassword");
            doThrow(new IllegalArgumentException("Invalid credentials")).when(userServiceMock).login(TEST_USERNAME, "wrongpassword");
            ui.getLoginButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            // Assert that the username is retained
            assertEquals(TEST_USERNAME, ui.getUsernameField().getText(), "Username field should retain the invalid username.");
            // Assert that the password is cleared
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password field should be empty after failed login.");
        });
    }

    @Test
    @DisplayName("Register with Empty Username Test")
    void testRegisterEmptyUsername() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("");
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty());
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
        });
    }
    
    @Test
    @DisplayName("Should Handle Sequential Registration and Login")
    void testSequentialRegistrationAndLogin() throws Exception {
        User mockUser = new User(1, TEST_USERNAME, TEST_PASSWORD);
        when(userServiceMock.registerUser(TEST_USERNAME, TEST_PASSWORD)).thenReturn(mockUser);
        when(userServiceMock.login(TEST_USERNAME, TEST_PASSWORD)).thenReturn(mockUser);

        // Register
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });
        Thread.sleep(500);

        // Login with same credentials
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getLoginButton().doClick();
        });

        verify(userServiceMock).registerUser(TEST_USERNAME, TEST_PASSWORD);
        verify(userServiceMock).login(TEST_USERNAME, TEST_PASSWORD);
    }
    
    @Test
    @DisplayName("Should Handle Registration With Existing Username")
    void testRegistrationWithExistingUsername() throws Exception {
        when(userServiceMock.registerUser(TEST_USERNAME, TEST_PASSWORD))
            .thenThrow(new IllegalArgumentException("Username already exists"));

        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(TEST_USERNAME, ui.getUsernameField().getText());
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
        });
    }
    
    @Test
    @DisplayName("Should Handle UI Component State After Failed Operations")
    void testUIComponentStateAfterFailedOperations() throws Exception {
        when(userServiceMock.registerUser(anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Registration failed"));

        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getRegisterButton().isEnabled());
            assertTrue(ui.getLoginButton().isEnabled());
            assertTrue(ui.getUsernameField().isEnabled());
            assertTrue(ui.getPasswordField().isEnabled());
        });
    }
    
    @Test
    @DisplayName("Should Handle Multiple Failed Login Attempts")
    void testMultipleFailedLoginAttempts() throws Exception {
        when(userServiceMock.login(anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid credentials"));

        for (int i = 0; i < 3; i++) {
            final String attempt = "attempt" + i;
            SwingUtilities.invokeAndWait(() -> {
                ui.getUsernameField().setText(attempt);
                ui.getPasswordField().setText(TEST_PASSWORD);
                ui.getLoginButton().doClick();
            });
            Thread.sleep(200);
        }

        verify(userServiceMock, times(3)).login(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Test launchTodoUI with null user")
    void testLaunchTodoUIWithNullUser() throws Exception {
        Method launchTodoUIMethod = UI.class.getDeclaredMethod("launchTodoUI", User.class);
        launchTodoUIMethod.setAccessible(true);
        
        Exception exception = assertThrows(InvocationTargetException.class, () -> {
            launchTodoUIMethod.invoke(ui, (User) null);
        });
        
         Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("Cannot launch TodoUI: user is null.", cause.getMessage());
    }
    
    @Test
    @DisplayName("Test validateInput with blank username")
    void testValidateInputWithBlankUsername() throws Exception {
        Method validateInputMethod = UI.class.getDeclaredMethod("validateInput", String.class, String.class);
        validateInputMethod.setAccessible(true);
        
        boolean result = (boolean) validateInputMethod.invoke(ui, "   ", "password123");
        assertFalse(result, "Should return false for blank username");
    }
    
    @Test
    @DisplayName("Test validateInput with blank password")
    void testValidateInputWithBlankPassword() throws Exception {
        Method validateInputMethod = UI.class.getDeclaredMethod("validateInput", String.class, String.class);
        validateInputMethod.setAccessible(true);
        
        boolean result = (boolean) validateInputMethod.invoke(ui, "username", "   ");
        assertFalse(result, "Should return false for blank password");
    }
    
    @Test
    @DisplayName("Test validateInput with both fields blank")
    void testValidateInputWithBothFieldsBlank() throws Exception {
        Method validateInputMethod = UI.class.getDeclaredMethod("validateInput", String.class, String.class);
        validateInputMethod.setAccessible(true);
        
        boolean result = (boolean) validateInputMethod.invoke(ui, "   ", "   ");
        assertFalse(result, "Should return false when both fields are blank");
    }
    
}
