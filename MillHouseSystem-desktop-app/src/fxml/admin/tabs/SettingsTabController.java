package fxml.admin.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

import fxml.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class SettingsTabController {

    @FXML private TableView<PriceSetting> pricesTable;
    @FXML private TextField lowStockThresholdField;
    @FXML private TextField electricityCostField;

    private ObservableList<PriceSetting> prices = FXCollections.observableArrayList();

    @FXML 
    private void initialize() {
    	
        setupTable();  // Call this FIRST
        loadPrices();
        loadThreshold();
    }
    
    private void setupTable() {
        // Clear any existing columns
        pricesTable.getColumns().clear();
        
        TableColumn<PriceSetting, String> grainCol = new TableColumn<>("Grain Type");
        grainCol.setCellValueFactory(cell -> cell.getValue().grainTypeProperty());
        grainCol.setPrefWidth(200);
        
        TableColumn<PriceSetting, String> priceCol = new TableColumn<>("Price per KG (ETB)");
        priceCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(String.format("ETB %.2f", cell.getValue().getPricePerKg())));
        priceCol.setPrefWidth(200);
        
        pricesTable.getColumns().addAll(grainCol, priceCol);
    }

    private void loadPrices() {
        prices.clear();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT grain_type, price_per_kg FROM settings WHERE grain_type != 'System'")) {
            while (rs.next()) {
                prices.add(new PriceSetting(rs.getString("grain_type"), rs.getDouble("price_per_kg")));
            }
            pricesTable.setItems(prices);
        } catch (SQLException e) { 
            e.printStackTrace();
            // Add sample data for testing
            prices.add(new PriceSetting("Wheat", 45.00));
            prices.add(new PriceSetting("Teff", 85.00));
            prices.add(new PriceSetting("Corn", 40.00));
            prices.add(new PriceSetting("Barley", 42.00));
            pricesTable.setItems(prices);
        }
    }

    private void loadThreshold() {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT low_stock_threshold FROM settings LIMIT 1")) {
            if (rs.next()) {
                lowStockThresholdField.setText(String.valueOf(rs.getDouble("low_stock_threshold")));
            } else {
                lowStockThresholdField.setText("200"); // Default
            }
        } catch (SQLException e) { 
            e.printStackTrace();
            lowStockThresholdField.setText("200"); // Default
        }
    }

    @FXML 
    private void handleUpdatePrices() { 
        loadPrices(); 
        showAlert("Success", "Prices refreshed", Alert.AlertType.INFORMATION);
    }
    
    @FXML 
    private void handleSaveSystemSettings() { 
        try {
            String threshold = lowStockThresholdField.getText();
            if (!threshold.matches("\\d+")) {
                showAlert("Error", "Please enter a valid number", Alert.AlertType.ERROR);
                return;
            }
            
            // Update threshold in settings
            String sql = "UPDATE settings SET low_stock_threshold = ? WHERE grain_type = 'Wheat'";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDouble(1, Double.parseDouble(threshold));
                ps.executeUpdate();
            }
            
            showAlert("Success", "Settings saved", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to save settings", Alert.AlertType.ERROR);
        }
    }

    public void refresh() { 
        loadPrices(); 
        loadThreshold(); 
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
    
    // Inner class for PriceSetting
    public static class PriceSetting {
        private SimpleStringProperty grainType;
        private SimpleDoubleProperty pricePerKg;
        
        public PriceSetting(String grainType, double pricePerKg) {
            this.grainType = new SimpleStringProperty(grainType);
            this.pricePerKg = new SimpleDoubleProperty(pricePerKg);
        }
        
        public String getGrainType() { return grainType.get(); }
        public void setGrainType(String grainType) { this.grainType.set(grainType); }
        public SimpleStringProperty grainTypeProperty() { return grainType; }
        
        public double getPricePerKg() { return pricePerKg.get(); }
        public void setPricePerKg(double pricePerKg) { this.pricePerKg.set(pricePerKg); }
        public SimpleDoubleProperty pricePerKgProperty() { return pricePerKg; }
    }
}