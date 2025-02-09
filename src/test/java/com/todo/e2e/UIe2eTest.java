package com.todo.e2e;

import com.todo.service.UserService;
import com.todo.service.TodoService;
import com.todo.ui.*;
import com.todo.config.DatabaseConfig;
import java.sql.Connection;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UIe2eTest {

    private UI ui;
    private static final String TEST_USERNAME = "e2etestuser";
    private static final String TEST_PASSWORD = "e2etestpass123";

    @BeforeEach
    void setUp() throws Exception {
    	
    	try (Connection conn = DatabaseConfig.getConnection()) {
            conn.createStatement().execute("DELETE FROM todos");
            conn.createStatement().execute("DELETE FROM users");
        }
    	
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
        // First register a user
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });
        Thread.sleep(500);

        // Then attempt login
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getLoginButton().doClick();
        });
        Thread.sleep(500);

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(ui.isVisible(), "Login UI should be hidden after successful login.");
            boolean todoUIVisible = false;
            TodoUI todoUI = null;
            
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof TodoUI && frame.isVisible()) {
                    todoUIVisible = true;
                    todoUI = (TodoUI) frame;
                    break;
                }
            }
            assertTrue(todoUIVisible, "TodoUI should be visible after successful login.");
            
            if (todoUI != null) {
                todoUI.dispose();
            }
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
    
    @Test
    @DisplayName("Should Complete Full User Registration And Login Cycle")
    void testCompleteUserCycle() throws Exception {
        String username = "testUser";
        String password = "testPass123";

      
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(username);
            ui.getPasswordField().setText(password);
            ui.getRegisterButton().doClick();
        });
        
       
        Thread.sleep(1000);

       
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty(), "Username field should be cleared after registration");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), "Password field should be cleared after registration");
        });

       
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(username);
            ui.getPasswordField().setText(password);
            ui.getLoginButton().doClick();
        });
        
       
        Thread.sleep(1000);

       
        SwingUtilities.invokeAndWait(() -> {
            // First verify login UI is hidden
            assertFalse(ui.isVisible(), "Login UI should not be visible after successful login");
            
       
            boolean todoUIFound = false;
            TodoUI foundTodoUI = null;
            
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof TodoUI todoUI) {
                    todoUIFound = true;
                    foundTodoUI = todoUI;
                    assertTrue(frame.isVisible(), "TodoUI should be visible");
                    
                           try {
                        var field = TodoUI.class.getDeclaredField("usernameLabel");
                        field.setAccessible(true);
                        JLabel usernameLabel = (JLabel) field.get(todoUI);
                        assertTrue(usernameLabel.getText().contains(username), 
                            "Username should be displayed in TodoUI");
                    } catch (Exception e) {
                        fail("Failed to verify username display: " + e.getMessage());
                    }
                    break;
                }
            }
            
            assertTrue(todoUIFound, "TodoUI should be found among active frames");
            
                   if (foundTodoUI != null) {
                foundTodoUI.dispose();
            }
        });
    }
    
    @Test
    @DisplayName("Should Handle Concurrent User Operations")
    void testConcurrentUserOperations() throws Exception {
        // Create multiple users rapidly
        for (int i = 0; i < 5; i++) {
            final int index = i;
            SwingUtilities.invokeAndWait(() -> {
                ui.getUsernameField().setText(TEST_USERNAME + index);
                ui.getPasswordField().setText(TEST_PASSWORD);
                ui.getRegisterButton().doClick();
            });
            Thread.sleep(200);
        }

        // Try to login with each user
        for (int i = 0; i < 5; i++) {
            final int index = i;
            SwingUtilities.invokeAndWait(() -> {
                ui.getUsernameField().setText(TEST_USERNAME + index);
                ui.getPasswordField().setText(TEST_PASSWORD);
                ui.getLoginButton().doClick();
            });
            Thread.sleep(200);
        }
    }
    
    @Test
    @DisplayName("Should Handle System State Recovery")
    void testSystemStateRecovery() throws Exception {
        // Register user
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });
        Thread.sleep(500);

        // Close UI
        SwingUtilities.invokeAndWait(() -> ui.dispose());

        // Create new UI instance
        SwingUtilities.invokeAndWait(() -> {
            UserService userService = new UserService();
            TodoService todoService = new TodoService();
            ui = new UI(userService, todoService);
            ui.setVisible(true);
        });

        // Try to login with previous credentials
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getLoginButton().doClick();
        });
        Thread.sleep(500);

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(ui.isVisible());
            boolean todoUIFound = false;
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof TodoUI && frame.isVisible()) {
                    todoUIFound = true;
                    break;
                }
            }
            assertTrue(todoUIFound);
        });
    }
    
    @Test
    @DisplayName("Should Handle UI Component States")
    void testUIComponentStates() throws Exception {
        // Test initial state
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getRegisterButton().isEnabled());
            assertTrue(ui.getLoginButton().isEnabled());
            assertTrue(ui.getUsernameField().isEnabled());
            assertTrue(ui.getPasswordField().isEnabled());
            assertTrue(ui.getUsernameField().getText().isEmpty());
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
        });

        // Test state after invalid input
        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText("");
            ui.getPasswordField().setText("");
            ui.getLoginButton().doClick();
        });
        Thread.sleep(200);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getRegisterButton().isEnabled());
            assertTrue(ui.getLoginButton().isEnabled());
            assertTrue(ui.getUsernameField().isEnabled());
            assertTrue(ui.getPasswordField().isEnabled());
        });
    }
    
   
    
    
    
    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (ui != null) ui.dispose();
        });
    }
}
