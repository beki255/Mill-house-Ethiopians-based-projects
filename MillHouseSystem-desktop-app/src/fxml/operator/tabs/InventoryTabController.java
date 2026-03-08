package fxml.operator.tabs;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

import fxml.DatabaseConnection;
import fxml.CurrentUser;

public class InventoryTabController {

    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private Label lowStockAlertLabel;
    @FXML private Button reportLowStockBtn;

    private ObservableList<InventoryItem> inventory = FXCollections.observableArrayList();
    private static final double LOW_STOCK_THRESHOLD = 200.0; // Default threshold in kg

    @FXML 
    private void initialize() {
        setupTable();
        loadInventory();
        checkLowStock();
        
        // Setup low stock report button
        if (reportLowStockBtn != null) {
            reportLowStockBtn.setOnAction(e -> handleReportLowStock());
        }
    }

    private void setupTable() {
        // Clear existing columns
        inventoryTable.getColumns().clear();
        
        TableColumn<InventoryItem, String> type = new TableColumn<>("Grain Type");
        type.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGrainType()));
        type.setPrefWidth(150);
        
        TableColumn<InventoryItem, String> stock = new TableColumn<>("Current Stock (kg)");
        stock.setCellValueFactory(d -> {
            double stockValue = d.getValue().getCurrentStock();
            String stockText = String.format("%.2f kg", stockValue);
            
            // Color coding for low stock
            SimpleStringProperty property = new SimpleStringProperty(stockText);
            
            // We'll use cell factory for coloring instead
            return property;
        });
        stock.setPrefWidth(150);
        
        TableColumn<InventoryItem, String> status = new TableColumn<>("Stock Status");
        status.setCellValueFactory(d -> {
            double stockValue = d.getValue().getCurrentStock();
            String statusText;
            
            if (stockValue <= 0) {
                statusText = "OUT OF STOCK";
            } else if (stockValue <= LOW_STOCK_THRESHOLD) {
                statusText = "LOW STOCK";
            } else {
                statusText = "AVAILABLE";
            }
            return new SimpleStringProperty(statusText);
        });
        status.setPrefWidth(120);
        
        TableColumn<InventoryItem, String> recommended = new TableColumn<>("Recommendation");
        recommended.setCellValueFactory(d -> {
            double stockValue = d.getValue().getCurrentStock();
            String recommendation;
            
            if (stockValue <= 0) {
                recommendation = "URGENT: Restock immediately";
            } else if (stockValue <= LOW_STOCK_THRESHOLD) {
                recommendation = "Consider restocking soon";
            } else {
                recommendation = "Stock level OK";
            }
            return new SimpleStringProperty(recommendation);
        });
        recommended.setPrefWidth(200);
        
        inventoryTable.getColumns().addAll(type, stock, status, recommended);
        inventoryTable.setItems(inventory);
        
        // Add cell factory for coloring stock column
        stock.setCellFactory(column -> new TableCell<InventoryItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item);
                    
                    // Get the actual stock value from the row
                    InventoryItem rowItem = getTableView().getItems().get(getIndex());
                    double stockValue = rowItem.getCurrentStock();
                    
                    if (stockValue <= 0) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #ffebee;");
                    } else if (stockValue <= LOW_STOCK_THRESHOLD) {
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-background-color: #fff3cd;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Add cell factory for coloring status column
        status.setCellFactory(column -> new TableCell<InventoryItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item);
                    
                    switch (item) {
                        case "OUT OF STOCK":
                            setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-color: #e74c3c; -fx-padding: 5;");
                            break;
                        case "LOW STOCK":
                            setStyle("-fx-text-fill: #856404; -fx-font-weight: bold; -fx-background-color: #fff3cd; -fx-padding: 5;");
                            break;
                        case "AVAILABLE":
                            setStyle("-fx-text-fill: #155724; -fx-font-weight: bold; -fx-background-color: #d4edda; -fx-padding: 5;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }

    private void loadInventory() {
        inventory.clear();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT grain_type, current_stock_kg FROM inventory ORDER BY grain_type")) {
            
            while (rs.next()) {
                inventory.add(new InventoryItem(rs.getString(1), rs.getDouble(2)));
            }
            
        } catch (SQLException e) { 
            e.printStackTrace();
            // Add sample data for testing
            inventory.add(new InventoryItem("Wheat", 150.0));  // LOW STOCK
            inventory.add(new InventoryItem("Teff", 800.0));   // AVAILABLE
            inventory.add(new InventoryItem("Corn", 0.0));     // OUT OF STOCK
            inventory.add(new InventoryItem("Barley", 600.0)); // AVAILABLE
        }
    }

    private void checkLowStock() {
        StringBuilder alertMessage = new StringBuilder();
        int lowStockCount = 0;
        int outOfStockCount = 0;
        
        for (InventoryItem item : inventory) {
            double stock = item.getCurrentStock();
            
            if (stock <= 0) {
                outOfStockCount++;
                alertMessage.append("⚠️ ").append(item.getGrainType()).append(" is OUT OF STOCK!\n");
            } else if (stock <= LOW_STOCK_THRESHOLD) {
                lowStockCount++;
                alertMessage.append("⚠️ ").append(item.getGrainType()).append(" is LOW: ").append(String.format("%.2f", stock)).append(" kg remaining\n");
            }
        }
        
        if (outOfStockCount > 0 || lowStockCount > 0) {
            String header = "Stock Alert: ";
            if (outOfStockCount > 0) {
                header += outOfStockCount + " item(s) out of stock";
                if (lowStockCount > 0) {
                    header += ", " + lowStockCount + " item(s) low";
                }
            } else {
                header += lowStockCount + " item(s) low on stock";
            }
            
            lowStockAlertLabel.setText(header + "\n" + alertMessage.toString());
            
            if (outOfStockCount > 0) {
                lowStockAlertLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; " +
                                           "-fx-background-color: #ffebee; -fx-border-color: #ef9a9a; " +
                                           "-fx-border-radius: 5; -fx-padding: 10;");
            } else {
                lowStockAlertLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; " +
                                           "-fx-background-color: #fff3cd; -fx-border-color: #ffeaa7; " +
                                           "-fx-border-radius: 5; -fx-padding: 10;");
            }
        } else {
            lowStockAlertLabel.setText("✓ All grain stocks are at sufficient levels");
            lowStockAlertLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; " +
                                       "-fx-background-color: #d4edda; -fx-border-color: #c3e6cb; " +
                                       "-fx-border-radius: 5; -fx-padding: 10;");
        }
    }
    
    @FXML
    private void handleReportLowStock() {
        // Get low stock items
        StringBuilder report = new StringBuilder();
        report.append("LOW STOCK REPORT\n");
        report.append("Generated by: ").append(CurrentUser.getInstance().getStaffName()).append("\n");
        report.append("Date: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        report.append("================================\n\n");
        
        for (InventoryItem item : inventory) {
            double stock = item.getCurrentStock();
            
            if (stock <= 0) {
                report.append("❌ ").append(item.getGrainType()).append(": OUT OF STOCK\n");
                report.append("   Action Required: URGENT RESTOCK NEEDED\n\n");
            } else if (stock <= LOW_STOCK_THRESHOLD) {
                report.append("⚠️ ").append(item.getGrainType()).append(": ").append(String.format("%.2f", stock)).append(" kg remaining\n");
                report.append("   Threshold: ").append(LOW_STOCK_THRESHOLD).append(" kg\n");
                report.append("   Status: LOW STOCK - Consider restocking\n\n");
            }
        }
        
        // Show report to operator
        TextArea reportArea = new TextArea(report.toString());
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        
        ScrollPane scrollPane = new ScrollPane(reportArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        
        Alert reportAlert = new Alert(Alert.AlertType.INFORMATION);
        reportAlert.setTitle("Low Stock Report");
        reportAlert.setHeaderText("Inventory Status Report");
        reportAlert.getDialogPane().setContent(scrollPane);
        
        // Add Send to Admin button
        ButtonType sendToAdminButton = new ButtonType("Send Report to Admin");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        reportAlert.getButtonTypes().setAll(sendToAdminButton, closeButton);
        
        reportAlert.showAndWait().ifPresent(response -> {
            if (response == sendToAdminButton) {
                sendReportToAdmin(report.toString());
            }
        });
    }
    
    private void sendReportToAdmin(String report) {
        try {
            // Save report to database notifications table
            String sql = "INSERT INTO admin_notifications (title, message, sender, priority, created_date) " +
                        "VALUES (?, ?, ?, ?, NOW())";
            
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                
                String operatorName = CurrentUser.getInstance().getStaffName();
                if (operatorName == null) operatorName = "Operator";
                
                ps.setString(1, "Low Stock Report");
                ps.setString(2, report);
                ps.setString(3, operatorName);
                ps.setString(4, "HIGH"); // Priority
                ps.executeUpdate();
                
                showAlert("Report Sent", 
                         "Low stock report has been sent to Administrator.\n" +
                         "Admin will review and take necessary action.",
                         Alert.AlertType.INFORMATION);
                
            }
        } catch (SQLException e) {
            System.err.println("Failed to save report: " + e.getMessage());
            showAlert("Error", 
                     "Could not send report to database. Please inform admin manually.",
                     Alert.AlertType.WARNING);
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public void refresh() { 
        loadInventory(); 
        checkLowStock();
    }

    public static class InventoryItem {
        private String grainType;
        private double currentStock;
        
        public InventoryItem(String g, double s) { 
            grainType = g; 
            currentStock = s; 
        }
        
        public String getGrainType() { 
            return grainType; 
        }
        
        public double getCurrentStock() { 
            return currentStock; 
        }
        
        public void setCurrentStock(double stock) {
            currentStock = stock;
        }
    }
}