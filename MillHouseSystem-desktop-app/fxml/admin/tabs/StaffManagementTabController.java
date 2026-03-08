package fxml.admin.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.*;

import fxml.CurrentUser;
import fxml.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class StaffManagementTabController {

    @FXML private TableView<Staff> staffTable;
    @FXML private TextField staffNameField, staffPhoneField, staffSalaryField, staffUsernameField, staffTaskField;
    @FXML private ComboBox<String> staffRoleCombo, staffDepartmentCombo;
    @FXML private PasswordField staffPasswordField;
    @FXML private DatePicker hireDatePicker;
    @FXML private Label totalStaffLabel, activeStaffLabel, totalSalaryExpenseLabel;

    private ObservableList<Staff> staffList = FXCollections.observableArrayList();

    @FXML 
    private void initialize() {
        staffRoleCombo.setItems(FXCollections.observableArrayList(
            "ADMIN", "OPERATOR", "ACCOUNTANT", "DRIVER", "SUPERVISOR"
        ));
        staffDepartmentCombo.setItems(FXCollections.observableArrayList("Management", "Milling", "Finance", "Delivery", "Operations"));
        setupTable();
        loadStaff();
        cleanupOrphanedRecords();
        // Auto-generate username when name is entered
        staffNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && staffUsernameField.getText().isEmpty()) {
                String username = generateUsername(newValue);
                staffUsernameField.setText(username);
            }
        });
        
        // Auto-generate password
        staffPasswordField.setText(generateRandomPassword());
    }

    private void setupTable() {
        staffTable.getColumns().clear();
        
        TableColumn<Staff, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        nameCol.setPrefWidth(150);
        
        TableColumn<Staff, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(d -> d.getValue().usernameProperty());
        usernameCol.setPrefWidth(100);
        
        TableColumn<Staff, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> d.getValue().roleProperty());
        roleCol.setPrefWidth(110);
        
        TableColumn<Staff, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(d -> d.getValue().departmentProperty());
        deptCol.setPrefWidth(120);
        
        TableColumn<Staff, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(d -> d.getValue().phoneProperty());
        phoneCol.setPrefWidth(110);
        
        TableColumn<Staff, String> salaryCol = new TableColumn<>("Salary (ETB)");
        salaryCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("ETB %.2f", d.getValue().getMonthlySalary())));
        salaryCol.setPrefWidth(120);
        
        TableColumn<Staff, String> tasksCol = new TableColumn<>("Tasks");
        tasksCol.setCellValueFactory(d -> d.getValue().tasksProperty());
        tasksCol.setPrefWidth(180);
        
        TableColumn<Staff, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> d.getValue().statusProperty());
        statusCol.setPrefWidth(90);
        statusCol.setCellFactory(column -> new TableCell<Staff, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(status);
                    if ("Active".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        staffTable.getColumns().addAll(nameCol, usernameCol, roleCol, deptCol, phoneCol, salaryCol, tasksCol, statusCol);
        staffTable.setItems(staffList);
    }

    private void loadStaff() {
        staffList.clear();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM staff_members ORDER BY name")) {
            while (rs.next()) {
                Staff staff = new Staff(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getString("department"),
                    rs.getString("phone"),
                    rs.getDouble("monthly_salary"),
                    rs.getString("assigned_tasks"),
                    rs.getString("status")
                );
                staffList.add(staff);
            }
            updateSummary();
        } catch (SQLException e) { 
            e.printStackTrace();
            loadSampleStaff();
        }
    }
    
    private void loadSampleStaff() {
        // Only show sample if no staff exists
        if (staffList.isEmpty()) {
            System.out.println("No staff found. Add staff using the form above.");
        }
    }

    private void updateSummary() {
        totalStaffLabel.setText(String.valueOf(staffList.size()));
        long active = staffList.stream().filter(s -> "Active".equals(s.getStatus())).count();
        activeStaffLabel.setText(String.valueOf(active));
        double total = staffList.stream()
            .filter(s -> "Active".equals(s.getStatus()))
            .mapToDouble(Staff::getMonthlySalary)
            .sum();
        totalSalaryExpenseLabel.setText("ETB " + String.format("%.2f", total));
    }

   
    @FXML 
    private void handleAddStaff() { 
        System.out.println("DEBUG: Add Staff clicked");
        
        String name = staffNameField.getText().trim();
        String username = staffUsernameField.getText().trim();
        String phone = staffPhoneField.getText().trim();
        String password = staffPasswordField.getText();
        String role = staffRoleCombo.getValue();
        String department = staffDepartmentCombo.getValue();
        String salaryText = staffSalaryField.getText().trim();
        String tasks = staffTaskField.getText().trim();
        
        // Validation
        if (name.isEmpty() || username.isEmpty() || phone.isEmpty() || password.isEmpty() || 
            role == null || department == null || salaryText.isEmpty()) {
            showAlert("Error", "Please fill all required fields", Alert.AlertType.ERROR);
            return;
        }
        
        // Validate phone
        if (!isValidPhone(phone)) {
            showAlert("Error", "Phone must be 10 digits starting with 09 or 07", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            double salary = Double.parseDouble(salaryText);
            if (salary <= 0) {
                showAlert("Error", "Salary must be greater than 0", Alert.AlertType.ERROR);
                return;
            }
            
            // Check if username already exists
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Error", "Username already exists", Alert.AlertType.ERROR);
                    return;
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to check username: " + e.getMessage(), Alert.AlertType.ERROR);
                return;
            }
            
            // Start transaction - create fresh connection for transaction
            Connection c = null;
            try {
                c = DatabaseConnection.getConnection();
                if (c == null) {
                    showAlert("Error", "Cannot connect to database", Alert.AlertType.ERROR);
                    return;
                }
                
                boolean originalAutoCommit = c.getAutoCommit();
                c.setAutoCommit(false);
                
                try {
                    // Insert into users table
                    String userSql = "INSERT INTO users (username, password, role, name, phone) VALUES (?, ?, ?, ?, ?)";
                    int userId = -1;
                    try (PreparedStatement ps = c.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, username);
                        ps.setString(2, password);
                        ps.setString(3, role.toUpperCase());
                        ps.setString(4, name);
                        ps.setString(5, phone);
                        ps.executeUpdate();
                        
                        ResultSet rs = ps.getGeneratedKeys();
                        if (rs.next()) {
                            userId = rs.getInt(1);
                        }
                    }
                    
                    // Insert into staff_members table
                    String staffSql = "INSERT INTO staff_members (name, username, role, department, phone, monthly_salary, assigned_tasks, status, hire_date) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?, 'Active', CURDATE())";
                    int staffId = -1;
                    try (PreparedStatement staffPs = c.prepareStatement(staffSql, Statement.RETURN_GENERATED_KEYS)) {
                        staffPs.setString(1, name);
                        staffPs.setString(2, username);
                        staffPs.setString(3, role);
                        staffPs.setString(4, department);
                        staffPs.setString(5, phone);
                        staffPs.setDouble(6, salary);
                        staffPs.setString(7, tasks);
                        staffPs.executeUpdate();
                        
                        ResultSet rs = staffPs.getGeneratedKeys();
                        if (rs.next()) {
                            staffId = rs.getInt(1);
                        }
                    }
                    
                    // If role is DRIVER, add to drivers table
                    if (role.equalsIgnoreCase("DRIVER")) {
                        String driverSql = "INSERT INTO drivers (staff_id, name, phone, vehicle_type, vehicle_plate, status) " +
                                          "VALUES (?, ?, ?, 'Truck', 'To be assigned', 'Available')";
                        try (PreparedStatement driverPs = c.prepareStatement(driverSql)) {
                            driverPs.setInt(1, staffId);
                            driverPs.setString(2, name);
                            driverPs.setString(3, phone);
                            driverPs.executeUpdate();
                        }
                    }
                    
                    c.commit();
                    
                    showAlert("Success", 
                             "Staff member added successfully!\n" +
                             "Name: " + name + "\n" +
                             "Username: " + username + "\n" +
                             "Password: " + password + "\n" +
                             "Role: " + role + "\n" +
                             (role.equalsIgnoreCase("DRIVER") ? "\nDriver account created in drivers table." : ""),
                             Alert.AlertType.INFORMATION);
                    
                    // Clear form and generate new password
                    clearForm();
                    loadStaff();
                    
                } catch (SQLException e) {
                    if (c != null) {
                        try {
                            c.rollback();
                        } catch (SQLException rollbackEx) {
                            System.err.println("Error rolling back: " + rollbackEx.getMessage());
                        }
                    }
                    throw e;
                } finally {
                    if (c != null) {
                        try {
                            c.setAutoCommit(originalAutoCommit);
                        } catch (SQLException autoCommitEx) {
                            System.err.println("Error resetting auto-commit: " + autoCommitEx.getMessage());
                        }
                    }
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to add staff: " + e.getMessage(), Alert.AlertType.ERROR);
            } finally {
                // Close the transaction connection
                if (c != null) {
                    try {
                        c.close();
                    } catch (SQLException closeEx) {
                        System.err.println("Error closing connection: " + closeEx.getMessage());
                    }
                }
            }
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid salary amount", Alert.AlertType.ERROR);
        }
    }
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^(09|07)\\d{8}$");
    }
    
    private String generateUsername(String fullName) {
        // Convert "John Doe" to "john.doe" or "john" if single name
        String[] names = fullName.toLowerCase().split(" ");
        if (names.length == 1) {
            return names[0];
        } else {
            return names[0] + "." + names[names.length - 1];
        }
    }
    
    private String generateRandomPassword() {
        // Generate a random 10-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return password.toString();
    }
    
    private void clearForm() {
        staffNameField.clear();
        staffUsernameField.clear();
        staffPhoneField.clear();
        staffPasswordField.setText(generateRandomPassword());
        staffRoleCombo.setValue(null);
        staffDepartmentCombo.setValue(null);
        staffSalaryField.clear();
        staffTaskField.clear();
        hireDatePicker.setValue(null);
    }
    
    @FXML 
    private void handleUpdateStaffStatus() { 
        System.out.println("DEBUG: Update Status clicked");
        
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a staff member", Alert.AlertType.ERROR);
            return;
        }
        
        // Create a dialog to update status
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update Staff Status");
        dialog.setHeaderText("Update status for " + selected.getName());
        
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("Active", "Inactive", "On Leave"));
        statusCombo.setValue(selected.getStatus());
        
        VBox vbox = new VBox(10, new Label("Select new status:"), statusCombo);
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return statusCombo.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newStatus -> {
            if (newStatus != null) {
                String sql = "UPDATE staff_members SET status = ? WHERE id = ?";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, selected.getId());
                    ps.executeUpdate();
                    
                    selected.setStatus(newStatus);
                    staffTable.refresh();
                    updateSummary();
                    
                    showAlert("Success", "Status updated to: " + newStatus, Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Error", "Failed to update status: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    private boolean canDeleteStaff(Staff staff) {
        if (staff == null) {
            showAlert("Error", "No staff member selected", Alert.AlertType.ERROR);
            return false;
        }
        
        // Check if current user is trying to delete themselves
        String currentUser = CurrentUser.getInstance().getUsername();
        if (staff.getUsername().equals(currentUser)) {
            showAlert("Cannot Delete", 
                     "You cannot delete your own account while logged in.\n" +
                     "Please have another administrator perform this action.",
                     Alert.AlertType.WARNING);
            return false;
        }
        
        try (Connection c = DatabaseConnection.getConnection()) {
            // Check if staff exists
            String checkSql = "SELECT COUNT(*) FROM staff_members WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setInt(1, staff.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    showAlert("Error", "Staff member no longer exists", Alert.AlertType.ERROR);
                    return false;
                }
            }
            
            // Check if staff has any active orders
            String orderCheck = "SELECT COUNT(*) FROM orders WHERE assigned_to = ? AND status NOT IN ('Delivered', 'Cancelled')";
            try (PreparedStatement ps = c.prepareStatement(orderCheck)) {
                ps.setString(1, staff.getName());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Cannot Delete", 
                             staff.getName() + " has " + rs.getInt(1) + " active order(s).\n" +
                             "Please reassign orders before deleting.", 
                             Alert.AlertType.WARNING);
                    return false;
                }
            }
            
            // Check if staff has any pending salary payments
            String salaryCheck = "SELECT COUNT(*) FROM salary_payments WHERE staff_id = ? AND status = 'Pending'";
            try (PreparedStatement ps = c.prepareStatement(salaryCheck)) {
                ps.setInt(1, staff.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Cannot Delete", 
                             staff.getName() + " has pending salary payments.\n" +
                             "Please process payments before deleting.", 
                             Alert.AlertType.WARNING);
                    return false;
                }
            }
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to validate deletion: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }
    
   
    @FXML 
    private void handleDeleteStaff() { 
        System.out.println("DEBUG: Delete Staff clicked");
        
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a staff member to delete", Alert.AlertType.ERROR);
            return;
        }
        
        // Check if staff can be deleted
        if (!canDeleteStaff(selected)) {
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Staff Member");
        confirm.setContentText("Are you sure you want to delete " + selected.getName() + "?\n\n" +
                              "This will remove them from:\n" +
                              "• Staff members list\n" +
                              "• Login system\n" +
                              (selected.getRole().equalsIgnoreCase("DRIVER") ? "• Drivers list\n" : "") +
                              "\nWarning: This action cannot be undone!");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("User confirmed deletion of staff: " + selected.getName());
                
                try {
                    Connection c = null;
                    try {
                        c = DatabaseConnection.getConnection();
                        c.setAutoCommit(false);
                        
                        // STEP 1: Get user ID from users table
                        int userId = -1;
                        String getUserSql = "SELECT id FROM users WHERE username = ?";
                        try (PreparedStatement ps = c.prepareStatement(getUserSql)) {
                            ps.setString(1, selected.getUsername());
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                userId = rs.getInt("id");
                                System.out.println("DEBUG: Found user ID: " + userId + " for username: " + selected.getUsername());
                            }
                        }
                        
                        if (userId == -1) {
                            showAlert("Error", "User account not found for " + selected.getUsername(), 
                                     Alert.AlertType.ERROR);
                            c.rollback();
                            return;
                        }
                        
                        // STEP 2: Update transactions to set operator_id to NULL
                        String updateTransSql = "UPDATE transactions SET operator_id = NULL WHERE operator_id = ?";
                        try (PreparedStatement ps = c.prepareStatement(updateTransSql)) {
                            ps.setInt(1, userId);
                            int updated = ps.executeUpdate();
                            System.out.println("DEBUG: Updated " + updated + " transactions (set operator_id to NULL)");
                        }
                        
                        // STEP 3: Delete from drivers table if applicable
                        if (selected.getRole().equalsIgnoreCase("DRIVER")) {
                            String driverSql = "DELETE FROM drivers WHERE staff_id = ?";
                            try (PreparedStatement ps = c.prepareStatement(driverSql)) {
                                ps.setInt(1, selected.getId());
                                int deleted = ps.executeUpdate();
                                System.out.println("DEBUG: Deleted " + deleted + " driver record(s)");
                            }
                        }
                        
                        // STEP 4: Delete from staff_members table
                        String staffSql = "DELETE FROM staff_members WHERE id = ?";
                        int staffDeleted = 0;
                        try (PreparedStatement ps = c.prepareStatement(staffSql)) {
                            ps.setInt(1, selected.getId());
                            staffDeleted = ps.executeUpdate();
                            System.out.println("DEBUG: Deleted " + staffDeleted + " staff record(s)");
                        }
                        
                        if (staffDeleted == 0) {
                            showAlert("Error", "Staff member not found or already deleted", 
                                     Alert.AlertType.ERROR);
                            c.rollback();
                            return;
                        }
                        
                        // STEP 5: Delete from users table
                        String userSql = "DELETE FROM users WHERE id = ?";
                        try (PreparedStatement ps = c.prepareStatement(userSql)) {
                            ps.setInt(1, userId);
                            int userDeleted = ps.executeUpdate();
                            System.out.println("DEBUG: Deleted " + userDeleted + " user record(s)");
                        }
                        
                        c.commit();
                        
                        showAlert("Success", 
                                 "Staff member " + selected.getName() + " deleted successfully!\n" +
                                 "Note: Their transactions have been preserved with operator set to NULL.",
                                 Alert.AlertType.INFORMATION);
                        
                        // Refresh the staff list
                        loadStaff();
                        
                    } catch (SQLException e) {
                        if (c != null) {
                            try {
                                c.rollback();
                                System.out.println("DEBUG: Transaction rolled back due to: " + e.getMessage());
                            } catch (SQLException rollbackEx) {
                                System.err.println("Error rolling back: " + rollbackEx.getMessage());
                            }
                        }
                        
                        // Check specific error types
                        if (e.getMessage().contains("No staff member found")) {
                            showAlert("Error", "Staff member not found or already deleted", 
                                     Alert.AlertType.ERROR);
                        } else if (e.getMessage().contains("foreign key constraint")) {
                            showAlert("Error", 
                                     "Cannot delete staff member because they are referenced in other records.\n" +
                                     "Please contact database administrator to update constraints.",
                                     Alert.AlertType.ERROR);
                        } else {
                            showAlert("Error", "Failed to delete staff: " + e.getMessage(), 
                                     Alert.AlertType.ERROR);
                        }
                        
                        e.printStackTrace();
                        
                    } finally {
                        if (c != null) {
                            try {
                                c.setAutoCommit(true);
                                c.close();
                            } catch (SQLException closeEx) {
                                System.err.println("Error closing connection: " + closeEx.getMessage());
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to initialize deletion: " + e.getMessage(), 
                             Alert.AlertType.ERROR);
                }
            }
        });
    }
    private void cleanupOrphanedRecords() {
        try (Connection c = DatabaseConnection.getConnection()) {
            // Instead of deleting, just list orphaned records
            String checkSql = "SELECT u.id, u.username, u.role FROM users u " +
                             "LEFT JOIN staff_members s ON u.username = s.username " +
                             "WHERE s.id IS NULL AND u.role != 'CUSTOMER'";
            
            try (Statement stmt = c.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println("Orphaned user record: ID=" + rs.getInt("id") + 
                                     ", Username=" + rs.getString("username") + 
                                     ", Role=" + rs.getString("role"));
                }
                
                if (count > 0) {
                    System.out.println("Found " + count + " orphaned user records. Manual cleanup required.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking orphaned records: " + e.getMessage());
        }
    }

    // Helper method to check if staff is referenced in other tables
    private boolean isStaffReferenced(Connection c, int staffId) throws SQLException {
        // Check drivers table
        String checkDriversSql = "SELECT COUNT(*) FROM drivers WHERE staff_id = ?";
        try (PreparedStatement ps = c.prepareStatement(checkDriversSql)) {
            ps.setInt(1, staffId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("DEBUG: Staff is referenced in drivers table");
                return true;
            }
        }
        
        // Check orders table (if staff is assigned as operator)
        // First check if 'assigned_to' column exists
        String checkColumnSql = "SHOW COLUMNS FROM orders LIKE 'assigned_to'";
        try (Statement stmt = c.createStatement(); ResultSet rs = stmt.executeQuery(checkColumnSql)) {
            if (rs.next()) {
                String checkOrdersSql = "SELECT COUNT(*) FROM orders WHERE assigned_to LIKE ?";
                try (PreparedStatement ps = c.prepareStatement(checkOrdersSql)) {
                    ps.setString(1, "%Staff ID: " + staffId + "%");
                    ResultSet ordersRs = ps.executeQuery();
                    if (ordersRs.next() && ordersRs.getInt(1) > 0) {
                        System.out.println("DEBUG: Staff is referenced in orders table (assigned_to)");
                        return true;
                    }
                }
            }
        }
        
        // Check transactions table
        String checkTransactionsSql = "SELECT COUNT(*) FROM transactions WHERE operator_id = ?";
        try (PreparedStatement ps = c.prepareStatement(checkTransactionsSql)) {
            ps.setInt(1, staffId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("DEBUG: Staff is referenced in transactions table");
                return true;
            }
        }
        
        System.out.println("DEBUG: Staff is NOT referenced in other tables");
        return false;
    }
        


    // Helper method to update orders assigned to deleted staff
    private void updateAssignedOrders(Connection c, int staffId, String staffName) throws SQLException {
        // Check if 'assigned_to' column exists
        String checkColumnSql = "SHOW COLUMNS FROM orders LIKE 'assigned_to'";
        try (Statement stmt = c.createStatement(); ResultSet rs = stmt.executeQuery(checkColumnSql)) {
            if (rs.next()) {
                // Update orders where this staff was assigned as operator
                String updateOrdersSql = "UPDATE orders SET assigned_to = 'Unassigned' WHERE assigned_to LIKE ?";
                try (PreparedStatement ps = c.prepareStatement(updateOrdersSql)) {
                    ps.setString(1, "%" + staffName + "%");
                    int updated = ps.executeUpdate();
                    System.out.println("DEBUG: Updated " + updated + " orders (assigned_to)");
                }
            }
        }
        
        // Check if 'assigned_driver' column exists
        checkColumnSql = "SHOW COLUMNS FROM orders LIKE 'assigned_driver'";
        try (Statement stmt = c.createStatement(); ResultSet rs = stmt.executeQuery(checkColumnSql)) {
            if (rs.next()) {
                // Update orders where this staff was assigned as driver
                String updateDriverOrdersSql = "UPDATE orders SET assigned_driver = 'Unassigned' WHERE assigned_driver LIKE ?";
                try (PreparedStatement ps = c.prepareStatement(updateDriverOrdersSql)) {
                    ps.setString(1, "%" + staffName + "%");
                    int updated = ps.executeUpdate();
                    System.out.println("DEBUG: Updated " + updated + " orders (assigned_driver)");
                }
            }
        }
    }
    @FXML 
    private void handleChangeMyPassword() { 
        System.out.println("DEBUG: Change My Password clicked");
        
        // Create password change dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change My Password");
        dialog.setHeaderText("Change Your Password");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        
        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current Password");
        currentPasswordField.setStyle("-fx-pref-width: 250;");
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Password (min 8 characters)");
        newPasswordField.setStyle("-fx-pref-width: 250;");
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm New Password");
        confirmPasswordField.setStyle("-fx-pref-width: 250;");
        
        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: #e74c3c;");
        
        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPasswordField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);
        grid.add(validationLabel, 0, 3, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Add validation
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        // Real-time validation
        Runnable validatePasswords = () -> {
            String currentPass = currentPasswordField.getText();
            String newPass = newPasswordField.getText();
            String confirmPass = confirmPasswordField.getText();
            
            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                validationLabel.setText("All fields are required");
                okButton.setDisable(true);
            } else if (newPass.length() < 8) {
                validationLabel.setText("New password must be at least 8 characters");
                okButton.setDisable(true);
            } else if (!newPass.equals(confirmPass)) {
                validationLabel.setText("New passwords do not match");
                okButton.setDisable(true);
            } else {
                validationLabel.setText("");
                okButton.setDisable(false);
            }
        };
        
        currentPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validatePasswords.run());
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validatePasswords.run());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validatePasswords.run());
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String currentPassword = currentPasswordField.getText();
                String newPassword = newPasswordField.getText();
                
                // Verify current password
                String currentUser = CurrentUser.getInstance().getUsername();
                
                String verifySql = "SELECT password FROM users WHERE username = ?";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(verifySql)) {
                    ps.setString(1, currentUser);
                    ResultSet rs = ps.executeQuery();
                    
                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        if (!currentPassword.equals(dbPassword)) {
                            showAlert("Error", "Current password is incorrect", Alert.AlertType.ERROR);
                            return;
                        }
                        
                        // Update password
                        String updateSql = "UPDATE users SET password = ? WHERE username = ?";
                        try (PreparedStatement updatePs = c.prepareStatement(updateSql)) {
                            updatePs.setString(1, newPassword);
                            updatePs.setString(2, currentUser);
                            updatePs.executeUpdate();
                            
                            showAlert("Success", 
                                     "Password changed successfully!\n" +
                                     "Please use your new password for next login.",
                                     Alert.AlertType.INFORMATION);
                        }
                    }
                } catch (SQLException e) {
                    showAlert("Error", "Failed to change password: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    @FXML 
    private void handleResetPassword() { 
        System.out.println("DEBUG: Reset Password clicked");
        
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a staff member", Alert.AlertType.ERROR);
            return;
        }
        
        String newPassword = generateRandomPassword();
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Reset password for " + selected.getName());
        confirm.setContentText("New password will be: " + newPassword + "\n\nReset password?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "UPDATE users SET password = ? WHERE username = ?";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, newPassword);
                    ps.setString(2, selected.getUsername());
                    int rows = ps.executeUpdate();
                    
                    if (rows > 0) {
                        showAlert("Success", 
                                 "Password reset successfully for " + selected.getName() + "\n" +
                                 "New password: " + newPassword + "\n\n" +
                                 "Please inform the staff member of their new password.",
                                 Alert.AlertType.INFORMATION);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to reset password: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    public void refresh() { 
        loadStaff(); 
    }
    
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }
    
    // Inner Staff class
    public static class Staff {
        private SimpleStringProperty name, username, role, department, phone, tasks, status;
        private double monthlySalary;
        private int id;
        
        public Staff(int id, String name, String username, String role, String department, 
                    String phone, double monthlySalary, String tasks, String status) {
            this.id = id;
            this.name = new SimpleStringProperty(name);
            this.username = new SimpleStringProperty(username);
            this.role = new SimpleStringProperty(role);
            this.department = new SimpleStringProperty(department);
            this.phone = new SimpleStringProperty(phone);
            this.monthlySalary = monthlySalary;
            this.tasks = new SimpleStringProperty(tasks);
            this.status = new SimpleStringProperty(status);
        }
        
        public int getId() { return id; }
        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        public String getUsername() { return username.get(); }
        public SimpleStringProperty usernameProperty() { return username; }
        public String getRole() { return role.get(); }
        public SimpleStringProperty roleProperty() { return role; }
        public String getDepartment() { return department.get(); }
        public SimpleStringProperty departmentProperty() { return department; }
        public String getPhone() { return phone.get(); }
        public SimpleStringProperty phoneProperty() { return phone; }
        public double getMonthlySalary() { return monthlySalary; }
        public String getTasks() { return tasks.get(); }
        public SimpleStringProperty tasksProperty() { return tasks; }
        public String getStatus() { return status.get(); }
        public void setStatus(String status) { this.status.set(status); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}