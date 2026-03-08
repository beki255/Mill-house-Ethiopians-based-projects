package fxml;

import fxml.driver.tabs.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DriverMainController {

    @FXML private DashboardTabController dashboardTabController;
    @FXML private DeliveryManagementTabController deliveryTabController;
    @FXML private RoutePlanningTabController routeTabController;
    @FXML private DeliveryHistoryTabController historyTabController;
    
    // ADD THIS FIELD - it exists in the FXML
    @FXML private Label welcomeLabel;

    @FXML private void initialize() {
        System.out.println("Driver Dashboard Loaded — All Tabs Ready!");
        
        // Set welcome message with user's name
        if (welcomeLabel != null && CurrentUser.getInstance().getStaffName() != null) {
            welcomeLabel.setText("Welcome, " + CurrentUser.getInstance().getStaffName());
        }
    }

    @FXML private void handleRefreshAll() {
        dashboardTabController.refresh();
        deliveryTabController.refresh();
        routeTabController.refresh();
        historyTabController.refresh();
        
        // Force immediate refresh of delivery status
        if (deliveryTabController != null) {
            deliveryTabController.refresh();
        }
        
        showAlert("Success", "All data refreshed!", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleLogout() {
        try {
            // Now welcomeLabel should not be null
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Mill House System - Login");
            
            // Clear current user session
            CurrentUser.getInstance().clear();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }
}