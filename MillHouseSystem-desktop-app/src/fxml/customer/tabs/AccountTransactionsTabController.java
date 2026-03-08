package fxml.customer.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDateTime;

import fxml.CustomerSession;
import fxml.DatabaseConnection;

public class AccountTransactionsTabController {

    @FXML private Label customerNameLabel;
    @FXML private Label creditBalanceLabel;
    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableView<OrderHistory> orderHistoryTable;
    @FXML private Label recentOrderStatusLabel;
    private ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private ObservableList<OrderHistory> orderHistory = FXCollections.observableArrayList();

    @FXML 
    private void initialize() {
        loadCustomerInfo();
        loadTransactions();
        loadOrderHistory();
        loadRecentOrderStatus();
        setupTables();
    }
    
 // CHANGE FROM PRIVATE TO PUBLIC
 // Add method to load recent order status with live updates
    public void loadRecentOrderStatus() {
        int customerId = CustomerSession.getCustomerId();
        if (customerId <= 0) return;
        
        String sql = "SELECT o.id, o.status, o.order_type, o.raw_type, o.estimated_weight, " +
                    "o.assigned_driver, dl.delivered_date, dl.status as delivery_status, " +
                    "d.phone as driver_phone, o.driver_notes " +
                    "FROM orders o " +
                    "LEFT JOIN delivery_logs dl ON o.id = dl.order_id " +
                    "LEFT JOIN drivers d ON dl.driver_name = d.name " +
                    "WHERE o.customer_id = ? " +
                    "AND o.status NOT IN ('Delivered', 'Cancelled') " +
                    "ORDER BY o.created_date DESC LIMIT 5";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            
            StringBuilder statusMessage = new StringBuilder();
            statusMessage.append("🚚 Active Orders Status\n\n");
            
            boolean hasOrders = false;
            while (rs.next()) {
                hasOrders = true;
                int orderId = rs.getInt("id");
                String status = rs.getString("status");
                String orderType = rs.getString("order_type");
                String grainType = rs.getString("raw_type");
                double weight = rs.getDouble("estimated_weight");
                String driver = rs.getString("assigned_driver");
                String deliveryStatus = rs.getString("delivery_status");
                String driverPhone = rs.getString("driver_phone");
                String driverNotes = rs.getString("driver_notes");
                Timestamp deliveredDate = rs.getTimestamp("delivered_date");
                
                String orderInfo = String.format(
                    "📦 Order #%d: %s - %.1f kg %s\n" +
                    "📊 Status: %s\n",
                    orderId, orderType, weight, grainType, status
                );
                
                if (driver != null && !driver.isEmpty()) {
                    orderInfo += "🚚 Driver: " + driver + "\n";
                    if (driverPhone != null) {
                        orderInfo += "📞 Contact: " + driverPhone + "\n";
                    }
                }
                
                if (deliveryStatus != null && !deliveryStatus.isEmpty()) {
                    orderInfo += "📍 Delivery: " + deliveryStatus + "\n";
                }
                
                if (driverNotes != null && !driverNotes.isEmpty()) {
                    orderInfo += "📝 Note: " + driverNotes + "\n";
                }
                
                if (deliveredDate != null) {
                    orderInfo += "✅ Delivered: " + deliveredDate.toString().substring(0, 16) + "\n";
                }
                
                statusMessage.append(orderInfo).append("\n");
            }
            
            if (!hasOrders) {
                statusMessage.append("No active orders. All orders are completed or cancelled.");
            }
            
            if (recentOrderStatusLabel != null) {
                recentOrderStatusLabel.setText(statusMessage.toString());
                recentOrderStatusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-wrap-text: true;");
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading recent order status: " + e.getMessage());
        }
    }
 // ADD THIS METHOD
    public void refreshRecentOrderStatus() {
        loadRecentOrderStatus();
    }
    private void handleCancelOrder(OrderHistory order) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Order");
        confirm.setHeaderText("Cancel Order Confirmation");
        confirm.setContentText("Are you sure you want to cancel this order?\n\n" +
                              "Type: " + order.getOrderType() + "\n" +
                              "Grain: " + order.getGrainType() + "\n" +
                              "Weight: " + order.getWeight() + "kg");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Fix: Use order ID instead of multiple fields for identification
                String sql = "UPDATE orders SET status = 'Cancelled' WHERE id = ?";
                
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    
                    // We need to get the actual order ID
                    String getOrderIdSql = "SELECT id FROM orders WHERE customer_id = ? " +
                                          "AND raw_type = ? AND estimated_weight = ? " +
                                          "ORDER BY created_date DESC LIMIT 1";
                    
