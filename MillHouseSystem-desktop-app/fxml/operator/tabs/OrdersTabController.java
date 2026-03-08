package fxml.operator.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

import fxml.DatabaseConnection;
import fxml.Order;

public class OrdersTabController {

    @FXML private TableView<Order> ordersTable;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Button refreshButton, processOrderButton, assignMillButton;
    @FXML private Label orderCountLabel;

    private ObservableList<Order> orders = FXCollections.observableArrayList();
    private ObservableList<String> statusOptions = FXCollections.observableArrayList(
        "All", "Pending", "Assigned to Mill", "Processing", "Ready for Delivery", "Delivered", "Cancelled"
    );

    @FXML
    private void initialize() {
        System.out.println("=== ORDERS TAB INITIALIZED ===");
        
        setupTable();
        statusFilterCombo.setItems(statusOptions);
        statusFilterCombo.setValue("All");
        
        loadOrders();
        
        // Set up search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterOrders(newVal);
        });
        
        // Status filter listener
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            filterByStatus(newVal);
        });
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
        statusCol.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Pending":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "Assigned to Mill":
                        case "Processing":
                            setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                            break;
                        case "Ready for Delivery":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "Delivered":
                            setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                            break;
                        case "Cancelled":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });
        
        TableColumn<Order, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> {
            LocalDateTime date = cell.getValue().getCreatedDate();
            return new javafx.beans.property.SimpleStringProperty(date != null ? date.toString().substring(0, 16) : "N/A");
        });
        dateCol.setPrefWidth(150);
        
        ordersTable.getColumns().addAll(idCol, customerCol, typeCol, grainCol, weightCol, statusCol, dateCol);
        ordersTable.setItems(orders);
    }

    private void loadOrders() {
        orders.clear();
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT o.id, ");
        sqlBuilder.append("COALESCE(o.customer_name, c.name, 'Unknown') as customer_name, ");
        sqlBuilder.append("o.raw_type, o.order_type, o.estimated_weight, o.status, ");
        sqlBuilder.append("o.created_date ");
        sqlBuilder.append("FROM orders o ");
        sqlBuilder.append("LEFT JOIN customers c ON o.customer_id = c.id ");
        sqlBuilder.append("WHERE o.status IN ('Pending', 'Assigned to Mill', 'Processing') ");
        sqlBuilder.append("ORDER BY CASE o.status ");
        sqlBuilder.append("  WHEN 'Pending' THEN 1 ");
        sqlBuilder.append("  WHEN 'Assigned to Mill' THEN 2 ");
        sqlBuilder.append("  WHEN 'Processing' THEN 3 ");
        sqlBuilder.append("  ELSE 4 ");
        sqlBuilder.append("END, o.created_date DESC");
        
        String sql = sqlBuilder.toString();
        System.out.println("Loading orders with SQL: " + sql);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setCustomerName(rs.getString("customer_name"));
                order.setOrderType(rs.getString("order_type"));
                order.setRawType(rs.getString("raw_type"));
                order.setEstimatedWeight(rs.getDouble("estimated_weight"));
                order.setStatus(rs.getString("status"));
                order.setCreatedDate(rs.getTimestamp("created_date") != null ? 
                                   rs.getTimestamp("created_date").toLocalDateTime() : null);
                
                orders.add(order);
            }
            
            orderCountLabel.setText("Total Orders: " + count);
            System.out.println("Successfully loaded " + count + " orders");
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load orders: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleRefresh() {
        loadOrders();
        showAlert("Refreshed", "Orders data refreshed successfully", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleProcessOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an order to process", Alert.AlertType.ERROR);
            return;
        }
        
        String currentStatus = selected.getStatus();
        final String[] statusInfo = new String[2]; // [0] = action, [1] = newStatus
        
        if ("Pending".equals(currentStatus)) {
            statusInfo[0] = "Start Processing";
            statusInfo[1] = "Processing";
        } else if ("Processing".equals(currentStatus)) {
            statusInfo[0] = "Mark as Ready for Delivery";
            statusInfo[1] = "Ready for Delivery";
        } else {
            showAlert("Info", "Order is already " + currentStatus, Alert.AlertType.INFORMATION);
            return;
        }
        
        final String action = statusInfo[0];
        final String newStatus = statusInfo[1];
        final int orderId = selected.getId();
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Process Order");
        confirm.setHeaderText(action);
        confirm.setContentText("Change Order #" + orderId + " status from '" + 
                             currentStatus + "' to '" + newStatus + "'?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateOrderStatus(orderId, newStatus);
            }
        });
    }

    @FXML
    private void handleAssignToMill() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an order to assign to mill", Alert.AlertType.ERROR);
            return;
        }
        
        if (!"Pending".equals(selected.getStatus())) {
            showAlert("Error", "Only pending orders can be assigned to mill", Alert.AlertType.ERROR);
            return;
        }
        
        // Show dialog to select mill
        TextInputDialog dialog = new TextInputDialog("Mill 1");
        dialog.setTitle("Assign to Mill");
        dialog.setHeaderText("Assign Order to Mill");
        dialog.setContentText("Enter mill name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(millName -> {
            final int orderId = selected.getId();
            assignOrderToMill(orderId, millName);
        });
    }

    private void updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            int updated = ps.executeUpdate();
            
            if (updated > 0) {
                showAlert("Success", "Order #" + orderId + " status updated to '" + newStatus + "'", 
                         Alert.AlertType.INFORMATION);
                loadOrders(); // Refresh the table
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update order status: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void assignOrderToMill(int orderId, String millName) {
        String sql = "UPDATE orders SET status = 'Assigned to Mill', assigned_to = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, millName);
            ps.setInt(2, orderId);
            int updated = ps.executeUpdate();
            
            if (updated > 0) {
                showAlert("Success", "Order #" + orderId + " assigned to " + millName, 
                         Alert.AlertType.INFORMATION);
                loadOrders(); // Refresh the table
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to assign order to mill: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filterOrders(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            ordersTable.setItems(orders);
            return;
        }
        
        String lowerSearch = searchText.toLowerCase();
        ObservableList<Order> filtered = FXCollections.observableArrayList();
        
        for (Order order : orders) {
            boolean matches = false;
            
            // Check customer name
            if (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }
            
            // Check grain type
            if (order.getRawType() != null && order.getRawType().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }
            
            // Check order type
            if (order.getOrderType() != null && order.getOrderType().toLowerCase().contains(lowerSearch)) {
                matches = true;
            }
            
            // Check order ID
            if (String.valueOf(order.getId()).contains(lowerSearch)) {
                matches = true;
            }
            
            if (matches) {
                filtered.add(order);
            }
        }
        
        ordersTable.setItems(filtered);
        orderCountLabel.setText("Filtered Orders: " + filtered.size() + " / " + orders.size());
    }

    private void filterByStatus(String status) {
        if ("All".equals(status)) {
            ordersTable.setItems(orders);
            orderCountLabel.setText("Total Orders: " + orders.size());
            return;
        }
        
        ObservableList<Order> filtered = FXCollections.observableArrayList();
        
        for (Order order : orders) {
            if (status.equals(order.getStatus())) {
                filtered.add(order);
            }
        }
        
        ordersTable.setItems(filtered);
        orderCountLabel.setText(status + " Orders: " + filtered.size());
    }

    public void refresh() {
        loadOrders();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
}