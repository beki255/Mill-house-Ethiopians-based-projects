package fxml;



import java.time.LocalDateTime;

public class Transaction {
 private int id;
 private int customerId;
 private int operatorId;
 private String rawType;
 private double rawWeight;
 private double paymentAmount;
 private String paymentType;
 private LocalDateTime transactionDate;
 private String customerName;
 
 public Transaction() {}
 
 // Getters and setters
 public int getId() { return id; }
 public void setId(int id) { this.id = id; }
 public int getCustomerId() { return customerId; }
 public void setCustomerId(int customerId) { this.customerId = customerId; }
 public int getOperatorId() { return operatorId; }
 public void setOperatorId(int operatorId) { this.operatorId = operatorId; }
 public String getRawType() { return rawType; }
 public void setRawType(String rawType) { this.rawType = rawType; }
 public double getRawWeight() { return rawWeight; }
 public void setRawWeight(double rawWeight) { this.rawWeight = rawWeight; }
 public double getPaymentAmount() { return paymentAmount; }
 public void setPaymentAmount(double paymentAmount) { this.paymentAmount = paymentAmount; }
 public String getPaymentType() { return paymentType; }
 public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
 public LocalDateTime getTransactionDate() { return transactionDate; }
 public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
 public String getCustomerName() { return customerName; }
 public void setCustomerName(String customerName) { this.customerName = customerName; }
}