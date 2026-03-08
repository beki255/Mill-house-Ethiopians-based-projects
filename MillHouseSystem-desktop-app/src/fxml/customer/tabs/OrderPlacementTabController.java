package fxml.customer.tabs;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
    
    // Mobile Money Fields
    @FXML private VBox mobileMoneyDetailsBox;
    @FXML private ComboBox<String> mobileMoneyTypeCombo;
    @FXML private TextField mobilePhoneField;
    @FXML private PasswordField mobilePinField;
    @FXML private TextField mobileReferenceField;
    
    // Bank Transfer Fields
    @FXML private VBox bankTransferDetailsBox;
    @FXML private ComboBox<String> bankNameCombo;
    @FXML private TextField accountNumberField;
    @FXML private TextField accountHolderField;
    @FXML private TextField paymentReferenceField;

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

        // Setup mobile money types combo
        mobileMoneyTypeCombo.setItems(FXCollections.observableArrayList(
            "TeleBirr",
            "M-Pesa",
            "HelloCash",
            "CBE Birr",
            "Other Mobile Money"
        ));

        // Setup bank names combo
        bankNameCombo.setItems(FXCollections.observableArrayList(
            "CBE",
            "Awash Bank",
            "Abay Bank",
            "Dashen Bank",
            "Bank of Abyssinia",
            "Wegagen Bank",
            "NIB International Bank",
            "Oromia International Bank",
            "Cooperative Bank of Oromia",
            "Other Bank"
        ));

        estimatedWeightField.textProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        grainTypeCombo.valueProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        orderTypeCombo.valueProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        deliveryTypeCombo.valueProperty().addListener((obs, ov, nv) -> calculateEstimatedCost());
        paymentMethodCombo.valueProperty().addListener((obs, ov, nv) -> {
            updatePaymentMethodInfo();
            togglePaymentDetails();
        });

        loadActiveOrders();
        setupOrderStatusListener();
        loadCustomerBalance();
        
        // Initialize payment details sections as hidden
        mobileMoneyDetailsBox.setVisible(false);
        mobileMoneyDetailsBox.setManaged(false);
        bankTransferDetailsBox.setVisible(false);
        bankTransferDetailsBox.setManaged(false);
    }
    
    private void togglePaymentDetails() {
        String selectedMethod = paymentMethodCombo.getValue();
        
        // Show/Hide Mobile Money Section
        boolean showMobileMoney = "Mobile Money".equals(selectedMethod);
        mobileMoneyDetailsBox.setVisible(showMobileMoney);
        mobileMoneyDetailsBox.setManaged(showMobileMoney);
        
        // Show/Hide Bank Transfer Section
        boolean showBankDetails = "Bank Transfer".equals(selectedMethod);
        bankTransferDetailsBox.setVisible(showBankDetails);
        bankTransferDetailsBox.setManaged(showBankDetails);
        
        // Clear fields when hiding
        if (!showMobileMoney) {
            mobileMoneyTypeCombo.setValue(null);
            mobilePhoneField.clear();
            mobilePinField.clear();
            mobileReferenceField.clear();
        }
        
        if (!showBankDetails) {
            bankNameCombo.setValue(null);
            accountNumberField.clear();
            accountHolderField.clear();
            paymentReferenceField.clear();
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
                info = "Use your mobile money app to pay.\nComplete the mobile money details below.";
                color = "#1abc9c";
                break;
            case "Bank Transfer":
                info = "Transfer to our bank account.\nComplete the bank details below.";
                color = "#3498db";
                break;
            case "Credit":
                info = "Deduct from your credit balance.\nOrder will be processed immediately.";
                color = "#e67e22";
                break;
        }
        
        System.out.println("Payment method info: " + info);
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

        // Additional validation for Mobile Money
        if ("Mobile Money".equals(paymentMethodCombo.getValue())) {
            if (mobileMoneyTypeCombo.getValue() == null || mobileMoneyTypeCombo.getValue().isEmpty()) {
                showAlert("Error", "Please select mobile money type", Alert.AlertType.ERROR);
                return;
            }
            if (mobilePhoneField.getText().isEmpty()) {
                showAlert("Error", "Please enter your phone number", Alert.AlertType.ERROR);
                return;
            }
            
            // Validate phone number format
            String phone = mobilePhoneField.getText().trim();
            if (!phone.matches("09\\d{8}")) {
                showAlert("Error", "Phone number should be 10 digits starting with 09", Alert.AlertType.ERROR);
                return;
            }
        }

        // Additional validation for Bank Transfer
        if ("Bank Transfer".equals(paymentMethodCombo.getValue())) {
            if (bankNameCombo.getValue() == null || bankNameCombo.getValue().isEmpty()) {
                showAlert("Error", "Please select your bank for bank transfer", Alert.AlertType.ERROR);
                return;
            }
            if (accountNumberField.getText().isEmpty()) {
                showAlert("Error", "Please enter your bank account number", Alert.AlertType.ERROR);
                return;
            }
            if (accountHolderField.getText().isEmpty()) {
                showAlert("Error", "Please enter the account holder name", Alert.AlertType.ERROR);
                return;
            }
            
            // Validate account number format (basic validation)
            String accountNo = accountNumberField.getText().trim();
            if (!accountNo.matches("\\d+")) {
                showAlert("Error", "Account number should contain only numbers", Alert.AlertType.ERROR);
                return;
            }
            if (accountNo.length() < 8 || accountNo.length() > 20) {
                showAlert("Error", "Account number should be between 8-20 digits", Alert.AlertType.ERROR);
                return;
            }
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

        // Prepare order confirmation message
        StringBuilder confirmationMsg = new StringBuilder();
        confirmationMsg.append(String.format(
            "Order Type: %s\n" +
            "Grain: %s\n" +
            "Weight: %.2f kg\n" +
            "Delivery: %s\n" +
            "Payment Method: %s\n" +
            "Estimated Cost: %s\n\n",
            orderTypeCombo.getValue(),
            grainTypeCombo.getValue(),
            weight,
            deliveryTypeCombo.getValue(),
            paymentMethodCombo.getValue(),
            estimatedCostLabel.getText()
        ));
        
        // Add mobile money details if applicable
        if ("Mobile Money".equals(paymentMethodCombo.getValue())) {
            confirmationMsg.append("Mobile Money Details:\n");
            confirmationMsg.append("Type: ").append(mobileMoneyTypeCombo.getValue()).append("\n");
            confirmationMsg.append("Phone: ").append(mobilePhoneField.getText()).append("\n");
            if (!mobileReferenceField.getText().isEmpty()) {
                confirmationMsg.append("Reference: ").append(mobileReferenceField.getText()).append("\n");
            }
            confirmationMsg.append("\n");
        }
        
        // Add bank transfer details if applicable
        if ("Bank Transfer".equals(paymentMethodCombo.getValue())) {
            confirmationMsg.append("Bank Transfer Details:\n");
            confirmationMsg.append("Bank: ").append(bankNameCombo.getValue()).append("\n");
            confirmationMsg.append("Account Number: ").append(accountNumberField.getText()).append("\n");
            confirmationMsg.append("Account Holder: ").append(accountHolderField.getText()).append("\n");
            if (!paymentReferenceField.getText().isEmpty()) {
                confirmationMsg.append("Reference: ").append(paymentReferenceField.getText()).append("\n");
            }
            confirmationMsg.append("\n");
        }
        
        confirmationMsg.append("Place this order?");

        // Show order confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Order");
        confirm.setHeaderText("Order & Payment Details");
        confirm.setContentText(confirmationMsg.toString());
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Save order to DB
                saveOrderToDatabase(weight, totalCost);
            }
        });
    }
    
    private void saveOrderToDatabase(double weight, double totalCost) {
        String sql = "INSERT INTO orders (customer_id, order_type, raw_type, estimated_weight, " +
                    "delivery_type, payment_method, payment_amount, status, created_date, " +
                    "mobile_money_type, mobile_phone, mobile_pin, mobile_reference, " +
                    "bank_name, account_number, account_holder, payment_reference) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending', NOW(), ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, CustomerSession.getCustomerId());
            ps.setString(2, orderTypeCombo.getValue());
            ps.setString(3, grainTypeCombo.getValue());
            ps.setDouble(4, weight);
            ps.setString(5, deliveryTypeCombo.getValue());
            ps.setString(6, paymentMethodCombo.getValue());
            ps.setDouble(7, totalCost);
            
            // Set mobile money details if applicable
            if ("Mobile Money".equals(paymentMethodCombo.getValue())) {
                ps.setString(8, mobileMoneyTypeCombo.getValue());
                ps.setString(9, mobilePhoneField.getText());
                ps.setString(10, mobilePinField.getText());
                ps.setString(11, mobileReferenceField.getText());
                // Set bank details as null
                ps.setString(12, null);
                ps.setString(13, null);
                ps.setString(14, null);
                ps.setString(15, null);
            }
            // Set bank transfer details if applicable
            else if ("Bank Transfer".equals(paymentMethodCombo.getValue())) {
                // Set mobile money details as null
                ps.setString(8, null);
                ps.setString(9, null);
                ps.setString(10, null);
                ps.setString(11, null);
                
                ps.setString(12, bankNameCombo.getValue());
                ps.setString(13, accountNumberField.getText());
                ps.setString(14, accountHolderField.getText());
                ps.setString(15, paymentReferenceField.getText());
            } else {
                // Set both mobile money and bank details as null
                ps.setString(8, null);
                ps.setString(9, null);
                ps.setString(10, null);
                ps.setString(11, null);
                ps.setString(12, null);
                ps.setString(13, null);
                ps.setString(14, null);
                ps.setString(15, null);
            }
            
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
                    
                    // Prepare success message
                    StringBuilder successMsg = new StringBuilder();
                    successMsg.append("Order #").append(orderId).append(" placed successfully!\n");
                    successMsg.append("Payment Method: ").append(paymentMethodCombo.getValue()).append("\n");
                    successMsg.append("Amount: ").append(estimatedCostLabel.getText()).append("\n\n");
                    
                    // Add payment instructions for mobile money
                    if ("Mobile Money".equals(paymentMethodCombo.getValue())) {
                        successMsg.append("📱 MOBILE MONEY INSTRUCTIONS:\n");
                        successMsg.append("1. Transfer ").append(estimatedCostLabel.getText()).append(" to:\n");
                        successMsg.append("   • TeleBirr: 0911-234-567\n");
                        successMsg.append("   • M-Pesa: 0922-345-678\n");
                        successMsg.append("   • HelloCash: 0933-456-789\n");
                        successMsg.append("2. Use 'ORD-").append(orderId).append("' as reference\n");
                        successMsg.append("3. Your order will process after payment confirmation\n");
                        successMsg.append("4. Payment confirmation usually takes 5-10 minutes\n\n");
                    }
                    
                    // Add payment instructions for bank transfer
                    if ("Bank Transfer".equals(paymentMethodCombo.getValue())) {
                        successMsg.append("💳 BANK TRANSFER INSTRUCTIONS:\n");
                        successMsg.append("1. Transfer ").append(estimatedCostLabel.getText()).append(" to:\n");
                        successMsg.append("   • CBE: 1000-1234567-89\n");
                        successMsg.append("   • Awash Bank: 013-4567890-12\n");
                        successMsg.append("   • Abay Bank: 123-7894561-23\n");
                        successMsg.append("2. Use 'ORD-").append(orderId).append("' as reference\n");
                        successMsg.append("3. Your order will process after payment confirmation\n");
                        successMsg.append("4. Bank confirmation usually takes 1-2 hours\n\n");
                    }
                    
                    successMsg.append("You can track its progress in 'My Active Orders'.\n");
                    successMsg.append("An operator will process it shortly.");
                    
                    showAlert("Success", successMsg.toString(), Alert.AlertType.INFORMATION);
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
    
    private void clearForm() {
        orderTypeCombo.setValue(null);
        grainTypeCombo.setValue(null);
        estimatedWeightField.clear();
        deliveryTypeCombo.setValue(null);
        paymentMethodCombo.setValue(null);
        estimatedCostLabel.setText("ETB 0.00");
        
        // Clear mobile money fields
        mobileMoneyTypeCombo.setValue(null);
        mobilePhoneField.clear();
        mobilePinField.clear();
        mobileReferenceField.clear();
        
        // Clear bank transfer fields
        bankNameCombo.setValue(null);
        accountNumberField.clear();
        accountHolderField.clear();
        paymentReferenceField.clear();
        
        // Hide payment details sections
        mobileMoneyDetailsBox.setVisible(false);
        mobileMoneyDetailsBox.setManaged(false);
        bankTransferDetailsBox.setVisible(false);
        bankTransferDetailsBox.setManaged(false);
    }
    
    // Add payment details to Order class
    public static class Order {
        private int id;
        private String orderType, rawType, deliveryType, status, paymentMethod;
        private String mobileMoneyType, mobilePhone, mobilePin, mobileReference;
        private String bankName, accountNumber, accountHolder, paymentReference;
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
        
        // Mobile Money getters/setters
        public String getMobileMoneyType() { return mobileMoneyType; }
        public void setMobileMoneyType(String mobileMoneyType) { this.mobileMoneyType = mobileMoneyType; }
        
        public String getMobilePhone() { return mobilePhone; }
        public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }
        
        public String getMobilePin() { return mobilePin; }
        public void setMobilePin(String mobilePin) { this.mobilePin = mobilePin; }
        
        public String getMobileReference() { return mobileReference; }
        public void setMobileReference(String mobileReference) { this.mobileReference = mobileReference; }
        
        // Bank Transfer getters/setters
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        
        public String getAccountHolder() { return accountHolder; }
        public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
        
        public String getPaymentReference() { return paymentReference; }
        public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
        
        public double getEstimatedWeight() { return estimatedWeight; }
        public void setEstimatedWeight(double estimatedWeight) { this.estimatedWeight = estimatedWeight; }
    }
    
    private void loadActiveOrders() {
        String sql = "SELECT id, order_type, raw_type, estimated_weight, delivery_type, " +
                    "payment_method, status, mobile_money_type, mobile_phone, mobile_reference, " +
                    "bank_name, account_number, account_holder, payment_reference " +
                    "FROM orders WHERE customer_id = ? ORDER BY created_date DESC";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, CustomerSession.getCustomerId());
            ResultSet rs = ps.executeQuery();
            
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
                
                // Load mobile money details if available
                order.setMobileMoneyType(rs.getString("mobile_money_type"));
                order.setMobilePhone(rs.getString("mobile_phone"));
                order.setMobileReference(rs.getString("mobile_reference"));
                
                // Load bank details if available
                order.setBankName(rs.getString("bank_name"));
                order.setAccountNumber(rs.getString("account_number"));
                order.setAccountHolder(rs.getString("account_holder"));
                order.setPaymentReference(rs.getString("payment_reference"));
                
                activeOrdersTable.getItems().add(order);
            }
            
            System.out.println("Loaded " + activeOrdersTable.getItems().size() + " orders for customer");
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error loading orders: " + e.getMessage());
        }
    }
    
    // Rest of existing methods remain the same...
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
    
    public void refresh() { 
        loadActiveOrders(); 
        loadCustomerBalance();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }
}