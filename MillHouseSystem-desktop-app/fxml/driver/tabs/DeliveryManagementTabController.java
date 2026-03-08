package fxml.driver.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import fxml.CurrentUser;
import fxml.DatabaseConnection;
import java.sql.*;


import fxml.Order;

public class DeliveryManagementTabController {

    @FXML private TableView<Delivery> deliveriesTable;
    @FXML private ComboBox<String> deliveryStatusCombo;
    @FXML private TextField vehicleStatusField;
    @FXML private Label driverNameLabel;
    @FXML private Label deliveryCountLabel; // Add this to FXML
  

    @FXML
    private ComboBox<String> statusComboBox;  // or however you select the new status

    @FXML
    private TextArea driverNotesField;        // or TextField – adjust if name is different


    private ObservableList<Delivery> deliveries = FXCollections.observableArrayList();

    @FXML 
    private void initialize() {
        System.out.println("=== INITIALIZING DRIVER DELIVERY TAB ===");
        
        deliveryStatusCombo.setItems(FXCollections.observableArrayList(
            "Preparing", "Loaded", "On Route", "Delivered", "Returned", "Delayed"
        ));
        
        // Set driver name
        String driverName = CurrentUser.getInstance().getStaffName();
        if (driverNameLabel != null) {
            driverNameLabel.setText("Driver: " + (driverName != null ? driverName : "Unknown"));
        }
        
        debugDriverAssignments(); // Check what's assigned
        setupTable();
        loadAssignedDeliveries();
    }
    
    private void debugDriverAssignments() {
        System.out.println("Checking driver assignments...");
        
        String driverName = CurrentUser.getInstance().getStaffName();
        System.out.println("Current driver: " + driverName);
        
        if (driverName == null) {
            System.out.println("ERROR: Driver name is null!");
            return;
        }
        
        // Check orders assigned to this driver
        String sql = "SELECT COUNT(*) as assigned_count FROM orders " +
                    "WHERE assigned_driver LIKE ? AND status IN ('Assigned to Driver', 'On Route', 'Ready for Delivery')";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, "%" + driverName + "%");
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt("assigned_count");
                System.out.println("Orders assigned to " + driverName + ": " + count);
                
