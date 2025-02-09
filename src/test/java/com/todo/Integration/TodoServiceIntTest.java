package com.todo.Integration;

import com.todo.model.Priority;
import com.todo.model.Status;
import com.todo.model.Tags;
import com.todo.model.Todo;
import com.todo.service.TodoService;
import com.todo.service.UserService;
import com.todo.config.DatabaseConfig;

import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

            stmt.execute("DELETE FROM todos");
            stmt.execute("DELETE FROM users CASCADE");

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

        var retrievedTodo = todoService.getTodoById(createdTodo.getId());
        assertNotNull(retrievedTodo);
        assertEquals("Task 1", retrievedTodo.getTitle());
        assertEquals(Tags.Work, retrievedTodo.getTags());
        assertEquals(Priority.MEDIUM, retrievedTodo.getPriority());

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

        var newTodoService = new TodoService();
        var deletedTodo = newTodoService.getTodoById(todo.getId());
        assertNull(deletedTodo);
    }

    @Test
    @DisplayName("Get All Todos for User with Multiple Service Instances")
    void testGetAllTodos() {

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

        var newTodoService = new TodoService();
        var allTodos = newTodoService.getTodosByUserId(userId);
        assertEquals(2, allTodos.size());
        assertTrue(allTodos.stream().anyMatch(t -> t.getTitle().equals("Task 1")));
        assertTrue(allTodos.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
    }

    @Test
    @DisplayName("Error Handling and Database Constraints")
    void testErrorHandling() {

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

        assertFalse(todoService.deleteTodoById(999));

        assertNull(todoService.getTodoById(999));
    }

    @Test
    @DisplayName("Database Connection and Transaction Management")
    void testDatabaseTransactions() {
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
 
        var retrievedTodo = todoService.getTodoById(todo.getId());
        assertNotNull(retrievedTodo);
        assertEquals("Transaction Test", retrievedTodo.getTitle());
    }
    
    @Test
    @DisplayName("Transaction Rollback on Failed Todo Creation")
    void testTransactionRollbackOnFailure() {

        int nonExistentUserId = 99999;
        assertThrows(RuntimeException.class, () -> {
            todoService.createTodo(
                1,
                nonExistentUserId,
                "Test Todo",
                "Description",
                LocalDate.now(),
                Priority.LOW,
                Tags.Work
            );
        });

        var todos = todoService.getTodosByUserId(nonExistentUserId);
        assertTrue(todos.isEmpty());
    }
    
    @Test
    @DisplayName("Concurrent Todo Operations")
    void testConcurrentTodoOperations() throws InterruptedException {
        int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentHashMap<Integer, Todo> createdTodos = new ConcurrentHashMap<>();

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    Todo todo = todoService.createTodo(
                        index + 1,
                        userId,
                        "Concurrent Task " + index,
                        "Description",
                        LocalDate.now(),
                        Priority.MEDIUM,
                        Tags.Work
                    );
                    createdTodos.put(index, todo);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(numThreads, successCount.get());

        var allTodos = todoService.getTodosByUserId(userId);
        assertEquals(numThreads, allTodos.size());

        createdTodos.forEach((index, todo) -> {
            var retrievedTodo = todoService.getTodoById(todo.getId());
            assertNotNull(retrievedTodo);
            assertEquals("Concurrent Task " + index, retrievedTodo.getTitle());
        });
    }
    
    @Test
    @DisplayName("Edge Cases in Todo Status Updates")
    void testTodoStatusEdgeCases() {
        // Create a todo
        var todo = todoService.createTodo(
            1,
            userId,
            "Status Test",
            "Description",
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );

        todo = todoService.updateTodo(
            todo.getId(),
            userId,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            todo.getPriority(),
            todo.getTags(),
            true
        );
        assertEquals(Status.COMPLETED, todo.getStatus());
        
        todo = todoService.updateTodo(
            todo.getId(),
            userId,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            todo.getPriority(),
            todo.getTags(),
            false
        );
        assertEquals(Status.PENDING, todo.getStatus());
    }
    
    @Test
    @DisplayName("User-Specific ID Management")
    void testUserSpecificIdManagement() {

        todoService.setNextUserSpecificId(1);
        Todo todo1 = todoService.createTodo(
            todoService.getNextUserSpecificId(),
            userId,
            "Task 1",
            "Description",
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );

        Todo todo2 = todoService.createTodo(
            todoService.getNextUserSpecificId(),
            userId,
            "Task 2",
            "Description",
            LocalDate.now(),
            Priority.MEDIUM,
            Tags.Home
        );

        List<Todo> todos = todoService.getTodosByUserId(userId);
        assertEquals(2, todos.size());

        Todo retrievedTodo1 = todoService.getTodoById(todo1.getId());
        Todo retrievedTodo2 = todoService.getTodoById(todo2.getId());
        
        assertNotNull(retrievedTodo1, "First todo should exist");
        assertNotNull(retrievedTodo2, "Second todo should exist");
        assertEquals("Task 1", retrievedTodo1.getTitle());
        assertEquals("Task 2", retrievedTodo2.getTitle());

        TodoService newTodoService = new TodoService();
        newTodoService.setNextUserSpecificId(3);
        
        Todo todo3 = newTodoService.createTodo(
            newTodoService.getNextUserSpecificId(),
            userId,
            "Task 3",
            "Description",
            LocalDate.now(),
            Priority.HIGH,
            Tags.Urgent
        );

        List<Todo> allTodos = todoService.getTodosByUserId(userId);
        assertEquals(3, allTodos.size(), "Should have all three todos");
      
        boolean foundTask1 = false, foundTask2 = false, foundTask3 = false;
        
        for (Todo todo : allTodos) {
            switch (todo.getTitle()) {
                case "Task 1":
                    foundTask1 = true;
                    break;
                case "Task 2":
                    foundTask2 = true;
                    break;
                case "Task 3":
                    foundTask3 = true;
                    break;
            }
        }
        
        assertTrue(foundTask1, "Task 1 should exist");
        assertTrue(foundTask2, "Task 2 should exist");
        assertTrue(foundTask3, "Task 3 should exist");
    }
    
    @Test
    @DisplayName("Test Complex Transaction Scenarios")
    void testComplexTransactionScenarios() {
        // First create a successful todo
        Todo todo = todoService.createTodo(
            1,
            userId,
            "Valid Todo",
            "Description",
            LocalDate.now(),
            Priority.MEDIUM,
            Tags.Work
        );
        assertNotNull(todo);
        assertThrows(RuntimeException.class, () -> {
            todoService.createTodo(
                2,
                -999, 
                "Invalid Todo",
                "Description",
                LocalDate.now(),
                Priority.LOW,
                Tags.Work
            );
        });
        Todo retrievedTodo = todoService.getTodoById(todo.getId());
        assertNotNull(retrievedTodo);
        assertEquals("Valid Todo", retrievedTodo.getTitle());
    }

    @Test
    @DisplayName("Test Updating Non-existent Todo")
    void testUpdateNonExistentTodo() {
        assertThrows(NoSuchElementException.class, () -> {
            todoService.updateTodo(
                99999, // Non-existent ID
                userId,
                "Updated Title",
                "Updated Description",
                LocalDate.now(),
                Priority.HIGH,
                Tags.Urgent,
                true
            );
        });
    }

    @Test
    @DisplayName("Test Todo Status Transitions")
    void testTodoStatusTransitions() {
      
        Todo todo = todoService.createTodo(
            1,
            userId,
            "Status Test",
            "Description",
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );

        todo = todoService.updateTodo(
            todo.getId(),
            userId,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            todo.getPriority(),
            todo.getTags(),
            true 
        );
        assertTrue(todo.isCompleted());
        assertEquals(Status.COMPLETED, todo.getStatus());

        todo = todoService.updateTodo(
            todo.getId(),
            userId,
            todo.getTitle(),
            todo.getDescription(),
            todo.getDueDate(),
            todo.getPriority(),
            todo.getTags(),
            false 
        );
        assertFalse(todo.isCompleted());
        assertEquals(Status.PENDING, todo.getStatus());
    }
    
    @Test
    @DisplayName("Test Todo Creation with Invalid Tags")
    void testCreateTodoWithInvalidTags() {
        assertThrows(IllegalArgumentException.class, () -> {
            todoService.createTodo(
                1,
                userId,
                "Test Todo",
                "Description",
                LocalDate.now(),
                Priority.LOW,
                null
            );
        });
    }
    
    @Test
    @DisplayName("Test Todo Creation with Invalid Tags")
    void testCreateTodoWithInvalidPriority() {
        assertThrows(IllegalArgumentException.class, () -> {
            todoService.createTodo(
                1,
                userId,
                "Test Todo",
                "Description",
                LocalDate.now(),
                null,
                Tags.Urgent
                
            );
        });
    }
    
    @Test
    @DisplayName("Test Todo Creation with Invalid Due Date")
    void testCreateTodoWithInvalidDueDate() {
        assertThrows(IllegalArgumentException.class, () -> {
            todoService.createTodo(
                1,
                userId,
                "Test Todo",
                "Description",
                null,
                Priority.MEDIUM,
                Tags.Urgent
                
            );
        });
    }
    
    @Test
    @DisplayName("Test Todo Creation Title Validation")
    void testTodoCreationTitleValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "", "Description", 
                LocalDate.now(), Priority.LOW, Tags.Work)
        );
    }
    
    @Test
    @DisplayName("Test Todo Creation Description Validation")
    void testTodoCreationDescriptionValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "Test", "", 
                LocalDate.now(), Priority.LOW, Tags.Work)
        );
    }
    
    @Test
    @DisplayName("Test Update Todo Status Edge Cases")
    void testUpdateTodoStatusEdgeCases() {
        Todo todo = todoService.createTodo(
            1,
            userId,
            "Status Test",
            "Description",
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );
        
        for(int i = 0; i < 5; i++) {
            todo = todoService.updateTodo(
                todo.getId(),
                userId,
                todo.getTitle(),
                todo.getDescription(),
                todo.getDueDate(),
                todo.getPriority(),
                todo.getTags(),
                i % 2 == 0
            );
            assertEquals(i % 2 == 0, todo.isCompleted());
        }
    }
    
    @Test
    @DisplayName("Test Get Todos With Invalid User ID")
    void testGetTodosInvalidUser() {
        List<Todo> todos = todoService.getTodosByUserId(-1);
        assertTrue(todos.isEmpty());
    }
    
    @Test
    @DisplayName("Test Update Non-Existent Todo Status")
    void testUpdateNonExistentTodoStatus() {
        assertThrows(NoSuchElementException.class, () ->
            todoService.updateTodo(99999, userId, "Title", "Desc",
                LocalDate.now(), Priority.HIGH, Tags.Work, true)
        );
    }
    
    
}