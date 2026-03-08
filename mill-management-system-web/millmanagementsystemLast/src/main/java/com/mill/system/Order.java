package com.mill.system;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Order {
    private int id;
    private String orderNumber;
    private int customerId;
    private int productId;
    private BigDecimal quantity;
    private BigDecimal totalPrice;
    private BigDecimal millingCharge;
    private BigDecimal orderFee;
    private String deliveryAddress;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private int assignedOperator;
    private int assignedDriver;
    private boolean isSpecialOrder;
    private String specialDescription;
    private Timestamp orderDate;
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    
    public BigDecimal getMillingCharge() { return millingCharge; }
    public void setMillingCharge(BigDecimal millingCharge) { this.millingCharge = millingCharge; }
    
    public BigDecimal getOrderFee() { return orderFee; }
    public void setOrderFee(BigDecimal orderFee) { this.orderFee = orderFee; }
    
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    
    public int getAssignedOperator() { return assignedOperator; }
    public void setAssignedOperator(int assignedOperator) { this.assignedOperator = assignedOperator; }
    
    public int getAssignedDriver() { return assignedDriver; }
    public void setAssignedDriver(int assignedDriver) { this.assignedDriver = assignedDriver; }
    
    public boolean isSpecialOrder() { return isSpecialOrder; }
    public void setSpecialOrder(boolean isSpecialOrder) { this.isSpecialOrder = isSpecialOrder; }
    
    public String getSpecialDescription() { return specialDescription; }
    public void setSpecialDescription(String specialDescription) { this.specialDescription = specialDescription; }
    
    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
}