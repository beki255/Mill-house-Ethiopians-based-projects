package fxml.login.components;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

import fxml.LoginMainController;

public class InitialScreenController {

    private LoginMainController mainController;

    @FXML public VBox initialScreen;

    public void setMainController(LoginMainController controller) {
        this.mainController = controller;
    }

    @FXML
    private void handleStaffLogin() {
        System.out.println("Staff Login clicked!");
        mainController.showStaffLogin();
    }

    @FXML
    private void handleCustomerLogin() {
        System.out.println("Customer Portal clicked!");
        mainController.showCustomerLogin();
    }

    public void show() {
        if (initialScreen != null) {
            initialScreen.setVisible(true);
            initialScreen.setManaged(true);
        }
    }

    public void hide() {
        if (initialScreen != null) {
            initialScreen.setVisible(false);
            initialScreen.setManaged(false);
        }
    }
    public void updateLanguage(ResourceBundle bundle) {
        if (bundle != null) {
            showStaffLogin.setText(bundle.getString("staff_login_button"));
            showCustomerLogin.setText(bundle.getString("customer_login_button"));
        }
    }
}