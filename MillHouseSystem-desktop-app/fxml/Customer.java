package fxml;

import java.security.Timestamp;

public class Customer {
    private int id;
    private String name;
    private String phone;
    private String locationCoordinates;
    private String deliveryAddress;
    private double creditBalance;
    private String registrationType; // "PORTAL" or "OPERATOR"
    private String registeredBy; // "self" or operator username
    private Timestamp registrationDate;
    
    public Customer() {}
    
    public Customer(String name, String phone, String deliveryAddress) {
        this.name = name;
        this.phone = phone;
        this.deliveryAddress = deliveryAddress;
        this.registrationType = "PORTAL";
        this.registeredBy = "self";
        this.registrationDate = new Timestamp(System.currentTimeMillis());
    }
    
    // Constructor for operator-added customers
    public Customer(String name, String phone, String deliveryAddress, String operatorUsername) {
        this.name = name;
        this.phone = phone;
        this.deliveryAddress = deliveryAddress;
        this.registrationType = "OPERATOR";
        this.registeredBy = operatorUsername;
        this.registrationDate = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getLocationCoordinates() { return locationCoordinates; }
    public void setLocationCoordinates(String locationCoordinates) { this.locationCoordinates = locationCoordinates; }
    
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    
    public double getCreditBalance() { return creditBalance; }
    public void setCreditBalance(double creditBalance) { this.creditBalance = creditBalance; }
    
    public String getRegistrationType() { return registrationType; }
    public void setRegistrationType(String registrationType) { this.registrationType = registrationType; }
    
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    
    public Timestamp getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Timestamp registrationDate) { this.registrationDate = registrationDate; }
    
    // Helper method to check if customer can place orders
    public boolean canPlaceOrders() {
        // Both portal-registered and operator-added customers can place orders
        return true;
    }
    
    // Helper method to check if customer can login to portal
    public boolean canLoginToPortal() {
        // Only portal-registered customers can login
        return "PORTAL".equals(registrationType);
    }
}