package fxml;

import fxml.customer.tabs.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;

// ADD THESE IMPORTS
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

public class CustomerMainController {

    @FXML private OrderPlacementTabController orderTabController;
    @FXML private AccountTransactionsTabController accountTabController;
    @FXML private FeedbackTabController feedbackTabController;

    @FXML private Label welcomeLabel;
    @FXML private Label movingWelcomeLabel;
    @FXML private TabPane customerTabPane;
    
    @FXML private ImageView wheatImage;
    @FXML private ImageView teffImage;
    @FXML private ImageView cornImage;
    @FXML private ImageView barleyImage;
    
    private TranslateTransition moveTransition;
    private Node customerPane;
    
    // ADD THIS FIELD
    private Timeline refreshTimeline;
    
    @FXML
    private void initialize() {
        System.out.println("Customer Dashboard - All Tabs Loaded!");

        // Debug session state
        System.out.println("=== DASHBOARD INITIALIZATION ===");
        CustomerSession.printActiveSessions();
        System.out.println("Customer ID from session: " + CustomerSession.getCustomerId());
        System.out.println("===============================");

        // Check if user is logged in
        if (!CustomerSession.isLoggedIn()) {
            System.out.println("ERROR: Customer not logged in but dashboard loaded!");
            
            Platform.runLater(() -> {
                showAlert("Session Error", 
                         "You are not logged in or your session has expired.\n" +
                         "Please login again.", 
                         Alert.AlertType.ERROR);
                
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        Platform.runLater(() -> {
                            try {
                                Stage stage = findStage();
                                if (stage != null) {
                                    stage.close();
                                    System.out.println("Dashboard window closed due to session error");
                                }
                            } catch (Exception e) {
                                System.err.println("Could not close window: " + e.getMessage());
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            });
            return;
        }

        // Set welcome message
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + CustomerSession.getCustomerName());
        }
        
        try {
            // Start moving welcome message
            startMovingWelcomeMessage();
            
            // Setup hover effects
            setupImageHoverEffects();
            
            // START AUTO-REFRESH - every 30 seconds
            startAutoRefresh();
            
        } catch (Exception e) {
            System.err.println("Error initializing dashboard: " + e.getMessage());
        }

        // Refresh all tabs initially
        refreshAllTabs();
    }
    

 // Modify the startAutoRefresh method to refresh more frequently
 // In CustomerMainController.java - MODIFY startAutoRefresh method
    private void startAutoRefresh() {
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(10), event -> { // Changed from 30 to 10 seconds
                System.out.println("Auto-refreshing customer dashboard...");
                refreshAllTabs();
                refreshOrderStatusInAllTabs(); // ADD THIS LINE
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        System.out.println("Auto-refresh started (every 10 seconds)");
    }

    // ADD this method
    private void refreshOrderStatusInAllTabs() {
        if (orderTabController != null) {
            orderTabController.refresh();
        }
        if (accountTabController != null) {
            accountTabController.refreshOrderStatus(); // Call the new method
        }
    }

    // Add method to refresh order status specifically
    private void refreshOrderStatus() {
        if (orderTabController != null) {
            orderTabController.refresh();
        }
        if (accountTabController != null) {
            accountTabController.loadRecentOrderStatus();
        }
    }
    // MODIFY THIS METHOD - SIMPLIFY IT
    private void refreshAllTabs() {
        if (orderTabController != null) {
            orderTabController.refresh();
            System.out.println("Refreshed order tab");
        }
        if (accountTabController != null) {
            accountTabController.refresh();
            System.out.println("Refreshed account tab");
        }
        if (feedbackTabController != null) {
            feedbackTabController.refresh();
            System.out.println("Refreshed feedback tab");
        }
    }

    // REMOVE refreshOrderStatusInAllTabs() method - we don't need it

    // Helper method to find the stage
    private Stage findStage() {
        for (Node node : new Node[]{welcomeLabel, customerTabPane, customerPane}) {
            if (node != null && node.getScene() != null && node.getScene().getWindow() != null) {
                return (Stage) node.getScene().getWindow();
            }
        }
        return null;
    }

    // Rest of your existing methods remain the same...
    private void startMovingWelcomeMessage() {
        if (movingWelcomeLabel != null) {
            String customerName = CustomerSession.getCustomerName();
            movingWelcomeLabel.setText("🌟 Welcome " + customerName + " to Mill House - Quality Grain Processing Since 2010 🌟");
            
            moveTransition = new TranslateTransition(Duration.seconds(15), movingWelcomeLabel);
            moveTransition.setFromX(-500);
            moveTransition.setToX(500);
            moveTransition.setCycleCount(TranslateTransition.INDEFINITE);
            moveTransition.setAutoReverse(true);
            moveTransition.play();
        }
    }
    
    private void setupImageHoverEffects() {
        setupImageHover(wheatImage, "Wheat", "Premium Ethiopian wheat with high protein content. Perfect for bread and pasta.", "#f39c12");
        setupImageHover(teffImage, "Teff", "Traditional Ethiopian teff, rich in iron and calcium. Gluten-free and perfect for injera.", "#9b59b6");
        setupImageHover(cornImage, "Corn", "Fresh sweet corn harvested at peak ripeness. Rich in fiber and antioxidants.", "#e67e22");
        setupImageHover(barleyImage, "Barley", "Organic barley grown without pesticides. High in dietary fiber.", "#1abc9c");
    }
    
    private void setupImageHover(ImageView imageView, String grainType, String description, String color) {
        if (imageView == null) return;
        
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
            grainType + "\n\n" + description + "\n\nPrice: " + getGrainPrice(grainType) + "/kg"
        );
        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: " + color + ";");
        javafx.scene.control.Tooltip.install(imageView, tooltip);
        
        imageView.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            imageView.setStyle("-fx-effect: dropshadow(gaussian, " + color + ", 20, 0.7, 0, 0);");
            imageView.setScaleX(1.05);
            imageView.setScaleY(1.05);
        });
        
