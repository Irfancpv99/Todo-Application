package com.todo.e2e;

import com.todo.service.UserService;
import com.todo.service.TodoService;
import com.todo.ui.*;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UIe2eTest {

    private UI ui;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UserService userService = new UserService(); // Replace with real implementation
            TodoService todoService = new TodoService(); // Replace with real implementation
            ui = new UI(userService, todoService);
            ui.setVisible(true);
        });
    }

    @Test
    @DisplayName("Valid Registration")
    void testValidRegistration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("validUser");
            ui.getPasswordField().setText("securePass123");
            ui.getRegisterButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty(), "Username should clear after registration.");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password should clear after registration.");
        });
    }

    @Test
    @DisplayName("Registration with Blank Fields")
    void testRegistrationWithBlankFields() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("");
            ui.getPasswordField().setText("");
            ui.getRegisterButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty(), "Username field should remain empty.");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password field should remain empty.");
        });
    }

    @Test
    @DisplayName("Registration with Short Password")
    void testRegistrationWithShortPassword() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("userWithShortPass");
            ui.getPasswordField().setText("short");
            ui.getRegisterButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertEquals("userWithShortPass", ui.getUsernameField().getText(), "Username should remain after validation failure.");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password field should clear after validation failure.");
        });
    }

    @Test
    @DisplayName("Valid Login")
    void testValidLogin() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("validUser");
            ui.getPasswordField().setText("securePass123");
            ui.getLoginButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(ui.isVisible(), "Login UI should be hidden after successful login.");
            boolean todoUIVisible = false;
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof TodoUI && frame.isVisible()) {
                    todoUIVisible = true;
                    break;
                }
            }
            assertTrue(todoUIVisible, "TodoUI should be visible after successful login.");
        });
    }

    @Test
    @DisplayName("Login with Invalid Credentials")
    void testLoginWithInvalidCredentials() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("invalidUser");
            ui.getPasswordField().setText("wrongPassword");
            ui.getLoginButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertEquals("invalidUser", ui.getUsernameField().getText(), "Username should remain after failed login.");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password should clear after failed login.");
        });
    }

    @Test
    @DisplayName("Login with Blank Fields")
    void testLoginWithBlankFields() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("");
            ui.getPasswordField().setText("");
            ui.getLoginButton().doClick();
        });

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty(), "Username field should remain empty.");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password field should remain empty.");
        });
    }

    @Test
    @DisplayName("Login and Registration UI Responsiveness")
    void testRapidButtonClicks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("rapidUser");
            ui.getPasswordField().setText("rapidPass123");

            // Simulate rapid button clicks
            for (int i = 0; i < 2 ; i++) {
                ui.getRegisterButton().doClick();
                ui.getLoginButton().doClick();
            }
        });

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty(), "Username should clear after rapid interactions.");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password should clear after rapid interactions.");
        });
    }
    
    
    
    

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (ui != null) ui.dispose();
        });
    }
}
