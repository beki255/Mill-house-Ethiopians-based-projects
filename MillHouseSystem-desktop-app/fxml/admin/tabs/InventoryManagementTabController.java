package fxml.admin.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import java.sql.*;

import fxml.DatabaseConnection;
import fxml.GrainTypeManager;

public class InventoryManagementTabController {

    @FXML private TableView<GrainInventory> inventoryTable;
    @FXML private TextField grainNameField, currentStockField, pricePerKgField;
    @FXML private ComboBox<String> grainTypeCombo;
    @FXML private Label totalInventoryValueLabel;
    @FXML private Button addGrainBtn, updateGrainBtn, deleteGrainBtn;
    
    @FXML private TableView<LowStockReport> reportsTable;
    @FXML private Label reportsLabel;

    private ObservableList<GrainInventory> grainInventory = FXCollections.observableArrayList();
    private ObservableList<LowStockReport> lowStockReports = FXCollections.observableArrayList();

    @FXML 
    private void initialize() {
        System.out.println("=== ADMIN INVENTORY MANAGEMENT INITIALIZED ===");
        
        // Setup grain type options
        grainTypeCombo.setItems(FXCollections.observableArrayList(
            "Wheat", "Teff", "Corn", "Barley", "Sorghum", "Millet", "Rice"
        ));
        
        setupInventoryTable();
        setupReportsTable();
        loadInventory();
        loadLowStockReports();
        
        // Add selection listener for inventory table
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadSelectedGrain(newSelection);
            }
        });
    }
    
    private void setupInventoryTable() {
        inventoryTable.getColumns().clear();
        
        TableColumn<GrainInventory, String> grainCol = new TableColumn<>("Grain Type");
        grainCol.setCellValueFactory(cell -> cell.getValue().grainTypeProperty());
        grainCol.setPrefWidth(150);
        
        TableColumn<GrainInventory, String> stockCol = new TableColumn<>("Current Stock (kg)");
        stockCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(String.format("%.2f kg", cell.getValue().getCurrentStock())));
        stockCol.setPrefWidth(150);
        
        TableColumn<GrainInventory, String> priceCol = new TableColumn<>("Price per KG (ETB)");
        priceCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(String.format("ETB %.2f", cell.getValue().getPricePerKg())));
        priceCol.setPrefWidth(150);
        
        TableColumn<GrainInventory, String> valueCol = new TableColumn<>("Inventory Value (ETB)");
        valueCol.setCellValueFactory(cell -> {
            double value = cell.getValue().getCurrentStock() * cell.getValue().getPricePerKg();
            return new SimpleStringProperty(String.format("ETB %.2f", value));
        });
        valueCol.setPrefWidth(180);
        
        TableColumn<GrainInventory, String> statusCol = new TableColumn<>("Stock Status");
        statusCol.setCellValueFactory(cell -> {
            double stock = cell.getValue().getCurrentStock();
            String status;
            if (stock <= 0) {
                status = "OUT OF STOCK";
            } else if (stock <= 200) {
                status = "LOW STOCK";
            } else {
                status = "AVAILABLE";
            }
            return new SimpleStringProperty(status);
        });
        statusCol.setPrefWidth(120);
        statusCol.setCellFactory(column -> new TableCell<GrainInventory, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "OUT OF STOCK":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #ffebee; -fx-padding: 5;");
                            break;
                        case "LOW STOCK":
                            setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-background-color: #fff3cd; -fx-padding: 5;");
                            break;
                        case "AVAILABLE":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-background-color: #d4edda; -fx-padding: 5;");
                            break;
                    }
                }
            }
        });
        
        inventoryTable.getColumns().addAll(grainCol, stockCol, priceCol, valueCol, statusCol);
        inventoryTable.setItems(grainInventory);
    }
    
    private void setupReportsTable() {
        reportsTable.getColumns().clear();
        
        TableColumn<LowStockReport, String> dateCol = new TableColumn<>("Report Date");
        dateCol.setCellValueFactory(cell -> cell.getValue().reportDateProperty());
        dateCol.setPrefWidth(150);
        
        TableColumn<LowStockReport, String> senderCol = new TableColumn<>("Reported By");
        senderCol.setCellValueFactory(cell -> cell.getValue().senderProperty());
        senderCol.setPrefWidth(150);
        
        TableColumn<LowStockReport, String> titleCol = new TableColumn<>("Report Title");
        titleCol.setCellValueFactory(cell -> cell.getValue().titleProperty());
        titleCol.setPrefWidth(200);
        
        TableColumn<LowStockReport, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(cell -> cell.getValue().priorityProperty());
        priorityCol.setPrefWidth(100);
        priorityCol.setCellFactory(column -> new TableCell<LowStockReport, String>() {
            @Override
            protected void updateItem(String priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(priority);
                    switch (priority) {
                        case "HIGH":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        case "MEDIUM":
                            setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                            break;
                        case "LOW":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });
        
        TableColumn<LowStockReport, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> cell.getValue().statusProperty());
        statusCol.setPrefWidth(100);
        
        reportsTable.getColumns().addAll(dateCol, senderCol, titleCol, priorityCol, statusCol);
        reportsTable.setItems(lowStockReports);
        
        // Add double-click listener to view full report
        reportsTable.setRowFactory(tv -> {
            TableRow<LowStockReport> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    LowStockReport report = row.getItem();
                    viewReportDetails(report);
                }
            });
            return row;
        });
    }
    
    private void loadInventory() {
        grainInventory.clear();
        
        // FIXED SQL: Ensure price is never null
        String sql = "SELECT i.grain_type, i.current_stock_kg, " +
                    "COALESCE(s.price_per_kg, 45.00) as price_per_kg " +  // Default price if null
                    "FROM inventory i " +
                    "LEFT JOIN settings s ON i.grain_type = s.grain_type " +
                    "ORDER BY i.grain_type";
        
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            
            double totalValue = 0;
            int count = 0;
            
            while (rs.next()) {
                count++;
                GrainInventory grain = new GrainInventory(
                    rs.getString("grain_type"),
                    rs.getDouble("current_stock_kg"),
                    rs.getDouble("price_per_kg")
                );
                grainInventory.add(grain);
                
                totalValue += grain.getCurrentStock() * grain.getPricePerKg();
                
                System.out.println("Loaded grain: " + grain.getGrainType() + 
                                 " - Stock: " + grain.getCurrentStock() + 
                                 "kg - Price: ETB " + grain.getPricePerKg());
            }
            
            totalInventoryValueLabel.setText(String.format("Total Inventory Value: ETB %.2f", totalValue));
            System.out.println("Loaded " + count + " grain types from inventory");
            
        } catch (SQLException e) { 
            System.err.println("Error loading inventory: " + e.getMessage());
            e.printStackTrace();
            loadSampleInventory();
        }
    }
    
    private void loadSampleInventory() {
        System.out.println("Loading sample inventory data...");
        
        grainInventory.add(new GrainInventory("Wheat", 150.0, 45.00));
        grainInventory.add(new GrainInventory("Teff", 800.0, 85.00));
        grainInventory.add(new GrainInventory("Corn", 0.0, 40.00));
        grainInventory.add(new GrainInventory("Barley", 600.0, 42.00));
        
        double totalValue = grainInventory.stream()
            .mapToDouble(g -> g.getCurrentStock() * g.getPricePerKg())
            .sum();
        
        totalInventoryValueLabel.setText(String.format("Total Inventory Value: ETB %.2f", totalValue));
    }
    
    private void loadLowStockReports() {
        lowStockReports.clear();
        
        // First check if table exists
        if (!tableExists("admin_notifications")) {
            System.out.println("admin_notifications table doesn't exist yet");
            reportsLabel.setText("No reports table found. Operators can send reports.");
            return;
        }
        
        String sql = "SELECT id, title, message, sender, priority, status, created_date " +
                    "FROM admin_notifications " +
                    "WHERE title LIKE '%Stock%' OR title LIKE '%stock%' " +
                    "ORDER BY created_date DESC";
        
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                LowStockReport report = new LowStockReport(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("message"),
                    rs.getString("sender"),
                    rs.getString("priority"),
                    rs.getString("status"),
                    rs.getTimestamp("created_date")
                );
                lowStockReports.add(report);
            }
            
            reportsLabel.setText("Low Stock Reports: " + count + " report(s)");
            System.out.println("Loaded " + count + " low stock reports");
            
        } catch (SQLException e) { 
            System.err.println("Error loading reports: " + e.getMessage());
            reportsLabel.setText("Error loading reports");
        }
    }
    
    private boolean tableExists(String tableName) {
        String sql = "SHOW TABLES LIKE '" + tableName + "'";
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    private void loadSelectedGrain(GrainInventory grain) {
        grainNameField.setText(grain.getGrainType());
        grainTypeCombo.setValue(grain.getGrainType());
        currentStockField.setText(String.format("%.2f", grain.getCurrentStock()));
        pricePerKgField.setText(String.format("%.2f", grain.getPricePerKg()));
    }
    
    @FXML 
    private void handleAddGrain() {
        String grainType = grainTypeCombo.getValue();
        String stockText = currentStockField.getText().trim();
        String priceText = pricePerKgField.getText().trim();
        
        // Validation
        if (grainType == null || grainType.isEmpty()) {
            showAlert("Error", "Please select grain type", Alert.AlertType.ERROR);
            return;
        }
        
        // Check if grain already exists
        for (GrainInventory grain : grainInventory) {
            if (grain.getGrainType().equalsIgnoreCase(grainType)) {
                showAlert("Error", "Grain type '" + grainType + "' already exists!", Alert.AlertType.ERROR);
                return;
            }
        }
        
        try {
            double stock = stockText.isEmpty() ? 0.0 : Double.parseDouble(stockText);
            double price = priceText.isEmpty() ? 0.0 : Double.parseDouble(priceText);
            
            if (price <= 0) {
                showAlert("Error", "Price must be greater than 0", Alert.AlertType.ERROR);
                return;
                
            }
            
            // Start transaction
            Connection c = DatabaseConnection.getConnection();
            c.setAutoCommit(false);
            
            try {
                // Add to inventory table
                String inventorySql = "INSERT INTO inventory (grain_type, current_stock_kg) VALUES (?, ?)";
                try (PreparedStatement ps = c.prepareStatement(inventorySql)) {
                    ps.setString(1, grainType);
                    ps.setDouble(2, stock);
                    ps.executeUpdate();
                }
                
                // Add to settings table (prices)
                String settingsSql = "INSERT INTO settings (grain_type, price_per_kg, delivery_fee_per_kg) VALUES (?, ?, 5.00)";
                try (PreparedStatement ps = c.prepareStatement(settingsSql)) {
                    ps.setString(1, grainType);
                    ps.setDouble(2, price);
                    ps.executeUpdate();
                }
                
                c.commit();
                
                // Update local data
                grainInventory.add(new GrainInventory(grainType, stock, price));
                inventoryTable.refresh();
                GrainTypeManager.refreshGrainTypes();
                
                showAlert("Success", 
                         "Grain '" + grainType + "' added successfully!\n" +
                         "Stock: " + stock + " kg\n" +
                         "Price: ETB " + price + " per kg",
                         Alert.AlertType.INFORMATION);
                
                clearForm();
                
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for stock and price", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            showAlert("Error", "Failed to add grain: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML 
    private void handleUpdateGrain() {
        GrainInventory selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a grain to update", Alert.AlertType.ERROR);
            return;
        }
        
        String grainType = grainTypeCombo.getValue();
        String stockText = currentStockField.getText().trim();
        String priceText = pricePerKgField.getText().trim();
        
        if (grainType == null || grainType.isEmpty()) {
            showAlert("Error", "Please select grain type", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            double stock = stockText.isEmpty() ? 0.0 : Double.parseDouble(stockText);
            double price = priceText.isEmpty() ? 0.0 : Double.parseDouble(priceText);
            
            if (price <= 0) {
                showAlert("Error", "Price must be greater than 0", Alert.AlertType.ERROR);
                return;
            }
            
            // Start transaction
            Connection c = DatabaseConnection.getConnection();
            c.setAutoCommit(false);
            
            try {
                // Update inventory table
                String inventorySql = "UPDATE inventory SET current_stock_kg = ? WHERE grain_type = ?";
                try (PreparedStatement ps = c.prepareStatement(inventorySql)) {
                    ps.setDouble(1, stock);
                    ps.setString(2, selected.getGrainType());
                    ps.executeUpdate();
                }
                
                // Update settings table (prices)
                String settingsSql = "UPDATE settings SET price_per_kg = ? WHERE grain_type = ?";
                try (PreparedStatement ps = c.prepareStatement(settingsSql)) {
                    ps.setDouble(1, price);
                    ps.setString(2, selected.getGrainType());
                    int rows = ps.executeUpdate();
                    
                    // If grain type changed in settings, update it
                    if (rows == 0) {
                        // Try to insert if doesn't exist
                        String insertSql = "INSERT INTO settings (grain_type, price_per_kg, delivery_fee_per_kg) VALUES (?, ?, 5.00)";
                        try (PreparedStatement insertPs = c.prepareStatement(insertSql)) {
                            insertPs.setString(1, selected.getGrainType());
                            insertPs.setDouble(2, price);
                            insertPs.executeUpdate();
                        }
                    }
                }
                
                // If grain type changed
                if (!selected.getGrainType().equals(grainType)) {
                    // Update grain type name in inventory
                    String updateNameSql = "UPDATE inventory SET grain_type = ? WHERE grain_type = ?";
                    try (PreparedStatement ps = c.prepareStatement(updateNameSql)) {
                        ps.setString(1, grainType);
                        ps.setString(2, selected.getGrainType());
                        ps.executeUpdate();
                    }
                    
                    // Update grain type name in settings
                    String updateSettingsSql = "UPDATE settings SET grain_type = ? WHERE grain_type = ?";
                    try (PreparedStatement ps = c.prepareStatement(updateSettingsSql)) {
                        ps.setString(1, grainType);
                        ps.setString(2, selected.getGrainType());
                        ps.executeUpdate();
                    }
                }
                
                c.commit();
                
                selected.setGrainType(grainType);
                selected.setCurrentStock(stock);
                selected.setPricePerKg(price);
                inventoryTable.refresh();
                
                GrainTypeManager.refreshGrainTypes();
                
                // Update total value
                double totalValue = grainInventory.stream()
                    .mapToDouble(g -> g.getCurrentStock() * g.getPricePerKg())
                    .sum();
                totalInventoryValueLabel.setText(String.format("Total Inventory Value: ETB %.2f", totalValue));
                
                showAlert("Success", 
                         "Grain updated successfully!\n" +
                         "New Name: " + grainType + "\n" +
                         "Stock: " + stock + " kg\n" +
                         "Price: ETB " + price + " per kg",
                         Alert.AlertType.INFORMATION);
                
                clearForm();
                
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for stock and price", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            showAlert("Error", "Failed to update grain: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML 
    private void handleDeleteGrain() {
        GrainInventory selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a grain to delete", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Grain Type");
        confirm.setContentText("Are you sure you want to delete '" + selected.getGrainType() + "'?\n" +
                              "This will remove it from inventory and pricing.\n\n" +
                              "Warning: This action cannot be undone!");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Start transaction
                    Connection c = DatabaseConnection.getConnection();
                    c.setAutoCommit(false);
                    
                    try {
                        // Delete from inventory
                        String inventorySql = "DELETE FROM inventory WHERE grain_type = ?";
                        try (PreparedStatement ps = c.prepareStatement(inventorySql)) {
                            ps.setString(1, selected.getGrainType());
                            ps.executeUpdate();
                        }
                        
                        // Delete from settings
                        String settingsSql = "DELETE FROM settings WHERE grain_type = ?";
                        try (PreparedStatement ps = c.prepareStatement(settingsSql)) {
                            ps.setString(1, selected.getGrainType());
                            ps.executeUpdate();
                        }
                        
                        c.commit();
                        
                        // Update local data
                        grainInventory.remove(selected);
                        // REFRESH GRAIN TYPES FOR ALL USERS
                        GrainTypeManager.refreshGrainTypes();
                        // Update total value
                        double totalValue = grainInventory.stream()
                            .mapToDouble(g -> g.getCurrentStock() * g.getPricePerKg())
                            .sum();
                        totalInventoryValueLabel.setText(String.format("Total Inventory Value: ETB %.2f", totalValue));
                        
                        showAlert("Success", 
                                 "Grain '" + selected.getGrainType() + "' deleted successfully!",
                                 Alert.AlertType.INFORMATION);
                        
                        clearForm();
                        
                    } catch (SQLException e) {
                        c.rollback();
                        throw e;
                    } finally {
                        c.setAutoCommit(true);
                    }
                    
                } catch (SQLException e) {
                    showAlert("Error", "Failed to delete grain: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    @FXML
    private void handleMarkReportAsRead() {
        LowStockReport selected = reportsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a report", Alert.AlertType.ERROR);
            return;
        }
        
        String sql = "UPDATE admin_notifications SET status = 'READ', read_date = NOW() WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, selected.getId());
            ps.executeUpdate();
            
            selected.setStatus("READ");
            reportsTable.refresh();
            
            showAlert("Success", "Report marked as read", Alert.AlertType.INFORMATION);
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to update report: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleMarkReportAsResolved() {
        LowStockReport selected = reportsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a report", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mark as Resolved");
        confirm.setHeaderText("Resolve Low Stock Report");
        confirm.setContentText("Mark this report as resolved?\n\n" +
                              "Title: " + selected.getTitle() + "\n" +
                              "Sender: " + selected.getSender());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "UPDATE admin_notifications SET status = 'RESOLVED', resolved_date = NOW() WHERE id = ?";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, selected.getId());
                    ps.executeUpdate();
                    
                    selected.setStatus("RESOLVED");
                    reportsTable.refresh();
                    
                    showAlert("Success", "Report marked as resolved", Alert.AlertType.INFORMATION);
                    
                } catch (SQLException e) {
                    showAlert("Error", "Failed to update report: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    @FXML
    private void handleDeleteReport() {
        LowStockReport selected = reportsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a report to delete", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Report");
        confirm.setHeaderText("Delete Low Stock Report");
        confirm.setContentText("Are you sure you want to delete this report?\n\n" +
                              "Title: " + selected.getTitle() + "\n" +
                              "Date: " + selected.getReportDate());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "DELETE FROM admin_notifications WHERE id = ?";
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, selected.getId());
                    ps.executeUpdate();
                    
                    lowStockReports.remove(selected);
                    
                    showAlert("Success", "Report deleted successfully", Alert.AlertType.INFORMATION);
                    
                } catch (SQLException e) {
                    showAlert("Error", "Failed to delete report: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void viewReportDetails(LowStockReport report) {
        TextArea reportArea = new TextArea(report.getMessage());
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        reportArea.setPrefRowCount(15);
        
        ScrollPane scrollPane = new ScrollPane(reportArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        
        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Report Details");
        detailsAlert.setHeaderText("Report: " + report.getTitle() + "\n" +
                                  "From: " + report.getSender() + " | " + report.getReportDate());
        detailsAlert.getDialogPane().setContent(scrollPane);
        
        // Add action buttons
        ButtonType markReadButton = new ButtonType("Mark as Read");
        ButtonType markResolvedButton = new ButtonType("Mark as Resolved");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        if ("UNREAD".equals(report.getStatus())) {
            detailsAlert.getButtonTypes().setAll(markReadButton, markResolvedButton, closeButton);
        } else {
            detailsAlert.getButtonTypes().setAll(markResolvedButton, closeButton);
        }
        
        detailsAlert.showAndWait().ifPresent(response -> {
            if (response == markReadButton) {
                handleMarkReportAsRead();
            } else if (response == markResolvedButton) {
                handleMarkReportAsResolved();
            }
        });
    }
    
    private void clearForm() {
        grainNameField.clear();
        grainTypeCombo.setValue(null);
        currentStockField.clear();
        pricePerKgField.clear();
        inventoryTable.getSelectionModel().clearSelection();
    }
    
    public void refresh() { 
        loadInventory(); 
        loadLowStockReports();
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
    
    // Inner classes
    public static class GrainInventory {
        private SimpleStringProperty grainType;
        private SimpleDoubleProperty currentStock;
        private SimpleDoubleProperty pricePerKg;
        
        public GrainInventory(String grainType, double currentStock, double pricePerKg) {
            this.grainType = new SimpleStringProperty(grainType);
            this.currentStock = new SimpleDoubleProperty(currentStock);
            this.pricePerKg = new SimpleDoubleProperty(pricePerKg);
        }
        
        public String getGrainType() { return grainType.get(); }
        public void setGrainType(String grainType) { this.grainType.set(grainType); }
        public SimpleStringProperty grainTypeProperty() { return grainType; }
        
        public double getCurrentStock() { return currentStock.get(); }
        public void setCurrentStock(double currentStock) { this.currentStock.set(currentStock); }
        public SimpleDoubleProperty currentStockProperty() { return currentStock; }
        
        public double getPricePerKg() { return pricePerKg.get(); }
        public void setPricePerKg(double pricePerKg) { this.pricePerKg.set(pricePerKg); }
        public SimpleDoubleProperty pricePerKgProperty() { return pricePerKg; }
    }
    
    public static class LowStockReport {
        private int id;
        private SimpleStringProperty title;
        private SimpleStringProperty message;
        private SimpleStringProperty sender;
        private SimpleStringProperty priority;
        private SimpleStringProperty status;
        private SimpleStringProperty reportDate;
        
        public LowStockReport(int id, String title, String message, String sender, 
                            String priority, String status, Timestamp createdDate) {
            this.id = id;
            this.title = new SimpleStringProperty(title);
            this.message = new SimpleStringProperty(message);
            this.sender = new SimpleStringProperty(sender);
            this.priority = new SimpleStringProperty(priority);
            this.status = new SimpleStringProperty(status);
            this.reportDate = new SimpleStringProperty(
                createdDate != null ? createdDate.toString().substring(0, 19) : "N/A"
            );
        }
        
        public int getId() { return id; }
        
        public String getTitle() { return title.get(); }
        public void setTitle(String title) { this.title.set(title); }
        public SimpleStringProperty titleProperty() { return title; }
        
        public String getMessage() { return message.get(); }
        public void setMessage(String message) { this.message.set(message); }
        public SimpleStringProperty messageProperty() { return message; }
        
        public String getSender() { return sender.get(); }
        public void setSender(String sender) { this.sender.set(sender); }
        public SimpleStringProperty senderProperty() { return sender; }
        
        public String getPriority() { return priority.get(); }
        public void setPriority(String priority) { this.priority.set(priority); }
        public SimpleStringProperty priorityProperty() { return priority; }
        
        public String getStatus() { return status.get(); }
        public void setStatus(String status) { this.status.set(status); }
        public SimpleStringProperty statusProperty() { return status; }
        
        public String getReportDate() { return reportDate.get(); }
        public void setReportDate(String reportDate) { this.reportDate.set(reportDate); }
        public SimpleStringProperty reportDateProperty() { return reportDate; }
    }
}