        imageView.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            imageView.setStyle("");
            imageView.setScaleX(1.0);
            imageView.setScaleY(1.0);
        });
        
        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2) {
                showGrainDetails(grainType);
            }
        });
    }
    
    private String getGrainPrice(String grainType) {
        switch (grainType) {
            case "Wheat": return "ETB 45.00";
            case "Teff": return "ETB 85.00";
            case "Corn": return "ETB 40.00";
            case "Barley": return "ETB 42.00";
            default: return "ETB 0.00";
        }
    }
    
    private void showGrainDetails(String grainName) {
        String details = "";
        String price = "";
        
        switch (grainName) {
            case "Wheat":
                details = "Our premium wheat is sourced from the finest Ethiopian farms. " +
                         "It has high protein content making it perfect for bread, pasta, and baked goods. " +
                         "Grown organically without pesticides.";
                price = "ETB 45.00/kg";
                break;
            case "Teff":
                details = "Traditional Ethiopian teff, the smallest grain in the world but packed with nutrition. " +
                         "Rich in iron, calcium, and protein. Gluten-free and perfect for making injera, " +
                         "the traditional Ethiopian flatbread.";
                price = "ETB 85.00/kg";
                break;
            case "Corn":
                details = "Fresh sweet corn harvested at peak ripeness from local Ethiopian farms. " +
                         "Rich in fiber, vitamins, and antioxidants. Perfect for roasting, boiling, " +
                         "or grinding into flour.";
                price = "ETB 40.00/kg";
                break;
            case "Barley":
                details = "Organic barley grown without pesticides in the highlands of Ethiopia. " +
                         "High in dietary fiber and beneficial for heart health. Great for soups, stews, " +
                         "and brewing.";
                price = "ETB 42.00/kg";
                break;
        }
        
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle(grainName + " Details");
        infoAlert.setHeaderText("Premium " + grainName + " - " + price);
        infoAlert.setContentText(details);
        infoAlert.showAndWait();
    }
    
    // Order handlers for grain buttons
    @FXML
    private void handleOrderWheat() {
        if (orderTabController != null) {
            orderTabController.prefillOrderForm("Wheat");
            customerTabPane.getSelectionModel().select(0);
        }
    }
    
    @FXML
    private void handleOrderTeff() {
        if (orderTabController != null) {
            orderTabController.prefillOrderForm("Teff");
            customerTabPane.getSelectionModel().select(0);
        }
    }
    
    @FXML
    private void handleOrderCorn() {
        if (orderTabController != null) {
            orderTabController.prefillOrderForm("Corn");
            customerTabPane.getSelectionModel().select(0);
        }
    }
    
    @FXML
    private void handleOrderBarley() {
        if (orderTabController != null) {
            orderTabController.prefillOrderForm("Barley");
            customerTabPane.getSelectionModel().select(0);
        }
    }

    @FXML
    private void handleViewPriceList() {
        Alert priceAlert = new Alert(Alert.AlertType.INFORMATION);
        priceAlert.setTitle("Current Price List");
        priceAlert.setHeaderText("Mill House Grain Prices (per kg)");
        
        String priceContent = 
            "🌟 PREMIUM GRAINS 🌟\n\n" +
            "Wheat: ETB 45.00\n" +
            "Teff: ETB 85.00\n" +
            "Corn: ETB 40.00\n" +
            "Barley: ETB 42.00\n\n" +
            "💎 QUALITY GUARANTEE 💎\n" +
            "• 100% Organic\n" +
            "• Freshly Harvested\n" +
            "• Ethically Sourced\n" +
            "• Traditional Ethiopian\n\n" +
            "🚚 Delivery Fee: ETB 5.00 per kg\n" +
            "⚙️ Milling Only: ETB 8.00 per kg";
        
        priceAlert.setContentText(priceContent);
        priceAlert.showAndWait();
    }


    @FXML
    private void handleRefreshData() {
        refreshAllTabs();
        
        // Restart moving message
        if (moveTransition != null) {
            moveTransition.stop();
            startMovingWelcomeMessage();
        }
        
        showAlert("Success", "All data refreshed!", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Logout");
        confirm.setHeaderText("Logout from Customer Portal");
        confirm.setContentText("Are you sure you want to logout?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Stop the auto-refresh timer
                if (refreshTimeline != null) {
                    refreshTimeline.stop();
                }
                
                CustomerSession.clearSession();
                Stage stage = (Stage) welcomeLabel.getScene().getWindow();
                stage.close();
            }
        });
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }
}