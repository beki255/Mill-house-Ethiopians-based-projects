package fxml.driver.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;

public class DeliveryHistoryTabController {

    @FXML private TableView<DeliveryHistory> historyTable;
    @FXML private DatePicker historyDatePicker;

    private ObservableList<DeliveryHistory> history = FXCollections.observableArrayList();

    @FXML private void initialize() {
        setupTable();
    }

    private void setupTable() {
        TableColumn<DeliveryHistory, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<DeliveryHistory, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<DeliveryHistory, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<DeliveryHistory, String> timeCol = new TableColumn<>("Delivery Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("deliveryTime"));

        historyTable.getColumns().setAll(dateCol, addrCol, statusCol, timeCol);
        historyTable.setItems(history);
    }

    @FXML private void handleViewHistory() {
        LocalDate date = historyDatePicker.getValue();
        if (date == null) {
            showAlert("Error", "Please select a date", Alert.AlertType.WARNING);
            return;
        }

        history.clear();
        // Sample data — replace with real DB query later
        history.addAll(
            new DeliveryHistory(date.toString(), "Bole Medhanealem", "Delivered", "09:30 AM"),
            new DeliveryHistory(date.toString(), "Kazanchis", "Delivered", "11:15 AM"),
            new DeliveryHistory(date.toString(), "4 Kilo", "Returned", "14:20 PM")
        );

        showAlert("Success", "Loaded history for " + date, Alert.AlertType.INFORMATION);
    }

    public void refresh() {
        if (historyDatePicker.getValue() != null) {
            handleViewHistory();
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }

    public static class DeliveryHistory {
        private String date, address, status, deliveryTime;
        public DeliveryHistory(String d, String a, String s, String t) {
            date = d; address = a; status = s; deliveryTime = t;
        }
        public String getDate() { return date; }
        public String getAddress() { return address; }
        public String getStatus() { return status; }
        public String getDeliveryTime() { return deliveryTime; }
    }
}