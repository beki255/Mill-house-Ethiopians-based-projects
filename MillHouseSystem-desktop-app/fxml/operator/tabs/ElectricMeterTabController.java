package fxml.operator.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

import fxml.DatabaseConnection;
import fxml.CurrentUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

public class ElectricMeterTabController {

    @FXML private DatePicker readingDatePicker;
    @FXML private TextField meterReadingField;
    @FXML private Label todayStatusLabel;
    @FXML private TableView<MeterReading> readingsTable;
    
    private ObservableList<MeterReading> readings = FXCollections.observableArrayList();

    @FXML 
    public void initialize() {
        // Set default to today
        readingDatePicker.setValue(LocalDate.now());
        
        // Setup table
        setupTable();
        
        // Check today's status
        checkTodayStatus();
        
        // Load recent readings
        loadRecentReadings();
    }
    
    private void setupTable() {
        TableColumn<MeterReading, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("readingDate"));
        
        TableColumn<MeterReading, String> readingCol = new TableColumn<>("Meter Reading");
        readingCol.setCellValueFactory(new PropertyValueFactory<>("meterReading"));
        
        TableColumn<MeterReading, String> recordedByCol = new TableColumn<>("Recorded By");
        recordedByCol.setCellValueFactory(new PropertyValueFactory<>("recordedBy"));
        
        TableColumn<MeterReading, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("recordedTime"));
        
        readingsTable.getColumns().setAll(dateCol, readingCol, recordedByCol, timeCol);
        readingsTable.setItems(readings);
    }
    
    private void checkTodayStatus() {
        LocalDate today = LocalDate.now();
        String sql = "SELECT meter_reading FROM electric_meter_readings WHERE reading_date = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(today));
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                double reading = rs.getDouble("meter_reading");
                todayStatusLabel.setText("✅ Today's reading recorded: " + reading + " kWh");
                todayStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            } else {
                todayStatusLabel.setText("❌ No reading recorded for today");
                todayStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
            
        } catch (SQLException e) {
            todayStatusLabel.setText("Error checking status");
        }
    }
    
    private void loadRecentReadings() {
        readings.clear();
        String sql = "SELECT * FROM electric_meter_readings ORDER BY reading_date DESC LIMIT 10";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                MeterReading reading = new MeterReading(
                    rs.getDate("reading_date").toLocalDate(),
                    rs.getDouble("meter_reading"),
                    rs.getString("recorded_by"),
                    rs.getTimestamp("recorded_at")
                );
                readings.add(reading);
            }
            
        } catch (SQLException e) {
            System.out.println("Error loading readings: " + e.getMessage());
        }
    }
    
    @FXML
    public void handleSubmitReading() {
        // Validate
        if (readingDatePicker.getValue() == null) {
            showAlert("Error", "Please select date", Alert.AlertType.ERROR);
            return;
        }
        
        String readingText = meterReadingField.getText().trim();
        if (readingText.isEmpty()) {
            showAlert("Error", "Please enter meter reading", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            double reading = Double.parseDouble(readingText);
            if (reading < 0) {
                showAlert("Error", "Meter reading cannot be negative", Alert.AlertType.ERROR);
                return;
            }
            
            // Check if reading already exists for this date
            LocalDate selectedDate = readingDatePicker.getValue();
            if (isReadingExists(selectedDate)) {
                showAlert("Error", "Meter reading already recorded for " + selectedDate, Alert.AlertType.ERROR);
                return;
            }
            
            // Save reading
            saveReading(selectedDate, reading);
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number", Alert.AlertType.ERROR);
        }
    }
    
    private boolean isReadingExists(LocalDate date) {
        String sql = "SELECT COUNT(*) as count FROM electric_meter_readings WHERE reading_date = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.out.println("Error checking reading: " + e.getMessage());
        }
        
        return false;
    }
    
    private void saveReading(LocalDate date, double reading) {
        String sql = "INSERT INTO electric_meter_readings (reading_date, meter_reading, recorded_by) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(date));
            ps.setDouble(2, reading);
            ps.setString(3, CurrentUser.getInstance().getUsername());
            
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                showAlert("Success", "Meter reading recorded successfully!", Alert.AlertType.INFORMATION);
                meterReadingField.clear();
                checkTodayStatus();
                loadRecentReadings();
            }
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to save reading: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
    
    public void refresh() {
        checkTodayStatus();
        loadRecentReadings();
    }
    
    // Data class for table
    public static class MeterReading {
        private LocalDate readingDate;
        private double meterReading;
        private String recordedBy;
        private Timestamp recordedAt;
        
        public MeterReading(LocalDate readingDate, double meterReading, String recordedBy, Timestamp recordedAt) {
            this.readingDate = readingDate;
            this.meterReading = meterReading;
            this.recordedBy = recordedBy;
            this.recordedAt = recordedAt;
        }
        
        public String getReadingDate() { return readingDate.toString(); }
        public String getMeterReading() { return String.format("%.2f kWh", meterReading); }
        public String getRecordedBy() { return recordedBy; }
        public String getRecordedTime() { 
            return recordedAt != null ? recordedAt.toLocalDateTime().toLocalTime().toString() : "";
        }
    }
}