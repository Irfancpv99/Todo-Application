package com.todo.UnitTest;

import com.todo.model.Priority;
import com.todo.model.Tags;
import com.todo.model.Todo;
import com.todo.service.TodoService;
import com.todo.ui.TodoUI;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.swing.*;

import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;

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
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton markCompletedButton;

    @SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        todoUI = new TodoUI(todoService, "TestUser", 1);
        
        // Get references to UI components using reflection
        try {
            titleField = getPrivateField(todoUI, "titleField", JTextField.class);
            descriptionField = getPrivateField(todoUI, "descriptionField", JTextField.class);
            dateField = getPrivateField(todoUI, "dateField", JTextField.class);
            priorityComboBox = getPrivateField(todoUI, "priorityComboBox", JComboBox.class);
            tagsComboBox = getPrivateField(todoUI, "tagsComboBox", JComboBox.class);
            todoTable = getPrivateField(todoUI, "todoTable", JTable.class);
            tableModel = getPrivateField(todoUI, "tableModel", DefaultTableModel.class);
            addButton = findButtonByText(todoUI, "Add");
            updateButton = findButtonByText(todoUI, "Update");
            deleteButton = findButtonByText(todoUI, "Delete");
            markCompletedButton = findButtonByText(todoUI, "Mark Completed");
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
    @DisplayName("Test Add Todo When In Update Mode")
    void testAddTodoInUpdateMode() {
        setPrivateField(todoUI, "isUpdateMode", true);
        
        addButton.doClick();
        
         verify(todoService, never()).createTodo(
            anyInt(), anyInt(), anyString(), anyString(), 
            any(LocalDate.class), any(Priority.class), any(Tags.class)
        );
         setPrivateField(todoUI, "isUpdateMode", false);
    }
    
    @Test
    @DisplayName("Test Add Todo With IllegalArgumentException")
    void testAddTodoWithIllegalArgumentException() {
        titleField.setText("Test Todo");
        descriptionField.setText("Test Description");
        dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        when(todoService.createTodo(
            anyInt(), anyInt(), anyString(), anyString(), 
            any(LocalDate.class), any(Priority.class), any(Tags.class)
        )).thenThrow(new IllegalArgumentException("Test exception"));
      
        addButton.doClick();
      
        assertEquals("Test Todo", titleField.getText());
        assertEquals("Test Description", descriptionField.getText());
    }

    @Test
    @DisplayName("Test Update Todo With No Selection")
    void testUpdateTodoWithNoSelection() {
       
        todoTable.clearSelection();
        
        updateButton.doClick();
        
        verify(todoService, never()).updateTodo(
            anyInt(), anyInt(), anyString(), anyString(), 
            any(LocalDate.class), any(Priority.class), any(Tags.class), anyBoolean()
        );
    }

    @Test
    @DisplayName("Test Update Todo With DateTimeParseException")
    void testUpdateTodoWithDateTimeParseException() {
       
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", 
                LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
        setupTableWithTestData(testTodo);
        todoTable.setRowSelectionInterval(0, 0);
        
        dateField.setText("invalid-date");
      
        updateButton.doClick();
    
        verify(todoService, never()).updateTodo(
            anyInt(), anyInt(), anyString(), anyString(), 
            any(LocalDate.class), any(Priority.class), any(Tags.class), anyBoolean()
        );
    }

    @Test
    @DisplayName("Test Update Todo With IllegalArgumentException")
    void testUpdateTodoWithIllegalArgumentException() {
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", 
                LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
        setupTableWithTestData(testTodo);
        todoTable.setRowSelectionInterval(0, 0);
        
        invokePopulateFieldsFromSelectedRow();
        assertEquals("Test Todo", titleField.getText(), "Title field should be populated after manual call");
        titleField.setText("Updated Todo");
        descriptionField.setText("Updated Description");
        dateField.setText(LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        when(todoService.updateTodo(
            anyInt(), anyInt(), anyString(), anyString(), 
            any(LocalDate.class), any(Priority.class), any(Tags.class), anyBoolean()
        )).thenThrow(new IllegalArgumentException("Test exception"));
        
        updateButton.doClick();
        
        assertEquals("Updated Todo", titleField.getText(), "Fields should retain updated values after exception");
        assertEquals("Updated Description", descriptionField.getText(), "Fields should retain updated values after exception");
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
    @DisplayName("Test Delete Todo With No Selection")
    void testDeleteTodoWithNoSelection() {
        todoTable.clearSelection();
        
        deleteButton.doClick();
        verify(todoService, never()).deleteTodoById(anyInt());
    }

    @Test
    @DisplayName("Test Delete Todo Failure Case")
    void testDeleteTodoFailureCase() {
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", 
                LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
        setupTableWithTestData(testTodo);
        todoTable.setRowSelectionInterval(0, 0);
        
        when(todoService.deleteTodoById(1)).thenReturn(false);
        
        reset(todoService);
        when(todoService.deleteTodoById(1)).thenReturn(false);
        when(todoService.getTodosByUserId(anyInt())).thenReturn(List.of(testTodo));
        
        deleteButton.doClick();
        verify(todoService).deleteTodoById(1);
        assertEquals(1, todoTable.getRowCount(), "Todo should still be present after failed deletion");
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
    @DisplayName("Test Mark Todo As Completed With Null Todo")
    void testMarkTodoCompletedWithNullTodo() {
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", 
                LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
        setupTableWithTestData(testTodo);
        todoTable.setRowSelectionInterval(0, 0);
        
        when(todoService.getTodoById(1)).thenReturn(null);
        
        markCompletedButton.doClick();
        
         verify(todoService, never()).updateTodo(
            anyInt(), anyInt(), anyString(), anyString(), 
            any(LocalDate.class), any(Priority.class), any(Tags.class), eq(true)
        );
    }

    @Test
    @DisplayName("Test Mark Todo As Completed With No Selection")
    void testMarkTodoCompletedWithNoSelection() {
        todoTable.clearSelection();
    
        markCompletedButton.doClick();
        
        verify(todoService, never()).updateTodo(
            anyInt(), anyInt(), anyString(), anyString(), 
            any(LocalDate.class), any(Priority.class), any(Tags.class), anyBoolean()
        );
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
        List<Todo> todos = Arrays.asList(
            new Todo(1, 1, "High Priority", "Test", LocalDate.now(), Priority.HIGH, Tags.Work),
            new Todo(2, 1, "Low Priority", "Test", LocalDate.now(), Priority.LOW, Tags.Work)
        );
        when(todoService.getTodosByUserId(1)).thenReturn(todos);
        
        JComboBox<Priority> filterComboBox = findComboBoxByName(todoUI, "filterPriorityComboBox");
        filterComboBox.setSelectedItem(Priority.HIGH);
        findAndClickButton(todoUI, "Apply Filter");
        
        findAndClickButton(todoUI, "Clear Filter");
        
         JTable todoTable = (JTable) getPrivateField(todoUI, "todoTable");
        assertEquals(2, todoTable.getRowCount(), "All todos should be visible after clearing filter");
    }
    
    @Test
    @DisplayName("Test Clear Fields Button")
    void testClearFields() {
         JTextField titleField = (JTextField) getPrivateField(todoUI, "titleField");
        JTextField descriptionField = (JTextField) getPrivateField(todoUI, "descriptionField");
        JTextField dateField = (JTextField) getPrivateField(todoUI, "dateField");
        
        titleField.setText("Test Title");
        descriptionField.setText("Test Description");
        dateField.setText("2025-01-01");
        
        findAndClickButton(todoUI, "Clear");
        
         assertTrue(titleField.getText().isEmpty(), "Title field should be empty");
        assertTrue(descriptionField.getText().isEmpty(), "Description field should be empty");
        assertTrue(dateField.getText().isEmpty(), "Date field should be empty");
    }
    
    @Test
    @DisplayName("Test isCellEditable Method in TableModel")
    void testIsCellEditable() {
        assertFalse(tableModel.isCellEditable(0, 0), "Table cells should not be editable");
        assertFalse(tableModel.isCellEditable(0, 1), "Table cells should not be editable");
    }
    
    @Test
    @DisplayName("Date Format Validation Should Handle Invalid Inputs")
    void testDateFormatValidation() {
        record InvalidDateTest(String date, String description) {}
     
        var titleField = getPrivateField(todoUI, "titleField", JTextField.class);
        var descField = getPrivateField(todoUI, "descriptionField", JTextField.class);
        var dateField = getPrivateField(todoUI, "dateField", JTextField.class);
        var priorityBox = getPrivateField(todoUI, "priorityComboBox", JComboBox.class);
        var tagsBox = getPrivateField(todoUI, "tagsComboBox", JComboBox.class);
        
        titleField.setText("Valid Todo");
        descField.setText("Valid Description");
        dateField.setText("2025-12-31");
        priorityBox.setSelectedItem(Priority.HIGH);
        tagsBox.setSelectedItem(Tags.Work);
        
        when(todoService.createTodo(
            anyInt(), anyInt(), eq("Valid Todo"), eq("Valid Description"),
            any(LocalDate.class), eq(Priority.HIGH), eq(Tags.Work)))
        .thenReturn(new Todo(1, 1, "Valid Todo", "Valid Description", 
                           LocalDate.parse("2025-12-31"), Priority.HIGH, Tags.Work));
        
        findButtonByText(todoUI, "Add").doClick();
        
        verify(todoService).createTodo(
            anyInt(), anyInt(), eq("Valid Todo"), eq("Valid Description"),
            any(LocalDate.class), eq(Priority.HIGH), eq(Tags.Work));
        
        var invalidDates = Arrays.asList(
            new InvalidDateTest("01-01-2025", "Wrong format"),
            new InvalidDateTest("2025/01/01", "Wrong separator"),
            new InvalidDateTest("2025-13-01", "Invalid month"),
            new InvalidDateTest("2025-01-32", "Invalid day"),
            new InvalidDateTest("202S-01-01", "Non-numeric")
        );
        
         reset(todoService);
        
        for (var testCase : invalidDates) {
            
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
        
    @Test
    @DisplayName("Test List Selection Listener")
    void testListSelectionListener() {
  
        Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description",
                LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
        when(todoService.getTodosByUserId(1)).thenReturn(List.of(testTodo));
        when(todoService.getTodoById(1)).thenReturn(testTodo);

        invokeRefreshTable();
        titleField.setText("");
        descriptionField.setText("");
        dateField.setText("");
        todoTable.setRowSelectionInterval(0, 0);
        assertEquals("Test Todo", titleField.getText(), 
            "Fields should be populated when a row is selected");
        titleField.setText("");
        descriptionField.setText("");
        dateField.setText("");
        invokePopulateFieldsFromSelectedRow();
        assertEquals("Test Todo", titleField.getText(), 
            "Fields should be populated when populateFieldsFromSelectedRow is called");
        
        titleField.setText("");
        descriptionField.setText("");
        dateField.setText("");
        todoTable.clearSelection();
        
        invokePopulateFieldsFromSelectedRow();
        assertEquals("", titleField.getText(), 
        		  "Fields should remain empty when no row is selected");
    }

        @Test
        @DisplayName("Test populateFieldsFromSelectedRow With No Selection")
        void testPopulateFieldsFromSelectedRowWithNoSelection() {
            todoTable.clearSelection();
            
            invokePopulateFieldsFromSelectedRow();
            verify(todoService, never()).getTodoById(anyInt());

        }

        @Test
        @DisplayName("Test populateFieldsFromSelectedRow With Null Todo")
        void testPopulateFieldsFromSelectedRowWithNullTodo() {
            Todo testTodo = new Todo(1, 1, "Test Todo", "Test Description", 
                    LocalDate.now().plusDays(1), Priority.HIGH, Tags.Work);
            setupTableWithTestData(testTodo);
            
            todoTable.setRowSelectionInterval(0, 0);
            when(todoService.getTodoById(1)).thenReturn(null);
            
            titleField.setText("Unchanged");
            descriptionField.setText("Unchanged");
            invokePopulateFieldsFromSelectedRow();
            
            assertEquals("Unchanged", titleField.getText(), "Fields should not change if todo is null");
            assertEquals("Unchanged", descriptionField.getText(), "Fields should not change if todo is null");
        }
        
        @Test
        @DisplayName("Test main Method")
        void testMainMethod() {
            Thread mainThread = new Thread(() -> {
                TodoUI.main(new String[0]);
                 try {
                    Thread.sleep(100);
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (window instanceof TodoUI) {
                            window.dispose();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            mainThread.start();
            try {
                mainThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted");
            }
    }
    
    // Helper methods
        
    private void setupTableWithTestData(Todo todo) {
        List<Todo> todos = new ArrayList<>();
        todos.add(todo);
        when(todoService.getTodosByUserId(1)).thenReturn(todos);
        when(todoService.getTodoById(1)).thenReturn(todo);
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
    
    private void invokePopulateFieldsFromSelectedRow() {
        try {
            Method method = TodoUI.class.getDeclaredMethod("populateFieldsFromSelectedRow");
            method.setAccessible(true);
            method.invoke(todoUI);
        } catch (Exception e) {
            fail("Failed to invoke populateFieldsFromSelectedRow: " + e.getMessage());
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
    private void setPrivateField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            fail("Failed to set field: " + fieldName + ", error: " + e.getMessage());
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