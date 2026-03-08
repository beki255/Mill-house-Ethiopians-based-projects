package fxml.login.components;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import fxml.LoginMainController;
import fxml.DatabaseConnection;
import fxml.CurrentUser;
import java.sql.*;
import java.util.ResourceBundle;

public class StaffLoginFormController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheckbox;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label errorLabel;
    @FXML private Label passwordHintLabel;
    @FXML private Button loginButton;
    
    @FXML public VBox staffForm;

    private LoginMainController mainController;

    public void setMainController(LoginMainController controller) {
        this.mainController = controller;
        System.out.println("DEBUG: Main controller injected into StaffLoginFormController");
    }

    @FXML
    private void initialize() {
        System.out.println("DEBUG: StaffLoginFormController initialized");
        errorLabel.setText("");
        
        // Initialize password hint label
        passwordHintLabel.setText("Password must be at least 8 characters");
        passwordHintLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px;");
        passwordHintLabel.setVisible(false);
        
        // Hide the visible password field initially
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        
        // Add listeners for show/hide password
        setupPasswordVisibility();
        
        // Add validation listener for password
        setupPasswordValidation();
    }
    
    private void setupPasswordVisibility() {
        // Bind the two password fields
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());
        
        // Handle checkbox change
        showPasswordCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Show password
                visiblePasswordField.setText(passwordField.getText());
                visiblePasswordField.setVisible(true);
                visiblePasswordField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
            } else {
                // Hide password
                passwordField.setText(visiblePasswordField.getText());
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                visiblePasswordField.setVisible(false);
                visiblePasswordField.setManaged(false);
            }
        });
    }
    
    private void setupPasswordValidation() {
        // Show password hint when password field gets focus
        passwordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                passwordHintLabel.setVisible(true);
            } else if (!visiblePasswordField.isFocused()) {
                passwordHintLabel.setVisible(false);
            }
        });
        
        visiblePasswordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                passwordHintLabel.setVisible(true);
            } else if (!passwordField.isFocused()) {
                passwordHintLabel.setVisible(false);
            }
        });
        
        // Real-time password validation
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePassword(newValue);
        });
        
        visiblePasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePassword(newValue);
        });
    }
    
    private void validatePassword(String password) {
        if (password.length() < 8 && password.length() > 0) {
            passwordHintLabel.setText("Password must be at least 8 characters");
            passwordHintLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-weight: bold;");
        } else if (password.length() >= 8) {
            passwordHintLabel.setText("✓ Password meets minimum length");
            passwordHintLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            passwordHintLabel.setText("Password must be at least 8 characters");
            passwordHintLabel.setStyle("-fx-text-fill: #95a5a6;");
        }
    }

    @FXML
    private void handleLogin() {
        System.out.println("DEBUG: Staff login button clicked!");
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        System.out.println("DEBUG: Username: " + username);
        System.out.println("DEBUG: Password length: " + password.length());
        
        // VALIDATION
        if (username.isEmpty()) {
            showError("Please enter username");
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Please enter password");
            if (showPasswordCheckbox.isSelected()) {
                visiblePasswordField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            return;
        }
        
        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            passwordHintLabel.setVisible(true);
            if (showPasswordCheckbox.isSelected()) {
                visiblePasswordField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            return;
        }
        
        // Try database login ONLY - NO HARDCODED CREDENTIALS
        System.out.println("DEBUG: Trying database login...");
        if (tryDatabaseLogin(username, password)) {
            System.out.println("DEBUG: Database login successful!");
            return;
        }
        
        // All attempts failed
        System.out.println("DEBUG: Login failed");
        showError("Invalid username or password");
    }
    
    private boolean tryDatabaseLogin(String username, String password) {
        System.out.println("DEBUG: Attempting database authentication...");
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            System.out.println("DEBUG: Database connection: " + (conn != null ? "OK" : "FAILED"));
            
            if (conn == null) {
                showError("Cannot connect to database");
                return false;
            }
            
            // Simple query - just check users table
            String sql = "SELECT id, username, password, role, name FROM users WHERE username = ?";
            System.out.println("DEBUG: Executing SQL: " + sql);
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int userId = rs.getInt("id");
                String dbUsername = rs.getString("username");
                String dbPassword = rs.getString("password");
                String dbRole = rs.getString("role");
                String dbName = rs.getString("name");
                
                System.out.println("DEBUG: User found in database:");
                System.out.println("DEBUG:   User ID: " + userId);
                System.out.println("DEBUG:   Username: " + dbUsername);
                System.out.println("DEBUG:   DB Password: " + dbPassword);
                System.out.println("DEBUG:   Entered Password: " + password);
                System.out.println("DEBUG:   Role: " + dbRole);
                System.out.println("DEBUG:   Name: " + dbName);
                
                if (password.equals(dbPassword)) {
                    System.out.println("DEBUG: Password matches!");
                    
                    // Check if staff account is active - PASS THE EXISTING CONNECTION
                    if (!checkStaffStatus(username, conn)) {
                        showError("Your account is inactive. Contact administrator.");
                        return false;
                    }
                    
                    // FIXED: Properly set current user with correct parameters
                    String staffName = (dbName != null && !dbName.isEmpty()) ? dbName : dbUsername;
                    CurrentUser.getInstance().setStaffInfo(userId, staffName, dbUsername, dbRole);
                    
                    // Update last login using the SAME connection
                    String updateSql = "UPDATE users SET last_login = NOW() WHERE username = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setString(1, username);
                        updatePs.executeUpdate();
                        System.out.println("DEBUG: Last login updated");
                    }
                    
                    // Load dashboard based on role
                    System.out.println("DEBUG: Loading dashboard for role: " + dbRole);
                    
                    if (dbRole == null || dbRole.isEmpty()) {
                        showError("User role not assigned. Contact administrator.");
                        return false;
                    }
                    
                    mainController.loadDashboard(dbRole);
                    return true;
                } else {
                    System.out.println("DEBUG: Password does not match!");
                    showError("Invalid password");
                    return false;
                }
            } else {
                System.out.println("DEBUG: User not found in database");
                showError("User not found");
                return false;
            }
            
        } catch (SQLException e) {
            System.out.println("DEBUG: Database error: " + e.getMessage());
            e.printStackTrace();
            showError("Database error: " + e.getMessage());
            return false;
        } finally {
            // Properly close resources
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // DO NOT close the connection here - let DatabaseConnection manage it
                // if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("DEBUG: Error closing resources: " + e.getMessage());
            }
        }
    }
    private boolean checkStaffStatus(String username, Connection conn) {
        try {
            String sql = "SELECT status FROM staff_members WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String status = rs.getString("status");
                    return "Active".equals(status);
                }
            }
            // If no staff record, allow login (for admin)
            return true;
        } catch (SQLException e) {
            System.out.println("DEBUG: Error checking staff status: " + e.getMessage());
            return true; // Allow login if check fails
        }
    }

    @FXML
    private void handleBack() {
        System.out.println("DEBUG: Back button clicked");
        mainController.showInitialScreen();
    }
    public void show() {
        System.out.println("DEBUG: Showing staff login form");
        if (staffForm != null) {
            staffForm.setVisible(true);
            staffForm.setManaged(true);
            usernameField.clear();
            passwordField.clear();
            visiblePasswordField.clear();
            errorLabel.setText("");
            showPasswordCheckbox.setSelected(false);
            passwordHintLabel.setVisible(false);
            // Ensure password field is visible
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        }
    }

    public void hide() {
        System.out.println("DEBUG: Hiding staff login form");
        if (staffForm != null) {
            staffForm.setVisible(false);
            staffForm.setManaged(false);
        }
    }
    
    // ADD THIS MISSING METHOD
    private void showError(String msg) {
        System.out.println("DEBUG: Error: " + msg);
        if (errorLabel != null) {
            errorLabel.setText(msg);
            errorLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-weight: bold;");
        }
    }
    
    // ADD THIS HELPER METHOD FOR ALERTS
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }
    public void updateLanguage(ResourceBundle bundle) {
        if (bundle != null) {
            // Update all text elements
            usernameField.setPromptText(bundle.getString("username_prompt"));
            passwordField.setPromptText(bundle.getString("password_prompt"));
            visiblePasswordField.setPromptText(bundle.getString("password_prompt"));
            showPasswordCheckbox.setText(bundle.getString("show_password"));
            rememberMeCheckbox.setText(bundle.getString("remember_me"));
            loginButton.setText(bundle.getString("login_button"));
            passwordHintLabel.setText(bundle.getString("password_hint"));
        }
    }
}