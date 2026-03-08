package fxml.accountant.tabs;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.*;

import fxml.DatabaseConnection;

public class DashboardTabController {

    @FXML private Label todaySummaryLabel;

    @FXML private void initialize() {
        loadTodaySummary();
    }

    public void refresh() {
        loadTodaySummary();
    }

    private void loadTodaySummary() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COALESCE(SUM(payment_amount_ETB), 0) as today_revenue " +
                 "FROM transactions WHERE DATE(transaction_date) = CURDATE()")) {

            if (rs.next()) {
                double revenue = rs.getDouble("today_revenue");
                todaySummaryLabel.setText("Today's Revenue: ETB " + String.format("%.2f", revenue));
            }
        } catch (SQLException e) {
            todaySummaryLabel.setText("Error loading revenue");
            e.printStackTrace();
        }
    }
}