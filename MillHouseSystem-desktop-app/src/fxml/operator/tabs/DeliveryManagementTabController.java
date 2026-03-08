package fxml.operator.tabs;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import fxml.DatabaseConnection;
import fxml.Order;

public class DeliveryManagementTabController {

    @FXML private TableView<Order> ordersTable;
    @FXML private ComboBox<String> driverComboBox;
    @FXML private Button assignButton;
    @FXML private TextField searchField;
    @FXML private Button refreshButton;
    private Timer refreshTimer;
    private ObservableList<Order> orders = FXCollections.observableArrayList();
    private ObservableList<String> drivers = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        System.out.println("=== DELIVERY MANAGEMENT TAB INITIALIZED ===");
        
        // Check if delivery_logs table exists
        if (!checkDeliveryLogsTable()) {
            System.out.println("WARNING: delivery_logs table doesn't exist!");
            showAlert("Warning", 
                     "Delivery logs table not found. Some features may not work properly.\n" +
                     "Please contact administrator to run database schema update.",
                     Alert.AlertType.WARNING);
        }
        
        setupTable();
        loadDrivers();
        loadOrders();
        
        // Set up search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterOrders(newVal);
        });
        
        // Start auto-refresh timer (every 30 seconds)
        startAutoRefresh();
    }
    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    loadDrivers(); // Refresh driver list
                    System.out.println("Auto-refreshed driver list");
                });
            }
        }, 30000, 30000); // Every 30 seconds
    }
 // Add cleanup method if needed
    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
    private void setupTable() {
        // Clear existing columns
        ordersTable.getColumns().clear();
        
        // Add columns
        TableColumn<Order, Integer> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getId()).asObject());
        idCol.setPrefWidth(80);
        
        TableColumn<Order, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getCustomerName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "N/A");
        });
        customerCol.setPrefWidth(150);
        
        TableColumn<Order, String> typeCol = new TableColumn<>("Order Type");
        typeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOrderType()));
        typeCol.setPrefWidth(120);
        
        TableColumn<Order, String> grainCol = new TableColumn<>("Grain");
        grainCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getRawType()));
        grainCol.setPrefWidth(100);
        
        TableColumn<Order, Double> weightCol = new TableColumn<>("Weight (kg)");
        weightCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getEstimatedWeight()).asObject());
        weightCol.setPrefWidth(100);
        
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));
        statusCol.setPrefWidth(120);
        
        TableColumn<Order, String> driverCol = new TableColumn<>("Assigned Driver");
        driverCol.setCellValueFactory(cell -> {
            String driver = cell.getValue().getAssignedDriver();
            return new javafx.beans.property.SimpleStringProperty(driver != null ? driver : "Not Assigned");
        });
        driverCol.setPrefWidth(150);
        
        ordersTable.getColumns().addAll(idCol, customerCol, typeCol, grainCol, weightCol, statusCol, driverCol);
        ordersTable.setItems(orders);
    }
    

 // IN: fxml/operator/tabs/DeliveryManagementTabController.java
    private void loadDrivers() {
        drivers.clear();
        driverComboBox.getItems().clear();
        
        // Get drivers who are either Available OR have no active deliveries
        String sql = "SELECT d.name, d.status, " +
                    "(SELECT COUNT(*) FROM orders o " +
                    " WHERE o.assigned_driver LIKE CONCAT('%', d.name, '%') " +
                    " AND o.status IN ('Assigned to Driver', 'On Route', 'Ready for Delivery')) as active_deliveries " +
                    "FROM drivers d " +
                    "WHERE d.status != 'Off Duty' " +
                    "ORDER BY d.status = 'Available' DESC, d.name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String driverName = rs.getString("name");
                String status = rs.getString("status");
                int activeDeliveries = rs.getInt("active_deliveries");
                
                // Show drivers as Available if they have no active deliveries, regardless of current status
                String displayStatus = (activeDeliveries == 0) ? "Available" : status;
                
                drivers.add(driverName);
                driverComboBox.getItems().add(driverName + " (" + displayStatus + ")");
            }
            
            System.out.println("Loaded " + drivers.size() + " drivers");
            
            if (drivers.isEmpty()) {
                driverComboBox.setPromptText("No available drivers");
                assignButton.setDisable(true);
            } else {
                assignButton.setDisable(false);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load drivers: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private void loadOrders() {
        orders.clear();
        
        // Load ONLY orders that are Ready for Delivery and NOT assigned to any driver
        String sql = "SELECT o.*, COALESCE(o.customer_name, c.name, 'Unknown') as customer_name " +
                    "FROM orders o " +
                    "LEFT JOIN customers c ON o.customer_id = c.id " +
                    "WHERE o.status = 'Ready for Delivery' " +
                    "AND (o.assigned_driver IS NULL OR o.assigned_driver = '') " +
                    "ORDER BY o.created_date ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setCustomerId(rs.getInt("customer_id"));
                order.setOrderType(rs.getString("order_type"));
                order.setRawType(rs.getString("raw_type"));
                order.setEstimatedWeight(rs.getDouble("estimated_weight"));
                order.setDeliveryType(rs.getString("delivery_type"));
                order.setStatus(rs.getString("status"));
                order.setCustomerAddress(rs.getString("customer_address"));
                order.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
                order.setCustomerName(rs.getString("customer_name"));
                order.setOrderSource(rs.getString("order_source"));
                order.setAssignedDriver(rs.getString("assigned_driver"));
                
                orders.add(order);
            }
            
            System.out.println("Loaded " + orders.size() + " orders ready for delivery assignment");
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load orders: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    @FXML
    private void handleAssignDriver() {  // Changed from handleAssignToDriver to match FXML
        Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
        String selectedDriver = driverComboBox.getValue();
        
        if (selectedOrder == null || selectedDriver == null) {
            showAlert("Error", "Please select an order and a driver", Alert.AlertType.ERROR);
            return;
        }      
        // Confirm assignment
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Driver Assignment");
        confirm.setHeaderText("Assign Order to Driver");
        confirm.setContentText("Assign Order #" + selectedOrder.getId() + " to " + selectedDriver + "?\n\n" +
                              "Customer: " + selectedOrder.getCustomerName() + "\n" +
                              "Type: " + selectedOrder.getOrderType() + " - " + selectedOrder.getRawType());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                assignOrderToDriver(selectedOrder.getId(), selectedDriver);
            }
        });
    }
 // IN: fxml/operator/tabs/DeliveryManagementTabController.java
    private void assignOrderToDriver(int orderId, String driverName) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getFreshConnection();
            if (conn == null) {
                showAlert("Error", "Cannot connect to database", Alert.AlertType.ERROR);
                return;
            }           
            
            // Start transaction
            conn.setAutoCommit(false);       
            
            try {
                // 1. Update the order
                String updateOrderSql = "UPDATE orders SET assigned_driver = ?, status = 'Assigned to Driver' WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateOrderSql)) {
                    ps.setString(1, driverName);
                    ps.setInt(2, orderId);
                    int updated = ps.executeUpdate();
                    
                    if (updated == 0) {
                        throw new SQLException("Order not found");
                    }
                }
                
                // 2. Update driver status to 'On Delivery' ONLY if they don't have other deliveries
                String checkActiveSql = "SELECT COUNT(*) as active_count FROM orders " +
                                       "WHERE assigned_driver LIKE ? " +
                                       "AND status IN ('Assigned to Driver', 'On Route')";
                
                boolean hasOtherDeliveries = false;
                try (PreparedStatement checkPs = conn.prepareStatement(checkActiveSql)) {
                    checkPs.setString(1, "%" + driverName + "%");
                    ResultSet rs = checkPs.executeQuery();
                    if (rs.next()) {
                        hasOtherDeliveries = rs.getInt("active_count") > 1; // >1 because current order is included
                    }
                }
                
                // Only update driver status if this is their first delivery assignment
                if (!hasOtherDeliveries) {
                    String updateDriverSql = "UPDATE drivers SET status = 'On Delivery' WHERE name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateDriverSql)) {
                        ps.setString(1, driverName);
                        ps.executeUpdate();
                    }
                }
                
                // 3. Insert into delivery_logs
                try {
                    String logSql = "INSERT INTO delivery_logs (order_id, driver_name, assigned_date, status) " +
                                  "VALUES (?, ?, NOW(), 'Assigned')";
                    try (PreparedStatement ps = conn.prepareStatement(logSql)) {
                        ps.setInt(1, orderId);
                        ps.setString(2, driverName);
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    // Continue anyway - delivery_logs is optional for now
                }
                
                conn.commit();
                
                showAlert("Success", 
                         "Order #" + orderId + " successfully assigned to " + driverName + "\n" +
                         "The driver will appear as 'Available' again when all deliveries are completed.",
                         Alert.AlertType.INFORMATION);
                
                // Refresh data
                loadOrders();
                loadDrivers(); // IMPORTANT: Refresh driver list
                
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                throw e;
            } 
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to assign driver: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    // Helper method to check if a column exists in a table
    private boolean checkIfColumnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = ? " +
                    "AND COLUMN_NAME = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }    private boolean checkDeliveryLogsTable() {
        String sql = "SHOW TABLES LIKE 'delivery_logs'";
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    @FXML
    private void handleRefresh() {
        loadDrivers();
        loadOrders();
        showAlert("Refreshed", "Delivery management data refreshed", Alert.AlertType.INFORMATION);
    }
    private void filterOrders(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            ordersTable.setItems(orders);
            return;
        }
        String lowerSearch = searchText.toLowerCase();
        ObservableList<Order> filtered = FXCollections.observableArrayList();
        
        for (Order order : orders) {
            if ((order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(lowerSearch)) ||
                (order.getRawType() != null && order.getRawType().toLowerCase().contains(lowerSearch)) ||
                (order.getOrderType() != null && order.getOrderType().toLowerCase().contains(lowerSearch)) ||
                String.valueOf(order.getId()).contains(lowerSearch)) {
                filtered.add(order);
            }
        }
        ordersTable.setItems(filtered);
    }
    public void refresh() {
        loadOrders();
        loadDrivers();
    }
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
}