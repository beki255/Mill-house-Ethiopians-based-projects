package fxml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    private static Stage primaryStage;
    private static Stage currentStage;  // Track current stage
    @Override
    public void start(Stage stage) throws Exception {
    	primaryStage = stage;
        currentStage = stage; 
        // Update database first
        DatabaseConnection.updateDatabaseSchema();
        DatabaseConnection.updateOrderStatusFlow();
        DatabaseConnection.repairDeliveryLogsTable();
        DatabaseConnection.updateOrderStatusFlow();
        DatabaseConnection.ensureDeliveryLogsTable();
        // Test connection
        DatabaseConnection.testConnection();
        GrainTypeManager.refreshGrainTypes();
        
        // Cleanup
        DatabaseCleanup.cleanupSystem();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            
            // Get controller and debug
            LoginMainController controller = loader.getController();
            if (controller != null) {
                System.out.println("Login controller loaded successfully");
                // Call debug method
                controller.debugFXMLInjection();
            } else {
                System.err.println("ERROR: Login controller is null!");
            }
            
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Mill House Management System - Ethiopia");
            primaryStage.setMaximized(true);
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("ERROR loading FXML: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
   
    private void checkInitialSetup() {
        // Check if admin user exists
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM users WHERE role = 'ADMIN'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next() && rs.getInt("count") == 0) {
                    System.out.println("⚠️ No admin user found!");
                    System.out.println("Please create an admin account through the Staff Management tab.");
                    System.out.println("Or run SetupInitialAdmin.java to create initial admin.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking initial setup: " + e.getMessage());
        }
    }
    private void cleanupDefaultDrivers() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String[] driverNames = {"Driver 1", "Driver 2", "Driver 3"};
            
            for (String name : driverNames) {
                // Delete from drivers table
                String deleteDrivers = "DELETE FROM drivers WHERE name = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteDrivers)) {
                    ps.setString(1, name);
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) {
                        System.out.println("Deleted driver: " + name);
                    }
                }
                
                // Delete from staff_members
                String deleteStaff = "DELETE FROM staff_members WHERE name = ? AND role = 'DRIVER'";
                try (PreparedStatement ps = conn.prepareStatement(deleteStaff)) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
                
                // Delete from users
                String deleteUsers = "DELETE FROM users WHERE name = ? AND role = 'DRIVER'";
                try (PreparedStatement ps = conn.prepareStatement(deleteUsers)) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error cleaning up default drivers: " + e.getMessage());
        }
    }
 // Add this method to Main.java or create a separate cleanup utility:
    private static void simplifiedSchemaUpdate() {
        String[] simpleUpdates = {
            // Create delivery_logs table with minimal structure
            "CREATE TABLE IF NOT EXISTS delivery_logs (" +
            "  id INT PRIMARY KEY AUTO_INCREMENT," +
            "  order_id INT NOT NULL," +
            "  driver_name VARCHAR(100) NOT NULL," +
            "  assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  delivered_date TIMESTAMP NULL," +
            "  status VARCHAR(50) DEFAULT 'Assigned'" +
            ")",
            
            // Ensure customer_name column exists in orders
            "ALTER TABLE orders ADD COLUMN customer_name VARCHAR(100) AFTER customer_id",
            
            // Update orders with customer names
            "UPDATE orders o JOIN customers c ON o.customer_id = c.id SET o.customer_name = c.name WHERE o.customer_name IS NULL OR o.customer_name = ''"
        };
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String sql : simpleUpdates) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                    System.out.println("✅ Executed: " + sql.substring(0, Math.min(50, sql.length())) + "...");
                } catch (SQLException e) {
                    if (e.getMessage().contains("Duplicate column") || 
                        e.getMessage().contains("already exists")) {
                        System.out.println("✅ Already exists");
                    } else {
                        System.err.println("⚠️ Update failed: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Simplified schema update failed: " + e.getMessage());
        }
    }
    private void cleanupDefaultOrders() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Delete orders that are test/default data
            String deleteSql = "DELETE FROM orders WHERE " +
                              "(customer_name LIKE '%Test%' OR " +
                              "customer_name LIKE '%Demo%' OR " +
                              "customer_name LIKE '%Example%' OR " +
                              "assigned_driver IN ('Driver 1', 'Driver 2', 'Driver 3')) " +
                              "AND status = 'Prepared for Delivery'";
            
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(deleteSql);
                System.out.println("Deleted " + deleted + " default/prepared orders");
            }
            
            // Also clean up any orders assigned to deleted drivers
            String cleanupSql = "UPDATE orders SET assigned_driver = NULL, status = 'Ready for Assignment' " +
                               "WHERE assigned_driver IN ('Driver 1', 'Driver 2', 'Driver 3')";
            try (Statement stmt = conn.createStatement()) {
                int updated = stmt.executeUpdate(cleanupSql);
                System.out.println("Updated " + updated + " orders from deleted drivers");
            }
            
        } catch (SQLException e) {
            System.err.println("Error cleaning up default orders: " + e.getMessage());
        }
    }
    public static void openDashboard(Parent root, String title) {
        // Close current stage if exists
        if (currentStage != null && currentStage.isShowing()) {
            currentStage.close();
        }
        Stage newStage = new Stage();
        currentStage = newStage;
        newStage.setScene(new Scene(root));
        newStage.setTitle(title);
        newStage.setMaximized(true);
        newStage.initStyle(StageStyle.DECORATED);
        newStage.setAlwaysOnTop(true);
        newStage.setAlwaysOnTop(false);
        newStage.show();
    }
    public static void showLoginScreen() {
        try {
            // Close current stage
            if (currentStage != null && currentStage.isShowing()) {
                currentStage.close();
            }
            
            // Load login
            Parent root = FXMLLoader.load(Main.class.getResource("/fxml/Login.fxml"));
            Stage loginStage = new Stage();
            currentStage = loginStage;
            loginStage.setTitle("Mill House Management System - Login");
            loginStage.setScene(new Scene(root));
            loginStage.setMaximized(true);
            loginStage.show();
            
        } catch (Exception e) { 
            e.printStackTrace();
        }
    }
    public static void showOperatorDashboard() {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource("/fxml/OperatorDashboard.fxml"));
            openDashboard(root, "Mill House - Operator Dashboard");
        } catch (Exception e) { e.printStackTrace(); }
    }
    public static void showAdminDashboard() {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource("/fxml/AdminDashboard.fxml"));
            openDashboard(root, "Mill House - Admin Dashboard");
        } catch (Exception e) { e.printStackTrace(); }
    }
    @Override
    public void stop() {
        DatabaseConnection.closeConnection();
        System.out.println("Application closed — Database connection terminated.");
    }
    public static void main(String[] args) {
        launch(args);
    }
}
