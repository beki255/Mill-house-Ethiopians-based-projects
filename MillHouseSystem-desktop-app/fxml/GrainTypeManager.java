// Create a new utility class: fxml/GrainTypeManager.java
package fxml;

import java.sql.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GrainTypeManager {
    
    private static ObservableList<String> grainTypes = FXCollections.observableArrayList();
    
    public static ObservableList<String> getGrainTypes() {
        if (grainTypes.isEmpty()) {
            loadGrainTypesFromDatabase();
        }
        return grainTypes;
    }
    
    public static void refreshGrainTypes() {
        loadGrainTypesFromDatabase();
    }
    
    private static void loadGrainTypesFromDatabase() {
        grainTypes.clear();
        
        String sql = "SELECT grain_type FROM inventory ORDER BY grain_type";
        
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            
            while (rs.next()) {
                String grainType = rs.getString("grain_type");
                if (grainType != null && !grainType.trim().isEmpty()) {
                    grainTypes.add(grainType);
                }
            }
            
            // If no grain types in inventory, load from settings
            if (grainTypes.isEmpty()) {
                loadGrainTypesFromSettings();
            }
            
            System.out.println("Loaded " + grainTypes.size() + " grain types from database");
            
        } catch (SQLException e) {
            System.err.println("Error loading grain types: " + e.getMessage());
            loadDefaultGrainTypes();
        }
    }
    
    private static void loadGrainTypesFromSettings() {
        String sql = "SELECT grain_type FROM settings ORDER BY grain_type";
        
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            
            while (rs.next()) {
                String grainType = rs.getString("grain_type");
                if (grainType != null && !grainType.trim().isEmpty()) {
                    grainTypes.add(grainType);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading grain types from settings: " + e.getMessage());
        }
    }
    
    private static void loadDefaultGrainTypes() {
        grainTypes.addAll("Wheat", "Teff", "Corn", "Barley");
        System.out.println("Loaded default grain types");
    }
    
    public static boolean isValidGrainType(String grainType) {
        return grainTypes.contains(grainType);
    }
    
    public static void addGrainType(String newGrainType) {
        if (newGrainType != null && !newGrainType.trim().isEmpty() && !grainTypes.contains(newGrainType)) {
            grainTypes.add(newGrainType);
            FXCollections.sort(grainTypes);
        }
    }
}