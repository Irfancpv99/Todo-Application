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
        // Clean database first
        cleanDatabase();

        SwingUtilities.invokeAndWait(() -> {
            UserService userService = new UserService();
            TodoService todoService = new TodoService();
            ui = new UI(userService, todoService);
            ui.setVisible(true);
        });
        Thread.sleep(500); 
    }
    

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            
            for (Window window : Window.getWindows()) {
                window.dispose();
            }
        });
        cleanDatabase();
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

    	cleanDatabase();
        Thread.sleep(500);

        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });
        Thread.sleep(1000);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getUsernameField().getText().isEmpty(), 
                "Username field should be cleared after registration");
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty(), 
                "Password field should be cleared after registration");
        });

        SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getLoginButton().doClick();
        });
        Thread.sleep(1000); 
        
        SwingUtilities.invokeAndWait(() -> {
        
        	assertFalse(ui.isVisible(), "Login UI should be hidden");

        	TodoUI foundTodoUI = null;
            for (Window window : Window.getWindows()) {
                if (window instanceof TodoUI && window.isDisplayable()) {
                    foundTodoUI = (TodoUI) window;
                    break;
                }
            }

            assertNotNull(foundTodoUI, "TodoUI instance should exist");
            assertTrue(foundTodoUI.isDisplayable(), "TodoUI should be displayable");
            assertTrue(foundTodoUI.isVisible(), "TodoUI should be visible");

            if (foundTodoUI != null) {
                try {
                    var field = TodoUI.class.getDeclaredField("usernameLabel");
                    field.setAccessible(true);
                    JLabel usernameLabel = (JLabel) field.get(foundTodoUI);
                    String labelText = usernameLabel.getText();
                    assertTrue(labelText.contains(TEST_USERNAME), 
                        "Username '" + TEST_USERNAME + "' should be in label text: '" + labelText + "'");
                } catch (Exception e) {
                    fail("Failed to verify username label: " + e.getMessage());
                }
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
    	
    	SwingUtilities.invokeAndWait(() -> {
            ui.getUsernameField().setText(TEST_USERNAME);
            ui.getPasswordField().setText(TEST_PASSWORD);
            ui.getRegisterButton().doClick();
        });
        Thread.sleep(500);

        SwingUtilities.invokeAndWait(() -> ui.dispose());

        SwingUtilities.invokeAndWait(() -> {
            UserService userService = new UserService();
            TodoService todoService = new TodoService();
            ui = new UI(userService, todoService);
            ui.setVisible(true);
        });

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
       
       SwingUtilities.invokeAndWait(() -> {
            assertTrue(ui.getRegisterButton().isEnabled());
            assertTrue(ui.getLoginButton().isEnabled());
            assertTrue(ui.getUsernameField().isEnabled());
            assertTrue(ui.getPasswordField().isEnabled());
            assertTrue(ui.getUsernameField().getText().isEmpty());
            assertTrue(new String(ui.getPasswordField().getPassword()).isEmpty());
        });

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
    
    @Test
    @DisplayName("Test main method")
    void testMainMethod() throws Exception {
        Thread mainThread = new Thread(() -> {
            UI.main(new String[]{});
        });
        
        mainThread.start();
        Thread.sleep(500);
        
        boolean uiWindowFound = false;
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            if (window instanceof UI) {
                uiWindowFound = true;
                window.dispose(); 
                break;
            }
        }
        
        assertTrue(uiWindowFound, "UI window should be created by main method");
        
        mainThread.interrupt();
    }
    
    private void cleanDatabase() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.createStatement().execute("DELETE FROM todos");
            conn.createStatement().execute("DELETE FROM users");
        } catch (Exception e) {
            System.err.println("Failed to clean database: " + e.getMessage());
        }
    }
}
