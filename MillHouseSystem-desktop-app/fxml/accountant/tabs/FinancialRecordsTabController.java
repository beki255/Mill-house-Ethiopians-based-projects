package fxml.accountant.tabs;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

import fxml.DatabaseConnection;

public class FinancialRecordsTabController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<FinancialRecord> financialTable;
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label netProfitLabel;

    private ObservableList<FinancialRecord> financialRecords = FXCollections.observableArrayList();

    @FXML private void initialize() {
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        setupTable();
    }

    private void setupTable() {
        TableColumn<FinancialRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()));

        TableColumn<FinancialRecord, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        TableColumn<FinancialRecord, Double> revCol = new TableColumn<>("Revenue (ETB)");
        revCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getRevenue()).asObject());

        TableColumn<FinancialRecord, Double> expCol = new TableColumn<>("Expense (ETB)");
        expCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getExpense()).asObject());

        financialTable.getColumns().setAll(dateCol, descCol, revCol, expCol);
        financialTable.setItems(financialRecords);
    }

    @FXML private void handleGenerateReport() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showAlert("Error", "Please select date range", Alert.AlertType.ERROR);
            return;
        }

        financialRecords.clear();
        double totalRevenue = 0, totalExpenses = 0;

        String sql = "SELECT DATE(transaction_date) as date, 'Sales' as description, " +
                    "SUM(payment_amount_ETB) as revenue, 0 as expense " +
                    "FROM transactions WHERE transaction_date BETWEEN ? AND ? " +
                    "GROUP BY date " +
                    "UNION ALL " +
                    "SELECT DATE(payment_date) as date, 'Salary Payment' as description, " +
                    "0 as revenue, SUM(net_salary) as expense " +
                    "FROM salary_payments WHERE payment_date BETWEEN ? AND ? " +
                    "GROUP BY date ORDER BY date";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(startDatePicker.getValue()));
            ps.setDate(2, Date.valueOf(endDatePicker.getValue()));
            ps.setDate(3, Date.valueOf(startDatePicker.getValue()));
            ps.setDate(4, Date.valueOf(endDatePicker.getValue()));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FinancialRecord rec = new FinancialRecord(
                    rs.getString("date"),
                    rs.getString("description"),
                    rs.getDouble("revenue"),
                    rs.getDouble("expense")
                );
                financialRecords.add(rec);
                totalRevenue += rec.getRevenue();
                totalExpenses += rec.getExpense();
            }

            totalRevenueLabel.setText(String.format("ETB %.2f", totalRevenue));
            totalExpensesLabel.setText(String.format("ETB %.2f", totalExpenses));
            netProfitLabel.setText(String.format("ETB %.2f", totalRevenue - totalExpenses));

        } catch (SQLException e) {
            showAlert("Error", "Failed to generate report", Alert.AlertType.ERROR);
        }
    }

    public void refresh() { handleGenerateReport(); }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }

    public static class FinancialRecord {
        private String date, description;
        private double revenue, expense;
        public FinancialRecord(String d, String desc, double r, double e) {
            date = d; description = desc; revenue = r; expense = e;
        }
        public String getDate() { return date; }
        public String getDescription() { return description; }
        public double getRevenue() { return revenue; }
        public double getExpense() { return expense; }
    }
}