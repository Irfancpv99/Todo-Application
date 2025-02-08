package com.todo.Integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.Frame;

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


    @AfterEach
    void tearDown() {
        SwingUtilities.invokeLater(() -> {
            if (ui != null) ui.dispose();
            if (todoUI != null) todoUI.dispose();
        });
    }
}
