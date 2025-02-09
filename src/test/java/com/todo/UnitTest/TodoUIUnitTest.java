package com.todo.UnitTest;

import com.todo.model.*;
import com.todo.model.Tags;
import com.todo.service.TodoService;
import com.todo.ui.TodoUI;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.swing.*;

import java.awt.Container;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;



import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TodoUIUnitTest {
    private TodoUI todoUI;
    
    @Mock
    private TodoService todoService;
    
    private AutoCloseable closeable;
    private JTextField titleField;
    private JTextField descriptionField;
    private JTextField dateField;
    private JComboBox<Priority> priorityComboBox;
    private JComboBox<Tags> tagsComboBox;
    private JTable todoTable;

    @SuppressWarnings("unchecked")
	@BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        todoUI = new TodoUI(todoService, "TestUser",1);
        
        // Get references to UI components using reflection
        try {
            java.lang.reflect.Field titleFieldField = TodoUI.class.getDeclaredField("titleField");
            titleFieldField.setAccessible(true);
            titleField = (JTextField) titleFieldField.get(todoUI);

            java.lang.reflect.Field descFieldField = TodoUI.class.getDeclaredField("descriptionField");
            descFieldField.setAccessible(true);
            descriptionField = (JTextField) descFieldField.get(todoUI);

            java.lang.reflect.Field dateFieldField = TodoUI.class.getDeclaredField("dateField");
            dateFieldField.setAccessible(true);
            dateField = (JTextField) dateFieldField.get(todoUI);

            java.lang.reflect.Field priorityField = TodoUI.class.getDeclaredField("priorityComboBox");
            priorityField.setAccessible(true);
            priorityComboBox = (JComboBox<Priority>) priorityField.get(todoUI);

            java.lang.reflect.Field tagsField = TodoUI.class.getDeclaredField("tagsComboBox");
            tagsField.setAccessible(true);
            tagsComboBox = (JComboBox<Tags>) tagsField.get(todoUI);

            java.lang.reflect.Field tableField = TodoUI.class.getDeclaredField("todoTable");
            tableField.setAccessible(true);
            todoTable = (JTable) tableField.get(todoUI);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        todoUI.dispose();
    }

    @Test
    @DisplayName("Initail Stage of Todo UI")
    void testInitialUIState() {
        assertNotNull(titleField, "Title field should be initialized");
        assertNotNull(descriptionField, "Description field should be initialized");
        assertNotNull(dateField, "Date field should be initialized");
        assertNotNull(priorityComboBox, "Priority combo box should be initialized");
        assertNotNull(tagsComboBox, "Tags combo box should be initialized");
        assertNotNull(todoTable, "Todo table should be initialized");
        
        assertEquals("", titleField.getText(), "Title field should be empty initially");
        assertEquals("", descriptionField.getText(), "Description field should be empty initially");
        assertEquals("", dateField.getText(), "Date field should be empty initially");
    }

    @Test
    @DisplayName("Todo add Successful")
    void testAddTodoSuccess() {

        String title = "Test Todo";
        String description = "Test Description";
        String date = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        titleField.setText(title);
        descriptionField.setText(description);
        dateField.setText(date);
        priorityComboBox.setSelectedItem(Priority.HIGH);
        tagsComboBox.setSelectedItem(Tags.Work);
        
        when(todoService.createTodo(anyInt(), anyInt(), eq(title), eq(description), 
             any(LocalDate.class), eq(Priority.HIGH), eq(Tags.Work)))
            .thenReturn(new Todo(1, 1, title, description, LocalDate.parse(date), Priority.HIGH, Tags.Work));
        
        findAndClickButton(todoUI, "Add");

        verify(todoService).createTodo(anyInt(), anyInt(), eq(title), eq(description), 
                any(LocalDate.class), eq(Priority.HIGH), eq(Tags.Work));

        assertEquals("", titleField.getText());
        assertEquals("", descriptionField.getText());
        assertEquals("", dateField.getText());
    }

    @Test
    @DisplayName("Adding Todo with invalid Date")
    void testAddTodoWithInvalidDate() {
        titleField.setText("Test Todo");
        descriptionField.setText("Test Description");
        dateField.setText("invalid-date");

        findAndClickButton(todoUI, "Add");

        verify(todoService, never()).createTodo(anyInt(), anyInt(), anyString(), anyString(), 
                any(LocalDate.class), any(Priority.class), any(Tags.class));
    }

    @Test
    @DisplayName("Update Todo")
    void testUpdateTodoSuccess() {
      
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
                
        when(todoService.getTodoById(1)).thenReturn(testTodo);
        setupTableWithTestData(testTodo);
        todoTable.setRowSelectionInterval(0, 0);

        titleField.setText("Updated Todo");
        descriptionField.setText("Updated Description");
        dateField.setText(LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        findAndClickButton(todoUI, "Update");
        
        verify(todoService).updateTodo(eq(1), anyInt(), eq("Updated Todo"), eq("Updated Description"), 
                any(LocalDate.class), any(Priority.class), any(Tags.class), eq(false));
    }

    @Test
    @DisplayName("Delete Todo")
    void testDeleteTodo() {
   
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
        
        when(todoService.deleteTodoById(1)).thenReturn(true);
        setupTableWithTestData(testTodo);
        todoTable.setRowSelectionInterval(0, 0);
        findAndClickButton(todoUI, "Delete");
        verify(todoService).deleteTodoById(1);
    }

    @Test
    @DisplayName("Mark Todo As Completed")
    void testMarkTodoCompleted() {
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", 
                LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
        when(todoService.getTodoById(1)).thenReturn(testTodo);

        setupTableWithTestData(testTodo);
        todoTable.setRowSelectionInterval(0, 0);
        findAndClickButton(todoUI, "Mark Completed");
        verify(todoService).updateTodo(eq(1), anyInt(), anyString(), anyString(), 
                any(LocalDate.class), any(Priority.class), any(Tags.class), eq(true));
    }

    @Test
    @DisplayName("Filter Todo By Priority")
    void testFilterByPriority() {
        List<Todo> todos = new ArrayList<>();
        todos.add(new Todo(1, 1, "High Priority", "Test", LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work));
        todos.add(new Todo(2, 1, "Low Priority", "Test", LocalDate.now().plusDays(1), Priority.LOW, Tags.Work));
        
        when(todoService.getTodosByUserId(1)).thenReturn(todos);
        
        invokeRefreshTable();

        JComboBox<Priority> filterComboBox = findComboBoxByName(todoUI, "filterPriorityComboBox");
        filterComboBox.setSelectedItem(Priority.HIGH);

        findAndClickButton(todoUI, "Apply Filter");     
        assertEquals(1, todoTable.getRowCount());
        assertEquals("High Priority", todoTable.getValueAt(0, 1));
    }
    
    
    @Test
    @DisplayName("Test Clear Filter Button")
    void testClearFilter() {
        // Setup initial todos
        List<Todo> todos = Arrays.asList(
            new Todo(1, 1, "High Priority", "Test", LocalDate.now(), Priority.HIGH, Tags.Work),
            new Todo(2, 1, "Low Priority", "Test", LocalDate.now(), Priority.LOW, Tags.Work)
        );
        when(todoService.getTodosByUserId(1)).thenReturn(todos);
        
        // Apply filter first
        JComboBox<Priority> filterComboBox = findComboBoxByName(todoUI, "filterPriorityComboBox");
        filterComboBox.setSelectedItem(Priority.HIGH);
        findAndClickButton(todoUI, "Apply Filter");
        
        // Clear filter
        findAndClickButton(todoUI, "Clear Filter");
        
        // Verify table shows all todos
        JTable todoTable = (JTable) getPrivateField(todoUI, "todoTable");
        assertEquals(2, todoTable.getRowCount(), "All todos should be visible after clearing filter");
    }
    
    @Test
    @DisplayName("Test Clear Fields Button")
    void testClearFields() {
        // Set field values
        JTextField titleField = (JTextField) getPrivateField(todoUI, "titleField");
        JTextField descriptionField = (JTextField) getPrivateField(todoUI, "descriptionField");
        JTextField dateField = (JTextField) getPrivateField(todoUI, "dateField");
        
        titleField.setText("Test Title");
        descriptionField.setText("Test Description");
        dateField.setText("2025-01-01");
        
        // Click clear button
        findAndClickButton(todoUI, "Clear");
        
        // Verify fields are cleared
        assertTrue(titleField.getText().isEmpty(), "Title field should be empty");
        assertTrue(descriptionField.getText().isEmpty(), "Description field should be empty");
        assertTrue(dateField.getText().isEmpty(), "Date field should be empty");
    }
    
    @Test
    @DisplayName("Date Format Validation Should Handle Invalid Inputs")
    void testDateFormatValidation() {
        record InvalidDateTest(String date, String description) {}
        
        // First add a valid todo
        var titleField = getPrivateField(todoUI, "titleField", JTextField.class);
        var descField = getPrivateField(todoUI, "descriptionField", JTextField.class);
        var dateField = getPrivateField(todoUI, "dateField", JTextField.class);
        var priorityBox = getPrivateField(todoUI, "priorityComboBox", JComboBox.class);
        var tagsBox = getPrivateField(todoUI, "tagsComboBox", JComboBox.class);
        
        // Set valid values
        titleField.setText("Valid Todo");
        descField.setText("Valid Description");
        dateField.setText("2025-12-31");
        priorityBox.setSelectedItem(Priority.HIGH);
        tagsBox.setSelectedItem(Tags.Work);
        
        // Add valid todo
        when(todoService.createTodo(
            anyInt(), anyInt(), eq("Valid Todo"), eq("Valid Description"),
            any(LocalDate.class), eq(Priority.HIGH), eq(Tags.Work)))
        .thenReturn(new Todo(1, 1, "Valid Todo", "Valid Description", 
                           LocalDate.parse("2025-12-31"), Priority.HIGH, Tags.Work));
        
        findButtonByText(todoUI, "Add").doClick();
        
        // Verify valid todo was added
        verify(todoService).createTodo(
            anyInt(), anyInt(), eq("Valid Todo"), eq("Valid Description"),
            any(LocalDate.class), eq(Priority.HIGH), eq(Tags.Work));
        
        // Now test invalid dates
        var invalidDates = Arrays.asList(
            new InvalidDateTest("01-01-2025", "Wrong format"),
            new InvalidDateTest("2025/01/01", "Wrong separator"),
            new InvalidDateTest("2025-13-01", "Invalid month"),
            new InvalidDateTest("2025-01-32", "Invalid day"),
            new InvalidDateTest("202S-01-01", "Non-numeric")
        );
        
        // Reset verification counts after valid todo
        reset(todoService);
        
        for (var testCase : invalidDates) {
            // Set new invalid date while keeping other fields valid
            titleField.setText("Test Todo");
            descField.setText("Test Description");
            dateField.setText(testCase.date());
            priorityBox.setSelectedItem(Priority.HIGH);
            tagsBox.setSelectedItem(Tags.Work);
            
            findButtonByText(todoUI, "Add").doClick();
            
            verify(todoService, never()).createTodo(
                anyInt(), anyInt(), anyString(), anyString(), 
                any(LocalDate.class), any(Priority.class), any(Tags.class)
            );
        }
    }
    

    
    

    // Helper methods
    private void setupTableWithTestData(Todo todo) {
        List<Todo> todos = new ArrayList<>();
        todos.add(todo);
        when(todoService.getTodosByUserId(1)).thenReturn(todos);
        invokeRefreshTable();
    }
    
    

    private void invokeRefreshTable() {
        try {
            java.lang.reflect.Method refreshMethod = TodoUI.class.getDeclaredMethod("refreshTable");
            refreshMethod.setAccessible(true);
            refreshMethod.invoke(todoUI);
        } catch (Exception e) {
            fail("Failed to invoke refreshTable: " + e.getMessage());
        }
    }

    private void findAndClickButton(Container container, String text) {
        JButton button = findButtonByText(container, text);
        assertNotNull(button, "Button with text '" + text + "' not found");
        button.doClick();
    }

    private JButton findButtonByText(Container container, String text) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof JButton && text.equals(((JButton) comp).getText())) {
                return (JButton) comp;
            } else if (comp instanceof Container) {
                JButton button = findButtonByText((Container) comp, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }
    
    private Object getPrivateField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            fail("Failed to get field: " + fieldName);
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

    @SuppressWarnings("unchecked")
    private JComboBox<Priority> findComboBoxByName(Container container, String name) {
        try {
            java.lang.reflect.Field field = TodoUI.class.getDeclaredField(name);
            field.setAccessible(true);
            return (JComboBox<Priority>) field.get(todoUI);
        } catch (Exception e) {
            fail("Failed to find combo box: " + e.getMessage());
            return null;
        }
    }
}