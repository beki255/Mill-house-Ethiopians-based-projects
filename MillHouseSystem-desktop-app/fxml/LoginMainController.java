package fxml;

import fxml.login.components.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.scene.Node;

public class LoginMainController {

    @FXML private ChoiceBox<String> languageChoice;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblFooter;
    
    @FXML private InitialScreenController initialScreenController;
    @FXML private StaffLoginFormController staffFormController;
    @FXML private CustomerLoginFormController customerFormController;

    @FXML
    private void initialize() {
        System.out.println("LoginMainController initialized!");
        
        // Initialize language choice box FIRST
        initializeLanguageChoiceBox();
        
        Platform.runLater(() -> {
            System.out.println("Injecting main controller into child components...");

            if (initialScreenController != null) {
                initialScreenController.setMainController(this);
            }
            if (staffFormController != null) {
                staffFormController.setMainController(this);
            }
            if (customerFormController != null) {
                customerFormController.setMainController(this);
            }

            showInitialScreen();
        });
    }
    
    private void initializeLanguageChoiceBox() {
        // Check if languageChoice exists
        if (languageChoice == null) {
            System.err.println("ERROR: languageChoice is null! Check FXML fx:id");
            return;
        }
        
        System.out.println("Initializing language choice box...");
        
        // Use Platform.runLater to ensure UI is ready
        Platform.runLater(() -> {
            try {
                // Clear any existing items
                languageChoice.getItems().clear();
                
                // Add language options
                languageChoice.getItems().addAll("English", "አማርኛ");
                
                // Set default value
                languageChoice.setValue("English");
                
                // Force the dropdown to be visible
                languageChoice.showingProperty().addListener((obs, oldVal, newVal) -> {
                    System.out.println("ChoiceBox showing: " + newVal);
                });
                
                // Add selection listener
                languageChoice.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        System.out.println("Language changed to: " + newValue);
                        handleLanguageChange(newValue);
                    }
                );
                
                // Set a tooltip
                languageChoice.setTooltip(new Tooltip("Select language"));
                
                // Make it focusable and request focus
                languageChoice.setFocusTraversable(true);
                
                // Debug: Print ChoiceBox properties
                System.out.println("ChoiceBox properties:");
                System.out.println("  - Visible: " + languageChoice.isVisible());
                System.out.println("  - Disabled: " + languageChoice.isDisabled());
                System.out.println("  - Focusable: " + languageChoice.isFocusTraversable());
                System.out.println("  - Items count: " + languageChoice.getItems().size());
                
            } catch (Exception e) {
                System.err.println("Error initializing language choice box: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    
    private void handleLanguageChange(String selectedLanguage) {
        if (selectedLanguage == null) return;
        
        System.out.println("Handling language change to: " + selectedLanguage);
        
        try {
            if ("አማርኛ".equals(selectedLanguage)) {
                // Switch to Amharic
                updateUITextAmharic();
            } else {
                // Switch to English (default)
                updateUITextEnglish();
            }
            
            // Also update child controllers if they're visible
            updateChildControllers(selectedLanguage);
            
        } catch (Exception e) {
            System.err.println("Error changing language: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    private void updateUITextAmharic() {
        System.out.println("Updating UI to Amharic...");
        
        if (lblTitle != null) {
            lblTitle.setText("ሚል ሃውስ አስተዳደር ስርዓት");
        } else {
            System.err.println("lblTitle is null!");
        }
        
        if (lblSubtitle != null) {
            lblSubtitle.setText("ኢትዮጵያ • ከ2010 ጀምሮ ጥራት ያለው የእህል ማቀነባበሪያ");
        } else {
            System.err.println("lblSubtitle is null!");
        }
        
        if (lblFooter != null) {
            lblFooter.setText("© 2025 ሚል ሃውስ • አዲስ አበባ፣ ኢትዮጵያ");
        } else {
            System.err.println("lblFooter is null!");
        }
    }
    
    private void updateUITextEnglish() {
        System.out.println("Updating UI to English...");
        
        if (lblTitle != null) {
            lblTitle.setText("Mill House Management System");
        }
        
        if (lblSubtitle != null) {
            lblSubtitle.setText("Ethiopia • Quality Grain Processing Since 2010");
        }
        
        if (lblFooter != null) {
            lblFooter.setText("© 2025 Mill House • Addis Ababa, Ethiopia");
        }
    }

    private void updateLanguage(String language) {
        if ("አማርኛ".equals(language)) {
            // Amharic
            if (lblTitle != null) lblTitle.setText("ሚል ሃውስ አስተዳደር ስርዓት");
            if (lblSubtitle != null) lblSubtitle.setText("ኢትዮጵያ • ከ2010 ጀምሮ ጥራት ያለው የእህል ማቀነባበሪያ");
            if (lblFooter != null) lblFooter.setText("© 2025 ሚል ሃውስ • አዲስ አበባ፣ ኢትዮጵያ");
        } else {
            // English
            if (lblTitle != null) lblTitle.setText("Mill House Management System");
            if (lblSubtitle != null) lblSubtitle.setText("Ethiopia • Quality Grain Processing Since 2010");
            if (lblFooter != null) lblFooter.setText("© 2025 Mill House • Addis Ababa, Ethiopia");
        }
    }
    private void updateChildControllers(String language) {
        System.out.println("Updating child controllers for language: " + language);
        
        // You can add logic here to update text in child components
        // For now, just log which forms are visible
        
        if (initialScreenController != null && 
            initialScreenController.initialScreen != null &&
            initialScreenController.initialScreen.isVisible()) {
            System.out.println("Initial screen is visible - update its text");
            // Call method to update initial screen text
        }
        
        if (staffFormController != null && 
            staffFormController.staffForm != null &&
            staffFormController.staffForm.isVisible()) {
            System.out.println("Staff form is visible - update its text");
            // Call method to update staff form text
        }
        
        if (customerFormController != null && 
            customerFormController.customerForm != null &&
            customerFormController.customerForm.isVisible()) {
            System.out.println("Customer form is visible - update its text");
            // Call method to update customer form text
        }
    }
    
    // Debug method to check FXML injection
    public void debugFXMLInjection() {
        System.out.println("=== DEBUG FXML Injection ===");
        System.out.println("languageChoice: " + languageChoice);
        System.out.println("lblTitle: " + lblTitle);
        System.out.println("lblSubtitle: " + lblSubtitle);
        System.out.println("lblFooter: " + lblFooter);
        System.out.println("initialScreenController: " + initialScreenController);
        System.out.println("staffFormController: " + staffFormController);
        System.out.println("customerFormController: " + customerFormController);
        System.out.println("============================");
    }
    
    // Rest of your existing methods...
    public void showInitialScreen() {
        if (initialScreenController != null) initialScreenController.show();
        if (staffFormController != null) staffFormController.hide();
        if (customerFormController != null) customerFormController.hide();
    }

    public void showStaffLogin() {
        if (initialScreenController != null) initialScreenController.hide();
        if (staffFormController != null) staffFormController.show();
        if (customerFormController != null) customerFormController.hide();
    }

    public void showCustomerLogin() {
        if (initialScreenController != null) initialScreenController.hide();
        if (staffFormController != null) staffFormController.hide();
        if (customerFormController != null) customerFormController.show();
    }

    public void loadDashboard(String role) {
        try {
            String fxmlPath = switch (role.toUpperCase()) {
                case "ADMIN"      -> "/fxml/AdminDashboard.fxml";
                case "OPERATOR"   -> "/fxml/OperatorDashboard.fxml";
                case "ACCOUNTANT" -> "/fxml/AccountantDashboard.fxml";
                case "DRIVER"     -> "/fxml/DriverDashboard.fxml";
                default           -> "/fxml/OperatorDashboard.fxml";
            };

            System.out.println("DEBUG: Loading dashboard from: " + fxmlPath);
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Mill House - " + role);
            stage.setMaximized(true);
            stage.show();

            // Close ALL login windows
            closeAllLoginWindows();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load dashboard: " + e.getMessage()).show();
        }
    }

    public void loadCustomerDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/CustomerDashboard.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Customer Portal - Mill House");
            stage.setMaximized(true);
            stage.show();

            // Close ALL login windows
            closeAllLoginWindows();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeAllLoginWindows() {
        try {
            // Close ALL windows with "Login" in title
            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    String title = stage.getTitle();
                    if (title != null && title.contains("Login")) {
                        System.out.println("Closing login window: " + title);
                        stage.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}