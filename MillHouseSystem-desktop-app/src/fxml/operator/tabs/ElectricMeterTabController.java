package fxml.operator.tabs;

import fxml.DatabaseConnection;
import fxml.CurrentUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ElectricMeterTabController {

    @FXML private DatePicker readingDatePicker;
    @FXML private TextField meterReadingField, recordedByField, costPerUnitField;
    @FXML private Label currentCostLabel, monthlyUsageLabel, monthlyCostLabel, avgDailyUsageLabel;
    @FXML private TableView<MeterReading> readingsTable;
    
    private ObservableList<MeterReading> readingsList = FXCollections.observableArrayList();
    
    @FXML
    private void initialize() {
        System.out.println("Electric Meter Tab Initialized");
        
        // Set default values
        readingDatePicker.setValue(LocalDate.now());
        
        // Set recorded by to current user
        String currentUser = CurrentUser.getInstance().getStaffName();
        if (currentUser != null && !currentUser.isEmpty()) {
            recordedByField.setText(currentUser);
        } else {
            recordedByField.setText("Operator");
        }
        
        // Load current cost
        loadCurrentCost();
        
        // Setup table
        setupTable();
        
        // Load recent readings
        loadRecentReadings();
        
        // Calculate monthly summary
        calculateMonthlySummary();
    }
    
    private void setupTable() {
        // Clear existing columns (optional, since they're defined in FXML)
        // readingsTable.getColumns().clear();
        
        // Get the columns defined in FXML
        TableColumn<MeterReading, String> dateCol = (TableColumn<MeterReading, String>) readingsTable.getColumns().get(0);
        TableColumn<MeterReading, String> readingCol = (TableColumn<MeterReading, String>) readingsTable.getColumns().get(1);
        TableColumn<MeterReading, String> recordedByCol = (TableColumn<MeterReading, String>) readingsTable.getColumns().get(2);
        TableColumn<MeterReading, String> timeCol = (TableColumn<MeterReading, String>) readingsTable.getColumns().get(3);
        
        // Configure cell value factories
        dateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getReadingDate();
            return new javafx.beans.property.SimpleStringProperty(date != null ? date.toString() : "");
        });
        
        readingCol.setCellValueFactory(cell -> {
            double reading = cell.getValue().getMeterReading();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.2f kWh", reading));
        });
        
        recordedByCol.setCellValueFactory(cell -> {
            String recordedBy = cell.getValue().getRecordedBy();
            return new javafx.beans.property.SimpleStringProperty(recordedBy != null ? recordedBy : "");
        });
        
        timeCol.setCellValueFactory(cell -> {
            Timestamp recordedAt = cell.getValue().getRecordedAt();
            String time = "";
            if (recordedAt != null) {
                time = recordedAt.toLocalDateTime().toLocalTime().toString();
            }
            return new javafx.beans.property.SimpleStringProperty(time);
        });
        
        // Optional: Add cell factories for better formatting
        dateCol.setCellFactory(column -> new TableCell<MeterReading, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        
        readingCol.setCellFactory(column -> new TableCell<MeterReading, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER_RIGHT;");
                }
            }
        });
        
        // Set the items to the table
        readingsTable.setItems(readingsList);
    }
    
    private void loadCurrentCost() {
        String sql = "SELECT cost_per_unit FROM electricity_settings ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                double cost = rs.getDouble("cost_per_unit");
                currentCostLabel.setText(String.format("Current: ETB %.2f per kWh", cost));
                costPerUnitField.setText(String.format("%.2f", cost));
            } else {
                currentCostLabel.setText("Current: ETB 5.00 per kWh");
                costPerUnitField.setText("5.00");
            }
            
        } catch (SQLException e) {
            System.out.println("Error loading electricity cost: " + e.getMessage());
            currentCostLabel.setText("Current: ETB 5.00 per kWh");
            costPerUnitField.setText("5.00");
        }
    }
    
    @FXML
    private void handleSubmitReading() {
        LocalDate date = readingDatePicker.getValue();
        String readingText = meterReadingField.getText().trim();
        String recordedBy = recordedByField.getText().trim();
        
        // Validation
        if (date == null) {
            showAlert("Error", "Please select a date", Alert.AlertType.ERROR);
            return;
        }
        
        if (readingText.isEmpty()) {
            showAlert("Error", "Please enter meter reading", Alert.AlertType.ERROR);
            return;
        }
        
        if (recordedBy.isEmpty()) {
            showAlert("Error", "Please enter your name", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            double reading = Double.parseDouble(readingText);
            
            if (reading < 0) {
                showAlert("Error", "Reading cannot be negative", Alert.AlertType.ERROR);
                return;
            }
            
            // Check if reading already exists for this date
            String checkSql = "SELECT COUNT(*) FROM electric_meter_readings WHERE reading_date = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setDate(1, Date.valueOf(date));
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    // Update existing reading
                    String updateSql = "UPDATE electric_meter_readings SET meter_reading = ?, recorded_by = ? WHERE reading_date = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setDouble(1, reading);
                        updatePs.setString(2, recordedBy);
                        updatePs.setDate(3, Date.valueOf(date));
                        updatePs.executeUpdate();
                        showAlert("Success", "Updated existing reading for " + date, Alert.AlertType.INFORMATION);
                    }
                } else {
                    // Insert new reading
                    String insertSql = "INSERT INTO electric_meter_readings (reading_date, meter_reading, recorded_by) VALUES (?, ?, ?)";
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        insertPs.setDate(1, Date.valueOf(date));
                        insertPs.setDouble(2, reading);
                        insertPs.setString(3, recordedBy);
                        insertPs.executeUpdate();
                        showAlert("Success", "Meter reading submitted for " + date, Alert.AlertType.INFORMATION);
                    }
                }
            }
            
            // Refresh data
            loadRecentReadings();
            calculateMonthlySummary();
            clearForm();
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number for reading", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleUpdateCost() {
        String costText = costPerUnitField.getText().trim();
        
        try {
            double cost = Double.parseDouble(costText);
            
            if (cost <= 0) {
                showAlert("Error", "Cost must be greater than 0", Alert.AlertType.ERROR);
                return;
            }
            
            String sql = "INSERT INTO electricity_settings (cost_per_unit, updated_by) VALUES (?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, cost);
                ps.setString(2, CurrentUser.getInstance().getStaffName());
                ps.executeUpdate();
                
                currentCostLabel.setText(String.format("Current: ETB %.2f per kWh", cost));
                showAlert("Success", "Electricity cost updated to ETB " + cost + " per kWh", Alert.AlertType.INFORMATION);
                
                // Recalculate monthly cost
                calculateMonthlySummary();
            }
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number for cost", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            showAlert("Error", "Failed to update cost: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleRefreshReadings() {
        loadRecentReadings();
        calculateMonthlySummary();
        showAlert("Refreshed", "Readings and summary updated", Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void handleDeleteReading() {
        MeterReading selected = readingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a reading to delete", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Meter Reading");
        confirm.setContentText("Are you sure you want to delete reading from " + selected.getReadingDate() + "?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "DELETE FROM electric_meter_readings WHERE id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, selected.getId());
                    ps.executeUpdate();
                    
                    readingsList.remove(selected);
                    calculateMonthlySummary();
                    showAlert("Success", "Reading deleted", Alert.AlertType.INFORMATION);
                    
                } catch (SQLException e) {
                    showAlert("Error", "Failed to delete reading: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    @FXML
    private void handleClearForm() {
        readingDatePicker.setValue(LocalDate.now());
        meterReadingField.clear();
        // Keep recorded by field as is
    }
    
    private void loadRecentReadings() {
        readingsList.clear();
        
        String sql = "SELECT id, reading_date, meter_reading, recorded_by, recorded_at " +
                    "FROM electric_meter_readings ORDER BY reading_date DESC LIMIT 30";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                MeterReading reading = new MeterReading(
                    rs.getInt("id"),
                    rs.getDate("reading_date").toLocalDate(),
                    rs.getDouble("meter_reading"),
                    rs.getString("recorded_by"),
                    rs.getTimestamp("recorded_at")
                );
                readingsList.add(reading);
            }
            
        } catch (SQLException e) {
            System.out.println("Error loading readings: " + e.getMessage());
        }
    }
    
    private void calculateMonthlySummary() {
        // Get current month
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        System.out.println("DEBUG: Calculating monthly summary for " + firstDayOfMonth + " to " + lastDayOfMonth);
        
        // Get all readings for this month in chronological order
        String sql = "SELECT reading_date, meter_reading " +
                    "FROM electric_meter_readings " +
                    "WHERE reading_date BETWEEN ? AND ? " +
                    "ORDER BY reading_date ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(firstDayOfMonth));
            ps.setDate(2, Date.valueOf(lastDayOfMonth));
            
            ResultSet rs = ps.executeQuery();
            
            List<MeterReadingData> readings = new ArrayList<>();
            while (rs.next()) {
                readings.add(new MeterReadingData(
                    rs.getDate("reading_date").toLocalDate(),
                    rs.getDouble("meter_reading")
                ));
            }
            
            if (readings.size() >= 2) {
                // Calculate total monthly usage by summing up daily differences
                double totalUsage = 0;
                double previousReading = readings.get(0).getReading();
                LocalDate previousDate = readings.get(0).getDate();
                
                for (int i = 1; i < readings.size(); i++) {
                    double currentReading = readings.get(i).getReading();
                    LocalDate currentDate = readings.get(i).getDate();
                    
                    // Calculate days between readings
                    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(previousDate, currentDate);
                    if (daysBetween <= 0) daysBetween = 1; // Same day readings
                    
                    double dailyIncrement = (currentReading - previousReading) / daysBetween;
                    
                    // Only add positive increments (meter readings should increase)
                    if (dailyIncrement > 0) {
                        totalUsage += dailyIncrement * daysBetween;
                    }
                    
                    previousReading = currentReading;
                    previousDate = currentDate;
                }
                
                // Alternative simpler approach: total usage = last reading - first reading
                double simpleTotalUsage = readings.get(readings.size() - 1).getReading() - readings.get(0).getReading();
                
                // Use the simpler approach (more reliable for electricity meters)
                totalUsage = Math.max(simpleTotalUsage, totalUsage);
                
                // Get number of days with readings
                int daysWithReadings = readings.size();
                int totalDaysInMonth = now.lengthOfMonth();
                
                // Calculate average daily usage
                double avgDailyUsage = totalUsage / daysWithReadings;
                
                // Get current cost
                double costPerUnit = getCurrentCost();
                double monthlyCost = totalUsage * costPerUnit;
                
                // Format and display
                monthlyUsageLabel.setText(String.format("%.2f kWh", totalUsage));
                monthlyCostLabel.setText(String.format("ETB %.2f", monthlyCost));
                avgDailyUsageLabel.setText(String.format("%.2f kWh/day", avgDailyUsage));
                
                System.out.println("DEBUG: Monthly Usage = " + totalUsage + " kWh");
                System.out.println("DEBUG: Monthly Cost = ETB " + monthlyCost);
                System.out.println("DEBUG: Avg Daily = " + avgDailyUsage + " kWh/day");
                
            } else if (readings.size() == 1) {
                // Only one reading this month
                monthlyUsageLabel.setText("Insufficient data");
                monthlyCostLabel.setText("ETB 0.00");
                avgDailyUsageLabel.setText("Need more readings");
            } else {
                // No readings this month
                monthlyUsageLabel.setText("No data");
                monthlyCostLabel.setText("ETB 0.00");
                avgDailyUsageLabel.setText("0.00 kWh/day");
            }
            
        } catch (SQLException e) {
            System.out.println("Error calculating monthly summary: " + e.getMessage());
            e.printStackTrace();
            
            // Show sample data for debugging
            showSampleSummary();
        }
    }
 // Helper class for meter reading data
    private static class MeterReadingData {
        private LocalDate date;
        private double reading;
        
        public MeterReadingData(LocalDate date, double reading) {
            this.date = date;
            this.reading = reading;
        }
        
        public LocalDate getDate() { return date; }
        public double getReading() { return reading; }
    }

    private void showSampleSummary() {
        // For testing/debugging purposes
        monthlyUsageLabel.setText("156.75 kWh");
        monthlyCostLabel.setText("ETB 783.75");
        avgDailyUsageLabel.setText("5.23 kWh/day");
    }
    
    private double getCurrentCost() {
        String sql = "SELECT cost_per_unit FROM electricity_settings ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getDouble("cost_per_unit");
            }
            
        } catch (SQLException e) {
            System.out.println("Error getting current cost: " + e.getMessage());
        }
        
        return 5.00; // Default cost
    }
    
    private void clearForm() {
        readingDatePicker.setValue(LocalDate.now());
        meterReadingField.clear();
        // Keep recorded by field
    }
    
    public void refresh() {
        loadRecentReadings();
        loadCurrentCost();
        calculateMonthlySummary();
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
    
    // Data class for meter readings
    public static class MeterReading {
        private int id;
        private LocalDate readingDate;
        private double meterReading;
        private String recordedBy;
        private Timestamp recordedAt;
        
        public MeterReading(int id, LocalDate readingDate, double meterReading, String recordedBy, Timestamp recordedAt) {
            this.id = id;
            this.readingDate = readingDate;
            this.meterReading = meterReading;
            this.recordedBy = recordedBy;
            this.recordedAt = recordedAt;
        }
        
        // Getters for table display
        public LocalDate getReadingDate() { return readingDate; }
        public double getMeterReading() { return meterReading; }
        public String getRecordedBy() { return recordedBy; }
        public Timestamp getRecordedAt() { return recordedAt; }
        public int getId() { return id; }
        
        // String representations for table
        public String getReadingDateString() { return readingDate.toString(); }
        public String getMeterReadingString() { return String.format("%.2f kWh", meterReading); }
        public String getRecordedAtString() { 
            return recordedAt != null ? recordedAt.toLocalDateTime().toString().substring(11, 16) : ""; 
        }
    }
}