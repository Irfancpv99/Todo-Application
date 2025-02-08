package com.todo.e2e;

import com.todo.model.Priority;
import com.todo.model.Tags;
import com.todo.service.TodoService;
import com.todo.service.UserService;
import com.todo.ui.TodoUI;
import com.todo.ui.UI;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import com.todo.config.DatabaseConfig;

import org.junit.jupiter.api.Test;

public class TodoUIe2eTest {

    private TodoUI todoUI;
    private TodoService todoService;

    @BeforeEach
    void setUp() throws Exception {
        UserService userService = new UserService();
        int userId = userService.registerUser("TestUser", "password").getUserid();
        
        SwingUtilities.invokeAndWait(() -> {
            todoService = new TodoService();
            todoUI = new TodoUI(todoService, "TestUser", userId);
            todoUI.setVisible(true);
        });
    }
    
    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.createStatement().execute("DELETE FROM todos");
            conn.createStatement().execute("DELETE FROM users");
        }
        
        SwingUtilities.invokeAndWait(() -> {
            if (todoUI != null) todoUI.dispose();
        });
    }

    @Test
    @DisplayName("Add a New Todo")
    void testAddTodo() throws Exception {
        SwingUtilities.invokeAndWait(() -> addTestTodo("Test Todo", "Test Description", "2025-10-10", Priority.HIGH, Tags.Work));
        Thread.sleep(1000);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, getTodoTable().getRowCount(), "Todo should be added.");
        });
    }

    @Test
    @DisplayName("Update an Existing Todo")
    void testUpdateTodo() throws Exception {
        SwingUtilities.invokeAndWait(() -> addTestTodo("Original Title", "Original Desc","2025-10-10", Priority.LOW, Tags.Home));
        Thread.sleep(300);

        SwingUtilities.invokeAndWait(() -> {
            JTable todoTable = getTodoTable();
            todoTable.setRowSelectionInterval(0, 0); 
            setTodoFields("Updated Title", "Updated Desc","2025-02-10", Priority.HIGH, Tags.Urgent); // Update fields correctly
            getUpdateButton().doClick();
        });
        Thread.sleep(300);

        SwingUtilities.invokeAndWait(() -> {
            JTable todoTable = getTodoTable();
            assertEquals("Updated Title", todoTable.getValueAt(0, 1), "Title should be updated.");
            assertEquals("Updated Desc", todoTable.getValueAt(0, 2), "Description should be updated.");
            assertEquals("HIGH", todoTable.getValueAt(0, 4).toString(), "Priority should be updated.");
        });
        Thread.sleep(500);
    }

    @Test
    @DisplayName("Delete a Todo")
    void testDeleteTodo() throws Exception {
        SwingUtilities.invokeAndWait(() -> addTestTodo("Delete Test", "Test Desc","2025-10-10", Priority.MEDIUM, Tags.Work));
        Thread.sleep(300);

        SwingUtilities.invokeAndWait(() -> {
            JTable todoTable = getTodoTable();
            todoTable.setRowSelectionInterval(0, 0);
            getDeleteButton().doClick();
        });
        Thread.sleep(300);

        assertEquals(0, getTodoTable().getRowCount(), "Todo should be deleted.");
    }

    @Test
    @DisplayName("Mark Todo as Completed")
    void testMarkTodoAsCompleted() throws Exception {
        SwingUtilities.invokeAndWait(() -> addTestTodo("Complete Test", "Test Desc","2025-10-10", Priority.LOW, Tags.Home));
        Thread.sleep(300);

        SwingUtilities.invokeAndWait(() -> {
            JTable todoTable = getTodoTable();
            todoTable.setRowSelectionInterval(0, 0);
            getMarkCompletedButton().doClick();
        });
        Thread.sleep(300);

        SwingUtilities.invokeAndWait(() -> {
            JTable todoTable = getTodoTable();
            assertEquals("Completed", todoTable.getValueAt(0, 6), "Todo should be marked as completed.");
        });
    }

    @Test
    @DisplayName("Filter Todos by Priority")
    void testPriorityFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            addTestTodo("High Priority1", "Test Desc","2025-03-13", Priority.HIGH, Tags.Work);
            addTestTodo("Medium Priority", "Test Desc","2025-03-20", Priority.MEDIUM, Tags.Urgent);
            addTestTodo("Low Priority", "Test Desc","2025-06-10", Priority.LOW, Tags.Home);
            addTestTodo("High Priority2", "Test Desc","2025-04-10", Priority.HIGH, Tags.Urgent);
        });
        Thread.sleep(300);

        SwingUtilities.invokeAndWait(() -> {
            JComboBox<Priority> filterBox = getFilterPriorityComboBox();
            filterBox.setSelectedItem(Priority.HIGH);
            getApplyFilterButton().doClick();
        });
        Thread.sleep(600);

        assertEquals(2, getTodoTable().getRowCount(), "Only High Priority todos should be displayed.");
    }

    @Test
    @DisplayName("Validate Empty Fields and Invalid Dates")
    void testValidation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            setTodoFields("", "Test Desc","2025-03-10", Priority.HIGH, Tags.Work);
            getAddButton().doClick();
        });
        Thread.sleep(300);

        assertEquals(0, getTodoTable().getRowCount(), "Todo with empty title should not be added.");
        
        SwingUtilities.invokeAndWait(() -> {
            setTodoFields("title", "","2025-03-10", Priority.HIGH, Tags.Work);
            getAddButton().doClick();
        });
        Thread.sleep(300);

        assertEquals(0, getTodoTable().getRowCount(), "Todo with empty Describtion should not be added.");

        SwingUtilities.invokeAndWait(() -> {
            setTodoFields("Test", "Test Desc","2025-03-10", Priority.HIGH, Tags.Work);
            setDateField("invalid-date");
            getAddButton().doClick();
        });
        Thread.sleep(300);

        assertEquals(0, getTodoTable().getRowCount(), "Todo with invalid date should not be added.");
    }
    
    @Test
    @DisplayName("Test Logout Button")
    void testLogoutButton() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton logoutButton = findButton("Logout");
            assertNotNull(logoutButton, "Logout button should exist.");
            logoutButton.doClick();
        });

        Thread.sleep(800);

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(todoUI.isVisible(), "TodoUI should be closed after logout.");
            
            boolean loginUIVisible = false;
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof UI && frame.isVisible()) {
                    loginUIVisible = true;
                    break;
                }
            }
            assertTrue(loginUIVisible, "Login UI should be displayed after logout.");
        });
    }

    // Utility Methods
    
    private void addTestTodo(String title, String description, String dueDate, Priority priority, Tags tag) {
        setTodoFields(title, description,dueDate, priority, tag);
        todoService.setNextUserSpecificId(1);
        getAddButton().doClick();
    }

    private void setTodoFields(String title, String description,  String duedate, Priority priority, Tags tag) {
        getTitleField().setText(title);
        getDescriptionField().setText(description);
        setDateField(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        getPriorityComboBox().setSelectedItem(priority);
        getTagsComboBox().setSelectedItem(tag);
    }

    private void setDateField(String date) {
        getDateField().setText(date);
    }

    
    // Component Getters
    
    private JTextField getTitleField() {
        return (JTextField) getField("titleField");
    }

    private JTextField getDescriptionField() {
        return (JTextField) getField("descriptionField");
    }

    private JTextField getDateField() {
        return (JTextField) getField("dateField");
    }

    @SuppressWarnings("unchecked")
	private JComboBox<Priority> getPriorityComboBox() {
        return (JComboBox<Priority>) getField("priorityComboBox");
    }

    @SuppressWarnings("unchecked")
	private JComboBox<Tags> getTagsComboBox() {
        return (JComboBox<Tags>) getField("tagsComboBox");
    }

    private JTable getTodoTable() {
        return (JTable) getField("todoTable");
    }

    private JButton getAddButton() {
        return findButton("Add");
    }

    private JButton getUpdateButton() {
        return findButton("Update");
    }

    private JButton getDeleteButton() {
        return findButton("Delete");
    }

    private JButton getMarkCompletedButton() {
        return findButton("Mark Completed");
    }

    

    @SuppressWarnings("unchecked")
	private JComboBox<Priority> getFilterPriorityComboBox() {
        return (JComboBox<Priority>) getField("filterPriorityComboBox");
    }

    private JButton getApplyFilterButton() {
        return findButton("Apply Filter");
    }

    private JButton findButton(String text) {
        return findComponent(todoUI, JButton.class, text);
    }

    private <T extends Component> T findComponent(Container container, Class<T> clazz, String text) {
        for (Component comp : container.getComponents()) {
            if (clazz.isInstance(comp) && text.equals(((JButton) comp).getText())) {
                return clazz.cast(comp);
            } else if (comp instanceof Container) {
                T result = findComponent((Container) comp, clazz, text);
                if (result != null) return result;
            }
        }
        return null;
    }

    private Object getField(String fieldName) {
        try {
            var field = TodoUI.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(todoUI);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: " + fieldName, e);
        }
    }
}
