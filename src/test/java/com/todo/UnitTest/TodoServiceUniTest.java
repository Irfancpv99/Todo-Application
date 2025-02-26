package com.todo.UnitTest;

import com.todo.model.Todo;
import com.todo.service.TodoService;
import com.todo.model.Priority;
import com.todo.model.Tags;
import com.todo.model.Status;
import com.todo.config.DatabaseConfig;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



class TodoServiceUniTest {
    private TodoService todoService;
    private static final int TEST_USER_ID = 1;

    @BeforeEach
    void setUp() {
        todoService = new TodoService();
        setupTestDatabase();
    }    
    
     @AfterEach
      void tearDown() {
            cleanupTestDatabase();
        }

    
     private void setupTestDatabase() {
    	    try (Connection conn = DatabaseConfig.getConnection();
    	         Statement stmt = conn.createStatement()) {
    	        stmt.execute("DELETE FROM todos");
    	        stmt.execute("DELETE FROM users");
    	        stmt.execute("INSERT INTO users (id, username, password) VALUES (" + 
    	            TEST_USER_ID + ", 'testuser', 'testpass')");
    	    } catch (Exception e) {
    	        fail("Database setup failed: " + e.getMessage());
    	    }
    	}
    
    private void cleanupTestDatabase() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM todos");
        } catch (Exception e) {
            fail("Database cleanup failed: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Create and Retrieve Todo")
    void testCreateAndRetrieveTodo() {
        Todo createdTodo = todoService.createTodo(1, TEST_USER_ID, "Task 1", "Description", 
            LocalDate.now().plusDays(1), Priority.MEDIUM, Tags.Work);
        createdTodo.setUserSpecificId(1); 
        
        Todo retrievedTodo = todoService.getTodoById(createdTodo.getId());
        assertNotNull(retrievedTodo);
        assertEquals("Task 1", retrievedTodo.getTitle());

    }
    
    @Test
    @DisplayName("Update Todo")
    void testUpdateTodo() {
        Todo todo = todoService.createTodo(1, TEST_USER_ID, "Task 1", "Description", 
            LocalDate.now().plusDays(1), Priority.MEDIUM, Tags.Work);
        
        todoService.updateTodo(todo.getId(), TEST_USER_ID, "Updated Task", "Updated Description", 
            LocalDate.now().plusDays(2), Priority.HIGH, Tags.Home, true);
        
        Todo updatedTodo = todoService.getTodoById(todo.getId());
        assertNotNull(updatedTodo);
        assertEquals("Updated Task", updatedTodo.getTitle());
        assertTrue(updatedTodo.isCompleted());
    }
    
    @Test
    @DisplayName("Partial Updation of Todo")
    void testUpdateTodoPartialFields() {
        Todo todo = todoService.createTodo(1, 1, "Original Task", "Original Description", 
            LocalDate.now().plusDays(1), Priority.MEDIUM, Tags.Urgent);
        
        LocalDate originalDueDate = todo.getDueDate();
        todoService.updateTodo(todo.getId(), 1, "Updated Task", todo.getDescription(), 
            originalDueDate, Priority.HIGH, todo.getTags(), todo.isCompleted());
        
        Todo updatedTodo = todoService.getTodoById(todo.getId());
        assertEquals("Updated Task", updatedTodo.getTitle());
        assertEquals(Priority.HIGH, updatedTodo.getPriority());
        assertEquals("Original Description", updatedTodo.getDescription());
        assertEquals(originalDueDate, updatedTodo.getDueDate());
        assertEquals(Tags.Urgent, updatedTodo.getTags());
    }
    
    @Test
    @DisplayName("Test Next User Specific ID Management")
    void testNextUserSpecificId() {
        TodoService todoService = new TodoService();
        
        
        assertEquals(1, todoService.getNextUserSpecificId());
        
        
        assertEquals(2, todoService.getNextUserSpecificId());
        assertEquals(3, todoService.getNextUserSpecificId());
        
        
        todoService.setNextUserSpecificId(10);
        assertEquals(10, todoService.getNextUserSpecificId());
        assertEquals(11, todoService.getNextUserSpecificId());
    }
    
    @Test
    @DisplayName("Test Transaction Rollback on Error")
    void testTransactionRollback() {

        assertThrows(RuntimeException.class, () -> {
            todoService.createTodo(
                1,
                -1, 
                "Test Todo",
                "Description",
                LocalDate.now(),
                Priority.LOW,
                Tags.Work
            );
        });
        
      
        List<Todo> todos = todoService.getTodosByUserId(TEST_USER_ID);
        assertTrue(todos.isEmpty());
    }
    
    @Test
    @DisplayName("Delete Todo")
    void testDeleteTodo() {
        Todo todo = todoService.createTodo(1, TEST_USER_ID, "Task to Delete", "Description", 
            LocalDate.now().plusDays(1), Priority.LOW, Tags.Work);
        
        boolean deleted = todoService.deleteTodoById(todo.getId());
        assertTrue(deleted);
        
        Todo deletedTodo = todoService.getTodoById(todo.getId());
        assertNull(deletedTodo);
    }
    
    @Test
    @DisplayName("Get All Todos")
    void testGetAllTodos() {
        todoService.createTodo(1, TEST_USER_ID, "Task 1", "Description 1", 
            LocalDate.now().plusDays(1), Priority.LOW, Tags.Work);
        todoService.createTodo(2, TEST_USER_ID, "Task 2", "Description 2", 
            LocalDate.now().plusDays(2), Priority.HIGH, Tags.Urgent);
        
        List<Todo> allTodos = todoService.getTodosByUserId(TEST_USER_ID);
        assertEquals(2, allTodos.size());
    }
    
    @Test
    @DisplayName("Get Todo With Empty List")
    void testGetAllTodosWithEmptyList() {
        List<Todo> allTodos = todoService.getTodosByUserId(1);
        assertTrue(allTodos.isEmpty());
    }
    
    @Test
    @DisplayName("Update Non Existing Todo")
    void testUpdateNonExistentTodo() {
        assertThrows(NoSuchElementException.class, () -> {
            todoService.updateTodo(999, 1, "Updated Task", "Updated Description", 
                LocalDate.now().plusDays(2), Priority.HIGH, Tags.Home, true);
        });
    }
    
    @Test
    @DisplayName("Delete Non Existing Todo")
    void testDeleteNonExistentTodo() {
        boolean result = todoService.deleteTodoById(999);
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Test Todo Priority Updates")
    void testTodoPriorityUpdates() {
        // Create initial todo with LOW priority
        Todo todo = todoService.createTodo(
            1,
            TEST_USER_ID,
            "Priority Test",
            "Test Description",
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );
        
        // Update to MEDIUM priority
        todo = todoService.updateTodo(
            todo.getId(),
            TEST_USER_ID,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            Priority.MEDIUM,
            todo.getTags(),
            false
        );
        assertEquals(Priority.MEDIUM, todo.getPriority());
        
        // Update to HIGH priority
        todo = todoService.updateTodo(
            todo.getId(),
            TEST_USER_ID,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            Priority.HIGH,
            todo.getTags(),
            false
        );
        assertEquals(Priority.HIGH, todo.getPriority());
    }
    
    @Test
    @DisplayName("Test Todo Tag Updates")
    void testTodoTagUpdates() {
        // Create todo with Work tag
        Todo todo = todoService.createTodo(
            1,
            TEST_USER_ID,
            "Tag Test",
            "Test Description",
            LocalDate.now(),
            Priority.MEDIUM,
            Tags.Work
        );
        
        // Update to Home tag
        todo = todoService.updateTodo(
            todo.getId(),
            TEST_USER_ID,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            todo.getPriority(),
            Tags.Home,
            false
        );
        assertEquals(Tags.Home, todo.getTags());
        
        // Update to Urgent tag
        todo = todoService.updateTodo(
            todo.getId(),
            TEST_USER_ID,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            todo.getPriority(),
            Tags.Urgent,
            false
        );
        assertEquals(Tags.Urgent, todo.getTags());
    }

    @Test
    @DisplayName("Test Todo Due Date Validation")
    void testTodoDueDateValidation() {
        // Create todo with future date
        LocalDate futureDate = LocalDate.now().plusDays(7);
        Todo todo = todoService.createTodo(
            1,
            TEST_USER_ID,
            "Date Test",
            "Test Description",
            futureDate,
            Priority.LOW,
            Tags.Work
        );
        assertEquals(futureDate, todo.getDueDate());
        
        // Update with new future date
        LocalDate newFutureDate = LocalDate.now().plusDays(14);
        todo = todoService.updateTodo(
            todo.getId(),
            TEST_USER_ID,
            todo.getTitle(),
            todo.getDescription(),
            newFutureDate,
            todo.getPriority(),
            todo.getTags(),
            false
        );
        assertEquals(newFutureDate, todo.getDueDate());
    }
    
    @Test
    @DisplayName("Test Todo Status Flow")
    void testTodoStatusFlow() {
        Todo todo = todoService.createTodo(
            1, TEST_USER_ID, "Test Todo", "Description",
            LocalDate.now(), Priority.MEDIUM, Tags.Work
        );
        assertFalse(todo.isCompleted());
        

        todo = todoService.updateTodo(
            todo.getId(), TEST_USER_ID, todo.getTitle(),
            todo.getDescription(), todo.getDueDate(),
            todo.getPriority(), todo.getTags(), true
        );
        assertTrue(todo.isCompleted());
        assertEquals(Status.COMPLETED, todo.getStatus());
        
        todo = todoService.updateTodo(
            todo.getId(), TEST_USER_ID, todo.getTitle(),
            todo.getDescription(), todo.getDueDate(),
            todo.getPriority(), todo.getTags(), false
        );
        assertFalse(todo.isCompleted());
        assertEquals(Status.PENDING, todo.getStatus());
    }

    @Test
    @DisplayName("Test Todo Description Length Handling")
    void testTodoDescriptionLengthHandling() {
        String validDescription = "a".repeat(50);
        Todo todo = todoService.createTodo(
            1,
            TEST_USER_ID,
            "Description Test",
            validDescription,
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );
        assertNotNull(todo);
        assertEquals(validDescription, todo.getDescription());
        
        // Test with too long description (60 chars)
        String tooLongDescription = "a".repeat(60);
        assertThrows(RuntimeException.class, () -> {
            todoService.createTodo(
                2,
                TEST_USER_ID,
                "Long Description Test",
                tooLongDescription,
                LocalDate.now(),
                Priority.LOW,
                Tags.Work
            );
        
        });
    }
}