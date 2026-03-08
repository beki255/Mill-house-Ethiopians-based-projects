package fxml.driver.tabs;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import fxml.CurrentUser;
import fxml.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;

public class DashboardTabController {

    @FXML private Label todayDeliveriesLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label onRouteCountLabel;  // This shows On-Time Rate %
    @FXML private Label deliveredTodayLabel;
    @FXML private Label welcomeLabel;
    @FXML private TextField vehicleStatusField;

    @FXML 
    private void initialize() {
        loadDriverDashboard();
    }

    public void refresh() {
        loadDriverDashboard();
    }

    private void loadDriverDashboard() {
        try {
            String driverName = CurrentUser.getInstance().getStaffName();
            
            if (driverName == null || driverName.trim().isEmpty()) {
                System.err.println("ERROR: No driver logged in!");
                setDefaultDashboard("Driver");
                return;
            }
            
            System.out.println("Loading dashboard for driver: " + driverName);
            
            // Set welcome message
            if (welcomeLabel != null) {
                welcomeLabel.setText("Welcome, " + driverName + "!");
            }
            
            LocalDate today = LocalDate.now();
            java.sql.Date sqlToday = java.sql.Date.valueOf(today);
            
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null) {
                    System.err.println("ERROR: No database connection!");
                    setDefaultDashboard(driverName);
                    return;
                }
                
                // ============================================
                // CARD 1: PENDING DELIVERIES (For this driver TODAY)
                // ============================================
                String pendingSql = "SELECT COUNT(*) as count " +
                                   "FROM orders " +
                                   "WHERE (assigned_driver = ? OR assigned_driver LIKE ?) " +
                                   "AND DATE(created_date) = ? " +
                                   "AND status IN ('Assigned to Driver', 'Ready for Delivery')";
                
                int pendingCount = 0;
                try (PreparedStatement ps = conn.prepareStatement(pendingSql)) {
                    ps.setString(1, driverName);
                    ps.setString(2, "%" + driverName + "%");
                    ps.setDate(3, sqlToday);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pendingCount = rs.getInt("count");
                    }
                }
                
                // ============================================
                // CARD 2: TODAY'S COMPLETED (Delivered by this driver TODAY)
                // ============================================
                String deliveredSql = "SELECT COUNT(*) as count " +
                                     "FROM orders " +
                                     "WHERE (assigned_driver = ? OR assigned_driver LIKE ?) " +
                                     "AND DATE(delivery_completed_date) = ? " +
                                     "AND status = 'Delivered'";
                
                int deliveredToday = 0;
                try (PreparedStatement ps = conn.prepareStatement(deliveredSql)) {
                    ps.setString(1, driverName);
                    ps.setString(2, "%" + driverName + "%");
                    ps.setDate(3, sqlToday);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        deliveredToday = rs.getInt("count");
                    }
                }
                
                // ============================================
                // CARD 3: ON-TIME RATE % (This driver's performance - Last 7 days)
                // ============================================
                String onTimeSql = "SELECT " +
                                  "COUNT(*) as total_delivered, " +
                                  "SUM(CASE WHEN delivery_completed_date <= estimated_delivery_time " +
                                  "THEN 1 ELSE 0 END) as on_time_count " +
                                  "FROM orders " +
                                  "WHERE (assigned_driver = ? OR assigned_driver LIKE ?) " +
                                  "AND status = 'Delivered' " +
                                  "AND created_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
                
                int onTimeRate = 100; // Default to 100% if no data
                try (PreparedStatement ps = conn.prepareStatement(onTimeSql)) {
                    ps.setString(1, driverName);
                    ps.setString(2, "%" + driverName + "%");
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int totalDelivered = rs.getInt("total_delivered");
                        int onTimeCount = rs.getInt("on_time_count");
                        
                        if (totalDelivered > 0) {
                            onTimeRate = (int) Math.round((onTimeCount * 100.0) / totalDelivered);
                        }
                    }
                }
                
                // ============================================
                // UPDATE THE 3 CARDS
                // ============================================
                if (pendingCountLabel != null) {
                    pendingCountLabel.setText(String.valueOf(pendingCount));
                }
                
                if (deliveredTodayLabel != null) {
                    deliveredTodayLabel.setText(String.valueOf(deliveredToday));
                }
                
                if (onRouteCountLabel != null) {
                    // This shows On-Time Rate percentage
                    onRouteCountLabel.setText(onTimeRate + "%");
                }
                
                // Optional: Update the "Today's Deliveries" label (not in card)
                if (todayDeliveriesLabel != null) {
                    // Get total deliveries assigned today
                    String todayTotalSql = "SELECT COUNT(*) as count " +
                                          "FROM orders " +
                                          "WHERE (assigned_driver = ? OR assigned_driver LIKE ?) " +
                                          "AND DATE(created_date) = ? " +
                                          "AND status NOT IN ('Cancelled', 'Rejected')";
                    
                    try (PreparedStatement ps = conn.prepareStatement(todayTotalSql)) {
                        ps.setString(1, driverName);
                        ps.setString(2, "%" + driverName + "%");
                        ps.setDate(3, sqlToday);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            todayDeliveriesLabel.setText("Today's Deliveries: " + rs.getInt("count"));
                        }
                    }
                }
                
                System.out.println("Driver Dashboard for " + driverName + ":");
                System.out.println("  Pending: " + pendingCount);
                System.out.println("  Delivered Today: " + deliveredToday);
                System.out.println("  On-Time Rate: " + onTimeRate + "%");
                
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                setDefaultDashboard(driverName);
            }
            
        } catch (Exception e) {
            System.err.println("Dashboard error: " + e.getMessage());
            setDefaultDashboard("Driver");
        }
    }
    
    private void setDefaultDashboard(String driverName) {
        // Default values when database fails
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + driverName + "!");
        }
        if (todayDeliveriesLabel != null) {
            todayDeliveriesLabel.setText("Today's Deliveries: 0");
        }
        if (pendingCountLabel != null) {
            pendingCountLabel.setText("0");
        }
        if (deliveredTodayLabel != null) {
            deliveredTodayLabel.setText("0");
        }
        if (onRouteCountLabel != null) {
            onRouteCountLabel.setText("100%"); // Default to perfect score
        }
    }
    
    @FXML 
    private void handleUpdateVehicleStatus() {
        String status = vehicleStatusField.getText().trim();
        if (!status.isEmpty()) {
            String driverName = CurrentUser.getInstance().getStaffName();
            if (driverName != null) {
                updateDriverVehicleStatus(driverName, status);
                showAlert("Success", "Vehicle status updated to: " + status, Alert.AlertType.INFORMATION);
                vehicleStatusField.clear();
            } else {
                showAlert("Error", "Driver not identified", Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Please enter vehicle status", Alert.AlertType.ERROR);
        }
    }
    
    private void updateDriverVehicleStatus(String driverName, String status) {
        String sql = "UPDATE drivers SET vehicle_status = ? WHERE name = ? OR name LIKE ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, driverName);
            ps.setString(3, "%" + driverName + "%");
            int updated = ps.executeUpdate();
            System.out.println("Vehicle status updated for " + driverName + ": " + updated + " rows");
        } catch (SQLException e) {
            System.err.println("Error updating vehicle status: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
}