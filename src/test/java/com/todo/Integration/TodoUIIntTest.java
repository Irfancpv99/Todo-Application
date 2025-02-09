package com.todo.Integration;

import com.todo.model.Priority;
import com.todo.model.Tags;
import com.todo.ui.TodoUI;
import com.todo.model.User;
import com.todo.service.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

class TodoUIIntTest
 {
    private TodoUI todoUI;
    private TodoService todoService;
    private UserService userService;
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private int userId;
    
    // UI Components
    protected  JTextField titleField;
    protected  JTextField descriptionField;
    protected  JTextField dateField;
    protected  JComboBox<Priority> priorityComboBox;
    protected  JComboBox<Tags> tagsComboBox;
    private JTable todoTable;
    protected  JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton markCompletedButton;

    @BeforeEach
    void setUp() {
        try {
        	
        	 UserService.clearUsers();
             Thread.sleep(1000);
        	
            SwingUtilities.invokeAndWait(() -> {
                todoService = new TodoService();
                userService = new UserService();
                
                // Create test user and get userId
                User testUser = userService.registerUser(TEST_USERNAME, TEST_PASSWORD);
                userId = testUser.getUserid();
                todoUI = new TodoUI(todoService, TEST_USERNAME, userId);
                initializeComponents();
                todoUI.setVisible(true);
            });
            Thread.sleep(1000);// Allow UI to stabilize
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
    }
    
    @AfterEach
    void tearDown() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (todoUI != null) {
                    todoUI.dispose();
                }
                // Clean up test data
                UserService.clearUsers();
            });
        } catch (Exception e) {
            fail("Teardown failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeComponents() {
        try {
            titleField = (JTextField) getPrivateField("titleField");
            descriptionField = (JTextField) getPrivateField("descriptionField");
            dateField = (JTextField) getPrivateField("dateField");
            priorityComboBox = (JComboBox<Priority>) getPrivateField("priorityComboBox");
            tagsComboBox = (JComboBox<Tags>) getPrivateField("tagsComboBox");
            todoTable = (JTable) getPrivateField("todoTable");
            addButton = findButton("Add");
            updateButton = findButton("Update");
            deleteButton = findButton("Delete");
            markCompletedButton = findButton("Mark Completed");
        } catch (Exception e) {
            fail("Failed to initialize components: " + e.getMessage());
        }
    }

    private Object getPrivateField(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = TodoUI.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(todoUI);
    }
    
    
    void testFullTodoCycle() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                titleField.setText("Integration Test Todo");
                descriptionField.setText("Testing full cycle");
                dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                priorityComboBox.setSelectedItem(Priority.HIGH);
                tagsComboBox.setSelectedItem(Tags.Work);
            });

            // Increased wait time for UI update
            Thread.sleep(500);

            SwingUtilities.invokeAndWait(() -> addButton.doClick());
            Thread.sleep(1000);  // Increased wait time for add operation

            SwingUtilities.invokeAndWait(() -> {
                assertTrue(todoTable.getRowCount() > 0, "Todo should be added");
                assertEquals("Integration Test Todo", todoTable.getValueAt(0, 1));
                assertEquals("Testing full cycle", todoTable.getValueAt(0, 2));
            });
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    
    
    @Test
    void testMultipleTodoOperations() {
        try {
            addTestTodo("First Todo", "Description 1", Priority.HIGH, Tags.Work);
            addTestTodo("Second Todo", "Description 2", Priority.MEDIUM, Tags.Home);
            addTestTodo("Third Todo", "Description 3", Priority.LOW, Tags.Urgent);
            
            Thread.sleep(500);
            
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(3, todoTable.getRowCount(), "All todos should be added");
                
                JComboBox<Priority> filterComboBox = findFilterPriorityComboBox();
                filterComboBox.setSelectedItem(Priority.HIGH);
                findAndClickButton("Apply Filter");
            });
            
            Thread.sleep(500);
            
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(1, todoTable.getRowCount(), "Filter should show only HIGH priority todos");
                assertEquals("First Todo", todoTable.getValueAt(0, 1), "First todo should be visible");
                
                findAndClickButton("Clear Filter");
            });
            
            Thread.sleep(500);
            
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(3, todoTable.getRowCount(), "All todos should be visible after clearing filter");
            });
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    void testUpdateMultipleFields() {
        try {
            final CountDownLatch addLatch = new CountDownLatch(1);
            final CountDownLatch updateLatch = new CountDownLatch(1);

            SwingUtilities.invokeLater(() -> {
                try {
                    titleField.setText("Original Title");
                    descriptionField.setText("Original Description");
                    dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    priorityComboBox.setSelectedItem(Priority.LOW);
                    tagsComboBox.setSelectedItem(Tags.Work);
                    addButton.doClick();
                    addLatch.countDown();
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            });

            assertTrue(addLatch.await(5, TimeUnit.SECONDS));
            Thread.sleep(1000);

            SwingUtilities.invokeLater(() -> {
                try {
                    todoTable.setRowSelectionInterval(0, 0);
                    titleField.setText("Updated Title");
                    descriptionField.setText("Updated Description");
                    dateField.setText(LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    priorityComboBox.setSelectedItem(Priority.HIGH);
                    updateButton.doClick();
                    updateLatch.countDown();
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            });

            assertTrue(updateLatch.await(5, TimeUnit.SECONDS));
            Thread.sleep(1000);

            SwingUtilities.invokeAndWait(() -> {
                assertEquals("Updated Title", todoTable.getValueAt(0, 1));
                assertEquals("Updated Description", todoTable.getValueAt(0, 2));
                assertEquals(Priority.HIGH, todoTable.getValueAt(0, 4));
            });
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    void testTodoValidation() {
        try {
            // Test empty title
            SwingUtilities.invokeAndWait(() -> {
                titleField.setText("");
                descriptionField.setText("Valid Description");
                dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                addButton.doClick();
            });
            Thread.sleep(500);
            
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(0, todoTable.getRowCount(), "Todo with empty title should not be added");
            });

            // Test invalid date
            SwingUtilities.invokeAndWait(() -> {
                titleField.setText("Valid Title");
                dateField.setText("invalid-date");
                addButton.doClick();
            });
            Thread.sleep(500);
            
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(0, todoTable.getRowCount(), "Todo with invalid date should not be added");
            });
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Todo User Interface validation")
    void testUserInterfaceIntegration() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JLabel usernameLabel = findUsernameLabel();
                assertNotNull(usernameLabel, "Username label was not found.");
                assertTrue(usernameLabel.getText().contains(TEST_USERNAME), 
                    "Username label text is incorrect.");

                assertNotNull(addButton, "Add button was not found.");
                assertNotNull(updateButton, "Update button was not found.");
                assertNotNull(deleteButton, "Delete button was not found.");
                assertNotNull(markCompletedButton, "Mark Completed button was not found.");

                assertTrue(addButton.isEnabled(), "Add button should be enabled.");
                assertTrue(updateButton.isEnabled(), "Update button should be enabled.");
                assertTrue(deleteButton.isEnabled(), "Delete button should be enabled.");
                assertTrue(markCompletedButton.isEnabled(), "Mark Completed button should be enabled.");
            });
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    void testMarkTodoAsCompleted() {
        try {
            final CountDownLatch addLatch = new CountDownLatch(1);
            final CountDownLatch completeLatch = new CountDownLatch(1);

            // Use invokeAndWait instead of invokeLater for the first operation
            SwingUtilities.invokeAndWait(() -> {
                titleField.setText("Test Mark Completed");
                descriptionField.setText("Testing mark completed functionality");
                dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                priorityComboBox.setSelectedItem(Priority.MEDIUM);
                tagsComboBox.setSelectedItem(Tags.Home);
                addButton.doClick();
                addLatch.countDown();
            });

            assertTrue(addLatch.await(5, TimeUnit.SECONDS));
            // Increase wait time to ensure DB operation completes
            Thread.sleep(2000);

            // Use invokeAndWait for the second operation as well
            SwingUtilities.invokeAndWait(() -> {
                todoTable.setRowSelectionInterval(0, 0);
                markCompletedButton.doClick();
                completeLatch.countDown();
            });

            // Increase timeout
            assertTrue(completeLatch.await(10, TimeUnit.SECONDS), "Operation timed out");
            Thread.sleep(1000);

            SwingUtilities.invokeAndWait(() -> {
                assertEquals("Completed", todoTable.getValueAt(0, 6));
            });
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    void testConcurrentTodoOperations() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (int i = 0; i < 5; i++) {
                    titleField.setText("Concurrent Todo " + i);
                    descriptionField.setText("Description " + i);
                    dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    priorityComboBox.setSelectedItem(Priority.HIGH);
                    tagsComboBox.setSelectedItem(Tags.Work);
                    addButton.doClick();
                    try {
                        Thread.sleep(200); // Small delay between adds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            Thread.sleep(1000); // Wait for all DB operations
            
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(5, todoTable.getRowCount(), "All todos should be added");
            });
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Clear Fields Should Reset All Input Fields")
    void testClearFields() {
        // Set field values
        var titleField = getPrivateField(todoUI, "titleField", JTextField.class);
        var descriptionField = getPrivateField(todoUI, "descriptionField", JTextField.class);
        var dateField = getPrivateField(todoUI, "dateField", JTextField.class);
        
        titleField.setText("Test Title");
        descriptionField.setText("Test Description");
        dateField.setText("2025-01-01");
        
        // Click clear button
        findButtonByText(todoUI, "Clear").doClick();
        
        // Verify fields are cleared
        assertTrue(titleField.getText().isEmpty(), "Title field should be empty");
        assertTrue(descriptionField.getText().isEmpty(), "Description field should be empty");
        assertTrue(dateField.getText().isEmpty(), "Date field should be empty");
    }
    
    
    
    private void addTestTodo(String title, String description, Priority priority, Tags tag) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                titleField.setText(title);
                descriptionField.setText(description);
                dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                priorityComboBox.setSelectedItem(priority);
                tagsComboBox.setSelectedItem(tag);
                todoService.setNextUserSpecificId(1);
                addButton.doClick();
            });
            Thread.sleep(500); // Wait for DB operation
        } catch (Exception e) {
            fail("Failed to add test todo: " + e.getMessage());
        }
    }
    
    

    private JButton findButton(String text) {
        return findButtonInContainer(todoUI, text);
    }

    private JButton findButtonInContainer(Container container, String text) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton && text.equals(((JButton) comp).getText())) {
                return (JButton) comp;
            } else if (comp instanceof Container) {
                JButton button = findButtonInContainer((Container) comp, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private void findAndClickButton(String text) {
        JButton button = findButton(text);
        assertNotNull(button, "Button not found: " + text);
        button.doClick();
    }

    @SuppressWarnings("unchecked")
    private JComboBox<Priority> findFilterPriorityComboBox() {
        try {
            return (JComboBox<Priority>) getPrivateField("filterPriorityComboBox");
        } catch (Exception e) {
            fail("Failed to find filter priority combo box: " + e.getMessage());
            return null;
        }
    }

    private JLabel findUsernameLabel() {
        try {
            var field = TodoUI.class.getDeclaredField("usernameLabel");
            field.setAccessible(true);
            return (JLabel) field.get(todoUI);
        } catch (Exception e) {
            fail("Failed to find username label: " + e.getMessage());
            return null;
        }
    }
    
    private <T> T getPrivateField(Object obj, String fieldName, Class<T> fieldType) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(obj));
        } catch (Exception e) {
            fail("Failed to get field: " + fieldName + ", error: " + e.getMessage());
            return null;
        }
    }
    
    private JButton findButtonByText(Container container, String text) {
        for (var comp : container.getComponents()) {
            if (comp instanceof JButton button && text.equals(button.getText())) {
                return button;
            } else if (comp instanceof Container cont) {
                var found = findButtonByText(cont, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    
    
}