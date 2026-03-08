package fxml.driver.tabs;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RoutePlanningTabController {

    @FXML private TextArea routeDescriptionArea;
    @FXML private TextField estimatedTimeField;

    @FXML private void initialize() {
        // Ready for use
    }

    @FXML private void handleSaveRoute() {
        String route = routeDescriptionArea.getText().trim();
        String time = estimatedTimeField.getText().trim();

        if (route.isEmpty() || time.isEmpty()) {
            showAlert("Error", "Please fill route and estimated time", Alert.AlertType.ERROR);
            return;
        }

        try {
            Integer.parseInt(time); // validate number
            showAlert("Success", "Route saved!\nEstimated time: " + time + " minutes", Alert.AlertType.INFORMATION);
            routeDescriptionArea.clear();
            estimatedTimeField.clear();
        } catch (NumberFormatException e) {
            showAlert("Error", "Estimated time must be a number", Alert.AlertType.ERROR);
        }
    }

    public void refresh() {
        // Clear form on refresh if needed
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        new Alert(type, msg).showAndWait();
    }
}