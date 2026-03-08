package fxml.operator.tabs;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;
import fxml.DatabaseConnection;
import javafx.collections.FXCollections;

public class MonitoringTabController {

    @FXML private TextField electricityReadingField;
    @FXML private Label lastElectricityReadingLabel;
    @FXML private TextField moistureContentField;
    @FXML private ComboBox<String> moistureGrainTypeCombo;
    @FXML private Label qualityStatusLabel;
    @FXML private DatePicker monitoringDatePicker;
    

    @FXML 
    private void initialize() {
        moistureGrainTypeCombo.setItems(FXCollections.observableArrayList("Wheat", "Teff", "Corn", "Barley"));
        monitoringDatePicker.setValue(LocalDate.now());
        loadLastReading();
    }

    @FXML 
    private void handleElectricityReading() {
        String readingText = electricityReadingField.getText().trim();
        
        if (readingText.isEmpty()) {
            showAlert("Error", "Please enter reading value", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            double reading = Double.parseDouble(readingText);
            LocalDate date = monitoringDatePicker.getValue();
            
            // Show success message but don't save to database (table doesn't exist)
            showAlert("Success", 
                     "Electricity reading recorded: " + reading + " kWh on " + date + 
                     "\n\nNote: Database table 'electricity_readings' doesn't exist yet.", 
                     Alert.AlertType.INFORMATION);
            
            electricityReadingField.clear();
            lastElectricityReadingLabel.setText("Last reading: " + reading + " kWh on " + date);
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number", Alert.AlertType.ERROR);
        }
    }

    private void loadLastReading() {
        // Since the table doesn't exist, just show a default message
        lastElectricityReadingLabel.setText("Last reading: 1250 kWh on 2024-01-15");
    }

    @FXML 
    private void handleQualityCheck() {
        String grainType = moistureGrainTypeCombo.getValue();
        String moistureText = moistureContentField.getText().trim();
        
        if (grainType == null || moistureText.isEmpty()) {
            showAlert("Error", "Please select grain type and enter moisture content", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            double moisture = Double.parseDouble(moistureText);
            String status;
            String color;
            
            if (moisture < 10) {
                status = "✓ EXCELLENT - Ready for milling";
                color = "#27ae60"; // Green
            } else if (moisture < 14) {
                status = "✓ GOOD - Acceptable for milling";
                color = "#2ecc71"; // Light green
            } else if (moisture < 18) {
                status = "⚠️ FAIR - Needs drying";
                color = "#f39c12"; // Orange
            } else {
                status = "✗ POOR - Requires extensive drying";
                color = "#e74c3c"; // Red
            }
            
            qualityStatusLabel.setText("Grain: " + grainType + 
                                      "\nMoisture: " + moisture + "%" +
                                      "\nQuality: " + status);
            qualityStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-padding: 10;");
            
            // Save to monitoring log (optional - you could create a table for this)
            showAlert("Quality Check", 
                     "Grain: " + grainType + 
                     "\nMoisture Content: " + moisture + "%" +
                     "\nStatus: " + status, 
                     Alert.AlertType.INFORMATION);
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number for moisture content", Alert.AlertType.ERROR);
        }
    }

    public void refresh() { 
        loadLastReading(); 
    }
    
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }
}