                if (deliveryCountLabel != null) {
                    deliveryCountLabel.setText("Assigned deliveries: " + count);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking assignments: " + e.getMessage());
        }
    }

    private void setupTable() {
        // Clear existing columns
        deliveriesTable.getColumns().clear();
        
        TableColumn<Delivery, String> orderCol = new TableColumn<>("Order ID");
        orderCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        orderCol.setPrefWidth(80);
        
        TableColumn<Delivery, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerCol.setPrefWidth(150);
        
        TableColumn<Delivery, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("customerPhone"));
        phoneCol.setPrefWidth(100);
        
        TableColumn<Delivery, String> addressCol = new TableColumn<>("Delivery Address");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressCol.setPrefWidth(250);
        
        TableColumn<Delivery, String> grainCol = new TableColumn<>("Grain");
        grainCol.setCellValueFactory(new PropertyValueFactory<>("grainType"));
        grainCol.setPrefWidth(80);
        
        TableColumn<Delivery, String> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        weightCol.setPrefWidth(80);
        
        TableColumn<Delivery, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(column -> new TableCell<Delivery, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Assigned to Driver":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "On Route":
                            setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                            break;
                        case "Delivered":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        TableColumn<Delivery, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("customerNotes"));
        notesCol.setPrefWidth(200);
        
        deliveriesTable.getColumns().addAll(orderCol, customerCol, phoneCol, addressCol, 
                                           grainCol, weightCol, statusCol, notesCol);
        deliveriesTable.setItems(deliveries);
    }

    private void loadAssignedDeliveries() {
        deliveries.clear();
        System.out.println("Loading assigned deliveries for driver...");
        
        // Get current driver's name
        String driverName = CurrentUser.getInstance().getStaffName();
        if (driverName == null) {
            System.out.println("ERROR: Driver name is null! No deliveries loaded.");
            if (deliveryCountLabel != null) {
                deliveryCountLabel.setText("No deliveries assigned");
            }
            return; // Don't load sample data
        }
        
        System.out.println("Driver name for query: " + driverName);
        
        // Load deliveries assigned to this driver
        String sql = "SELECT o.id as order_id, c.name as customer_name, c.phone as customer_phone, " +
                    "c.delivery_address, o.raw_type as grain_type, o.estimated_weight as weight, " +
                    "o.status, o.customer_notes, o.operator_notes, o.assigned_driver " +
                    "FROM orders o " +
                    "JOIN customers c ON o.customer_id = c.id " +
                    "WHERE (o.assigned_driver LIKE ? OR o.assigned_driver = ?) " +
                    "AND o.status IN ('Assigned to Driver', 'On Route', 'Ready for Delivery') " +
                    "ORDER BY o.created_date";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, "%" + driverName + "%");
            ps.setString(2, driverName);
            
            System.out.println("Executing SQL: " + ps.toString());
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                Delivery delivery = new Delivery(
                    "ORD-" + rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getString("customer_phone"),
                    rs.getString("delivery_address"),
                    rs.getString("grain_type"),
                    rs.getDouble("weight"),
                    rs.getString("status"),
                    getCustomerNotes(rs),
                    rs.getString("assigned_driver")
                );
                deliveries.add(delivery);
            }
            
            System.out.println("Successfully loaded " + deliveries.size() + " deliveries from database");
            
            if (deliveryCountLabel != null) {
                deliveryCountLabel.setText("Your deliveries: " + deliveries.size());
            }
            
            // REMOVED: loadSampleDeliveries() call
            
        } catch (SQLException e) { 
            System.err.println("Error loading deliveries: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load deliveries: " + e.getMessage(), Alert.AlertType.ERROR);
            // REMOVED: loadSampleDeliveries() call
            if (deliveryCountLabel != null) {
                deliveryCountLabel.setText("Error loading deliveries");
            }
        }
    }

    // Also remove the loadSampleDeliveries method completely
    
    private String getCustomerNotes(ResultSet rs) throws SQLException {
        String notes = rs.getString("customer_notes");
        String operatorNotes = rs.getString("operator_notes");
        
        StringBuilder allNotes = new StringBuilder();
        if (notes != null && !notes.trim().isEmpty()) {
            allNotes.append("Customer: ").append(notes);
        }
        if (operatorNotes != null && !operatorNotes.trim().isEmpty()) {
            if (allNotes.length() > 0) allNotes.append("\n");
            allNotes.append("Operator: ").append(operatorNotes);
        }
        
        return allNotes.toString();
    }

    

 
    @FXML
    private void handleUpdateDeliveryStatus() {
        Delivery selected = deliveriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a delivery to update", Alert.AlertType.ERROR);
            return;
        }
        
        String newStatus = deliveryStatusCombo.getValue();
        if (newStatus == null) {
            showAlert("Error", "Please select a status", Alert.AlertType.ERROR);
            return;
        }
        
        // Create dialog for driver notes
        TextInputDialog notesDialog = new TextInputDialog();
        notesDialog.setTitle("Add Driver Notes");
        notesDialog.setHeaderText("Update Status: " + newStatus);
        notesDialog.setContentText("Add notes (optional):");
        
        notesDialog.showAndWait().ifPresent(notes -> {
            // EXTRACT ORDER ID PROPERLY - FIX THIS
            String orderIdStr = selected.getOrderId();
            System.out.println("DEBUG: Order ID string: " + orderIdStr);
            
            try {
                // Remove "ORD-" prefix if exists
                String cleanId = orderIdStr.replace("ORD-", "");
                int orderId = Integer.parseInt(cleanId);
                System.out.println("DEBUG: Parsed order ID: " + orderId);
                
                updateDeliveryStatusInDatabase(orderId, newStatus, notes);
            } catch (NumberFormatException e) {
                System.err.println("ERROR: Could not parse order ID from: " + orderIdStr);
                showAlert("Error", "Invalid order ID format: " + orderIdStr, Alert.AlertType.ERROR);
            }
        });
    }

 // MODIFY the updateDeliveryStatusInDatabase method
    private void updateDeliveryStatusInDatabase(int orderId, String newStatus, String driverNotes) {
        try {
            String sql = "UPDATE orders SET status = ?, driver_notes = ?, status_updated_at = NOW() WHERE id = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, newStatus);
                ps.setString(2, driverNotes);
                ps.setInt(3, orderId);
                int updated = ps.executeUpdate();
                
                if (updated > 0) {
                    // 1. Create notification for customer
                    createCustomerNotification(orderId, newStatus);
                    
                    // 2. Update delivery logs if delivered
                    if ("Delivered".equals(newStatus)) {
                        updateDeliveryLogs(orderId);
                        syncToTransactions(orderId);
                    }
                    
                    // 3. Refresh local table - IMPORTANT!
                    loadAssignedDeliveries();
                    
                    showAlert("Success", 
                             "Status updated to: " + newStatus + "\n" +
                             "Customer has been notified!", 
                             Alert.AlertType.INFORMATION);
                }
            }
        } catch (SQLException e) {
            showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
 // ADD this method to update delivery logs
    private void updateDeliveryLogs(int orderId) {
        String sql = "UPDATE delivery_logs SET status = 'Delivered', delivered_date = NOW() " +
                    "WHERE order_id = ? AND status != 'Delivered'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            int updated = ps.executeUpdate();
            System.out.println("Updated " + updated + " delivery log(s) for order #" + orderId);
        } catch (SQLException e) {
            System.err.println("Error updating delivery logs: " + e.getMessage());
        }
    }
    private void syncToTransactions(int orderId) {
        // Auto-sync delivered orders to transactions table
        String sql = "INSERT INTO transactions (customer_id, operator_id, raw_type, raw_weight_kg, " +
                    "payment_amount_ETB, payment_type, transaction_date, order_source) " +
                    "SELECT o.customer_id, NULL, o.raw_type, o.estimated_weight, " +
                    "COALESCE(o.payment_amount, 0), COALESCE(o.payment_method, 'Cash'), NOW(), 'Driver Delivery' " +
                    "FROM orders o WHERE o.id = ? AND o.status = 'Delivered'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error syncing to transactions: " + e.getMessage());
        }
    }

    private void createCustomerNotification(int orderId, String newStatus) {
        // Get customer info for this order
        String sql = "SELECT o.customer_id, COALESCE(o.customer_name, c.name, 'Customer') as customer_name " +
                    "FROM orders o " +
                    "LEFT JOIN customers c ON o.customer_id = c.id " +
                    "WHERE o.id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int customerId = rs.getInt("customer_id");
                String customerName = rs.getString("customer_name");
                
                // Create notification message based on status
                String message = "";
                switch (newStatus) {
                    case "On Route":
                        message = "🚚 Order #" + orderId + " is on the way! Driver is heading to your location.";
                        break;
                    case "Delivered":
                        message = "✅ Order #" + orderId + " has been delivered! Thank you for choosing Mill House.";
                        break;
                    case "Delayed":
                        message = "⏳ Order #" + orderId + " is delayed. Driver will contact you with updates.";
                        break;
                    default:
                        message = "📦 Order #" + orderId + " status updated: " + newStatus;
                }
                
                // Insert notification into customer_notifications table
                String notifySql = "INSERT INTO customer_notifications " +
                                  "(customer_id, order_id, message, status, created_date) " +
                                  "VALUES (?, ?, ?, 'UNREAD', NOW())";
                
                try (PreparedStatement notifyPs = conn.prepareStatement(notifySql)) {
                    notifyPs.setInt(1, customerId);
                    notifyPs.setInt(2, orderId);
                    notifyPs.setString(3, message);
                    notifyPs.executeUpdate();
                }
                
                System.out.println("Notification created for customer: " + customerName);
            }
        } catch (SQLException e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    private void updateOrderStatus(String orderId, String newStatus, String driverNotes) {
		// TODO Auto-generated method stub
		
	}

    private void updateOrderStatus(int orderId, String newStatus, String driverNotes) {
        String sql = "UPDATE orders SET status = ?, driver_notes = ?, updated_at = NOW() WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, newStatus);
            ps.setString(2, driverNotes.isEmpty() ? null : driverNotes);
            ps.setInt(3, orderId);
            
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                showAlert("Success", "Order #" + orderId + " status updated to '" + newStatus + "'", 
                          Alert.AlertType.INFORMATION);
                
                // ADD THIS: Create notification for customer
                createStatusNotification(orderId, newStatus);
                
                refresh(); // refresh table
                if (driverNotesField != null) driverNotesField.clear();
                deliveryStatusCombo.setValue(null);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to update order: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private void createStatusNotification(int orderId, String newStatus) {
        // Get customer info for this order
        String customerSql = "SELECT c.id, c.name, c.phone, o.customer_name " +
                            "FROM orders o " +
                            "JOIN customers c ON o.customer_id = c.id " +
                            "WHERE o.id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(customerSql)) {
            
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int customerId = rs.getInt("id");
                String customerName = rs.getString("customer_name");
                String phone = rs.getString("phone");
                
                // Create notification in database (optional)
                String notificationSql = "INSERT INTO customer_notifications " +
                                       "(customer_id, order_id, message, status, created_date) " +
                                       "VALUES (?, ?, ?, ?, NOW())";
                
                String message = "Order #" + orderId + " status updated: " + newStatus;
                if ("Delivered".equals(newStatus)) {
                    message = "🎉 Your order #" + orderId + " has been delivered!";
                } else if ("On Route".equals(newStatus)) {
                    message = "🚚 Your order #" + orderId + " is on the way!";
                }
                
                try (PreparedStatement notifPs = conn.prepareStatement(notificationSql)) {
                    notifPs.setInt(1, customerId);
                    notifPs.setInt(2, orderId);
                    notifPs.setString(3, message);
                    notifPs.setString(4, "UNREAD");
                    notifPs.executeUpdate();
                }
                
                // You could also implement push notifications or SMS here
                System.out.println("Notification created for customer: " + customerName);
            }
            
        } catch (SQLException e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }
    private String validateAndMapStatus(String status) {
        if (status == null) return "Pending";
        
        String normalized = status.trim();
        
        // Map common variations to standard values
        switch (normalized.toLowerCase()) {
            case "delivered":
            case "deliver":
            case "delivery":
                return "Delivered";
                
            case "on route":
            case "onroute":
            case "en route":
                return "On Route";
                
            case "pending":
            case "waiting":
                return "Pending";
                
            case "processing":
//            case "processing":
                return "Processing";
                
            case "assigned":
            case "assigned to driver":
                return "Assigned to Driver";
                
            case "cancelled":
            case "canceled":
                return "Cancelled";
                
            case "ready for delivery":
            case "ready":
                return "Ready for Delivery";
                
            default:
                // Return original but ensure it's not too long
                return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
        }
    }
    private void updateDeliveryLogs(int orderId, String status) {
        if ("Delivered".equals(status)) {
            String sql = "UPDATE delivery_logs SET status = 'Delivered', delivered_date = NOW() " +
                        "WHERE order_id = ? AND (status != 'Delivered' OR status IS NULL)";
            
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
                System.out.println("Updated delivery_logs for order #" + orderId + " to Delivered");
            } catch (SQLException e) {
                System.err.println("Error updating delivery_logs: " + e.getMessage());
            }
        }
    }
    private void checkAndFixStatusColumn(String errorMessage) {
        if (errorMessage.contains("Data truncated for column 'status'")) {
            System.out.println("WARNING: Status column may be too small. Need to update database schema.");
            
            // Try to fix dynamically
            try (Connection c = DatabaseConnection.getConnection();
                 Statement stmt = c.createStatement()) {
                
                // Try to alter the column
                String alterSql = "ALTER TABLE orders MODIFY COLUMN status VARCHAR(50)";
                stmt.executeUpdate(alterSql);
                System.out.println("Successfully altered orders.status column to VARCHAR(50)");
                
            } catch (SQLException e2) {
                System.err.println("Could not fix status column: " + e2.getMessage());
            }
        }
    }
    private void markOrderAsDelivered(String orderIdStr) {
        try {
            int orderId = Integer.parseInt(orderIdStr.replace("ORD-", ""));
            
            String sql = "UPDATE orders SET status = 'Delivered', delivery_completed_date = NOW() " +
                        "WHERE id = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
                
                System.out.println("Marked order #" + orderId + " as delivered");
                
                // AUTO-SYNC TO TRANSACTIONS
                syncOrderToTransactions(orderId);
                
                // Update driver status to available
                updateDriverStatus("Available");
                
            } catch (SQLException e) {
                System.err.println("Error marking order as delivered: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid order ID format: " + orderIdStr);
        }
    }

    private void syncOrderToTransactions(int orderId) {
        String sql = "INSERT INTO transactions (customer_id, operator_id, raw_type, raw_weight_kg, " +
                    "payment_amount_ETB, payment_type, transaction_date, order_source) " +
                    "SELECT " +
                    "  o.customer_id, " +
                    "  (SELECT id FROM users WHERE role = 'OPERATOR' LIMIT 1) as operator_id, " +
                    "  o.raw_type, " +
                    "  o.estimated_weight, " +
                    "  COALESCE(o.payment_amount, s.price_per_kg * o.estimated_weight, 0), " +
                    "  COALESCE(o.payment_method, 'Credit'), " +
                    "  NOW(), " +
                    "  'Customer Portal' " +
                    "FROM orders o " +
                    "LEFT JOIN settings s ON o.raw_type = s.grain_type " +
                    "WHERE o.id = ? " +
                    "  AND o.status = 'Delivered' " +
                    "  AND COALESCE(o.payment_amount, s.price_per_kg * o.estimated_weight, 0) > 0 " +
                    "  AND NOT EXISTS ( " +
                    "    SELECT 1 FROM transactions t " +
                    "    WHERE t.customer_id = o.customer_id " +
                    "      AND t.raw_type = o.raw_type " +
                    "      AND ABS(t.raw_weight_kg - o.estimated_weight) < 0.5 " +
                    "      AND DATE(t.transaction_date) = DATE(o.created_date) " +
                    "  )";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            int inserted = ps.executeUpdate();
            
            if (inserted > 0) {
                System.out.println("Auto-synced delivered order #" + orderId + " to transactions");
            }
            
        } catch (SQLException e) {
            System.err.println("Error syncing order to transactions: " + e.getMessage());
        }
    }
    
    private void updateDriverStatus(String status) {
        String driverName = CurrentUser.getInstance().getStaffName();
        if (driverName == null) return;
        
        String sql = "UPDATE drivers SET status = ? WHERE name LIKE ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, "%" + driverName + "%");
            int rows = ps.executeUpdate();
            System.out.println("Updated driver status for " + driverName + ": " + rows + " rows affected");
        } catch (SQLException e) {
            System.err.println("Error updating driver status: " + e.getMessage());
        }
    }

    @FXML 
    private void handleUpdateVehicleStatus() {
        String status = vehicleStatusField.getText().trim();
        if (!status.isEmpty()) {
            // Update driver's vehicle status in database
            updateVehicleStatusInDB(status);
            
            showAlert("Vehicle Updated", 
                     "Vehicle status: " + status + 
                     "\nThis information is visible to dispatchers.", 
                     Alert.AlertType.INFORMATION);
            vehicleStatusField.clear();
        }
    }
    
    private void updateVehicleStatusInDB(String status) {
        String driverName = CurrentUser.getInstance().getStaffName();
        if (driverName == null) return;
        
        String sql = "UPDATE drivers SET vehicle_status = ? WHERE name LIKE ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, "%" + driverName + "%");
            ps.executeUpdate();
            System.out.println("Updated vehicle status for " + driverName + " to: " + status);
        } catch (SQLException e) {
            System.err.println("Error updating vehicle status: " + e.getMessage());
        }
    }
    
    
    @FXML
    private void handleContactCustomer() {
        Delivery selected = deliveriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Please select a delivery to contact customer", 
                     Alert.AlertType.WARNING);
            return;
        }
        
        Alert contactAlert = new Alert(Alert.AlertType.INFORMATION);
        contactAlert.setTitle("Customer Contact");
        contactAlert.setHeaderText("Contact Information - Order #" + selected.getOrderId());
        contactAlert.setContentText("Customer: " + selected.getCustomerName() + "\n" +
                                   "Phone: " + selected.getCustomerPhone() + "\n" +
                                   "Address: " + selected.getAddress() + "\n" +
                                   "Grain: " + selected.getGrainType() + " (" + selected.getWeight() + " kg)\n" +
                                   "Status: " + selected.getStatus() + "\n\n" +
                                   "Notes: " + selected.getCustomerNotes());
        contactAlert.showAndWait();
    }

    public void refresh() {
        System.out.println("Refreshing driver deliveries...");
        loadAssignedDeliveries();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public static class Delivery {
        private String orderId; // Make sure this is storing the order ID properly
        private String customerName, customerPhone, address, grainType, status, customerNotes, assignedDriver;
        private double weight;
        
        public Delivery(String orderId, String customerName, String customerPhone, 
                       String address, String grainType, double weight, 
                       String status, String customerNotes, String assignedDriver) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.customerPhone = customerPhone;
            this.address = address;
            this.grainType = grainType;
            this.weight = weight;
            this.status = status;
            this.customerNotes = customerNotes;
            this.assignedDriver = assignedDriver;
        }
        public String getOrderId() { 
            // Ensure orderId is in correct format
            if (orderId != null && !orderId.startsWith("ORD-") && orderId.matches("\\d+")) {
                return "ORD-" + orderId;
            }
            return orderId != null ? orderId : ""; 
        }
        
//        public String getOrderId() { return orderId; }
        public String getCustomerName() { return customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public String getAddress() { return address; }
        public String getGrainType() { return grainType; }
        public double getWeight() { return weight; }
        public String getStatus() { return status; }
        public String getCustomerNotes() { return customerNotes; }
        public String getAssignedDriver() { return assignedDriver; }
        
        public void setStatus(String status) { this.status = status; }
    }
}