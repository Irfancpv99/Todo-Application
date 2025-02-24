package com.todo.ui;
import com.todo.model.Priority;
import com.todo.model.Tags;
import com.todo.model.Todo;
import com.todo.service.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.table.DefaultTableModel;

public class TodoUI extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int userId;
	private final TodoService todoService;
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
    private JButton logoutButton; 
    private JLabel usernameLabel; 
    private JButton markCompletedButton;
    private JComboBox<Priority> filterPriorityComboBox;
    private List<Todo> currentTodos;
    private boolean isUpdateMode = false;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TodoUI(TodoService todoService,  String username,int userId) {
        this.todoService = todoService;
        this.userId = userId;
        initializeUI(username);
    }

    private void initializeUI(String username) {
        setTitle("Todo Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
     // Create a panel for the username label
        
        JPanel headerPanel = new JPanel();
        usernameLabel = new JLabel("Logged in as: " + username);
        headerPanel.add(usernameLabel);
        add(headerPanel, BorderLayout.PAGE_START);
        
        // Input Panel
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.WEST);

        // Table Panel
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
        
        todoTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFieldsFromSelectedRow();
            }
        });

        pack();
        setLocationRelativeTo(null);
        refreshTable();
    }
    
    
    private void populateFieldsFromSelectedRow() {
        int selectedRow = todoTable.getSelectedRow();
        if (selectedRow != -1) {
        	
            // Get the todo ID from the selected row
            
        	int id = (int) todoTable.getValueAt(selectedRow, 0);
            Todo selectedTodo = todoService.getTodoById(id);
            
            if (selectedTodo != null) {
                // Populate the fields with the selected todo's data
                titleField.setText(selectedTodo.getTitle());
                descriptionField.setText(selectedTodo.getDescription());
                dateField.setText(selectedTodo.getDueDate().format(DATE_FORMATTER));
                priorityComboBox.setSelectedItem(selectedTodo.getPriority());
                tagsComboBox.setSelectedItem(selectedTodo.getTags());
            }
        }
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Title:"), gbc);
        titleField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(titleField, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Description:"), gbc);
        descriptionField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(descriptionField, gbc);

        // Date
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Due Date (YYYY-MM-DD):"), gbc);
        dateField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(dateField, gbc);

        // Priority
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Priority:"), gbc);
        priorityComboBox = new JComboBox<>(Priority.values());
        gbc.gridx = 1;
        panel.add(priorityComboBox, gbc);

        // Tags
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Tags:"), gbc);
        tagsComboBox = new JComboBox<>(Tags.values());
        gbc.gridx = 1;
        panel.add(tagsComboBox, gbc);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPriorityComboBox = new JComboBox<>(Priority.values());
        JButton applyFilterButton = new JButton("Apply Filter");
        JButton clearFilterButton = new JButton("Clear Filter");
        
        filterPanel.add(new JLabel("Filter by Priority:"));
        filterPanel.add(filterPriorityComboBox);
        filterPanel.add(applyFilterButton);
        filterPanel.add(clearFilterButton);
        
        // Create table
        String[] columnNames = {"ID", "Title", "Description", "Due Date", "Priority", "Tags", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        todoTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(todoTable);
        
        // Add components to panel
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add filter button listeners
        applyFilterButton.addActionListener(e -> applyPriorityFilter());
        clearFilterButton.addActionListener(e -> refreshTable());
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        markCompletedButton = new JButton("Mark Completed");
        logoutButton = new JButton("Logout"); 
        JButton clearButton = new JButton("Clear");
        
        addButton.addActionListener(e -> addTodo());
        updateButton.addActionListener(e -> updateTodo());
        deleteButton.addActionListener(e -> deleteTodo());
        markCompletedButton.addActionListener(e -> markTodoCompleted());
        clearButton.addActionListener(e -> {
            clearFields();
            isUpdateMode = false;
            addButton.setEnabled(true);
        });
        
     // Add action listener for the logout button
        logoutButton.addActionListener(e -> {
            dispose(); // Close the current TodoUI
            SwingUtilities.invokeLater(() -> {
                // Return to the login UI
                UserService userService = new UserService();
                TodoService todoService = new TodoService();
                UI ui = new UI(userService, todoService);
                ui.setVisible(true);
            });
        });
        
        panel.add(addButton);
        panel.add(updateButton);
        panel.add(deleteButton);
        panel.add(markCompletedButton);
        panel.add(clearButton);
        panel.add(logoutButton);
        return panel;
    }

    private LocalDate parseDate(String dateStr) throws DateTimeParseException {
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }

    private void addTodo() {
        if (isUpdateMode) {
            JOptionPane.showMessageDialog(this, "Please clear fields first or complete update");
            return;
        }
        
        try {
            String title = titleField.getText().trim();
            String description = descriptionField.getText().trim();
            
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (description.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Description cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            LocalDate dueDate = parseDate(dateField.getText());
            Priority priority = (Priority) priorityComboBox.getSelectedItem();
            Tags tag = (Tags) tagsComboBox.getSelectedItem();
            
            todoService.createTodo(0, userId, title, description, dueDate, priority, tag);
            clearFields();
            refreshTable();
            JOptionPane.showMessageDialog(this, "Todo added successfully!");
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTodo() {
        int selectedRow = todoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a todo to update");
            return;
        }
        
        isUpdateMode = false;
        addButton.setEnabled(true);

        try {
            int id = (int) todoTable.getValueAt(selectedRow, 0);
            String title = titleField.getText();
            String description = descriptionField.getText();
            LocalDate dueDate = parseDate(dateField.getText());
            Priority priority = (Priority) priorityComboBox.getSelectedItem();
            Tags tag =(Tags) tagsComboBox.getSelectedItem();
            
            todoService.updateTodo(id, userId, title, description, dueDate, priority, tag, false);
            clearFields();
            refreshTable();
            JOptionPane.showMessageDialog(this, "Todo updated successfully!");
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use yyyy-MM-dd", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        	
    }

    private void deleteTodo() {
        int selectedRow = todoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a todo to delete");
            return;
        }

        int id = (int) todoTable.getValueAt(selectedRow, 0);
        if (todoService.deleteTodoById(id)) {
            refreshTable();
            JOptionPane.showMessageDialog(this, "Todo deleted successfully!");
        }
    }

    private void markTodoCompleted() {
        int selectedRow = todoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a todo to mark as completed");
            return;
        }

        int id = (int) todoTable.getValueAt(selectedRow, 0);
        Todo todo = todoService.getTodoById(id);
        if (todo != null) {
            todoService.updateTodo(id, userId, todo.getTitle(), todo.getDescription(), 
                                 todo.getDueDate(), todo.getPriority(), todo.getTags(), true);
            refreshTable();
            JOptionPane.showMessageDialog(this, "Todo marked as completed!");
        }
    }

    private void applyPriorityFilter() {
        Priority selectedPriority = (Priority) filterPriorityComboBox.getSelectedItem();
        List<Todo> filteredTodos = currentTodos.stream()
                .filter(todo -> todo.getPriority() == selectedPriority)
                .toList();
        updateTableModel(filteredTodos);
    }

    private void refreshTable() {
        currentTodos = todoService.getTodosByUserId(userId);
        updateTableModel(currentTodos);
    }

    private void updateTableModel(List<Todo> todos) {
        tableModel.setRowCount(0);
        for (Todo todo : todos) {
            Object[] row = {
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.getDueDate(),
                todo.getPriority(),
                todo.getTags(),
                todo.isCompleted() ? "Completed" : "Pending"
            };
            tableModel.addRow(row);
        }
    }

    private void clearFields() {
        titleField.setText("");
        descriptionField.setText("");
        dateField.setText("");
        priorityComboBox.setSelectedIndex(0);
        tagsComboBox.setSelectedIndex(0);
        isUpdateMode = false;
        addButton.setEnabled(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TodoService todoService = new TodoService();
            TodoUI todoUI = new TodoUI(todoService,"Test User",1);
            todoUI.setVisible(true);
        });
    }
}