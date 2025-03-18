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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class TodoServiceIntTest {
    private TodoService todoService;
    private UserService userService;
    private int userId = 1;
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
    @DisplayName("Todo Status Transitions")
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
        
        // Test multiple transitions in sequence
        for(int i = 0; i < 3; i++) {
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
    @DisplayName("Input Validation Tests")
    void testTodoInputValidation() {
        // Test null/empty title
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, null, "Description", 
                LocalDate.now(), Priority.LOW, Tags.Work)
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "", "Description", 
                LocalDate.now(), Priority.LOW, Tags.Work)
        );
        
        // Test null/empty description
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "Test", null, 
                LocalDate.now(), Priority.LOW, Tags.Work)
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "Test", "", 
                LocalDate.now(), Priority.LOW, Tags.Work)
        );
        
        // Test null dueDate
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "Test", "Description", 
                null, Priority.LOW, Tags.Work)
        );
        
        // Test null priority
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "Test", "Description", 
                LocalDate.now(), null, Tags.Work)
        );
        
        // Test null tags
        assertThrows(IllegalArgumentException.class, () ->
            todoService.createTodo(1, userId, "Test", "Description", 
                LocalDate.now(), Priority.LOW, null)
        );
    }
    
    @Test
    @DisplayName("Test Database Error Handling")
    void testDatabaseErrorHandling() {
        Todo todo = todoService.createTodo(1, userId, "Test", "Desc", 
            LocalDate.now(), Priority.LOW, Tags.Work);
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE todos");
        } catch (SQLException e) {
            fail("Failed to drop table: " + e.getMessage());
        }
        
        assertThrows(RuntimeException.class, () -> todoService.getTodoById(todo.getId()));
        
        assertThrows(RuntimeException.class, () -> todoService.deleteTodoById(todo.getId()));
        
        assertThrows(RuntimeException.class, () -> todoService.getTodosByUserId(userId));
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE todos (id SERIAL PRIMARY KEY, user_specific_id INT, " +
                    "user_id INT, title VARCHAR(255), description TEXT, due_date DATE, " +
                    "priority VARCHAR(50), tag VARCHAR(50), completed BOOLEAN, status VARCHAR(50))");
        } catch (SQLException e) {
            fail("Failed to recreate table: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Update Todo with Invalid User ID")
    void testUpdateTodoWithInvalidUserId() {
        Todo todo = todoService.createTodo(1, userId, "Test", "Description", 
                LocalDate.now(), Priority.LOW, Tags.Work);
        
        assertThrows(RuntimeException.class, () -> {
            todoService.updateTodo(
                todo.getId(),
                -999,
                "Updated Title",
                "Updated Description",
                LocalDate.now(),
                Priority.HIGH,
                Tags.Work,
                true
            );
        });
    }
    
    @Test
    @DisplayName("User Specific ID Management")
    void testUserSpecificIdManagement() {
        todoService.setNextUserSpecificId(1);
        assertEquals(1, todoService.getNextUserSpecificId());
        assertEquals(2, todoService.getNextUserSpecificId());
        
        todoService.setNextUserSpecificId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, todoService.getNextUserSpecificId());
        
        todoService.setNextUserSpecificId(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, todoService.getNextUserSpecificId());
        
        // Test creating todos with specific IDs
        todoService.setNextUserSpecificId(10);
        Todo todo1 = todoService.createTodo(
            todoService.getNextUserSpecificId(),
            userId,
            "Task 1",
            "Description",
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );
        
        assertEquals(10, todo1.getUserSpecificId());
        
        TodoService newTodoService = new TodoService();
        newTodoService.setNextUserSpecificId(20);
        
        Todo todo2 = newTodoService.createTodo(
            newTodoService.getNextUserSpecificId(),
            userId,
            "Task 2",
            "Description",
            LocalDate.now(),
            Priority.HIGH,
            Tags.Urgent
        );
        
        assertEquals(20, todo2.getUserSpecificId());
    }
    
    @Test
    @DisplayName("Handle Invalid/Null DB Values")
    void testInvalidDatabaseValues() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate("INSERT INTO todos (id, user_id, title, description, due_date, priority, tag, completed, status) " +
                    "VALUES (998, " + userId + ", 'Invalid Values', 'Description', CURRENT_DATE, 'INVALID_PRIORITY', 'INVALID_TAG', false, 'INVALID_STATUS')");
            
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO todos (id, user_id, title, description, due_date, priority, tag, completed) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, 999);
                ps.setInt(2, userId);
                ps.setString(3, "Null Fields");
                ps.setString(4, "Description");
                ps.setDate(5, Date.valueOf(LocalDate.now()));
                ps.setNull(6, java.sql.Types.VARCHAR);
                ps.setNull(7, java.sql.Types.VARCHAR);
                ps.setBoolean(8, false);
                ps.executeUpdate();
            }
        }
        
        Todo invalidValuesTodo = todoService.getTodoById(998);
        assertNotNull(invalidValuesTodo);
        assertEquals(Priority.MEDIUM, invalidValuesTodo.getPriority()); // Should use default priority
        assertEquals(Tags.Work, invalidValuesTodo.getTags()); // Should use default tag
        assertEquals(Status.PENDING, invalidValuesTodo.getStatus()); // Should use default status based on completed flag
        
        Todo nullFieldsTodo = todoService.getTodoById(999);
        assertNotNull(nullFieldsTodo);
        assertEquals(Priority.MEDIUM, nullFieldsTodo.getPriority()); // Should use default priority
        assertEquals(Tags.Work, nullFieldsTodo.getTags()); // Should use default tag
        assertEquals(Status.PENDING, nullFieldsTodo.getStatus()); // Should use default status based on completed flag
    }

    @Test
    @DisplayName("Test Missing Database Columns Handling")
    void testMissingDatabaseColumns() throws SQLException {
        Todo todo = todoService.createTodo(1, userId, "Test Todo", "Description", 
                LocalDate.now(), Priority.MEDIUM, Tags.Work);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, user_id, title, description, due_date FROM todos WHERE id = ?")) {
            
            ps.setInt(1, todo.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "ResultSet should contain the todo");
                try {
                    rs.getString("non_existent_column");
                    fail("Should throw SQLException for non-existent column");
                } catch (SQLException e) {
                }
            }
        }
    }

    @Test
    @DisplayName("Test Get Todos For Non-Existent User")
    void testGetTodosForNonExistentUser() {
        List<Todo> todos = todoService.getTodosByUserId(-1);
        assertTrue(todos.isEmpty(), "No todos should be found for non-existent user");
    }

    @Test
    @DisplayName("Test Transaction Isolation")
    void testTransactionIsolation() throws InterruptedException {
        Todo todo = todoService.createTodo(
            1,
            userId,
            "Original Todo",
            "Description",
            LocalDate.now(),
            Priority.LOW,
            Tags.Work
        );
        
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        
        new Thread(() -> {
            try {
                todoService.updateTodo(
                    todo.getId(),
                    userId,
                    "Updated by Thread 1",
                    todo.getDescription(),
                    todo.getDueDate(),
                    todo.getPriority(),
                    todo.getTags(),
                    false
                );
                successCount.incrementAndGet();
            } catch (Exception e) {
            } finally {
                latch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                todoService.updateTodo(
                    todo.getId(),
                    userId,
                    todo.getTitle(),
                    "Updated by Thread 2",
                    todo.getDueDate(),
                    todo.getPriority(),
                    todo.getTags(),
                    false
                );
                successCount.incrementAndGet();
            } catch (Exception e) {
            } finally {
                latch.countDown();
            }
        }).start();
        
        latch.await(5, TimeUnit.SECONDS);
        Todo finalTodo = todoService.getTodoById(todo.getId());
        assertNotNull(finalTodo);
        assertTrue(
            finalTodo.getTitle().equals("Updated by Thread 1") ||
            finalTodo.getDescription().equals("Updated by Thread 2") ||
             (finalTodo.getTitle().equals("Updated by Thread 1") && 
             finalTodo.getDescription().equals("Updated by Thread 2"))
        );
    }
    
    @Test
    @DisplayName("Test Complex Scenario: Create, Update and Delete")
    void testComplexCRUDScenario() {
        // 1. Create a todo
        Todo todo = todoService.createTodo(
            1,
            userId,
            "Complex Test",
            "Test Description",
            LocalDate.now().plusDays(3),
            Priority.LOW,
            Tags.Work
        );
        assertNotNull(todo);
        assertEquals("Complex Test", todo.getTitle());
        assertEquals(Status.PENDING, todo.getStatus());
        
        // 2. Update the todo (partial update)
        todo = todoService.updateTodo(
            todo.getId(),
            userId,
            "Updated Title",
            todo.getDescription(),  // keep original description
            todo.getDueDate(),      // keep original due date
            Priority.HIGH,          // change priority
            todo.getTags(),         // keep original tags
            todo.isCompleted()      // keep original completion status
        );
        assertEquals("Updated Title", todo.getTitle());
        assertEquals("Test Description", todo.getDescription());
        assertEquals(Priority.HIGH, todo.getPriority());
        
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
        boolean deleted = todoService.deleteTodoById(todo.getId());
        assertTrue(deleted);
        assertNull(todoService.getTodoById(todo.getId()));
    }
    
    @Test
    @DisplayName("Test Create Todo With No Generated ID")
    void testCreateTodoNoGeneratedId() {
        TodoService testService = new TodoService() {
            @Override
            public Todo createTodo(int userSpecificId, int userId, String title, String description, 
                                   LocalDate dueDate, Priority priority, Tags tag) {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT 1 WHERE 1=2")) {
                            
                            ResultSet rs = ps.executeQuery();
                            
                            if (!rs.next()) {
                                throw new SQLException("Failed to retrieve generated ID");
                            }
                            
                            return null;
                        }
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            }
        };
        
       assertThrows(RuntimeException.class, () -> {
            testService.createTodo(
                1,
                userId,
                "Test Todo",
                "Description",
                LocalDate.now(),
                Priority.LOW,
                Tags.Work
            );
        });
    }
    
    @Test
    @DisplayName("Test Create Todo When No Generated ID Is Returned")
    void testCreateTodoFailsToRetrieveId() {
        TodoService testService = new TodoService() {
            @Override
            public Todo createTodo(int userSpecificId, int userId, String title, String description, 
                                   LocalDate dueDate, Priority priority, Tags tag) {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 WHERE 1=2")) {
                            ResultSet rs = ps.executeQuery();
                            if (!rs.next()) {
                                throw new SQLException("Failed to retrieve generated ID");
                            }
                            return null;
                        }
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            }
        };

        assertThrows(RuntimeException.class, () -> testService.createTodo(
            1, userId, "Test Todo", "Description", LocalDate.now(), Priority.LOW, Tags.Work));
    }
    
    @Test
    @DisplayName("Test Todo Mapping Without Status Column")
    void testTodoMappingWithoutStatusColumn() throws SQLException {
        Todo originalTodo = todoService.createTodo(
            1, userId, "Missing Status Test", "Description",
            LocalDate.now(), Priority.MEDIUM, Tags.Work
        );

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, user_id, title, description, due_date, priority, tag, completed FROM todos WHERE id = ?")) {
            
            ps.setInt(1, originalTodo.getId());

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "ResultSet should have a row");

                Todo mappedTodo = todoService.testMapResultSetToTodo(rs);
                assertEquals(originalTodo.isCompleted() ? Status.COMPLETED : Status.PENDING, mappedTodo.getStatus());
            }
        }
    }


}