package com.todo.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todo.model.Priority;
import com.todo.model.Status;
import com.todo.model.Tags;
import com.todo.model.Todo;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class TodoUniTest {
    private LocalDate dueDate;
    private Todo todo;

    @BeforeEach
    void setUp() {
        dueDate = LocalDate.now().plusDays(1); 
    }

    @Test
    @DisplayName("Todo should be correctly initialized with valid values")
    void testTodoInitialization() {
        todo = new Todo(1,1, "Write code", "Implementation of Todo class", dueDate, Priority.LOW, Tags.Home);
        
        assertEquals(1, todo.getId());
        assertEquals(0,todo.getUserId());
        assertEquals("Write code", todo.getTitle());
        assertEquals("Implementation of Todo class", todo.getDescription());
        assertEquals(dueDate, todo.getDueDate());
        assertEquals(Priority.LOW, todo.getPriority());
        assertFalse(todo.isCompleted());
    }

    @Test
    @DisplayName("Todo should be marked as completed when status is changed")
    void testMarkTodoCompleted() { 
        todo = new Todo(1,1, "Test TDD", "Practice TDD methodology", dueDate, Priority.HIGH,Tags.Work);
        todo.setCompleted(true);
        assertTrue(todo.isCompleted());
    }

    // Title validation tests
    @Test
    @DisplayName("Todo should not allow null title")
    void testNullTitle() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1,null, "Valid description", dueDate, Priority.MEDIUM, Tags.Urgent)
        );
    }

    @Test
    @DisplayName("Todo should not allow empty title")
    void testEmptyTitle() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1, 1,"", "Valid description", dueDate, Priority.MEDIUM, Tags.Urgent)
        );
    }

    @Test
    @DisplayName("Todo should not allow whitespace title")
    void testWhitespaceTitle() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, "    ", "Valid description", dueDate, Priority.MEDIUM, Tags.Work)
        );
    }
    
    @Test
    @DisplayName("Todo should not allow title longer than 20 characters")
    void testTitleLength() {
        String longTitle = "This title is way too long to be valid";
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, longTitle, "Valid description", dueDate, Priority.MEDIUM,Tags.Urgent)
        );
    }

    // Description validation tests
    @Test
    @DisplayName("Todo should not allow null description")
    void testNullDescription() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, "Valid title", null, dueDate, Priority.MEDIUM,Tags.Urgent)
        );
    }

    @Test
    @DisplayName("Todo should not allow empty description")
    void testEmptyDescription() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, "Valid title", "", dueDate, Priority.MEDIUM, Tags.Urgent)
        );
    }

    @Test
    @DisplayName("Todo should not allow description longer than 50 characters")
    void testDescriptionLength() {
        String longDescription = "This description is way too long to be valid as it exceeds fifty characters limit significantly";
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, "Valid title", longDescription, dueDate, Priority.MEDIUM, Tags.Home)
        );
    }

    // Due date validation tests
    @Test
    @DisplayName("Todo should not allow null due date")
    void testNullDueDate() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, "Valid title", "Valid description", null, Priority.LOW, Tags.Home)
        );
    }

    @Test
    @DisplayName("Todo should not allow past due date")
    void testPastDueDate() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, "Valid title", "Valid description", pastDate, Priority.LOW, Tags.Work)
        );
    }

    @Test
    @DisplayName("Todo should allow today's date as due date")
    void testTodayDueDate() {
        LocalDate today = LocalDate.now();
        todo = new Todo(1,1, "Valid title", "Valid description", today, Priority.LOW, Tags.Urgent);
        assertEquals(today, todo.getDueDate());
    }

    // Priority validation tests
    @Test
    @DisplayName("Todo should not allow null priority")
    void testNullPriority() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Todo(1,1, "Valid title", "Valid description", dueDate, null, Tags.Home)
        );
    }
    
    @Test
    void testTaskStatus() {
        todo = new Todo(1, 1, "Task", "Description",dueDate, Priority.HIGH, Tags.Home);
        todo.setStatus(Status.PENDING);
        assertEquals(Status.PENDING, todo.getStatus());

        todo.setStatus(Status.COMPLETED);
        assertEquals(Status.COMPLETED, todo.getStatus());
    }

    // Update tests
    @Test
    @DisplayName("Todo should allow valid updates to all fields")
    void testValidUpdates() {
        todo = new Todo(1,1, "Initial title", "Initial description", dueDate, Priority.LOW, Tags.Home);
        
        LocalDate newDueDate = dueDate.plusDays(1);
        todo.setTitle("Updated title");
        todo.setDescription("Updated description");
        todo.setDueDate(newDueDate);
        todo.setPriority(Priority.HIGH);
       
        
        assertEquals("Updated title", todo.getTitle());
        assertEquals("Updated description", todo.getDescription());
        assertEquals(newDueDate, todo.getDueDate());
        assertEquals(Priority.HIGH, todo.getPriority());
    }
    
    @Test
    @DisplayName(" Todo allow Valid Update on Tags") 
    void testTagsUpdate() {
        todo = new Todo(1, 1, "Task", "Description", dueDate, Priority.HIGH, Tags.Home);
        todo.setTags(Tags.Work);
        assertEquals(Tags.Work, todo.getTags());
    }
    
	}



