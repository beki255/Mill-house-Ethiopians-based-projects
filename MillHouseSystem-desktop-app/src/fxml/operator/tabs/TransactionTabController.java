package fxml.operator.tabs;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import fxml.CurrentUser;
import fxml.DatabaseConnection;
import fxml.GrainTypeManager;

public class TransactionTabController {

    @FXML public TextField customerNameField;
    @FXML public TextField customerPhoneField;
    @FXML private ComboBox<String> grainTypeCombo;
    @FXML private TextField weightField;
    @FXML private ComboBox<String> paymentTypeCombo;
    @FXML private Label calculatedAmountLabel;
    @FXML private TableView<Transaction> transactionTable;

    private ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    public Object mainTabPane;

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

    @FXML 
    private void initialize() {
        grainTypeCombo.setItems(GrainTypeManager.getGrainTypes());
        paymentTypeCombo.setItems(FXCollections.observableArrayList("Cash", "Credit", "Toll", "Mobile Money"));
        
        // IMPORTANT: Initialize table only once
        if (transactionTable.getColumns().isEmpty()) {
            setupTable();
        }
        
        weightField.textProperty().addListener((o, ov, nv) -> calculateAmount());
        grainTypeCombo.valueProperty().addListener((o, ov, nv) -> calculateAmount());
        loadTransactions();
        grainTypeCombo.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                GrainTypeManager.refreshGrainTypes();
            }
        });
    }
    
    private void setupTable() {
        // Clear existing columns to avoid duplication
        if (!transactionTable.getColumns().isEmpty()) {
            return; // Table already initialized
        }
        
        TableColumn<Transaction, String> cust = new TableColumn<>("Customer");
        cust.setCellValueFactory(d -> {
            String name = d.getValue().getCustomerName();
            return new SimpleStringProperty(name != null ? name : "Unknown");
        });
        cust.setPrefWidth(150);

        TableColumn<Transaction, String> grain = new TableColumn<>("Grain");
        grain.setCellValueFactory(d -> {
            String type = d.getValue().getRawType();
            return new SimpleStringProperty(type != null ? type : "");
        });
        grain.setPrefWidth(100);

        TableColumn<Transaction, Double> weight = new TableColumn<>("Weight");
        weight.setCellValueFactory(d -> 
            new SimpleDoubleProperty(d.getValue().getRawWeight()).asObject());
        weight.setPrefWidth(80);

        TableColumn<Transaction, Double> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(d -> 
            new SimpleDoubleProperty(d.getValue().getPaymentAmount()).asObject());
        amount.setPrefWidth(100);

        TableColumn<Transaction, String> pay = new TableColumn<>("Payment");
        pay.setCellValueFactory(d -> {
            String type = d.getValue().getPaymentType();
            return new SimpleStringProperty(type != null ? type : "");
        });
        pay.setPrefWidth(100);

        TableColumn<Transaction, String> time = new TableColumn<>("Time");
        time.setCellValueFactory(d -> {
            LocalDateTime date = d.getValue().getTransactionDate();
            if (date != null) {
                return new SimpleStringProperty(date.format(DATE_FORMATTER));
            } else {
                return new SimpleStringProperty("");
            }
        });
        time.setPrefWidth(150);
        
        TableColumn<Transaction, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(d -> {
            String source = d.getValue().getOrderSource();
            return new SimpleStringProperty(source != null ? source : "");
        });
        sourceCol.setPrefWidth(100);
        sourceCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String source, boolean empty) {
                super.updateItem(source, empty);
                if (empty || source == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(source);
                    if ("Customer Portal".equals(source)) {
                        setStyle("-fx-text-fill: #9b59b6; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    }
                }
            }
        });
        


        TableColumn<Transaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> {
            String status = d.getValue().getOrderStatus();
            String source = d.getValue().getOrderSource();
            
            if ("Customer Portal".equals(source) && status == null) {
                return new SimpleStringProperty("Delivered");
            } else if (status != null) {
                return new SimpleStringProperty(status);
            } else {
                return new SimpleStringProperty("Completed");
            }
        });
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(status);
                    if ("Delivered".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Add columns only once
        transactionTable.getColumns().addAll(cust, grain, weight, amount, pay, time, sourceCol);
        transactionTable.setItems(transactions);
    }

    @FXML
    private void handleSyncDeliveredOrders() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sync Delivered Orders");
        confirm.setHeaderText("Sync Customer Orders to Transactions");
        confirm.setContentText("This will find all delivered customer orders and add them to transactions.\n" +
                             "Only orders marked as 'Delivered' will be synced.\n\n" +
                             "Continue?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int synced = syncAllDeliveredOrders();
                
                if (synced > 0) {
                    showAlert("Success", 
                             "Successfully synced " + synced + " delivered orders to transactions!\n" +
                             "Refresh the table to see the new entries.",
                             Alert.AlertType.INFORMATION);
                    loadTransactions(); // Refresh the table
                } else {
                    showAlert("Info", 
                             "No new delivered orders to sync.\n" +
                             "All delivered orders are already in transactions.",
                             Alert.AlertType.INFORMATION);
                }
            }
        });
    }
    private int syncAllDeliveredOrders() {
        String sql = "INSERT INTO transactions (customer_id, operator_id, raw_type, raw_weight_kg, " +
                    "payment_amount_ETB, payment_type, transaction_date, order_source) " +
                    "SELECT DISTINCT " +
                    "  o.customer_id, " +
                    "  (SELECT id FROM users WHERE role = 'OPERATOR' LIMIT 1) as operator_id, " +
                    "  o.raw_type, " +
                    "  o.estimated_weight, " +
                    "  COALESCE(o.payment_amount, s.price_per_kg * o.estimated_weight, 0) as payment_amount, " +
                    "  COALESCE(o.payment_method, 'Credit'), " +
                    "  COALESCE(o.delivery_completed_date, o.created_date, NOW()), " +  // Changed here
                    "  'Customer Portal' " +
                    "FROM orders o " +
                    "LEFT JOIN settings s ON o.raw_type = s.grain_type " +
                    // Removed: LEFT JOIN delivery_logs dl ON dl.order_id = o.id
                    "WHERE o.status = 'Delivered' " +
                    "  AND o.payment_method IS NOT NULL " +
                    "  AND COALESCE(o.payment_amount, s.price_per_kg * o.estimated_weight, 0) > 0 " +
                    "  AND NOT EXISTS ( " +
                    "    SELECT 1 FROM transactions t " +
                    "    WHERE t.customer_id = o.customer_id " +
                    "      AND t.raw_type = o.raw_type " +
                    "      AND ABS(t.raw_weight_kg - o.estimated_weight) < 0.5 " +
                    "      AND DATE(t.transaction_date) = DATE(o.created_date) " +
                    "  )";
        
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement()) {
            
            return s.executeUpdate(sql);
            
        } catch (SQLException e) {
            System.err.println("Error syncing all delivered orders: " + e.getMessage());
            return 0;
        }
    }
    
    private void calculateAmount() {
        try {
            if (grainTypeCombo.getValue() == null || weightField.getText().isEmpty()) {
                calculatedAmountLabel.setText("ETB 0.00");
                return;
            }
            double weight = Double.parseDouble(weightField.getText());
            if (weight <= 0) {
                calculatedAmountLabel.setText("ETB 0.00");
                return;
            }
            double price = getPricePerKg(grainTypeCombo.getValue());
            calculatedAmountLabel.setText(String.format("ETB %.2f", weight * price));
        } catch (NumberFormatException e) {
            calculatedAmountLabel.setText("ETB 0.00");
        }
    }

    private double getPricePerKg(String grain) {
        if (grain == null) return 45.0;
        
        String sql = "SELECT price_per_kg FROM settings WHERE grain_type = ?";
        try (Connection c = DatabaseConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, grain);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { 
            e.printStackTrace();
        }
        return 45.0; // Default price
    }

    @FXML 
    private void handleTransaction() {
        // Validate form
        if (customerNameField.getText().trim().isEmpty()) {
            showAlert("Error", "Customer name is required!", Alert.AlertType.ERROR);
            return;
        }
        
        // Validate phone number
        String phone = customerPhoneField.getText().trim();
        if (phone.isEmpty()) {
            showAlert("Error", "Customer phone number is required!", Alert.AlertType.ERROR);
            return;
        }
        
        // Validate phone format (10 digits, starts with 09 or 07)
        if (phone.length() != 10 || (!phone.startsWith("09") && !phone.startsWith("07"))) {
            showAlert("Error", "Phone must be 10 digits starting with 09 or 07!", Alert.AlertType.ERROR);
            return;
        }
        
        if (grainTypeCombo.getValue() == null) {
            showAlert("Error", "Please select a grain type!", Alert.AlertType.ERROR);
            return;
        }
        
        if (weightField.getText().trim().isEmpty()) {
            showAlert("Error", "Weight is required!", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            double weight = Double.parseDouble(weightField.getText());
            if (weight <= 0) {
                showAlert("Error", "Weight must be greater than 0!", Alert.AlertType.ERROR);
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid weight value!", Alert.AlertType.ERROR);
            return;
        }
        
        if (paymentTypeCombo.getValue() == null) {
            showAlert("Error", "Please select a payment type!", Alert.AlertType.ERROR);
            return;
        }
        
        // Calculate amount
        double weight = Double.parseDouble(weightField.getText());
        double price = getPricePerKg(grainTypeCombo.getValue());
        double amount = weight * price;
        
        // MODIFIED: Check customer exists (both portal and operator registered)
        try (Connection c = DatabaseConnection.getConnection()) {
            int customerId = getCustomerId(c, phone);
            
            if (customerId == -1) {
                // Customer doesn't exist - create operator-registered customer
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("New Customer Detected");
                confirm.setHeaderText("Customer Not Found");
                confirm.setContentText("Customer with phone " + phone + " is not registered.\n" +
                                     "Do you want to:\n" +
                                     "1. Create new customer (operator-registered) AND process transaction\n" +
                                     "2. Cancel transaction");
                
                confirm.getButtonTypes().setAll(
                    new ButtonType("Create & Process", ButtonBar.ButtonData.YES),
                    ButtonType.CANCEL
                );
                
                confirm.showAndWait().ifPresent(response -> {
                    if (response.getButtonData() == ButtonBar.ButtonData.YES) {
                        // Create customer and process transaction
                        boolean success = createCustomerAndTransaction(
                            customerNameField.getText().trim(),
                            phone,
                            grainTypeCombo.getValue(),
                            weight,
                            amount,
                            paymentTypeCombo.getValue()
                        );
                        
                        if (success) {
                            showAlert("Success", 
                                     "New customer created and transaction processed!", 
                                     Alert.AlertType.INFORMATION);
                            clearForm();
                            loadTransactions();
                        }
                    }
                });
                return;
            }
            
            // Customer exists - proceed with normal transaction
            String customerName = customerNameField.getText().trim();
            String registeredName = getCustomerName(c, customerId);
            
            if (!customerName.equalsIgnoreCase(registeredName)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Customer Name Mismatch");
                confirm.setHeaderText("Name Verification");
                confirm.setContentText("Entered name: " + customerName + "\n" +
                                     "Registered name: " + registeredName + "\n\n" +
                                     "Continue anyway?");
                
                confirm.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        processTransaction(customerId, customerName, phone,
                                          grainTypeCombo.getValue(), weight, 
                                          amount, paymentTypeCombo.getValue());
                    }
                });
            } else {
                processTransaction(customerId, customerName, phone,
                                 grainTypeCombo.getValue(), weight, 
                                 amount, paymentTypeCombo.getValue());
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private boolean createCustomerAndTransaction(String customerName, String phone, 
                                                String grainType, double weight, 
                                                double amount, String paymentType) {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return false;
        
        try {
            boolean originalAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            
            try {
                // 1. Create new customer (operator registered)
                String operatorUsername = CurrentUser.getInstance().getUsername();
                if (operatorUsername == null) operatorUsername = "Operator";
                
                String insertCustomerSql = "INSERT INTO customers (name, phone, delivery_address, " +
                                          "credit_balance_ETB, registration_type, registered_by, registration_date) " +
                                          "VALUES (?, ?, 'Address not provided', 0.0, 'OPERATOR', ?, NOW())";
                
                int customerId = -1;
                try (PreparedStatement ps = c.prepareStatement(insertCustomerSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, customerName);
                    ps.setString(2, phone);
                    ps.setString(3, operatorUsername);
                    ps.executeUpdate();
                    
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        customerId = rs.getInt(1);
                    }
                }
                
                if (customerId == -1) {
                    c.rollback();
                    return false;
                }
                
                // 2. Get operator ID
                int operatorId = CurrentUser.getInstance().getStaffId();
                if (!operatorExistsInUsers(c, operatorId)) {
                    operatorId = getDefaultOperatorId(c);
                }
                
                // 3. Insert transaction
                String insertTransactionSql = "INSERT INTO transactions (customer_id, operator_id, raw_type, " +
                                            "raw_weight_kg, payment_amount_ETB, payment_type, transaction_date) " +
                                            "VALUES (?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement ps = c.prepareStatement(insertTransactionSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, operatorId);
                    ps.setString(3, grainType);
                    ps.setDouble(4, weight);
                    ps.setDouble(5, amount);
                    ps.setString(6, paymentType);
                    ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
                
                // 4. If credit payment, check balance (new customer has 0 balance)
                if ("Credit".equalsIgnoreCase(paymentType)) {
                    showAlert("Warning", 
                             "New customer has 0 credit balance!\n" +
                             "Transaction will fail. Please use cash or mobile money.",
                             Alert.AlertType.WARNING);
                    c.rollback();
                    return false;
                }
                
                c.commit();
                return true;
                
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(originalAutoCommit);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to create customer and transaction: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
            return false;
        }
    }
    
    private void processTransaction(int customerId, String customerName, String phone,
                                   String grainType, double weight, double amount, 
                                   String paymentType) {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) {
            showAlert("Error", "Cannot connect to database", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            boolean originalAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            
            try {
                // Get operator ID
                int operatorId = CurrentUser.getInstance().getStaffId();
                if (!operatorExistsInUsers(c, operatorId)) {
                    operatorId = getDefaultOperatorId(c);
                }
                
                // Insert transaction
                String sql = "INSERT INTO transactions (customer_id, operator_id, raw_type, " +
                            "raw_weight_kg, payment_amount_ETB, payment_type, transaction_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, operatorId);
                    ps.setString(3, grainType);
                    ps.setDouble(4, weight);
                    ps.setDouble(5, amount);
                    ps.setString(6, paymentType);
                    ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                    
                    int rows = ps.executeUpdate();
                    
                    if (rows > 0) {
                        // If payment is by credit, deduct from customer's balance
                        if ("Credit".equalsIgnoreCase(paymentType)) {
                            deductCreditFromCustomer(c, customerId, amount);
                        }
                        
                        c.commit();
                        
                        showAlert("Success", 
                                 "Transaction saved successfully!\n" +
                                 "Customer: " + customerName + "\n" +
                                 "Amount: ETB " + String.format("%.2f", amount),
                                 Alert.AlertType.INFORMATION);
                        
                        clearForm();
                        loadTransactions();
                    }
                }
                
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(originalAutoCommit);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save transaction: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    // ... rest of the existing methods remain the same ...

    private boolean saveTransactionToDatabase(String customerName, String phone, String grainType, 
            double weight, double amount, String paymentType) {
Connection c = DatabaseConnection.getConnection();
if (c == null) {
showAlert("Error", "Cannot connect to database", Alert.AlertType.ERROR);
return false;
}

try {
boolean originalAutoCommit = c.getAutoCommit();
c.setAutoCommit(false);

try {
// CHECK IF CUSTOMER EXISTS (must be registered in Customer Portal)
int customerId = getCustomerId(c, phone);
if (customerId == -1) {
showAlert("Error", 
"Customer not found!\n\n" +
"Customer with phone " + phone + " is not registered.\n" +
"Customers must register through the Customer Portal first.",
Alert.AlertType.ERROR);
return false;
}

// Verify customer name matches
if (!verifyCustomerName(c, customerId, customerName)) {
Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
confirm.setTitle("Customer Name Mismatch");
confirm.setHeaderText("Customer Name Verification");
confirm.setContentText("Entered name: " + customerName + "\n" +
         "Registered name: " + getCustomerName(c, customerId) + "\n\n" +
         "Continue anyway?");

Optional<ButtonType> result = confirm.showAndWait();
if (!result.isPresent() || result.get() != ButtonType.OK) {
return false;
}
}

// Get current operator ID
int operatorId = CurrentUser.getInstance().getStaffId();

// Check if operator exists in users table
if (!operatorExistsInUsers(c, operatorId)) {
operatorId = getDefaultOperatorId(c);
}

// Insert transaction
String sql = "INSERT INTO transactions (customer_id, operator_id, raw_type, raw_weight_kg, " +
"payment_amount_ETB, payment_type, transaction_date) " +
"VALUES (?, ?, ?, ?, ?, ?, ?)";

try (PreparedStatement ps = c.prepareStatement(sql)) {
ps.setInt(1, customerId);
ps.setInt(2, operatorId);
ps.setString(3, grainType);
ps.setDouble(4, weight);
ps.setDouble(5, amount);
ps.setString(6, paymentType);
ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));

int rows = ps.executeUpdate();

// If payment is by credit, deduct from customer's balance
if ("Credit".equalsIgnoreCase(paymentType)) {
deductCreditFromCustomer(c, customerId, amount);
}

c.commit(); // Commit transaction
return rows > 0;
}

} catch (SQLException e) {
c.rollback();
throw e;
} finally {
c.setAutoCommit(originalAutoCommit); // Restore original auto-commit state
}

} catch (SQLException e) {
e.printStackTrace();
showAlert("Database Error", "Failed to save transaction: " + e.getMessage(), Alert.AlertType.ERROR);
return false;
}
}
private int getCustomerId(Connection c, String phone) throws SQLException {
String sql = "SELECT id FROM customers WHERE phone = ?";
try (PreparedStatement ps = c.prepareStatement(sql)) {
ps.setString(1, phone);
ResultSet rs = ps.executeQuery();
return rs.next() ? rs.getInt("id") : -1;
}
}

private boolean verifyCustomerName(Connection c, int customerId, String enteredName) throws SQLException {
String sql = "SELECT name FROM customers WHERE id = ?";
try (PreparedStatement ps = c.prepareStatement(sql)) {
ps.setInt(1, customerId);
ResultSet rs = ps.executeQuery();
if (rs.next()) {
String registeredName = rs.getString("name");
return registeredName.equalsIgnoreCase(enteredName.trim());
}
}
return false;
}

private String getCustomerName(Connection c, int customerId) throws SQLException {
String sql = "SELECT name FROM customers WHERE id = ?";
try (PreparedStatement ps = c.prepareStatement(sql)) {
ps.setInt(1, customerId);
ResultSet rs = ps.executeQuery();
return rs.next() ? rs.getString("name") : "Unknown";
}
}

private void deductCreditFromCustomer(Connection c, int customerId, double amount) throws SQLException {
String sql = "UPDATE customers SET credit_balance_ETB = credit_balance_ETB - ? WHERE id = ? AND credit_balance_ETB >= ?";
try (PreparedStatement ps = c.prepareStatement(sql)) {
ps.setDouble(1, amount);
ps.setInt(2, customerId);
ps.setDouble(3, amount);
int rows = ps.executeUpdate();

if (rows == 0) {
throw new SQLException("Insufficient credit balance for customer ID: " + customerId);
}
}
}

private boolean operatorExistsInUsers(Connection c, int operatorId) throws SQLException {
String sql = "SELECT id FROM users WHERE id = ?";
try (PreparedStatement ps = c.prepareStatement(sql)) {
ps.setInt(1, operatorId);
ResultSet rs = ps.executeQuery();
return rs.next();
}
}

private int getDefaultOperatorId(Connection c) throws SQLException {
// Try to get admin user (id=1) or first available user
String sql = "SELECT id FROM users ORDER BY id LIMIT 1";
try (PreparedStatement ps = c.prepareStatement(sql)) {
ResultSet rs = ps.executeQuery();
if (rs.next()) {
return rs.getInt("id");
}
}
// If no users exist, we have a problem - but for now return 1
return 1;
}
    private int getOrCreateCustomer(Connection c, String customerName, String phone) throws SQLException {
        // Try to find existing customer by phone (more reliable than name)
        String findSql = "SELECT id FROM customers WHERE phone = ?";
        try (PreparedStatement ps = c.prepareStatement(findSql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        
        // Create new customer with phone
        String insertSql = "INSERT INTO customers (name, phone, credit_balance_ETB) VALUES (?, ?, 0.0)";
        try (PreparedStatement ps = c.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customerName);
            ps.setString(2, phone);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to get or create customer");
    }

    private void clearForm() {
        customerNameField.clear();
        customerPhoneField.clear();
        grainTypeCombo.setValue(null);
        weightField.clear();
        paymentTypeCombo.setValue(null);
        calculatedAmountLabel.setText("ETB 0.00");
        customerNameField.requestFocus(); // Set focus back to first field
    }

    private void loadTransactions() {
        transactions.clear();

        String sql = """
            -- Manual transactions entered by operator
            SELECT 
                t.id,
                COALESCE(c.name, 'Unknown') AS customer_name,
                t.raw_type,
                t.raw_weight_kg AS raw_weight_kg,
                t.payment_amount_ETB AS payment_amount_ETB,
                t.payment_type,
                t.transaction_date,
                'Operator' AS order_source,
                NULL AS order_status
            FROM transactions t
            LEFT JOIN customers c ON t.customer_id = c.id

            UNION ALL

            -- Delivered orders from customer portal (not yet in transactions)
            SELECT 
                NULL AS id,
                c.name AS customer_name,
                o.raw_type,
                o.estimated_weight AS raw_weight_kg,
                COALESCE(o.payment_amount, s.price_per_kg * o.estimated_weight, 0) AS payment_amount_ETB,
                COALESCE(o.payment_method, 'Credit') AS payment_type,
                o.created_date AS transaction_date,  -- Use order creation date
                'Customer Portal' AS order_source,
                o.status AS order_status
            FROM orders o
            LEFT JOIN customers c ON o.customer_id = c.id
            LEFT JOIN settings s ON o.raw_type = s.grain_type
            WHERE o.status = 'Delivered'
              AND o.payment_method IS NOT NULL
              AND COALESCE(o.payment_amount, s.price_per_kg * o.estimated_weight, 0) > 0
              AND NOT EXISTS (
                  SELECT 1 FROM transactions t2
                  WHERE t2.customer_id = o.customer_id
                    AND t2.raw_type = o.raw_type
                    AND ABS(t2.raw_weight_kg - o.estimated_weight) < 0.5
                    AND DATE(t2.transaction_date) = DATE(o.created_date)
              )

            ORDER BY transaction_date DESC
            LIMIT 100
            """;

        try (Connection c = DatabaseConnection.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Transaction transaction = new Transaction();

                transaction.setCustomerName(rs.getString("customer_name"));
                transaction.setRawType(rs.getString("raw_type"));
                transaction.setRawWeight(rs.getDouble("raw_weight_kg"));
                transaction.setPaymentAmount(rs.getDouble("payment_amount_ETB"));
                transaction.setPaymentType(rs.getString("payment_type"));
                transaction.setOrderSource(rs.getString("order_source"));

                Timestamp ts = rs.getTimestamp("transaction_date");
                if (ts != null) {
                    transaction.setTransactionDate(ts.toLocalDateTime());
                } else {
                    transaction.setTransactionDate(LocalDateTime.now());
                }

                String status = rs.getString("order_status");
                if (status != null) {
                    transaction.setOrderStatus(status);
                }

                transactions.add(transaction);
            }

            System.out.println("Successfully loaded " + transactions.size() + " transactions (including delivered portal orders)");

        } catch (SQLException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            loadSampleTransactions();
        }
    }
    private void syncDeliveredOrders() {
        // First, ensure the delivery_logs table has the delivered_date column
        ensureDeliveryLogsColumns();
        
        String sql = "INSERT INTO transactions (customer_id, operator_id, raw_type, raw_weight_kg, " +
                    "payment_amount_ETB, payment_type, transaction_date, order_source) " +
                    "SELECT " +
                    "  o.customer_id, " +
                    "  (SELECT id FROM users WHERE role = 'OPERATOR' LIMIT 1) as operator_id, " +
                    "  o.raw_type, " +
                    "  o.estimated_weight, " +
                    "  COALESCE(o.payment_amount, s.price_per_kg * o.estimated_weight) as payment_amount, " +
                    "  COALESCE(o.payment_method, 'Credit'), " +
                    "  COALESCE(o.created_date, NOW()), " +  // Use order creation date instead
                    "  'Customer Portal' " +
                    "FROM orders o " +
                    "LEFT JOIN settings s ON o.raw_type = s.grain_type " +
                    "WHERE o.status = 'Delivered' " +
                    "  AND NOT EXISTS ( " +
                    "    SELECT 1 FROM transactions t " +
                    "    WHERE t.customer_id = o.customer_id " +
                    "      AND t.raw_type = o.raw_type " +
                    "      AND ABS(t.raw_weight_kg - o.estimated_weight) < 0.1 " +
                    "      AND DATE(t.transaction_date) = DATE(o.created_date) " +
                    "  )";
        
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement()) {
            
            int synced = s.executeUpdate(sql);
            if (synced > 0) {
                System.out.println("Auto-synced " + synced + " delivered orders to transactions");
            }
            
        } catch (SQLException e) {
            System.err.println("Error syncing delivered orders: " + e.getMessage());
        }
    }

    private void ensureDeliveryLogsColumns() {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement()) {
            
            // Check if delivered_date column exists
            String checkSql = "SHOW COLUMNS FROM delivery_logs LIKE 'delivered_date'";
            ResultSet rs = s.executeQuery(checkSql);
            if (!rs.next()) {
                // Add the missing column
                String alterSql = "ALTER TABLE delivery_logs ADD COLUMN delivered_date TIMESTAMP NULL";
                s.executeUpdate(alterSql);
                System.out.println("Added delivered_date column to delivery_logs table");
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking/adding delivered_date column: " + e.getMessage());
        }
    }
    private void loadSampleTransactions() {
        System.out.println("Loading sample transactions...");
        
        // Create proper sample data with all fields populated
        Transaction t1 = new Transaction();
        t1.setCustomerName("John Doe");
        t1.setRawType("Wheat");
        t1.setRawWeight(100.0);
        t1.setPaymentAmount(4500.0);
        t1.setPaymentType("Cash");
        t1.setTransactionDate(LocalDateTime.now().minusHours(2));
        transactions.add(t1);
        
        Transaction t2 = new Transaction();
        t2.setCustomerName("Jane Smith");
        t2.setRawType("Teff");
        t2.setRawWeight(75.5);
        t2.setPaymentAmount(4152.5);
        t2.setPaymentType("Mobile Money");
        t2.setTransactionDate(LocalDateTime.now().minusDays(1));
        transactions.add(t2);
        
        Transaction t3 = new Transaction();
        t3.setCustomerName("Bob Wilson");
        t3.setRawType("Corn");
        t3.setRawWeight(120.0);
        t3.setPaymentAmount(5400.0);
        t3.setPaymentType("Credit");
        t3.setTransactionDate(LocalDateTime.now().minusDays(3));
        transactions.add(t3);
        
        System.out.println("Loaded " + transactions.size() + " sample transactions");
    }
    
    public void refresh() { 
        loadTransactions(); 
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    
    // Inner Transaction class
    public static class Transaction {
        private String customerName;
        private String rawType;
        private String paymentType;
        private String orderStatus;
        private String orderSource;
        private double rawWeight;
        private double paymentAmount;
        private LocalDateTime transactionDate;
        public String getOrderStatus() { return orderStatus; }
        public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
        
        public String getOrderSource() { return orderSource; }
        public void setOrderSource(String orderSource) { this.orderSource = orderSource; }
        // Default constructor
        public Transaction() {
            this.transactionDate = LocalDateTime.now(); // Initialize with current time
        }
        
        // Constructor with parameters
        public Transaction(String customerName, String rawType, String paymentType, 
                          double rawWeight, double paymentAmount, LocalDateTime transactionDate) {
            this.customerName = customerName;
            this.rawType = rawType;
            this.paymentType = paymentType;
            this.rawWeight = rawWeight;
            this.paymentAmount = paymentAmount;
            this.transactionDate = transactionDate != null ? transactionDate : LocalDateTime.now();
        }
        
        // Getters & setters
        public String getCustomerName() { 
            return customerName != null ? customerName : ""; 
        }
        
        public void setCustomerName(String s) { 
            customerName = s; 
        }
        
        public String getRawType() { 
            return rawType != null ? rawType : ""; 
        }
        
        public void setRawType(String s) { 
            rawType = s; 
        }
        
        public double getRawWeight() { 
            return rawWeight; 
        }
        
        public void setRawWeight(double d) { 
            rawWeight = d; 
        }
        
        public double getPaymentAmount() { 
            return paymentAmount; 
        }
        
        public void setPaymentAmount(double d) { 
            paymentAmount = d; 
        }
        
        public String getPaymentType() { 
            return paymentType != null ? paymentType : ""; 
        }
        
        public void setPaymentType(String s) { 
            paymentType = s; 
        }
        
        public LocalDateTime getTransactionDate() { 
            return transactionDate != null ? transactionDate : LocalDateTime.now(); 
        }
        
        public void setTransactionDate(LocalDateTime d) { 
            transactionDate = d; 
        }
    }
}