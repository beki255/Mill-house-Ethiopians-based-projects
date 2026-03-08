package fxml;

import fxml.operator.tabs.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class OperatorMainController {

    @FXML private BorderPane mainPane;
    @FXML private TabPane mainTabPane;
    @FXML private TransactionTabController transactionTabController;
    @FXML private InventoryTabController inventoryTabController;
    @FXML private OrdersTabController ordersTabController;
    @FXML private CustomersTabController customersTabController;
    @FXML private MonitoringTabController monitoringTabController;
    @FXML private DeliveryManagementTabController deliveryTabController;
    @FXML private ElectricMeterTabController electricMeterTabController;

    @FXML 
    private void initialize() {
        System.out.println("Operator Dashboard - All 6 Tabs Loaded!");
        
        
        // Add listener to handle tab selection
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                System.out.println("Tab selected: " + newTab.getText());
                
                // Refresh the selected tab
                switch (newTab.getText()) {
                    case "Delivery Management":
                        if (deliveryTabController != null) {
                            deliveryTabController.refresh();
                        }
                        break;
                    case "Orders":
                        if (ordersTabController != null) {
                            ordersTabController.refresh();
                        }
                        break;
                    case "Customers":
                        if (customersTabController != null) {
                            customersTabController.refresh(); // This line should work now
                        }
                        break;
                    case "Transactions":
                        if (transactionTabController != null) {
                            transactionTabController.refresh();
                        }
                        break;
                }
            }
        });
    }

    @FXML 
    private void handleRefreshAll() {

    	 // Add to handleRefreshAll() method
    	if (electricMeterTabController != null) electricMeterTabController.refresh();
        if (transactionTabController != null) transactionTabController.refresh();
        if (inventoryTabController != null) inventoryTabController.refresh();
        if (ordersTabController != null) ordersTabController.refresh();
        if (customersTabController != null) customersTabController.refresh(); // Fixed
        if (monitoringTabController != null) monitoringTabController.refresh();
        if (deliveryTabController != null) deliveryTabController.refresh();
        showAlert("Success", "All tabs refreshed!", Alert.AlertType.INFORMATION);
    }

    // ... rest of the methods remain the same ...


    // Add a public method to refresh specific tabs
    public void refreshDeliveryTab() {
        if (deliveryTabController != null) {
            deliveryTabController.refresh();
        }
    }
    
    public void refreshOrdersTab() {
        if (ordersTabController != null) {
            ordersTabController.refresh();
        }
    }

    @FXML 
    private void handleLogout() {
        try {
            // Now we can get the stage from mainPane
            Stage stage = (Stage) mainPane.getScene().getWindow();
            
            // Clear current user session
            CurrentUser.getInstance().clear();
            
            // Close this window
            stage.close();
            
            System.out.println("Operator logged out successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }
}