package com.todo.service;

import com.todo.config.DatabaseConfig;
import com.todo.model.Priority;
import com.todo.model.Status;
import com.todo.model.Tags;
import com.todo.model.Todo;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class TodoService {
    
    private int nextUserSpecificId = 1;

    public Todo createTodo(int userSpecificId, int userId, String title, String description, LocalDate dueDate, Priority priority, Tags tag) {
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        
        if (dueDate == null) {
            throw new IllegalArgumentException("DueDate cannot be null");
        }
        
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO todos (user_specific_id, user_id, title, description, due_date, priority, tag, completed, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
                
                    if (userId <= 0 || !userExists(conn, userId)) {
                        throw new SQLException("Invalid user ID: " + userId);
                    }
                    
                    ps.setInt(1, userSpecificId);
                    ps.setInt(2, userId);
                    ps.setString(3, title);
                    ps.setString(4, description);
                    ps.setDate(5, Date.valueOf(dueDate));
                    ps.setString(6, priority.toString());
                    ps.setString(7, tag.toString());
                    ps.setBoolean(8, false); 
                    ps.setString(9, Status.PENDING.toString()); 
                    
                    ResultSet rs = ps.executeQuery();
                    rs.next(); // Assuming it will always return a result
                    int generatedId = rs.getInt(1);
                    
                    conn.commit();
                    Todo todo = new Todo(generatedId, userId, title, description, dueDate, priority, tag);
                    todo.setUserSpecificId(userSpecificId);
                    todo.setCompleted(false);
                    todo.setStatus(Status.PENDING);
                    return todo;
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

    private boolean userExists(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Todo getTodoById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM todos WHERE id = ?")) {
            
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTodo(rs);
                }
            }
            return null;
            
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public Todo updateTodo(int id, int userId, String title, String description, LocalDate dueDate, Priority priority, Tags tag, boolean completed) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE todos SET user_id = ?, title = ?, description = ?, due_date = ?, priority = ?, tag = ?, completed = ?, status = ? WHERE id = ?")) {
            
            // Validate userId by checking if user exists
            if (userId <= 0 || !userExists(conn, userId)) {
                throw new SQLException("Invalid user ID: " + userId);
            }
            
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setDate(4, Date.valueOf(dueDate));
            ps.setString(5, priority.toString());
            ps.setString(6, tag.toString());
            ps.setBoolean(7, completed);
            ps.setString(8, completed ? Status.COMPLETED.toString() : Status.PENDING.toString());
            ps.setInt(9, id);
            
            int updatedRows = ps.executeUpdate();
            if (updatedRows == 0) {
                throw new NoSuchElementException("Todo with ID " + id + " not found.");
            }
            
            return getTodoById(id);
            
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public boolean deleteTodoById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM todos WHERE id = ?")) {
            
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }
    
    
    public List<Todo> getTodosByUserId(int userId) {
        List<Todo> todos = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM todos WHERE user_id = ?")) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    todos.add(mapResultSetToTodo(rs));
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
        
        return todos;
    }

    private Todo mapResultSetToTodo(ResultSet rs) throws SQLException {
       
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        LocalDate dueDate = rs.getDate("due_date").toLocalDate();
        
       
        String priorityStr = rs.getString("priority");
        Priority priority = Priority.MEDIUM; // Default value
        if (priorityStr != null) {
            try {
                priority = Priority.valueOf(priorityStr);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Invalid priority value in database: " + priorityStr);
            }
        }
        
      
        String tagStr = rs.getString("tag");
        Tags tag = Tags.Work; 
        if (tagStr != null) {
            try {
                tag = Tags.valueOf(tagStr);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Invalid tag value in database: " + tagStr);
            }
        }
        
       
        Todo todo = new Todo(id, userId, title, description, dueDate, priority, tag);
        
        
        try {
            int userSpecificId = rs.getInt("user_specific_id");
            if (!rs.wasNull()) {
                todo.setUserSpecificId(userSpecificId);
            }
        } catch (SQLException e) {
            
        }
        
        
        boolean completed = false;
        try {
            completed = rs.getBoolean("completed");
        } catch (SQLException e) {
            
        }
        todo.setCompleted(completed);
        
       
        try {
            String statusStr = rs.getString("status");
            if (statusStr != null) {
                try {
                    Status status = Status.valueOf(statusStr);
                    todo.setStatus(status);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Invalid status value in database: " + statusStr);
                    todo.setStatus(completed ? Status.COMPLETED : Status.PENDING);
                }
            } else {
                todo.setStatus(completed ? Status.COMPLETED : Status.PENDING);
            }
        } catch (SQLException e) {
            todo.setStatus(completed ? Status.COMPLETED : Status.PENDING);
        }

        return todo;
    }
    
    public void setNextUserSpecificId(int id) {
        this.nextUserSpecificId = id;
    }

    public int getNextUserSpecificId() {
        return nextUserSpecificId++;
    }
    
    public Todo testMapResultSetToTodo(ResultSet rs) throws SQLException {
        return mapResultSetToTodo(rs);
    }
}