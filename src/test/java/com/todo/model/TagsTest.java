package com.todo.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;


class TagsTest {
    private LocalDate dueDate;
    @BeforeEach
    void setUp() {
        dueDate = LocalDate.now().plusDays(1);
    }
    
    @Test
    @DisplayName("Todo should not allow null tags")
    void testNullTags() {
        Todo todo = new Todo(1,1, "Task without tags", "Description", dueDate, Priority.MEDIUM, Tags.Urgent);

        assertThrows(IllegalArgumentException.class, () -> todo.setTags(null), "Tags should not be null.");
    }
    
}