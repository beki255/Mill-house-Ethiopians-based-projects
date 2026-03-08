package fxml.login.components;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import fxml.DatabaseConnection;
import fxml.LoginMainController;
import fxml.CustomerSession;
import java.sql.*;
import java.util.ResourceBundle;

public class CustomerLoginFormController {

    @FXML private TextField customerPhoneField;
    @FXML private PasswordField customerPasswordField;
    @FXML private TextField customerVisiblePasswordField; // NEW: For showing password
    @FXML private CheckBox customerShowPasswordCheckbox; // NEW: Checkbox for show/hide password
    @FXML private Label customerErrorLabel;
    @FXML private Label phoneHintLabel; // NEW: For phone validation hint
    @FXML private Label passwordHintLabel; // NEW: For password validation hint
    @FXML private Button customerLoginButton;
    @FXML private Button customerRegisterButton;
    
    @FXML public VBox customerForm;

    private LoginMainController mainController;
    private boolean isRegisterDialogOpen = false; // Track if registration dialog is open

    public void setMainController(LoginMainController controller) {
        this.mainController = controller;
    }

    @FXML
    private void initialize() {
        System.out.println("CustomerLoginFormController initialized!");
        
        // Clear any previous error messages
        customerErrorLabel.setText("");
        
        // Initialize hint labels
        phoneHintLabel.setText("Phone must be 10 digits starting with 09 or 07");
        phoneHintLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.82); -fx-font-size: 16; -fx-font-weight: normal;");
        phoneHintLabel.setVisible(false);
        
