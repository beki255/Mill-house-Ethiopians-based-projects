package com.mill.system;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CartItem {
    private int id;
    private int customerId;
    private int productId;
    private int quantity;
    private Timestamp addedDate;
    
    // Additional fields from product join (for display purposes)
    private String productName;
    private String description;
    private String category;
    private BigDecimal sellPrice;
    private BigDecimal millingPrice;
    private int minQuantity;
    private String imageUrl;
    private double subtotal;
    
    // Constructors
    public CartItem() {
    }
    
    public CartItem(int id, int customerId, int productId, int quantity, Timestamp addedDate) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.addedDate = addedDate;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
    
    public int getProductId() {
        return productId;
    }
    
    public void setProductId(int productId) {
        this.productId = productId;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public Timestamp getAddedDate() {
        return addedDate;
    }
    
    public void setAddedDate(Timestamp addedDate) {
        this.addedDate = addedDate;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public BigDecimal getSellPrice() {
        return sellPrice;
    }
    
    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }
    
    public BigDecimal getMillingPrice() {
        return millingPrice;
    }
    
    public void setMillingPrice(BigDecimal millingPrice) {
        this.millingPrice = millingPrice;
    }
    
    public int getMinQuantity() {
        return minQuantity;
    }
    
    public void setMinQuantity(int minQuantity) {
        this.minQuantity = minQuantity;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public double getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
    
    // Utility methods
    
    /**
     * Calculate the subtotal for this cart item
     * Subtotal = (quantity * sellPrice) + (quantity * millingPrice)
     */
    public double calculateSubtotal() {
        if (sellPrice == null || millingPrice == null) {
            return 0.0;
        }
        double sellTotal = quantity * sellPrice.doubleValue();
        double millingTotal = quantity * millingPrice.doubleValue();
        this.subtotal = sellTotal + millingTotal;
        return this.subtotal;
    }
    
    /**
     * Calculate milling charge only for this item
     */
    public double calculateMillingCharge() {
        if (millingPrice == null) {
            return 0.0;
        }
        return quantity * millingPrice.doubleValue();
    }
    
    /**
     * Calculate product cost only (without milling) for this item
     */
    public double calculateProductCost() {
        if (sellPrice == null) {
            return 0.0;
        }
        return quantity * sellPrice.doubleValue();
    }
    
    /**
     * Check if quantity meets minimum requirement
     */
    public boolean meetsMinimumQuantity() {
        return quantity >= minQuantity;
    }
    
    /**
     * Get formatted price string
     */
    public String getFormattedSellPrice() {
        if (sellPrice == null) return "0.00";
        return String.format("$%.2f", sellPrice.doubleValue());
    }
    
    public String getFormattedMillingPrice() {
        if (millingPrice == null) return "0.00";
        return String.format("$%.2f", millingPrice.doubleValue());
    }
    
    public String getFormattedSubtotal() {
        return String.format("$%.2f", subtotal);
    }
    
    /**
     * Get formatted date
     */
    public String getFormattedDate() {
        if (addedDate == null) return "";
        return addedDate.toString();
    }
    
    @Override
    public String toString() {
        return "CartItem{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", productName='" + productName + '\'' +
                ", sellPrice=" + (sellPrice != null ? sellPrice.doubleValue() : 0) +
                ", millingPrice=" + (millingPrice != null ? millingPrice.doubleValue() : 0) +
                ", subtotal=" + subtotal +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return id == cartItem.id;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
}
