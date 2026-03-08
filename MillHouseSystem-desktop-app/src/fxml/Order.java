package fxml;

import java.time.LocalDateTime;

public class Order {
  
//    private String customerAddress;

    private int id;
    private int customerId;
    private String orderType;
    private String rawType;
    private double estimatedWeight;
    private String deliveryType;
    private String status;
    private LocalDateTime createdDate;
    private String customerName;
    private String orderSource;
    private String assignedDriver;
    private String assignedTo;
    private String customerAddress;
    private String customerNotes;
    private String operatorNotes;
    
    // Add these getters and setters:
    public String getCustomerNotes() { return customerNotes; }
    public void setCustomerNotes(String customerNotes) { this.customerNotes = customerNotes; }
    
    public String getOperatorNotes() { return operatorNotes; }
    public void setOperatorNotes(String operatorNotes) { this.operatorNotes = operatorNotes; }
    
    public Order() {}
    
    // Full constructor
    public Order(int id, int customerId, String orderType, String rawType, 
                 double estimatedWeight, String deliveryType, String status,
                 String customerAddress, LocalDateTime createdDate, 
                 String customerName, String orderSource, String assignedDriver) {
        this.id = id;
        this.customerId = customerId;
        this.orderType = orderType;
        this.rawType = rawType;
        this.estimatedWeight = estimatedWeight;
        this.deliveryType = deliveryType;
        this.status = status;
        this.customerAddress = customerAddress;
        this.createdDate = createdDate;
        this.customerName = customerName;
        this.orderSource = orderSource;
        this.assignedDriver = assignedDriver;
    }
    
    // Getters and setters - ALL OF THEM MUST EXIST
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    
    public String getRawType() { return rawType; }
    public void setRawType(String rawType) { this.rawType = rawType; }
    
    public double getEstimatedWeight() { return estimatedWeight; }
    public void setEstimatedWeight(double estimatedWeight) { this.estimatedWeight = estimatedWeight; }
    
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    // ADD THESE GETTERS AND SETTERS:
    public String getOrderSource() { return orderSource; }
    public void setOrderSource(String orderSource) { this.orderSource = orderSource; }
    
    public String getAssignedDriver() { return assignedDriver; }
    public void setAssignedDriver(String assignedDriver) { this.assignedDriver = assignedDriver; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
}