package fxml.operator.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import fxml.DatabaseConnection;
import fxml.CurrentUser;
import javafx.beans.property.SimpleStringProperty;

public class CustomersTabController {

    @FXML private TableView<Customer> customersTable;
    @FXML private TextField newCustomerName, newCustomerPhone, newCustomerAddress, searchField;
    @FXML private Label customerErrorLabel;
    @FXML private Button addCustomerBtn;
    @FXML private CheckBox createPortalAccountCheckbox;
    @FXML private PasswordField portalPasswordField;
    @FXML private Button deleteCustomerBtn;
    private ObservableList<Customer> customers = FXCollections.observableArrayList();

    @FXML 
    private void initialize() {
        System.out.println("=== INITIALIZING CUSTOMERS TAB ===");
        
        // Initialize FXML fields first
        if (createPortalAccountCheckbox != null && portalPasswordField != null) {
            // Setup portal password field visibility
            portalPasswordField.setVisible(false);
            portalPasswordField.setManaged(false);
            createPortalAccountCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                portalPasswordField.setVisible(newVal);
                portalPasswordField.setManaged(newVal);
                if (newVal && portalPasswordField.getText().isEmpty()) {
                    portalPasswordField.setText(generateRandomPassword());
                }
            });
        }
        
        setupTable();
        loadCustomers();
        
        // Disable delete button initially
        if (deleteCustomerBtn != null) {
            deleteCustomerBtn.setDisable(true);
            
            // Enable/disable delete button based on selection
            customersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    deleteCustomerBtn.setDisable(newSelection == null);
                }
            );
        }
    }
    @FXML
    private void handleDeleteCustomer() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a customer to delete", Alert.AlertType.ERROR);
            return;
        }
        
        // Check if customer has portal access (has password)
        boolean hasPortalAccess = checkIfCustomerHasPortalAccess(selected.getPhone());
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Customer");
        
        String warningMessage = "";
        if (hasPortalAccess) {
            warningMessage = "\n⚠️ WARNING: This customer has portal access!\n" +
                           "Deleting will remove their ability to login to Customer Portal.";
        }
        
        confirm.setContentText("Are you sure you want to delete this customer?\n\n" +
                              "Customer: " + selected.getName() + "\n" +
                              "Phone: " + selected.getPhone() + "\n" +
                              "Registration Type: " + selected.getRegistrationType() +
                              warningMessage + "\n\n" +
                              "This action cannot be undone!");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteCustomerFromDatabase(selected);
            }
        });
    }
    
    private boolean checkIfCustomerHasPortalAccess(String phone) {
        String sql = "SELECT password FROM customers WHERE phone = ? AND password IS NOT NULL";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking portal access: " + e.getMessage());
            return false;
        }
    }

    private void deleteCustomerFromDatabase(Customer customer) {
        String phone = customer.getPhone();
        int customerId = customer.getId();
        
        // Remove the local variable declaration and use a fresh connection
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                showAlert("Error", "Cannot connect to database", Alert.AlertType.ERROR);
                return;
            }
            
            conn.setAutoCommit(false);
            
            try {
                // Check if customer has any orders
                String checkOrdersSql = "SELECT COUNT(*) as order_count FROM orders WHERE customer_id = ?";
                int orderCount = 0;
                try (PreparedStatement ps = conn.prepareStatement(checkOrdersSql)) {
                    ps.setInt(1, customerId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        orderCount = rs.getInt("order_count");
                    }
                }
                
                // Check if customer has any transactions
                String checkTransactionsSql = "SELECT COUNT(*) as transaction_count FROM transactions WHERE customer_id = ?";
                int transactionCount = 0;
                try (PreparedStatement ps = conn.prepareStatement(checkTransactionsSql)) {
                    ps.setInt(1, customerId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        transactionCount = rs.getInt("transaction_count");
                    }
                }
                
                // Warn if customer has history
                if (orderCount > 0 || transactionCount > 0) {
                    Alert warning = new Alert(Alert.AlertType.CONFIRMATION);
                    warning.setTitle("Customer Has History");
                    warning.setHeaderText("Warning: Customer Has Order/Transaction History");
                    warning.setContentText("This customer has:\n" +
                                         "- " + orderCount + " order(s)\n" +
                                         "- " + transactionCount + " transaction(s)\n\n" +
                                         "Are you sure you want to delete? This will remove their personal information " +
                                         "but preserve order/transaction records.");
                    
                    warning.showAndWait().ifPresent(response -> {
                        if (response != ButtonType.OK) {
                            try {
                                conn.rollback();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    });
                }
                
                // Delete customer
                String deleteSql = "DELETE FROM customers WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setInt(1, customerId);
                    int rowsDeleted = ps.executeUpdate();
                    
                    if (rowsDeleted > 0) {
                        conn.commit();
                        
                        // Update orders to keep them but mark customer as deleted
                        String updateOrdersSql = "UPDATE orders SET customer_name = 'Deleted Customer' WHERE customer_id = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateOrdersSql)) {
                            updatePs.setInt(1, customerId);
                            updatePs.executeUpdate();
                        }
                        
                        showAlert("Success", 
                                 "Customer deleted successfully!\n" +
                                 "Name: " + customer.getName() + "\n" +
                                 "Phone: " + customer.getPhone() + "\n\n" +
                                 "Note: Their order history has been preserved with 'Deleted Customer' name.",
                                 Alert.AlertType.INFORMATION);
                        
                        // Refresh the table
                        loadCustomers();
                    } else {
                        conn.rollback();
                        showAlert("Error", "Customer not found or already deleted", Alert.AlertType.ERROR);
                    }
                }
                
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error rolling back: " + rollbackEx.getMessage());
                }
                
                // Check for foreign key constraint violations
                if (e.getMessage().contains("foreign key constraint")) {
                    showAlert("Cannot Delete", 
                             "Cannot delete customer because they have active orders or transactions.\n" +
                             "Please reassign or cancel their orders first.",
                             Alert.AlertType.ERROR);
                } else {
                    showAlert("Error", "Failed to delete customer: " + e.getMessage(), Alert.AlertType.ERROR);
                }
                e.printStackTrace();
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Database connection error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    
    private void debugDatabaseConnection() {
        System.out.println("Checking database connection...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.out.println("ERROR: Database connection is NULL!");
                customerErrorLabel.setText("Database connection failed!");
                return;
            }
            
            System.out.println("✓ Database connected successfully");
            
            // Check customers table
            String checkTableSql = "SHOW TABLES LIKE 'customers'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkTableSql)) {
                
                if (rs.next()) {
                    System.out.println("✓ Customers table exists");
                    
                    // Check table columns
                    String checkColumns = "SHOW COLUMNS FROM customers";
                    try (Statement stmt2 = conn.createStatement();
                         ResultSet rs2 = stmt2.executeQuery(checkColumns)) {
                        
                        System.out.println("Table columns:");
                        while (rs2.next()) {
                            System.out.println("  - " + rs2.getString("Field") + " : " + rs2.getString("Type"));
                        }
                    }
                    
                    // Count customers
                    String countSql = "SELECT COUNT(*) as total FROM customers";
                    try (Statement stmt3 = conn.createStatement();
                         ResultSet rs3 = stmt3.executeQuery(countSql)) {
                        
                        if (rs3.next()) {
                            int total = rs3.getInt("total");
                            System.out.println("Total customers in database: " + total);
                            customerErrorLabel.setText("Found " + total + " customers in database");
                        }
                    }
                    
                } else {
                    System.out.println("✗ Customers table does NOT exist!");
                    customerErrorLabel.setText("ERROR: Customers table missing in database");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            customerErrorLabel.setText("Database error: " + e.getMessage());
        }
    }

    private void setupTable() {
        // Clear existing columns
        customersTable.getColumns().clear();
        
        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getName();
            return new SimpleStringProperty(name != null ? name : "Unknown");
        });
        nameCol.setPrefWidth(180);
        
        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cell -> {
            String phone = cell.getValue().getPhone();
            return new SimpleStringProperty(phone != null ? phone : "N/A");
        });
        phoneCol.setPrefWidth(150);
        
        TableColumn<Customer, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(cell -> {
            String address = cell.getValue().getDeliveryAddress();
            return new SimpleStringProperty(address != null ? address : "No address");
        });
        addressCol.setPrefWidth(300);
        
        TableColumn<Customer, String> regTypeCol = new TableColumn<>("Registration Type");
        regTypeCol.setCellValueFactory(cell -> {
            String type = cell.getValue().getRegistrationType();
            String by = cell.getValue().getRegisteredBy();
            return new SimpleStringProperty(
                (type != null ? type : "Unknown") + 
                (by != null ? " (" + by + ")" : "")
            );
        });
        regTypeCol.setPrefWidth(200);
        
        TableColumn<Customer, String> dateCol = new TableColumn<>("Registered Date");
        dateCol.setCellValueFactory(cell -> {
            Timestamp date = cell.getValue().getRegistrationDate();
            return new SimpleStringProperty(
                date != null ? date.toString().substring(0, 10) : "N/A"
            );
        });
        dateCol.setPrefWidth(120);
        
        customersTable.getColumns().addAll(nameCol, phoneCol, addressCol, regTypeCol, dateCol);
        customersTable.setItems(customers);
    }

    private void loadCustomers() {
        customers.clear();
        System.out.println("Loading customers from database...");
        
        // SIMPLE SQL - avoid complex filters initially
        String sql = "SELECT * FROM customers ORDER BY name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                Customer customer = new Customer();
                
                // Safely get each column
                try {
                    customer.setId(rs.getInt("id"));
                    customer.setName(rs.getString("name"));
                    customer.setPhone(rs.getString("phone"));
                    customer.setDeliveryAddress(rs.getString("delivery_address"));
                    // REMOVED: credit_balance_ETB
                    customer.setRegistrationType(rs.getString("registration_type"));
                    customer.setRegisteredBy(rs.getString("registered_by"));
                    
                    Timestamp regDate = rs.getTimestamp("registration_date");
                    customer.setRegistrationDate(regDate);
                    
                    customers.add(customer);
                    
                    System.out.println("Loaded customer #" + count + ": " + 
                                     customer.getName() + " (" + customer.getPhone() + ") - " +
                                     customer.getRegistrationType());
                    
                } catch (SQLException e) {
                    System.err.println("Error loading customer #" + count + ": " + e.getMessage());
                }
            }
            
            System.out.println("Successfully loaded " + customers.size() + " customers");
            
            if (customers.isEmpty()) {
                System.out.println("WARNING: No customers found in database!");
                customerErrorLabel.setText("No customers found. Add customers using the form above.");
                loadSampleCustomers();
            } else {
                customerErrorLabel.setText("Loaded " + customers.size() + " customers");
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR loading customers: " + e.getMessage());
            e.printStackTrace();
            customerErrorLabel.setText("Database error: " + e.getMessage());
            
            // Load sample data for testing
            loadSampleCustomers();
        }
    }
    
    private void loadSampleCustomers() {
        System.out.println("Loading sample customers for display...");
        
        // Sample portal customers
        customers.add(new Customer(1, "Portal Customer 1", "0911111111", 
                                 "Bole, Addis Ababa", "PORTAL", "self"));
        customers.add(new Customer(2, "Portal Customer 2", "0922222222", 
                                 "Piassa, Addis Ababa", "PORTAL", "self"));
        
        // Sample operator-added customers
        customers.add(new Customer(3, "Operator Customer 1", "0933333333", 
                                 "Mexico, Addis Ababa", "OPERATOR", "operator1"));
        customers.add(new Customer(4, "Operator Customer 2", "0944444444", 
                                 "Kirkos, Addis Ababa", "OPERATOR", "operator2"));
        
        System.out.println("Loaded " + customers.size() + " sample customers");
        customerErrorLabel.setText("Using sample data (" + customers.size() + " customers)");
    }
    
    
    @FXML 
    private void handleAddCustomer() {
        String name = newCustomerName.getText().trim();
        String phone = newCustomerPhone.getText().trim();
        String address = newCustomerAddress.getText().trim();
        boolean createPortalAccount = createPortalAccountCheckbox.isSelected();
        String portalPassword = portalPasswordField.getText();
        
        // Validation
        if (name.isEmpty() || phone.isEmpty()) {
            showAlert("Error", "Name and phone are required", Alert.AlertType.ERROR);
            return;
        }
        
        // Validate phone format
        if (!isValidPhone(phone)) {
            showAlert("Error", "Phone must be 10 digits starting with 09 or 07", Alert.AlertType.ERROR);
            return;
        }
        
        // Check if customer already exists
        if (customerExists(phone)) {
            showAlert("Warning", "Customer with phone " + phone + " already exists", 
                     Alert.AlertType.WARNING);
            return;
        }
        
        // Validate password if creating portal account
        if (createPortalAccount) {
            if (portalPassword.isEmpty() || portalPassword.length() < 8) {
                showAlert("Error", "Portal password must be at least 8 characters", 
                         Alert.AlertType.ERROR);
                return;
            }
        }
        
        // Get current operator's username
        String operatorUsername = CurrentUser.getInstance().getUsername();
        if (operatorUsername == null) {
            operatorUsername = "Operator";
        }
        
        // If address is empty, provide default
        if (address.isEmpty()) {
            address = "Address not provided";
        }
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Insert customer
            String customerSql = "INSERT INTO customers (name, phone, delivery_address, " +
                               "registration_type, registered_by, password, credit_balance_ETB) " +
                               "VALUES (?, ?, ?, 'OPERATOR', ?, ?, 0.0)";
            
            int customerId = -1;
            try (PreparedStatement ps = conn.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setString(3, address);
                ps.setString(4, operatorUsername);
                ps.setString(5, createPortalAccount ? portalPassword : null);
                ps.executeUpdate();
                
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    customerId = rs.getInt(1);
                }
            }
            
            if (customerId > 0) {
                conn.commit();
                
                String message = "Customer added successfully!\n" +
                                "Name: " + name + "\n" +
                                "Phone: " + phone + "\n" +
                                "Type: OPERATOR (added by " + operatorUsername + ")\n";
                
                if (createPortalAccount) {
                    message += "\nPortal login created!\n" +
                              "Phone: " + phone + "\n" +
                              "Password: " + portalPassword + "\n\n" +
                              "Inform customer they can login to Customer Portal with their phone number.";
                } else {
                    message += "\nNo portal account created.\n" +
                              "Customer can place orders through operators only.";
                }
                
                showAlert("Success", message, Alert.AlertType.INFORMATION);
                
                // Clear form
                clearForm();
                loadCustomers();
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            showAlert("Error", "Failed to add customer: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) {}
            }
        }
    }
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return password.toString();
    }
    private void clearForm() {
        newCustomerName.clear();
        newCustomerPhone.clear();
        newCustomerAddress.clear();
        createPortalAccountCheckbox.setSelected(false);
        portalPasswordField.clear();
        portalPasswordField.setVisible(false);
        portalPasswordField.setManaged(false);
    }
    
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^(09|07)\\d{8}$");
    }
    
    private boolean customerExists(String phone) {
        String sql = "SELECT id FROM customers WHERE phone = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking customer exists: " + e.getMessage());
            return false;
        }
    }
    
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadCustomers();
            return;
        }
        
        customers.clear();
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + searchText + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                Customer customer = new Customer();
                customer.setName(rs.getString("name"));
                customer.setPhone(rs.getString("phone"));
                customer.setRegistrationType(rs.getString("registration_type"));
                customer.setRegisteredBy(rs.getString("registered_by"));
                customers.add(customer);
            }
            
            System.out.println("Search found " + count + " customers matching: " + searchText);
            
            if (count == 0) {
                showAlert("Search Results", "No customers found matching: " + searchText, 
                         Alert.AlertType.INFORMATION);
            }
            
        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            showAlert("Error", "Failed to search: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void refresh() { 
        System.out.println("Refreshing customers...");
        loadCustomers(); 
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public static class Customer {
        private int id;
        private String name, phone, deliveryAddress, registrationType, registeredBy;
        private Timestamp registrationDate;
        // REMOVED: private double creditBalance;
        
        public Customer() {}
        
        public Customer(int id, String name, String phone, String deliveryAddress, 
                       String registrationType, String registeredBy) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.deliveryAddress = deliveryAddress;
            this.registrationType = registrationType;
            this.registeredBy = registeredBy;
            this.registrationDate = new Timestamp(System.currentTimeMillis());
        }
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getName() { return name != null ? name : "Unknown"; }
        public void setName(String name) { this.name = name; }
        
        public String getPhone() { return phone != null ? phone : "N/A"; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getDeliveryAddress() { return deliveryAddress != null ? deliveryAddress : ""; }
        public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
        
        // REMOVED: getCreditBalance and setCreditBalance methods
        
        public String getRegistrationType() { return registrationType != null ? registrationType : "Unknown"; }
        public void setRegistrationType(String registrationType) { this.registrationType = registrationType; }
        
        public String getRegisteredBy() { return registeredBy != null ? registeredBy : "Unknown"; }
        public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
        
        public Timestamp getRegistrationDate() { return registrationDate; }
        public void setRegistrationDate(Timestamp registrationDate) { this.registrationDate = registrationDate; }
    }
}