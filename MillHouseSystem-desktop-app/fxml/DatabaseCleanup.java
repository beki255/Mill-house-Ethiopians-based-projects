// DatabaseCleanup.java
package fxml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseCleanup {
    
    public static void cleanupSystem() {
        System.out.println("=== STARTING SYSTEM CLEANUP ===");
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Delete default drivers
            deleteDefaultDrivers(conn);
            
            // 2. Clean up default orders
            cleanupDefaultOrders(conn);
            
            // 3. Reset orphaned assignments
            resetOrphanedAssignments(conn);
            
            System.out.println("=== SYSTEM CLEANUP COMPLETED ===");
            
        } catch (SQLException e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }
    
    private static void deleteDefaultDrivers(Connection conn) throws SQLException {
        String[] drivers = {"Driver 1", "Driver 2", "Driver 3"};
        
        for (String driver : drivers) {
            // Get driver ID first
            String getIdSql = "SELECT id FROM drivers WHERE name = ?";
            int driverId = -1;
            try (PreparedStatement ps = conn.prepareStatement(getIdSql)) {
                ps.setString(1, driver);
                var rs = ps.executeQuery();
                if (rs.next()) {
                    driverId = rs.getInt("id");
                }
            }
            
            if (driverId != -1) {
                // Update orders assigned to this driver
                String updateOrders = "UPDATE orders SET assigned_driver = NULL, status = 'Ready for Assignment' " +
                                    "WHERE assigned_driver = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateOrders)) {
                    ps.setString(1, driver);
                    int updated = ps.executeUpdate();
                    System.out.println("Updated " + updated + " orders from " + driver);
                }
                
                // Delete from drivers table
                String deleteDriver = "DELETE FROM drivers WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteDriver)) {
                    ps.setInt(1, driverId);
                    ps.executeUpdate();
                    System.out.println("Deleted driver: " + driver);
                }
            }
            
            // Delete from staff_members and users
            String[] tables = {"staff_members", "users"};
            for (String table : tables) {
                String deleteSql = "DELETE FROM " + table + " WHERE name = ? AND role = 'DRIVER'";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setString(1, driver);
                    ps.executeUpdate();
                }
            }
        }
    }
    
    private static void cleanupDefaultOrders(Connection conn) throws SQLException {
        // Safer cleanup without customer_name reference
        String deleteSql = "DELETE FROM orders WHERE " +
                          "created_date < DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                          "AND (assigned_driver IN ('Driver 1', 'Driver 2', 'Driver 3') " +
                          "OR status IN ('Test', 'Demo') " +
                          "OR raw_type LIKE 'test%' OR raw_type LIKE 'demo%')";
        
        try (Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(deleteSql);
            System.out.println("Deleted " + deleted + " default/test orders");
        } catch (SQLException e) {
            // Ignore errors - table might not have these columns
            System.out.println("Cleanup skipped: " + e.getMessage());
        }
    }

    private static void resetOrphanedAssignments(Connection conn) throws SQLException {
        try {
            String resetSql = "UPDATE orders o " +
                             "LEFT JOIN drivers d ON o.assigned_driver = d.name " +
                             "SET o.assigned_driver = NULL, o.status = 'Ready' " +
                             "WHERE o.assigned_driver IS NOT NULL " +
                             "AND (d.id IS NULL OR o.assigned_driver IN ('Driver 1', 'Driver 2', 'Driver 3'))";
            
            try (Statement stmt = conn.createStatement()) {
                int reset = stmt.executeUpdate(resetSql);
                System.out.println("Reset " + reset + " orphaned order assignments");
            }
        } catch (SQLException e) {
            // Ignore errors
            System.out.println("Reset assignments skipped: " + e.getMessage());
        }
    }
    // Call this from Main.java
    public static void main(String[] args) {
        DatabaseConnection.testConnection();
        cleanupSystem();
    }
}