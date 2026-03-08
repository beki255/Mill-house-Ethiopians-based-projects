package fxml.accountant.tabs;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

public class SalaryRecordsTabController {

    @FXML private TableView<Salary> salaryTable;
    @FXML private DatePicker salaryMonthPicker;

    private ObservableList<Salary> salaryRecords = FXCollections.observableArrayList();

    @FXML private void initialize() {
        salaryMonthPicker.setValue(LocalDate.now());
        setupTable();
    }

    private void setupTable() {
        TableColumn<Salary, String> nameCol = new TableColumn<>("Staff Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStaffName()));

        TableColumn<Salary, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));

        TableColumn<Salary, Double> amountCol = new TableColumn<>("Amount (ETB)");
        amountCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getAmount()).asObject());

        TableColumn<Salary, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        salaryTable.getColumns().setAll(nameCol, roleCol, amountCol, statusCol);
        salaryTable.setItems(salaryRecords);
    }

    @FXML private void handleViewSalary() {
        if (salaryMonthPicker.getValue() == null) {
            showAlert("Error", "Please select a month", Alert.AlertType.ERROR);
            return;
        }

        salaryRecords.clear();
        // Load real data from DB here
        // Example dummy data
        salaryRecords.add(new Salary("Ahmed Ali", "Operator", 12000, "Paid"));
        salaryRecords.add(new Salary("Fatima Mohammed", "Accountant", 18000, "Paid"));

        showAlert("Success", "Salary records loaded for " + salaryMonthPicker.getValue().getMonth(), Alert.AlertType.INFORMATION);
    }

    public void refresh() { handleViewSalary(); }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }

    public static class Salary {
        private String staffName, role, status;
        private double amount;
        public Salary(String n, String r, double a, String s) {
            staffName = n; role = r; amount = a; status = s;
        }
        public String getStaffName() { return staffName; }
        public String getRole() { return role; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
    }
}