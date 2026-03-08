package fxml.admin.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

import fxml.DatabaseConnection;

public class FinanceTabController {

    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private TableView<TransactionRow> financialTable;
    @FXML private Label reportSummaryLabel;
    @FXML private Label totalRevenueLabel, taxAmountLabel, netIncomeLabel, taxRateLabel;
    @FXML private TextField taxRateField;
    @FXML private Label electricityUsageLabel, electricityCostLabel;
    private double electricityCostPerUnit = 5.00;

    private ObservableList<TransactionRow> tableData = FXCollections.observableArrayList();
    private double taxRate = 15.0;

    @FXML 
    public void initialize() {
    	loadElectricitySettings();
        // Set default dates
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        
        // Set default tax rate
        taxRateField.setText("15.0");
        taxRateLabel.setText("(15.0%)");
        
        // Setup table
        financialTable.setItems(tableData);
    }
 // Add these methods
    private void loadElectricitySettings() {
        String sql = "SELECT cost_per_unit FROM electricity_settings ORDER BY id DESC LIMIT 1";
        
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            
            if (rs.next()) {
                electricityCostPerUnit = rs.getDouble("cost_per_unit");
            }
            
        } catch (SQLException e) {
            System.out.println("Using default electricity cost: 5.00 ETB/unit");
        }
    }

    private void calculateElectricityCost(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT reading_date, meter_reading FROM electric_meter_readings " +
                    "WHERE reading_date BETWEEN ? AND ? ORDER BY reading_date";
        
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            
            ResultSet rs = ps.executeQuery();
            double totalUsage = 0;
            int readingCount = 0;
            double previousReading = 0;
            LocalDate previousDate = null;
            
            while (rs.next()) {
                LocalDate currentDate = rs.getDate("reading_date").toLocalDate();
                double currentReading = rs.getDouble("meter_reading");
                readingCount++;
                
                if (previousDate != null) {
                    // Calculate daily usage
                    double dailyUsage = currentReading - previousReading;
                    if (dailyUsage > 0) {
                        totalUsage += dailyUsage;
                    }
                }
                
                previousReading = currentReading;
                previousDate = currentDate;
            }
            
            double totalCost = totalUsage * electricityCostPerUnit;
            
            electricityUsageLabel.setText(String.format("%.2f kWh", totalUsage));
            electricityCostLabel.setText(String.format("ETB %.2f", totalCost));
            
        } catch (SQLException e) {
            System.out.println("Error calculating electricity cost: " + e.getMessage());
            electricityUsageLabel.setText("N/A");
            electricityCostLabel.setText("N/A");
        }
    }
    @FXML 
    public void handleGenerateReport() {
        try {
            calculateReport();
        } catch (Exception e) {
            showAlert("Error", "Failed to generate report: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    public void handleUpdateTaxRate() {
        try {
            String rateText = taxRateField.getText().trim();
            double newRate = Double.parseDouble(rateText);
            
            if (newRate >= 0 && newRate <= 100) {
                taxRate = newRate;
                taxRateLabel.setText("(" + taxRate + "%)");
                calculateReport();
                showAlert("Success", "Tax rate updated to " + taxRate + "%", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Tax rate must be between 0 and 100%", Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number for tax rate", Alert.AlertType.ERROR);
        }
    }
    
    private void calculateReport() {
    	calculateElectricityCost(startDatePicker.getValue(), endDatePicker.getValue());
        // Clear old data
        tableData.clear();
        
        // Get dates
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showAlert("Error", "Please select both start and end dates", Alert.AlertType.ERROR);
            return;
        }
        
        // Calculate total revenue
        double totalRevenue = getTotalRevenue(startDate, endDate);
        
        // Calculate tax and net income
        double taxAmount = totalRevenue * (taxRate / 100.0);
        double netIncome = totalRevenue - taxAmount;
        
        // Update display
        totalRevenueLabel.setText(String.format("ETB %.2f", totalRevenue));
        taxAmountLabel.setText(String.format("ETB %.2f", taxAmount));
        netIncomeLabel.setText(String.format("ETB %.2f", netIncome));
        
        // Load daily transactions
        loadDailyTransactions(startDate, endDate);
        
        // Update summary
        reportSummaryLabel.setText(String.format(
            "Period: %s to %s | Total Revenue: ETB %.2f",
            startDate, endDate, totalRevenue
        ));
    }
    
    private double getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT SUM(payment_amount_ETB) as total FROM transactions " +
                    "WHERE transaction_date BETWEEN ? AND ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
            
        } catch (SQLException e) {
            System.out.println("Error getting revenue: " + e.getMessage());
        }
        
        return 0.0;
    }
    
    private void loadDailyTransactions(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT DATE(transaction_date) as date, " +
                    "COUNT(*) as count, " +
                    "SUM(payment_amount_ETB) as revenue " +
                    "FROM transactions " +
                    "WHERE transaction_date BETWEEN ? AND ? " +
                    "GROUP BY DATE(transaction_date) " +
                    "ORDER BY date";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String date = rs.getString("date");
                int count = rs.getInt("count");
                double revenue = rs.getDouble("revenue");
                double average = count > 0 ? revenue / count : 0;
                
                tableData.add(new TransactionRow(date, count, revenue, average));
            }
            
        } catch (SQLException e) {
            System.out.println("Error loading transactions: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
    
    public void refresh() {
        calculateReport();
    }
    
    // Simple data class
    public static class TransactionRow {
        private String date;
        private int count;
        private double revenue;
        private double average;
        
        public TransactionRow(String date, int count, double revenue, double average) {
            this.date = date;
            this.count = count;
            this.revenue = revenue;
            this.average = average;
        }
        
        public String getDate() { return date; }
        public int getCount() { return count; }
        public double getRevenue() { return revenue; }
        public double getAverage() { return average; }
    }
}