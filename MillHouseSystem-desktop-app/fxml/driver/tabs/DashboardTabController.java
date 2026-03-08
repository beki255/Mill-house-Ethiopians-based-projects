package fxml.driver.tabs;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class DashboardTabController {

    @FXML private Label todayDeliveriesLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label onRouteCountLabel;
    @FXML private Label deliveredTodayLabel;
    @FXML private TextField vehicleStatusField; // Add this field
    @FXML private Label welcomeLabel; // Add this if needed

    @FXML 
    private void initialize() {
        loadSummary();
    }

    public void refresh() {
        loadSummary();
    }

    private void loadSummary() {
        // You can connect to DB later — for now showing example data
        if (todayDeliveriesLabel != null) 
            todayDeliveriesLabel.setText("Today's Deliveries: 7");
        if (pendingCountLabel != null) 
            pendingCountLabel.setText("3");
        if (onRouteCountLabel != null) 
            onRouteCountLabel.setText("2");
        if (deliveredTodayLabel != null) 
            deliveredTodayLabel.setText("12");
        
        // Set welcome message if welcomeLabel exists
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Driver!");
        }
    }
    
    // ADD THIS METHOD - It's referenced in DashboardTab.fxml
    @FXML 
    private void handleUpdateVehicleStatus() {
        String status = vehicleStatusField.getText().trim();
        if (!status.isEmpty()) {
            showAlert("Vehicle Status Updated", "Vehicle status set to: " + status, Alert.AlertType.INFORMATION);
            vehicleStatusField.clear();
        } else {
            showAlert("Error", "Please enter vehicle status", Alert.AlertType.ERROR);
        }
    }
    
    // Helper method for alerts
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
}