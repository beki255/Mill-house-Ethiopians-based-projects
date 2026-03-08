package fxml.accountant.tabs;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class PaymentProcessingTabController {

    @FXML private TableView<Payment> pendingPaymentsTable;
    @FXML private TextField paymentAmountField;
    ;
    @FXML private ComboBox<String> paymentMethodCombo;

    private ObservableList<Payment> pendingPayments = FXCollections.observableArrayList();

    @FXML private void initialize() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
            "Cash", "Bank Transfer", "Check", "Mobile Money"
        ));
        setupTable();
        loadPendingPayments();
    }

    private void setupTable() {
        TableColumn<Payment, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));

        TableColumn<Payment, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        TableColumn<Payment, Double> amountCol = new TableColumn<>("Amount (ETB)");
        amountCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getAmount()).asObject());

        TableColumn<Payment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        pendingPaymentsTable.getColumns().setAll(typeCol, descCol, amountCol, statusCol);
        pendingPaymentsTable.setItems(pendingPayments);
    }

    private void loadPendingPayments() {
        pendingPayments.clear();
        // Sample data — replace with real DB query later
        pendingPayments.addAll(
            new Payment("Supplier", "Grain Purchase", 50000, "Pending"),
            new Payment("Utility", "Electricity Bill", 15000, "Pending"),
            new Payment("Maintenance", "Equipment Repair", 8000, "Pending")
        );
    }

    @FXML private void handleProcessPayment() {
        Payment selected = pendingPaymentsTable.getSelectionModel().getSelectedItem();
        String method = paymentMethodCombo.getValue();
        String amountText = paymentAmountField.getText();

        if (selected == null || method == null || amountText.isEmpty()) {
            showAlert("Error", "Select payment and enter amount", Alert.AlertType.ERROR);
            return;
        }

        try {
            Double.parseDouble(amountText);
            showAlert("Success", "Payment processed via " + method, Alert.AlertType.INFORMATION);
            paymentAmountField.clear();
            loadPendingPayments(); // refresh
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid amount", Alert.AlertType.ERROR);
        }
    }

    public void refresh() { loadPendingPayments(); }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }

    public static class Payment {
        private String type, description, status;
        private double amount;
        public Payment(String t, String d, double a, String s) {
            type = t; description = d; amount = a; status = s;
        }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
    }
}