                    int orderId = -1;
                    try (PreparedStatement getIdPs = c.prepareStatement(getOrderIdSql)) {
                        getIdPs.setInt(1, CustomerSession.getCustomerId());
                        getIdPs.setString(2, order.getGrainType());
                        getIdPs.setDouble(3, order.getWeight());
                        
                        ResultSet rs = getIdPs.executeQuery();
                        if (rs.next()) {
                            orderId = rs.getInt("id");
                        }
                    }
                    
                    if (orderId > 0) {
                        ps.setInt(1, orderId);
                        int updated = ps.executeUpdate();
                        if (updated > 0) {
                            showAlert("Success", "Order #" + orderId + " cancelled successfully!", 
                                     Alert.AlertType.INFORMATION);
                            loadOrderHistory(); // Refresh the table
                        }
                    } else {
                        showAlert("Error", "Could not find the order to cancel", 
                                 Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Error", "Failed to cancel order: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    public void refreshOrderStatus() {
        int customerId = CustomerSession.getCustomerId();
        if (customerId <= 0) return;
        
        String sql = "SELECT o.id, o.status, o.order_type, o.raw_type, o.estimated_weight, " +
                    "o.assigned_driver, dl.delivered_date, dl.status as delivery_status, " +
                    "d.phone as driver_phone, o.driver_notes " +
                    "FROM orders o " +
                    "LEFT JOIN delivery_logs dl ON o.id = dl.order_id " +
                    "LEFT JOIN drivers d ON dl.driver_name = d.name " +
                    "WHERE o.customer_id = ? " +
                    "AND o.status NOT IN ('Delivered', 'Cancelled') " +
                    "ORDER BY o.created_date DESC LIMIT 5";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            
            StringBuilder statusMessage = new StringBuilder();
            statusMessage.append("🚚 Active Orders Status\n\n");
            
            boolean hasOrders = false;
            while (rs.next()) {
                hasOrders = true;
                int orderId = rs.getInt("id");
                String status = rs.getString("status");
                String orderType = rs.getString("order_type");
                String grainType = rs.getString("raw_type");
                double weight = rs.getDouble("estimated_weight");
                String driver = rs.getString("assigned_driver");
                String deliveryStatus = rs.getString("delivery_status");
                String driverPhone = rs.getString("driver_phone");
                String driverNotes = rs.getString("driver_notes");
                Timestamp deliveredDate = rs.getTimestamp("delivered_date");
                
                String orderInfo = String.format(
                    "📦 Order #%d: %s - %.1f kg %s\n" +
                    "📊 Status: %s\n",
                    orderId, orderType, weight, grainType, status
                );
                
                if (driver != null && !driver.isEmpty()) {
                    orderInfo += "🚚 Driver: " + driver + "\n";
                    if (driverPhone != null) {
                        orderInfo += "📞 Contact: " + driverPhone + "\n";
                    }
                }
                
                if (deliveryStatus != null && !deliveryStatus.isEmpty()) {
                    orderInfo += "📍 Delivery: " + deliveryStatus + "\n";
                }
                
                if (driverNotes != null && !driverNotes.isEmpty()) {
                    orderInfo += "📝 Note: " + driverNotes + "\n";
                }
                
                if (deliveredDate != null) {
                    orderInfo += "✅ Delivered: " + deliveredDate.toString().substring(0, 16) + "\n";
                }
                
                statusMessage.append(orderInfo).append("\n");
            }
            
            if (!hasOrders) {
                statusMessage.append("No active orders. All orders are completed or cancelled.");
            }
            
            if (recentOrderStatusLabel != null) {
                recentOrderStatusLabel.setText(statusMessage.toString());
                recentOrderStatusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-wrap-text: true;");
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading recent order status: " + e.getMessage());
        }
    }

    // Also update the refresh method
    public void refresh() {
        loadCustomerInfo();
        loadTransactions();
        loadOrderHistory();
        refreshOrderStatus(); // ADD THIS LINE
    }
    private void setupTables() {
        // Setup transactions table
        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDate()));
        
        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDescription()));
        
        TableColumn<Transaction, Double> amountCol = new TableColumn<>("Amount (ETB)");
        amountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getAmount()).asObject());
        
        transactionsTable.getColumns().setAll(dateCol, descCol, amountCol);
        transactionsTable.setItems(transactions);
        
        // Setup order history table
        TableColumn<OrderHistory, String> orderDateCol = new TableColumn<>("Order Date");
        orderDateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOrderDate()));
        
        TableColumn<OrderHistory, String> orderTypeCol = new TableColumn<>("Type");
        orderTypeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOrderType()));
        
        TableColumn<OrderHistory, String> grainCol = new TableColumn<>("Grain");
        grainCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getGrainType()));
        
        TableColumn<OrderHistory, Double> weightCol = new TableColumn<>("Weight (kg)");
        weightCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getWeight()).asObject());
        
        TableColumn<OrderHistory, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));
        
        // Add Action column for active orders
        TableColumn<OrderHistory, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(new Callback<TableColumn<OrderHistory, Void>, TableCell<OrderHistory, Void>>() {
            @Override
            public TableCell<OrderHistory, Void> call(TableColumn<OrderHistory, Void> param) {
                return new TableCell<OrderHistory, Void>() {
                    private final Button repeatBtn = new Button("Repeat");
                    private final Button cancelBtn = new Button("Cancel");
                    private final HBox hbox = new HBox(5, repeatBtn, cancelBtn);
                    
                    {
                        repeatBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px;");
                        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
                        
                        repeatBtn.setOnAction(event -> {
                            OrderHistory order = getTableView().getItems().get(getIndex());
                            handleRepeatOrder();
                        });
                        
                        cancelBtn.setOnAction(event -> {
                            OrderHistory order = getTableView().getItems().get(getIndex());
                            handleCancelOrder(order);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            OrderHistory order = getTableView().getItems().get(getIndex());
                            // Only show cancel button for pending/processing orders
                            if ("Pending".equals(order.getStatus()) || "Processing".equals(order.getStatus())) {
                                cancelBtn.setVisible(true);
                            } else {
                                cancelBtn.setVisible(false);
                            }
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });
        
        orderHistoryTable.getColumns().setAll(orderDateCol, orderTypeCol, grainCol, weightCol, statusCol, actionCol);
        orderHistoryTable.setItems(orderHistory);
    }

    private void loadCustomerInfo() {
        int customerId = CustomerSession.getCustomerId();
        String sql = "SELECT name, credit_balance_ETB FROM customers WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                customerNameLabel.setText(rs.getString("name"));
                creditBalanceLabel.setText("ETB " + String.format("%.2f", rs.getDouble("credit_balance_ETB")));
            }
        } catch (SQLException e) { 
            e.printStackTrace();
            showAlert("Error", "Failed to load customer info: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadTransactions() {
        transactions.clear();
        int customerId = CustomerSession.getCustomerId();
        String sql = "SELECT transaction_date, 'Order Payment' as description, payment_amount_ETB " +
                    "FROM transactions WHERE customer_id = ? " +
                    "ORDER BY transaction_date DESC LIMIT 20";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setDate(rs.getTimestamp("transaction_date").toLocalDateTime().toString());
                t.setDescription(rs.getString("description"));
                t.setAmount(rs.getDouble("payment_amount_ETB"));
                transactions.add(t);
            }
            
         
        }catch (SQLException e) { 
            // Don't load sample data - just show empty table
            System.out.println("No transactions found: " + e.getMessage());
        }
    }
    
    @FXML 
    private void handleChangePassword() {
        // Create password change dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Change Your Account Password");
        
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
                int customerId = CustomerSession.getCustomerId();
                
                // Verify current password
                String verifySql = "SELECT password FROM customers WHERE id = ?";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(verifySql)) {
                    ps.setInt(1, customerId);
                    ResultSet rs = ps.executeQuery();
                    
                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        if (dbPassword == null) {
                            showAlert("Error", 
                                     "You don't have password access. Please contact administrator.",
                                     Alert.AlertType.ERROR);
                            return;
                        }
                        
                        if (!currentPassword.equals(dbPassword)) {
                            showAlert("Error", "Current password is incorrect", Alert.AlertType.ERROR);
                            return;
                        }
                        
                        // Update password
                        String updateSql = "UPDATE customers SET password = ? WHERE id = ?";
                        try (PreparedStatement updatePs = c.prepareStatement(updateSql)) {
                            updatePs.setString(1, newPassword);
                            updatePs.setInt(2, customerId);
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
    private void loadOrderHistory() {
        orderHistory.clear();
        int customerId = CustomerSession.getCustomerId();
        
        // Load ALL orders for the customer
        String sql = "SELECT created_date, order_type, raw_type, estimated_weight, status " +
                    "FROM orders WHERE customer_id = ? ORDER BY created_date DESC";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                OrderHistory oh = new OrderHistory();
                oh.setOrderDate(rs.getTimestamp("created_date").toLocalDateTime().toString());
                oh.setOrderType(rs.getString("order_type"));
                oh.setGrainType(rs.getString("raw_type"));
                oh.setWeight(rs.getDouble("estimated_weight"));
                oh.setStatus(rs.getString("status"));
                orderHistory.add(oh);
            }
            
            System.out.println("Loaded " + orderHistory.size() + " orders for customer #" + customerId);
            
        } catch (SQLException e) { 
            e.printStackTrace();
            showAlert("Error", "Failed to load order history: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void loadSampleOrderHistory() {
        // Sample data for testing
        orderHistory.add(new OrderHistory("2024-01-15", "Grain Pickup + Milling", "Wheat", 100.0, "Completed"));
        orderHistory.add(new OrderHistory("2024-01-10", "Milling Only", "Teff", 50.0, "Completed"));
        orderHistory.add(new OrderHistory("2024-01-05", "Home Delivery", "Corn", 75.0, "In Progress"));
    }
    
    // ADD THIS METHOD to handle repeat order from history
    @FXML 
    private void handleRepeatOrder() {
        OrderHistory selected = orderHistoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Please select an order from history to repeat", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Repeat Order");
        confirm.setHeaderText("Repeat Order Confirmation");
        confirm.setContentText("Create a new order with:\n" +
                              "Type: " + selected.getOrderType() + "\n" +
                              "Grain: " + selected.getGrainType() + "\n" +
                              "Weight: " + selected.getWeight() + "kg\n\n" +
                              "Do you want to proceed?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Here you could navigate to Order Placement tab and pre-fill the form
                // or directly create a new order in the database
                
                String sql = "INSERT INTO orders (customer_id, order_type, raw_type, estimated_weight, status) VALUES (?, ?, ?, ?, 'Pending')";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, CustomerSession.getCustomerId());
                    ps.setString(2, selected.getOrderType());
                    ps.setString(3, selected.getGrainType());
                    ps.setDouble(4, selected.getWeight());
                    ps.executeUpdate();
                    
                    showAlert("Success", 
                              "New order created based on your selected history!\n" +
                              "You can view it in the 'Place Order' tab.", 
                              Alert.AlertType.INFORMATION);
                    
                    // Refresh the order history
                    loadOrderHistory();
                } catch (SQLException e) {
                    showAlert("Error", "Failed to create repeat order: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }


    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public static class Transaction {
        private String date;
        private String description;
        private double amount;
        
        public Transaction() {}
        
        public Transaction(String date, String description, double amount) {
            this.date = date;
            this.description = description;
            this.amount = amount;
        }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }
    
    public static class OrderHistory {
        private String orderDate;
        private String orderType;
        private String grainType;
        private double weight;
        private String status;
        
        public OrderHistory() {}
        
        public OrderHistory(String orderDate, String orderType, String grainType, double weight, String status) {
            this.orderDate = orderDate;
            this.orderType = orderType;
            this.grainType = grainType;
            this.weight = weight;
            this.status = status;
        }
        
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        
        public String getGrainType() { return grainType; }
        public void setGrainType(String grainType) { this.grainType = grainType; }
        
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}