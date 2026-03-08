package fxml.admin.tabs;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import java.sql.*;

import fxml.DatabaseConnection;

public class DashboardTabController {

    @FXML private Label totalCustomersLabel, totalTransactionsLabel, totalRevenueLabel, inventoryValueLabel;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private PieChart grainDistributionChart;

    @FXML private void initialize() {
        loadData();
    }

    public void refresh() { loadData(); }

    private void loadData() {
        loadMetrics();
        loadRevenueChart();
        loadGrainChart();
    }

    private void loadMetrics() {
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM customers");
            if (rs.next()) totalCustomersLabel.setText(rs.getInt(1) + "");

            rs = s.executeQuery("SELECT COUNT(*) FROM transactions WHERE transaction_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            if (rs.next()) totalTransactionsLabel.setText(rs.getInt(1) + "");

            rs = s.executeQuery("SELECT COALESCE(SUM(payment_amount_ETB),0) FROM transactions WHERE transaction_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            if (rs.next()) totalRevenueLabel.setText("ETB " + String.format("%.2f", rs.getDouble(1)));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadRevenueChart() {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT DATE(transaction_date) d, SUM(payment_amount_ETB) r FROM transactions WHERE transaction_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY d")) {
            while (rs.next()) series.getData().add(new XYChart.Data<>(rs.getDate("d").toString(), rs.getDouble("r")));
        } catch (SQLException e) { }
        revenueChart.getData().add(series);
    }

    private void loadGrainChart() {
        grainDistributionChart.getData().clear();
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT grain_type, current_stock_kg FROM inventory")) {
            while (rs.next()) {
                grainDistributionChart.getData().add(new PieChart.Data(rs.getString("grain_type") + " (" + rs.getDouble("current_stock_kg") + "kg)", rs.getDouble("current_stock_kg")));
            }
        } catch (SQLException e) { }
    }
}