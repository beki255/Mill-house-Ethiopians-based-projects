package fxml;

import fxml.admin.tabs.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AdminMainController {

    @FXML private DashboardTabController dashboardTabController;
    @FXML private StaffManagementTabController staffTabController;
    @FXML private FinanceTabController financeTabController;
    @FXML private SettingsTabController settingsTabController;
    @FXML private InventoryManagementTabController inventoryTabController;
    @FXML private SalaryProcessingTabController salaryTabController;
    
    // Sidebar labels
    @FXML private Label sidebarActiveStaff;
    @FXML private Label sidebarTodayOrders;
    @FXML private Label sidebarLowStock;
    @FXML private Label welcomeAdminLabel;

    @FXML 
    private void initialize() {
        System.out.println("Admin Dashboard - All 6 Tabs Loaded Successfully!");
        System.out.println("Inventory Management Tab Ready for grain management!");
        
        // Update sidebar stats
        updateSidebarStats();
        
        // Set welcome message
        if (welcomeAdminLabel != null && CurrentUser.getInstance().getStaffName() != null) {
            welcomeAdminLabel.setText("Welcome, " + CurrentUser.getInstance().getStaffName());
        }
    }

    private void updateSidebarStats() {
        // Update active staff count
        updateActiveStaffCount();
        
        // Update today's orders count
        updateTodayOrdersCount();
        
        // Update low stock items count
        updateLowStockCount();
    }
    
    private void updateActiveStaffCount() {
        try {
            String sql = "SELECT COUNT(*) as count FROM staff_members WHERE status = 'Active'";
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                sidebarActiveStaff.setText(String.valueOf(rs.getInt("count")));
            }
        } catch (Exception e) {
            sidebarActiveStaff.setText("0");
        }
    }
    
    private void updateTodayOrdersCount() {
        try {
            String sql = "SELECT COUNT(*) as count FROM orders WHERE DATE(created_date) = CURDATE()";
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                sidebarTodayOrders.setText(String.valueOf(rs.getInt("count")));
            }
        } catch (Exception e) {
            sidebarTodayOrders.setText("0");
        }
    }
    
    private void updateLowStockCount() {
        try {
            String sql = "SELECT COUNT(*) as count FROM inventory WHERE current_stock_kg <= 200";
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                sidebarLowStock.setText(String.valueOf(rs.getInt("count")));
            }
        } catch (Exception e) {
            sidebarLowStock.setText("0");
        }
    }

    @FXML 
    private void handleRefreshAll() {
        if (dashboardTabController != null) dashboardTabController.refresh();
        if (staffTabController != null) staffTabController.refresh();
        if (financeTabController != null) financeTabController.refresh();
        if (settingsTabController != null) settingsTabController.refresh();
        if (inventoryTabController != null) inventoryTabController.refresh();
        if (salaryTabController != null) salaryTabController.refresh();
        
        // Update sidebar stats when refreshing
        updateSidebarStats();
        
        showAlert("Success", "All tabs refreshed and stats updated!", Alert.AlertType.INFORMATION);
    }

    @FXML 
    private void handleLogout() {
        System.out.println("=== ADMIN LOGOUT CLICKED ===");
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Logout");
        confirm.setHeaderText("Logout from Admin Dashboard");
        confirm.setContentText("Are you sure you want to logout?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("User confirmed logout");
                
                // Clear current user session
                CurrentUser.getInstance().clear();
                System.out.println("CurrentUser cleared!");
                
                try {
                    // Get the current stage
                    Stage stage = (Stage) welcomeAdminLabel.getScene().getWindow();
                    
                    // Close the current dashboard
                    stage.close();
                    
                    // Open login screen
                    openLoginScreen();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to logout", Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void openLoginScreen() {
        try {
            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            
            Stage loginStage = new Stage();
            loginStage.setTitle("Mill House Management System - Login");
            loginStage.setScene(new Scene(root));
            loginStage.setMaximized(true);
            loginStage.show();
            
            System.out.println("Login screen opened successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open login screen", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }
}