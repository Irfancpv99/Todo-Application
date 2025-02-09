package com.todo.Integration;

//import com.todo.model.Todo;
import com.todo.model.Priority;
import com.todo.model.Status;
import com.todo.model.Tags;
import com.todo.service.TodoService;
import com.todo.service.UserService;
import com.todo.config.DatabaseConfig;

import org.junit.jupiter.api.*;
import java.time.LocalDate;
//import java.util.List;
import java.util.NoSuchElementException;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class TodoServiceIntTest {
    private TodoService todoService;
    private UserService userService;
    private int userId;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpass";

    @BeforeEach
    void setUp() {
        todoService = new TodoService();
        userService = new UserService();
        setupTestDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanupTestDatabase();
    }

    private void setupTestDatabase() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            // Clean existing data
            stmt.execute("DELETE FROM todos");
            stmt.execute("DELETE FROM users CASCADE");
            
            // Create test user and store the ID
            var user = userService.registerUser(TEST_USERNAME, TEST_PASSWORD);
            userId = user.getUserid();
        } catch (Exception e) {
            fail("Database setup failed: " + e.getMessage());
        }
    }

    private void cleanupTestDatabase() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM todos");
            stmt.execute("DELETE FROM users CASCADE");
        } catch (Exception e) {
            fail("Database cleanup failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Create and Retrieve Todo with Database Persistence")
    void testCreateAndRetrieveTodo() {
        var createdTodo = todoService.createTodo(
            1,
            userId,
            "Task 1",
            "Description",
            LocalDate.now().plusDays(1),
            Priority.MEDIUM,
            Tags.Work
        );

        // Test immediate retrieval
        var retrievedTodo = todoService.getTodoById(createdTodo.getId());
        assertNotNull(retrievedTodo);
        assertEquals("Task 1", retrievedTodo.getTitle());
        assertEquals(Tags.Work, retrievedTodo.getTags());
        assertEquals(Priority.MEDIUM, retrievedTodo.getPriority());
        
        // Test retrieval in a new service instance to verify persistence
        var newTodoService = new TodoService();
        var persistedTodo = newTodoService.getTodoById(createdTodo.getId());
        assertNotNull(persistedTodo);
        assertEquals("Task 1", persistedTodo.getTitle());
    }

    @Test
    @DisplayName("Update Todo with Transaction Management")
    void testUpdateTodo() {
        var todo = todoService.createTodo(
            1,
            userId,
            "Task 1",
            "Description",
            LocalDate.now().plusDays(1),
            Priority.MEDIUM,
            Tags.Work
        );

        var updatedTodo = todoService.updateTodo(
            todo.getId(),
            userId,
            "Updated Task",
            "Updated Description",
            LocalDate.now().plusDays(2),
            Priority.HIGH,
            Tags.Home,
            true
        );

        assertNotNull(updatedTodo);
        assertEquals("Updated Task", updatedTodo.getTitle());
        assertTrue(updatedTodo.isCompleted());
        assertEquals(Status.COMPLETED, updatedTodo.getStatus());
        assertEquals(Tags.Home, updatedTodo.getTags());
        assertEquals(Priority.HIGH, updatedTodo.getPriority());
    }

    @Test
    @DisplayName("Partial Update of Todo Fields")
    void testUpdateTodoPartialFields() {
        var originalDueDate = LocalDate.now().plusDays(1);
        var todo = todoService.createTodo(
            1,
            userId,
            "Original Task",
            "Original Description",
            originalDueDate,
            Priority.MEDIUM,
            Tags.Urgent
        );

        // Update only title and priority
        var updatedTodo = todoService.updateTodo(
            todo.getId(),
            userId,
            "Updated Task",
            todo.getDescription(),
            originalDueDate,
            Priority.HIGH,
            todo.getTags(),
            todo.isCompleted()
        );

        assertEquals("Updated Task", updatedTodo.getTitle());
        assertEquals(Priority.HIGH, updatedTodo.getPriority());
        assertEquals("Original Description", updatedTodo.getDescription());
        assertEquals(originalDueDate, updatedTodo.getDueDate());
        assertEquals(Tags.Urgent, updatedTodo.getTags());
    }

    @Test
    @DisplayName("Delete Todo with Database Verification")
    void testDeleteTodo() {
        var todo = todoService.createTodo(
            1,
            userId,
            "Task to Delete",
            "Description",
            LocalDate.now().plusDays(1),
            Priority.LOW,
            Tags.Work
        );

        boolean deleted = todoService.deleteTodoById(todo.getId());
        assertTrue(deleted);

        // Verify deletion using a new service instance
        var newTodoService = new TodoService();
        var deletedTodo = newTodoService.getTodoById(todo.getId());
        assertNull(deletedTodo);
    }

    @Test
    @DisplayName("Get All Todos for User with Multiple Service Instances")
    void testGetAllTodos() {
        // Create todos with first service instance
        todoService.createTodo(
            1,
            userId,
            "Task 1",
            "Description 1",
            LocalDate.now().plusDays(1),
            Priority.LOW,
            Tags.Work
        );

        todoService.createTodo(
            2,
            userId,
            "Task 2",
            "Description 2",
            LocalDate.now().plusDays(2),
            Priority.HIGH,
            Tags.Urgent
        );

        // Verify with new service instance
        var newTodoService = new TodoService();
        var allTodos = newTodoService.getTodosByUserId(userId);
        assertEquals(2, allTodos.size());
        assertTrue(allTodos.stream().anyMatch(t -> t.getTitle().equals("Task 1")));
        assertTrue(allTodos.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
    }

    @Test
    @DisplayName("Error Handling and Database Constraints")
    void testErrorHandling() {
        // Test updating non-existent todo
        assertThrows(NoSuchElementException.class, () ->
            todoService.updateTodo(
                999,
                userId,
                "Updated Task",
                "Updated Description",
                LocalDate.now().plusDays(2),
                Priority.HIGH,
                Tags.Home,
                true
            )
        );

        // Test deleting non-existent todo
        assertFalse(todoService.deleteTodoById(999));

        // Test getting non-existent todo
        assertNull(todoService.getTodoById(999));
    }

    @Test
    @DisplayName("Database Connection and Transaction Management")
    void testDatabaseTransactions() {
        // Test successful transaction
        var todo = todoService.createTodo(
            1,
            userId,
            "Transaction Test",
            "Description",
            LocalDate.now().plusDays(1),
            Priority.MEDIUM,
            Tags.Work
        );
        assertNotNull(todo);
        
        // Verify persistence
        var retrievedTodo = todoService.getTodoById(todo.getId());
        assertNotNull(retrievedTodo);
        assertEquals("Transaction Test", retrievedTodo.getTitle());
    }
}