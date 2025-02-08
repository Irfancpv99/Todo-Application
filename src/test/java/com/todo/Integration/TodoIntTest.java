package com.todo.Integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

import com.todo.model.*;

class TodoIntTest {
    private LocalDate dueDate;
    private Todo todo;

    @BeforeEach
    void setUp() {
        dueDate = LocalDate.now().plusDays(1);
        todo = new Todo(1, 1, "Task", "Description", dueDate, Priority.HIGH, Tags.Home);
    }
    
    @Test
   @DisplayName("Todo Full Task Work flow")
    void testCompleteTaskWorkflow() {
        todo.setStatus(Status.PENDING);
        todo.setDescription("Updated description");
        todo.setDueDate(dueDate.plusDays(2));
        
        assertEquals("Updated description", todo.getDescription());
        assertEquals(dueDate.plusDays(2), todo.getDueDate());
        assertEquals(Status.PENDING, todo.getStatus());
        
        todo.setCompleted(true);
        todo.setStatus(Status.COMPLETED);
        
        assertTrue(todo.isCompleted());
        assertEquals(Status.COMPLETED, todo.getStatus());
    }
    
    @Test
    @DisplayName("Todo Full Work flow")
    void testFullWorkflow() {
        todo.setStatus(Status.PENDING);
        todo.setTags(Tags.Work);
        todo.setPriority(Priority.LOW);
        
        assertEquals(Status.PENDING, todo.getStatus());
        assertEquals(Tags.Work, todo.getTags());
        assertEquals(Priority.LOW, todo.getPriority());
        
        todo.setStatus(Status.COMPLETED);
        todo.setCompleted(true);
        
        assertTrue(todo.isCompleted());
        assertEquals(Status.COMPLETED, todo.getStatus());
    }
    
    @Test
    @DisplayName("Priority And Tags Work Flows")
    void testPriorityAndTagsWorkflow() {
        todo.setPriority(Priority.LOW);
        todo.setTags(Tags.Work);
        todo.setStatus(Status.PENDING);
        
        assertEquals(Priority.LOW, todo.getPriority());
        assertEquals(Tags.Work, todo.getTags());
        
        todo.setPriority(Priority.HIGH);
        todo.setTags(Tags.Urgent);
        
        assertEquals(Priority.HIGH, todo.getPriority());
        assertEquals(Tags.Urgent, todo.getTags());
    }
    
    @Test
    @DisplayName("Todo Status Transaction")
    void testStatusTransitions() {
        todo.setStatus(Status.PENDING);
        assertEquals(Status.PENDING, todo.getStatus());
        todo.setStatus(Status.COMPLETED);
        assertEquals(Status.COMPLETED, todo.getStatus());
    }
}