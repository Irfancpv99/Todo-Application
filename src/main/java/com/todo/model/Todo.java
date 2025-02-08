package com.todo.model;

import java.time.LocalDate;

public class Todo {
    private final int id;
    private int userId;
    private String title;
    private String description;
    private boolean completed;
    private LocalDate dueDate;
    private Priority priority;
    private Status status;
    private Tags tag;
    private int userSpecificId;
    private static final int MAX_TITLE_LENGTH = 20;
    private static final int MAX_DESCRIPTION_LENGTH = 50;

    public Todo(int id,  int userId, String title, String description, LocalDate dueDate, Priority priority, Tags tag) {
        this.id = id;
        validateAndSetTitle(title);
        validateAndSetDescription(description);
        validateAndSetDueDate(dueDate);
        validateAndSetPriority(priority);
        validateAndSetTags(tag);

        this.completed = false;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        validateAndSetTitle(title);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        validateAndSetDescription(description);
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        validateAndSetDueDate(dueDate);
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        validateAndSetPriority(priority);
    }

    public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Tags getTags() {
		return tag;
	}

	public void setTags(Tags tag) {
		validateAndSetTags(tag);
	}

	public void setCompleted(boolean completed) {
        this.completed = completed;
    }
	public void setUserSpecificId(int userSpecificId) {
	    this.userSpecificId = userSpecificId;
	}

	public int getUserSpecificId() {
	    return userSpecificId;
	}

    private void validateAndSetTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title cannot exceed " + MAX_TITLE_LENGTH + " characters");
        }
        this.title = title;
    }

    private void validateAndSetDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        this.description = description;
    }

    private void validateAndSetDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
        this.dueDate = dueDate;
    }
    

    private void validateAndSetPriority(Priority priority) {
        if (priority == null ) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        this.priority = priority;
    }
    
    
    private void validateAndSetTags(Tags tag) {
    	if (tag == null) {
            throw new IllegalArgumentException("Tags cannot be null");
    	}
    
    	this.tag = tag;
    }
}

    
