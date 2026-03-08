package fxml.customer.tabs;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

import fxml.CustomerSession;
import fxml.DatabaseConnection;

public class FeedbackTabController {

    @FXML private TextArea feedbackTextArea;
    @FXML private ComboBox<String> ratingCombo;
    @FXML private TableView<Feedback> feedbackHistoryTable;

    @FXML private void initialize() {
        ratingCombo.setItems(FXCollections.observableArrayList(
            "5 - Excellent", "4 - Good", "3 - Average", "2 - Poor", "1 - Very Poor"
        ));
        loadFeedbackHistory();
    }

    @FXML private void handleSubmitFeedback() {
        if (ratingCombo.getValue() == null || feedbackTextArea.getText().trim().isEmpty()) {
            showAlert("Error", "Please select rating and write feedback", Alert.AlertType.ERROR);
            return;
        }

        // Save to DB
        String sql = "INSERT INTO feedback (customer_id, rating, comments, date) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, CustomerSession.getCustomerId());
            ps.setString(2, ratingCombo.getValue().substring(0, 1));
            ps.setString(3, feedbackTextArea.getText());
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();

            showAlert("Thank You!", "Your feedback has been submitted!", Alert.AlertType.INFORMATION);
            feedbackTextArea.clear();
            ratingCombo.setValue(null);
            loadFeedbackHistory();
        } catch (SQLException e) {
            showAlert("Error", "Failed to submit feedback", Alert.AlertType.ERROR);
        }
    }

    private void loadFeedbackHistory() {
        // Load from DB
    }

    public void refresh() { loadFeedbackHistory(); }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }

    public static class Feedback {
        private String date, rating, comments;
        // getters
    }
}