        passwordHintLabel.setText("Password must be at least 8 characters");
        passwordHintLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.82); -fx-font-size: 16; -fx-font-weight: normal;");
        passwordHintLabel.setVisible(false);
        
        // Hide the visible password field initially
        customerVisiblePasswordField.setVisible(false);
        customerVisiblePasswordField.setManaged(false);
        
        // Setup password visibility toggle
        setupPasswordVisibility();
        
        // Setup validation listeners
        setupValidationListeners();
    }
    
    private void setupPasswordVisibility() {
        // Bind the two password fields
        customerPasswordField.textProperty().bindBidirectional(customerVisiblePasswordField.textProperty());
        
        // Handle checkbox change
        customerShowPasswordCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Show password
                customerVisiblePasswordField.setText(customerPasswordField.getText());
                customerVisiblePasswordField.setVisible(true);
                customerVisiblePasswordField.setManaged(true);
                customerPasswordField.setVisible(false);
                customerPasswordField.setManaged(false);
            } else {
                // Hide password
                customerPasswordField.setText(customerVisiblePasswordField.getText());
                customerPasswordField.setVisible(true);
                customerPasswordField.setManaged(true);
                customerVisiblePasswordField.setVisible(false);
                customerVisiblePasswordField.setManaged(false);
            }
        });
    }
    
    private void setupValidationListeners() {
        // Phone validation
        customerPhoneField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                phoneHintLabel.setVisible(true);
            } else {
                phoneHintLabel.setVisible(false);
            }
        });
        
        customerPhoneField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePhone(newValue);
        });
        
        // Password validation
        customerPasswordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                passwordHintLabel.setVisible(true);
            } else if (!customerVisiblePasswordField.isFocused()) {
                passwordHintLabel.setVisible(false);
            }
        });
        
        customerVisiblePasswordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                passwordHintLabel.setVisible(true);
            } else if (!customerPasswordField.isFocused()) {
                passwordHintLabel.setVisible(false);
            }
        });
        
        customerPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePassword(newValue);
        });
        
        customerVisiblePasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePassword(newValue);
        });
    }
    
    private void validatePhone(String phone) {
        if (phone.isEmpty()) {
            phoneHintLabel.setText("Phone must be 10 digits starting with 09 or 07");
            phoneHintLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.82); -fx-font-size: 16; -fx-font-weight: normal;");
        } else if (phone.length() != 10 || (!phone.startsWith("09") && !phone.startsWith("07"))) {
            phoneHintLabel.setText("✗ Phone must be 10 digits starting with 09 or 07");
            phoneHintLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-size: 16; -fx-font-weight: bold;");
        } else {
            phoneHintLabel.setText("✓ Valid phone number");
            phoneHintLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 16; -fx-font-weight: bold;");
        }
    }
    
    private void validatePassword(String password) {
        if (password.isEmpty()) {
            passwordHintLabel.setText("Password must be at least 8 characters");
            passwordHintLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.82); -fx-font-size: 16; -fx-font-weight: normal;");
        } else if (password.length() < 8) {
            passwordHintLabel.setText("✗ Password must be at least 8 characters");
            passwordHintLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-size: 16; -fx-font-weight: bold;");
        } else {
            passwordHintLabel.setText("✓ Password meets minimum length");
            passwordHintLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 16; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleCustomerLogin() {
        System.out.println("Login button clicked!");
        
        String phone = customerPhoneField.getText().trim();
        String password = customerPasswordField.getText();

        // Validation
        if (phone.isEmpty()) {
            showError("Please enter phone number");
            customerPhoneField.requestFocus();
            phoneHintLabel.setVisible(true);
            return;
        }
        
        if (password.isEmpty()) {
            showError("Please enter password");
            if (customerShowPasswordCheckbox.isSelected()) {
                customerVisiblePasswordField.requestFocus();
            } else {
                customerPasswordField.requestFocus();
            }
            passwordHintLabel.setVisible(true);
            return;
        }
        
        // Phone validation
        if (phone.length() != 10 || (!phone.startsWith("09") && !phone.startsWith("07"))) {
            showError("Phone must be 10 digits starting with 09 or 07");
            customerPhoneField.requestFocus();
            phoneHintLabel.setVisible(true);
            return;
        }
        
        // Password validation
        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            if (customerShowPasswordCheckbox.isSelected()) {
                customerVisiblePasswordField.requestFocus();
            } else {
                customerPasswordField.requestFocus();
            }
            passwordHintLabel.setVisible(true);
            return;
        }

        if (authenticateCustomer(phone, password)) {
            // IMPORTANT: Set customer session before loading dashboard
            setCustomerSession(phone);
            mainController.loadCustomerDashboard();
        } else {
            showError("Invalid phone or password");
        }
    }

    @FXML
    private void handleRegister() {
        System.out.println("Register button clicked!");
        
        if (isRegisterDialogOpen) {
            return; // Prevent opening multiple dialogs
        }
        
        isRegisterDialogOpen = true;
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Customer");
        dialog.setHeaderText("Create Your Account");
        
        // Make dialog modal
        dialog.initOwner(customerForm.getScene().getWindow());

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(25));
        
        // Create fields with validation
        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        nameField.setStyle("-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.7); -fx-padding: 12; -fx-background-radius: 8;");
        nameField.setPrefWidth(300);
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("09XXXXXXXX or 07XXXXXXXX");
        phoneField.setStyle("-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.7); -fx-padding: 12; -fx-background-radius: 8;");
        phoneField.setPrefWidth(300);
        
        TextField addressField = new TextField();
        addressField.setPromptText("Delivery Address");
        addressField.setStyle("-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.7); -fx-padding: 12; -fx-background-radius: 8;");
        addressField.setPrefWidth(300);
        
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password (min 8 characters)");
        passField.setStyle("-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.7); -fx-padding: 12; -fx-background-radius: 8;");
        passField.setPrefWidth(300);
        
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm Password");
        confirmField.setStyle("-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.7); -fx-padding: 12; -fx-background-radius: 8;");
        confirmField.setPrefWidth(300);
        
        // Validation labels
        Label phoneValidationLabel = new Label("Phone must be 10 digits starting with 09 or 07");
        phoneValidationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13; -fx-padding: 2 0 0 5;");
        
        Label passwordValidationLabel = new Label("Password must be at least 8 characters");
        passwordValidationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13; -fx-padding: 2 0 0 5;");
        
        Label confirmValidationLabel = new Label("Passwords must match");
        confirmValidationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13; -fx-padding: 2 0 0 5;");
        
        // Add validation listeners
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                phoneValidationLabel.setText("Phone must be 10 digits starting with 09 or 07");
                phoneValidationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13;");
            } else if (newValue.length() != 10 || (!newValue.startsWith("09") && !newValue.startsWith("07"))) {
                phoneValidationLabel.setText("✗ Phone must be 10 digits starting with 09 or 07");
                phoneValidationLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-size: 13; -fx-font-weight: bold;");
            } else {
                phoneValidationLabel.setText("✓ Valid phone number");
                phoneValidationLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13; -fx-font-weight: bold;");
            }
        });
        
        passField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                passwordValidationLabel.setText("Password must be at least 8 characters");
                passwordValidationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13;");
            } else if (newValue.length() < 8) {
                passwordValidationLabel.setText("✗ Password must be at least 8 characters");
                passwordValidationLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-size: 13; -fx-font-weight: bold;");
            } else {
                passwordValidationLabel.setText("✓ Password meets minimum length");
                passwordValidationLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13; -fx-font-weight: bold;");
            }
            
            // Update confirm password validation
            if (!confirmField.getText().isEmpty()) {
                if (!newValue.equals(confirmField.getText())) {
                    confirmValidationLabel.setText("✗ Passwords do not match");
                    confirmValidationLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-size: 13; -fx-font-weight: bold;");
                } else {
                    confirmValidationLabel.setText("✓ Passwords match");
                    confirmValidationLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13; -fx-font-weight: bold;");
                }
            }
        });
        
        confirmField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                confirmValidationLabel.setText("Passwords must match");
                confirmValidationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13;");
            } else if (!newValue.equals(passField.getText())) {
                confirmValidationLabel.setText("✗ Passwords do not match");
                confirmValidationLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-size: 13; -fx-font-weight: bold;");
            } else {
                confirmValidationLabel.setText("✓ Passwords match");
                confirmValidationLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13; -fx-font-weight: bold;");
            }
        });
        
        // Layout the grid
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(phoneValidationLabel, 1, 2);
        grid.add(new Label("Address:"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passField, 1, 4);
        grid.add(passwordValidationLabel, 1, 5);
        grid.add(new Label("Confirm:"), 0, 6);
        grid.add(confirmField, 1, 6);
        grid.add(confirmValidationLabel, 1, 7);
        
        // Add show password checkbox
        CheckBox showPasswordCheckbox = new CheckBox("Show passwords");
        showPasswordCheckbox.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 14;");
        showPasswordCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Create visible password fields
                TextField visiblePassField = new TextField();
                visiblePassField.setText(passField.getText());
                visiblePassField.setPromptText("Password (min 8 characters)");
                visiblePassField.setStyle(passField.getStyle());
                visiblePassField.setPrefWidth(300);
                
                TextField visibleConfirmField = new TextField();
                visibleConfirmField.setText(confirmField.getText());
                visibleConfirmField.setPromptText("Confirm Password");
                visibleConfirmField.setStyle(confirmField.getStyle());
                visibleConfirmField.setPrefWidth(300);
                
                // Bind the fields
                passField.textProperty().bindBidirectional(visiblePassField.textProperty());
                confirmField.textProperty().bindBidirectional(visibleConfirmField.textProperty());
                
                // Replace the password fields
                grid.getChildren().remove(passField);
                grid.getChildren().remove(confirmField);
                grid.add(visiblePassField, 1, 4);
                grid.add(visibleConfirmField, 1, 6);
            }
        });
        grid.add(showPasswordCheckbox, 1, 8);
        GridPane.setMargin(showPasswordCheckbox, new javafx.geometry.Insets(10, 0, 0, 0));

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);");
        
        // Add validation before allowing OK
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        
        // Enable OK button only when all fields are valid
        Runnable validateRegistration = () -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String pass = passField.getText();
            String confirm = confirmField.getText();
            
            boolean isValid = !name.isEmpty() &&
                            !phone.isEmpty() &&
                            !pass.isEmpty() &&
                            !confirm.isEmpty() &&
                            phone.length() == 10 &&
                            (phone.startsWith("09") || phone.startsWith("07")) &&
                            pass.length() >= 8 &&
                            pass.equals(confirm);
            
            okButton.setDisable(!isValid);
        };
        
        nameField.textProperty().addListener((observable, oldValue, newValue) -> validateRegistration.run());
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> validateRegistration.run());
        passField.textProperty().addListener((observable, oldValue, newValue) -> validateRegistration.run());
        confirmField.textProperty().addListener((observable, oldValue, newValue) -> validateRegistration.run());

        dialog.showAndWait().ifPresent(result -> {
            isRegisterDialogOpen = false;
            
            if (result == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim();
                String pass = passField.getText();
                String confirm = confirmField.getText();

                // Final validation
                if (name.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
                    showError("Please fill all required fields");
                    return;
                }
                if (!pass.equals(confirm)) {
                    showError("Passwords do not match");
                    return;
                }
                if (phone.length() != 10 || (!phone.startsWith("09") && !phone.startsWith("07"))) {
                    showError("Phone must be 10 digits starting with 09 or 07");
                    return;
                }
                if (pass.length() < 8) {
                    showError("Password must be at least 8 characters");
                    return;
                }

                if (registerCustomer(name, phone, address, pass)) {
                    showSuccess("Registration successful! You can now login.");
                    customerPhoneField.setText(phone);
                    customerPasswordField.clear();
                    customerVisiblePasswordField.clear();
                }
            }
        });
    }

    // ADD THIS METHOD to set customer session
    private void setCustomerSession(String phone) {
        String sql = "SELECT id, name, credit_balance_ETB FROM customers WHERE phone = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CustomerSession.setCurrentCustomer(
                    rs.getInt("id"),
                    rs.getString("name"),
                    phone,
                    rs.getDouble("credit_balance_ETB")
                );
                System.out.println("Customer session set for: " + rs.getString("name"));
                System.out.println("Session ID: " + CustomerSession.getCustomerId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean registerCustomer(String name, String phone, String address, String password) {
        String sql = "INSERT INTO customers (name, phone, delivery_address, password) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, address);
            ps.setString(4, password);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                showError("Phone number already registered");
            } else {
                showError("Registration failed: " + e.getMessage());
            }
            e.printStackTrace();
        }
        return false;
    }

    private boolean authenticateCustomer(String phone, String password) {
        // Check if customer exists and has password (portal access)
        String sql = "SELECT id, name, credit_balance_ETB, password FROM customers WHERE phone = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                // Check if customer has portal access (has password)
                String dbPassword = rs.getString("password");
                if (dbPassword == null) {
                    showError("No portal access. Please place orders through operators.");
                    return false;
                }
                
                // Check password match
                if (password.equals(dbPassword)) {
                    CustomerSession.setCurrentCustomer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        phone,
                        rs.getDouble("credit_balance_ETB")
                    );
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    @FXML
    private void handleBack() {
        System.out.println("Back button clicked!");
        mainController.showInitialScreen();
    }

    public void show() {
        if (customerForm != null) {
            customerForm.setVisible(true);
            customerForm.setManaged(true);
            // Clear fields when showing
            customerPhoneField.clear();
            customerPasswordField.clear();
            customerVisiblePasswordField.clear();
            customerErrorLabel.setText("");
            customerShowPasswordCheckbox.setSelected(false);
            phoneHintLabel.setVisible(false);
            passwordHintLabel.setVisible(false);
            // Ensure password field is visible
            customerPasswordField.setVisible(true);
            customerPasswordField.setManaged(true);
            customerVisiblePasswordField.setVisible(false);
            customerVisiblePasswordField.setManaged(false);
        }
    }

    public void hide() {
        if (customerForm != null) {
            customerForm.setVisible(false);
            customerForm.setManaged(false);
        }
    }

    private void showError(String msg) {
        if (customerErrorLabel != null) {
            customerErrorLabel.setText(msg);
            customerErrorLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-weight: bold;");
        }
    }

    private void showSuccess(String msg) {
        if (customerErrorLabel != null) {
            customerErrorLabel.setText(msg);
            customerErrorLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    public void updateLanguage(ResourceBundle bundle) {
        if (bundle != null) {
            customerPhoneField.setPromptText(bundle.getString("phone_prompt"));
            customerPasswordField.setPromptText(bundle.getString("password_prompt"));
            customerVisiblePasswordField.setPromptText(bundle.getString("password_prompt"));
            customerShowPasswordCheckbox.setText(bundle.getString("show_password"));
            customerLoginButton.setText(bundle.getString("login_button"));
            customerRegisterButton.setText(bundle.getString("register_button"));
            phoneHintLabel.setText(bundle.getString("phone_hint"));
            passwordHintLabel.setText(bundle.getString("password_hint"));
        }
    }
}