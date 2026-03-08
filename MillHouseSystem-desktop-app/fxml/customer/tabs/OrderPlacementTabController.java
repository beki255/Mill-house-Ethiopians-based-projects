package fxml.customer.tabs;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.sql.*;

import fxml.DatabaseConnection;
import fxml.GrainTypeManager;
import fxml.CustomerSession;

public class OrderPlacementTabController {

    @FXML private ComboBox<String> orderTypeCombo;
    @FXML private ComboBox<String> grainTypeCombo;
    @FXML private TextField estimatedWeightField;
    @FXML private ComboBox<String> deliveryTypeCombo;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private Label estimatedCostLabel;
    @FXML private TableView<Order> activeOrdersTable;
    @FXML private Label orderStatusInfoLabel;
    @FXML private Label customerBalanceLabel;

    @FXML 
    private void initialize() {
        orderTypeCombo.setItems(FXCollections.observableArrayList(
            "Grain Pickup + Milling", "Milling Only", "Delivery Only"
        ));
        grainTypeCombo.setItems(GrainTypeManager.getGrainTypes());
        grainTypeCombo.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                GrainTypeManager.refreshGrainTypes();
            }
        });
        deliveryTypeCombo.setItems(FXCollections.observableArrayList("Pickup", "Home Delivery"));
        paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash on Delivery", "Mobile Money", "Bank Transfer", "Credit"));

        estimatedWeightField.textProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        grainTypeCombo.valueProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        orderTypeCombo.valueProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        deliveryTypeCombo.valueProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        paymentMethodCombo.valueProperty().addListener((obs, ov, nv) -> updatePaymentMethodInfo());

        loadActiveOrders();
        setupOrderStatusListener();
        loadCustomerBalance();
    }
    
    private void loadCustomerBalance() {
        int customerId = CustomerSession.getCustomerId();
        if (customerId <= 0) return;
        
        String sql = "SELECT credit_balance_ETB FROM customers WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("credit_balance_ETB");
                customerBalanceLabel.setText("Available Credit: ETB " + String.format("%.2f", balance));
                
                // If balance is 0 or negative, disable credit option
                if (balance <= 0) {
                    paymentMethodCombo.getItems().remove("Credit");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading customer balance: " + e.getMessage());
        }
    }
    public void prefillOrderForm(String grainType) {
        // Prefill the grain type in the combo box
        grainTypeCombo.setValue(grainType);
        
        // Set default weight
        estimatedWeightField.setText("10.0");
        
        // Set default delivery type
        deliveryTypeCombo.setValue("Home Delivery");
        
        // Set default payment method
        paymentMethodCombo.setValue("Cash on Delivery");
        
        // Calculate and show estimated cost
        calculateEstimatedCost();
        
        // Show info message
        showAlert("Ready to Order", 
                 grainType + " order form has been pre-filled!\n" +
                 "Please adjust weight and other options as needed,\n" +
                 "then click 'Place Order' to complete your purchase.",
                 Alert.AlertType.INFORMATION);
    }
    public void refreshOrderStatus() {
        loadActiveOrders();
        
        // Update the status info for selected order
        Order selected = activeOrdersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOrderStatusDetails(selected);
        }
    }

    // Modify the refresh method
    public void refresh() { 
        loadActiveOrders(); 
        loadCustomerBalance();
        refreshOrderStatus(); // ADD THIS LINE
    }

    private void refreshActiveOrderStatus() {
        // Get current selection to preserve it
        Order selected = activeOrdersTable.getSelectionModel().getSelectedItem();
        int selectedId = selected != null ? selected.getId() : -1;
        
        // Refresh data
        loadActiveOrders();
        
        // Reselect if still exists
        if (selectedId > 0) {
            for (Order order : activeOrdersTable.getItems()) {
                if (order.getId() == selectedId) {
                    activeOrdersTable.getSelectionModel().select(order);
                    showOrderStatusDetails(order);
                    break;
                }
            }
        }
    }
    private void updatePaymentMethodInfo() {
        String selectedMethod = paymentMethodCombo.getValue();
        if (selectedMethod == null) return;
        
        String info = "";
        String color = "#2c3e50";
        
        switch (selectedMethod) {
            case "Cash on Delivery":
                info = "Pay in cash when receiving your order.";
                color = "#27ae60";
                break;
            case "Mobile Money":
                info = "Use your mobile banking app to pay.\nAccount: 0911-XXXX-XX";
                color = "#3498db";
                break;
            case "Bank Transfer":
                info = "Transfer to: Commercial Bank\nAccount: 1000-XXXX-XXXX";
                color = "#9b59b6";
                break;
            case "Credit":
                info = "Deduct from your credit balance.\nOrder will be processed immediately.";
                color = "#e67e22";
                break;
        }
        
        // You could show this info in a label if you add one
        System.out.println("Payment method info: " + info);
    }
    public void refresh1() { 
        loadActiveOrders(); 
        loadCustomerBalance();
    }
    @FXML 
    private void handlePlaceOrder() {
        // Check if customer is logged in
        if (!CustomerSession.isLoggedIn()) {
            showAlert("Error", "You must be logged in to place an order. Please login again.", 
                     Alert.AlertType.ERROR);
            return;
        }
        
        // Validate all fields
        if (orderTypeCombo.getValue() == null || grainTypeCombo.getValue() == null ||
            estimatedWeightField.getText().isEmpty() || deliveryTypeCombo.getValue() == null ||
            paymentMethodCombo.getValue() == null) {
            showAlert("Error", "Please fill all fields including payment method", Alert.AlertType.ERROR);
            return;
        }

        // Validate weight
        double weight;
        try {
            weight = Double.parseDouble(estimatedWeightField.getText());
            if (weight < 5) {
                showAlert("Error", "Minimum order weight is 5 kg", Alert.AlertType.ERROR);
                return;
            }
            if (weight > 1000) {
                showAlert("Error", "Maximum order weight is 1000 kg. For larger orders, contact us.", 
                         Alert.AlertType.ERROR);
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid weight", Alert.AlertType.ERROR);
            return;
        }

        // Calculate cost
        double pricePerKg = getPricePerKg(grainTypeCombo.getValue());
        double deliveryCost = "Home Delivery".equals(deliveryTypeCombo.getValue()) ? weight * 5 : 0;
        double totalCost = weight * pricePerKg + deliveryCost;
        
        // Check credit balance if credit payment selected
        if ("Credit".equals(paymentMethodCombo.getValue())) {
            if (!checkCreditBalance(totalCost)) {
                showAlert("Insufficient Credit", 
                         "Your credit balance is insufficient for this order.\n" +
                         "Please choose another payment method.",
                         Alert.AlertType.ERROR);
                return;
            }
        }

        // Show order confirmation with payment details
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Order");
        confirm.setHeaderText("Order & Payment Details");
        confirm.setContentText(String.format(
            "Order Type: %s\n" +
            "Grain: %s\n" +
            "Weight: %.2f kg\n" +
            "Delivery: %s\n" +
            "Payment Method: %s\n" +
            "Estimated Cost: %s\n\n" +
            "Place this order?",
            orderTypeCombo.getValue(),
            grainTypeCombo.getValue(),
            weight,
            deliveryTypeCombo.getValue(),
            paymentMethodCombo.getValue(),
            estimatedCostLabel.getText()
        ));
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Save order to DB
                saveOrderToDatabase(weight, totalCost);
            }
        });
    }
    
    private boolean checkCreditBalance(double requiredAmount) {
        int customerId = CustomerSession.getCustomerId();
        if (customerId <= 0) return false;
        
        String sql = "SELECT credit_balance_ETB FROM customers WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("credit_balance_ETB");
                return balance >= requiredAmount;
            }
        } catch (SQLException e) {
            System.err.println("Error checking credit balance: " + e.getMessage());
        }
        return false;
    }
    
    private void saveOrderToDatabase(double weight, double totalCost) {
        String sql = "INSERT INTO orders (customer_id, order_type, raw_type, estimated_weight, " +
                    "delivery_type, payment_method, payment_amount, status, created_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending', NOW())";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, CustomerSession.getCustomerId());
            ps.setString(2, orderTypeCombo.getValue());
            ps.setString(3, grainTypeCombo.getValue());
            ps.setDouble(4, weight);
            ps.setString(5, deliveryTypeCombo.getValue());
            ps.setString(6, paymentMethodCombo.getValue());
            ps.setDouble(7, totalCost);
            ps.executeUpdate();
            
            // Get the generated order ID
            try (Statement stmt = c.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (rs.next()) {
                    int orderId = rs.getInt(1);
                    
                    // If credit payment, deduct from balance
                    if ("Credit".equals(paymentMethodCombo.getValue())) {
                        deductCreditBalance(totalCost);
                    }
                    
                    showAlert("Success", 
                              "Order #" + orderId + " placed successfully!\n" +
                              "Payment Method: " + paymentMethodCombo.getValue() + "\n" +
                              "Amount: " + estimatedCostLabel.getText() + "\n" +
                              "You can track its progress in 'My Active Orders'.\n" +
                              "An operator will process it shortly.",
                              Alert.AlertType.INFORMATION);
                }
            }
            
            clearForm();
            loadActiveOrders();
            loadCustomerBalance();
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to place order: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    private void deductCreditBalance(double amount) {
        int customerId = CustomerSession.getCustomerId();
        if (customerId <= 0) return;
        
        String sql = "UPDATE customers SET credit_balance_ETB = credit_balance_ETB - ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deducting credit balance: " + e.getMessage());
        }
    }

    private void calculateEstimatedCost() {
        try {
            if (grainTypeCombo.getValue() == null || estimatedWeightField.getText().isEmpty()) {
                estimatedCostLabel.setText("ETB 0.00");
                return;
            }
            double weight = Double.parseDouble(estimatedWeightField.getText());
            double price = getPricePerKg(grainTypeCombo.getValue());
            double deliveryCost = "Home Delivery".equals(deliveryTypeCombo.getValue()) ? weight * 5 : 0;
            estimatedCostLabel.setText(String.format("ETB %.2f", weight * price + deliveryCost));
        } catch (Exception e) {
            estimatedCostLabel.setText("ETB 0.00");
        }
    }

    private double getPricePerKg(String grain) {
        String sql = "SELECT price_per_kg FROM settings WHERE grain_type = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, grain);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { 
            System.err.println("Error getting price: " + e.getMessage());
        }
        // Default prices
        switch (grain) {
            case "Wheat": return 45.0;
            case "Teff": return 85.0;
            case "Corn": return 40.0;
            case "Barley": return 42.0;
            default: return 45.0;
        }
    }

    @FXML 
    private void handleCancelOrder() {
        Order selected = activeOrdersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an order to cancel", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText("Cancel Order #" + selected.getId());
        confirm.setContentText("Are you sure you want to cancel this order?\n" +
                              "If paid by credit, amount will be refunded.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Update order status in database
                String sql = "UPDATE orders SET status = 'Cancelled' WHERE id = ?";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, selected.getId());
                    ps.executeUpdate();
                    
                    // If order was paid by credit, refund
                    if ("Credit".equals(selected.getPaymentMethod())) {
                        refundCreditBalance(selected.getId());
                    }
                    
                    showAlert("Success", "Order #" + selected.getId() + " has been cancelled", Alert.AlertType.INFORMATION);
                    loadActiveOrders();
                    loadCustomerBalance();
                } catch (SQLException e) {
                    showAlert("Error", "Failed to cancel order: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void refundCreditBalance(int orderId) {
        // Get order amount and customer ID
        String sql = "SELECT customer_id, payment_amount FROM orders WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int customerId = rs.getInt("customer_id");
                double amount = rs.getDouble("payment_amount");
                
                // Refund to customer balance
                String refundSql = "UPDATE customers SET credit_balance_ETB = credit_balance_ETB + ? WHERE id = ?";
                try (PreparedStatement refundPs = c.prepareStatement(refundSql)) {
                    refundPs.setDouble(1, amount);
                    refundPs.setInt(2, customerId);
                    refundPs.executeUpdate();
                    System.out.println("Refunded ETB " + amount + " to customer #" + customerId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error refunding credit: " + e.getMessage());
        }
    }

    @FXML 
    private void handleRepeatOrder() {
        Order selected = activeOrdersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Please select an order to repeat", Alert.AlertType.WARNING);
            return;
        }

        // Populate form with selected order details
        orderTypeCombo.setValue(selected.getOrderType());
        grainTypeCombo.setValue(selected.getRawType());
        estimatedWeightField.setText(String.valueOf(selected.getEstimatedWeight()));
        deliveryTypeCombo.setValue(selected.getDeliveryType());
        paymentMethodCombo.setValue(selected.getPaymentMethod());
        
        showAlert("Ready", "Order details loaded. Click 'Place Order' to confirm.", Alert.AlertType.INFORMATION);
    }

    private void clearForm() {
        orderTypeCombo.setValue(null);
        grainTypeCombo.setValue(null);
        estimatedWeightField.clear();
        deliveryTypeCombo.setValue(null);
        paymentMethodCombo.setValue(null);
        estimatedCostLabel.setText("ETB 0.00");
    }

    private void loadActiveOrders() {
        String sql = "SELECT id, order_type, raw_type, estimated_weight, delivery_type, payment_method, status " +
                    "FROM orders WHERE customer_id = ? ORDER BY created_date DESC";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, CustomerSession.getCustomerId());
            ResultSet rs = ps.executeQuery();
            
            // Clear table first
            activeOrdersTable.getItems().clear();
            
            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setOrderType(rs.getString("order_type"));
                order.setRawType(rs.getString("raw_type"));
                order.setEstimatedWeight(rs.getDouble("estimated_weight"));
                order.setDeliveryType(rs.getString("delivery_type"));
                order.setPaymentMethod(rs.getString("payment_method"));
                order.setStatus(rs.getString("status"));
                
                activeOrdersTable.getItems().add(order);
            }
            
            // Setup table columns if not already set
            setupTableColumns();
            
            System.out.println("Loaded " + activeOrdersTable.getItems().size() + " orders for customer");
            
        } catch (SQLException e) {
            e.printStackTrace();
            // Don't show error alert, just log it
            System.err.println("Error loading orders: " + e.getMessage());
        }
        
    }
    private void setupTableColumns() {
        if (activeOrdersTable.getColumns().isEmpty()) {
            TableColumn<Order, String> orderIdCol = new TableColumn<>("Order #");
            orderIdCol.setCellValueFactory(cell -> 
                new javafx.beans.property.SimpleStringProperty("ORD-" + cell.getValue().getId()));
            orderIdCol.setPrefWidth(80);
            
            TableColumn<Order, String> orderTypeCol = new TableColumn<>("Order Type");
            orderTypeCol.setCellValueFactory(cell -> 
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getOrderType()));
            orderTypeCol.setPrefWidth(130);
            
            TableColumn<Order, String> grainCol = new TableColumn<>("Grain");
            grainCol.setCellValueFactory(cell -> 
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getRawType()));
            grainCol.setPrefWidth(100);
            
            TableColumn<Order, Double> weightCol = new TableColumn<>("Weight (kg)");
            weightCol.setCellValueFactory(cell -> 
                new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getEstimatedWeight()).asObject());
            weightCol.setPrefWidth(90);
            
            TableColumn<Order, String> deliveryCol = new TableColumn<>("Delivery");
            deliveryCol.setCellValueFactory(cell -> 
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getDeliveryType()));
            deliveryCol.setPrefWidth(100);
            
            TableColumn<Order, String> paymentCol = new TableColumn<>("Payment");
            paymentCol.setCellValueFactory(cell -> 
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getPaymentMethod()));
            paymentCol.setPrefWidth(120);
            
            TableColumn<Order, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(cell -> 
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));
            statusCol.setPrefWidth(120);
            
            // Add action column for cancel button
            TableColumn<Order, Void> actionCol = new TableColumn<>("Action");
            actionCol.setCellFactory(new Callback<TableColumn<Order, Void>, TableCell<Order, Void>>() {
                @Override
                public TableCell<Order, Void> call(TableColumn<Order, Void> param) {
                    return new TableCell<Order, Void>() {
                        private final Button cancelBtn = new Button("Cancel");
                        
                        {
                            cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
                            cancelBtn.setOnAction(event -> {
                                Order order = getTableView().getItems().get(getIndex());
                                handleCancelOrderFromTable(order);
                            });
                        }
                        
                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                Order order = getTableView().getItems().get(getIndex());
                                // Only show cancel button for orders that can be cancelled
                                if ("Pending".equals(order.getStatus()) || "Processing".equals(order.getStatus())) {
                                    cancelBtn.setVisible(true);
                                } else {
                                    cancelBtn.setVisible(false);
                                }
                                setGraphic(cancelBtn);
                            }
                        }
                    };
                }
            });
            
            activeOrdersTable.getColumns().addAll(orderIdCol, orderTypeCol, grainCol, weightCol, 
                                                deliveryCol, paymentCol, statusCol, actionCol);
        }
    }
    private void handleCancelOrderFromTable(Order order) {
        handleCancelOrder(); // This will use the existing handleCancelOrder method
    }

    private void setupOrderStatusListener() {
        // Listen for table selection to show detailed status
        activeOrdersTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showOrderStatusDetails(newSelection);
                }
            });
    }
    
    private void showOrderStatusDetails(Order order) {
        String status = order.getStatus();
        String details = "";
        
        switch (status) {
            case "Pending":
                details = "Your order is waiting to be assigned to a mill.\n" +
                         "Expected assignment: Within 2 hours.";
                break;
            case "Assigned to Mill":
                details = "Your order has been assigned to a milling station.\n" +
                         "Processing will begin shortly.";
                break;
            case "Processing":
                details = "Your grain is currently being processed.\n" +
                         "Estimated completion: 4-6 hours.";
                break;
            case "Ready for Delivery":
                if ("Home Delivery".equals(order.getDeliveryType())) {
                    details = "Your order is ready and waiting for driver assignment.\n" +
                             "Delivery will be scheduled within 24 hours.";
                } else {
                    details = "Your order is ready for pickup!\n" +
                             "You can collect it during business hours.";
                }
                break;
            case "Assigned to Driver":
                details = "Driver has been assigned to your delivery.\n" +
                         "You will be contacted by the driver soon.";
                break;
            case "On Route":
                details = "Your order is out for delivery!\n" +
                         "The driver is on the way to your address.";
                break;
            case "Delivered":
                details = "Your order has been delivered successfully!\n" +
                         "Thank you for choosing Mill House.";
                break;
            case "Cancelled":
                details = "This order has been cancelled.\n" +
                         "If paid by credit, amount has been refunded.";
                break;
            default:
                details = "Status: " + status;
        }
        
        if (orderStatusInfoLabel != null) {
            orderStatusInfoLabel.setText("Order #" + order.getId() + " - " + details);
            orderStatusInfoLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14; -fx-wrap-text: true;");
        }
    }

 

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }

    public static class Order {
        private int id;
        private String orderType, rawType, deliveryType, status, paymentMethod;
        private double estimatedWeight;
        
        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        
        public String getRawType() { return rawType; }
        public void setRawType(String rawType) { this.rawType = rawType; }
        
        public String getDeliveryType() { return deliveryType; }
        public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public double getEstimatedWeight() { return estimatedWeight; }
        public void setEstimatedWeight(double estimatedWeight) { this.estimatedWeight = estimatedWeight; }
    }
}