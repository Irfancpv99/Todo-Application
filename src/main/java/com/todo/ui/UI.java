package com.todo.ui;

import javax.swing.*;
import java.awt.*;
import com.todo.service.UserService;
import com.todo.service.TodoService;

import com.todo.model.User;

public class UI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final UserService userService;
    private final TodoService todoService;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton registerButton;
    private final JButton loginButton;
    
    public static void main(String[] args) {
    	
        UserService userService = new UserService();
        TodoService todoService = new TodoService();
        
        
        SwingUtilities.invokeLater(() -> {
            UI ui = new UI(userService, todoService);
            ui.setVisible(true);
        });
    }
    
    public UI(UserService userService, TodoService todoService) {
    	
    	
        this.userService = userService;
        this.todoService=todoService;
        
        

        setTitle("Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        registerButton = new JButton("Register");
        loginButton = new JButton("Login");
        panel.add(registerButton);
        panel.add(loginButton);

        add(panel);

        registerButton.addActionListener(e -> handleRegister());
        loginButton.addActionListener(e -> handleLogin());
        usernameField.setName("usernameField");
        passwordField.setName("passwordField");
        registerButton.setName("registerButton");
        loginButton.setName("loginButton");
    }

    private void handleRegister() {
        try {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (!validateInput(username, password)) return;

            User user = userService.registerUser(username, password);
            showMessage("Registration successful for user: " + user.getUsername(), JOptionPane.INFORMATION_MESSAGE);
            clearFields();
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), JOptionPane.ERROR_MESSAGE);
        } finally {
            clearPasswordField();
        }
    }
    
    private void handleLogin() {
        try {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            // Validate first
            if (!validateInput(username, password)) return;
            
            // Then attempt login
            User user = userService.login(username, password);
            if (user == null) {
                showMessage("Invalid username or password.", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            showMessage("Login successful - " + user.getUsername(), JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            launchTodoUI(user);
            dispose(); // Close login window
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), JOptionPane.ERROR_MESSAGE);
        } finally {
            clearPasswordField();
        }
    }
    
    private void launchTodoUI(User user) {
    	
    	if (user == null) {
            throw new IllegalArgumentException("Cannot launch TodoUI: user is null.");
        }
        SwingUtilities.invokeLater(() -> {
            TodoUI todoUI = new TodoUI(todoService,user.getUsername(), user.getUserid());
            todoUI.setVisible(true);
        });
    }
    

    private boolean validateInput(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            showMessage("Username and password cannot be empty.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (password.length() < 8) {
            showMessage("Password must be at least 8 characters long.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }

    private void clearPasswordField() {
        char[] passwordChars = passwordField.getPassword();
        java.util.Arrays.fill(passwordChars, '\0');
        passwordField.setText("");
    }

    private void showMessage(String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, "Message", messageType);
    }

    // Getters for testing
    public JTextField getUsernameField() {
        return usernameField;
    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }

    public JButton getRegisterButton() {
        return registerButton;
    }

    public JButton getLoginButton() {
        return loginButton;
    }
}
