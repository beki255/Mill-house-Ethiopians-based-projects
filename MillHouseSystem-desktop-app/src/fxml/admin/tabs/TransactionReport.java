package fxml.admin.tabs;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class TransactionReport {
    private SimpleStringProperty date;
    private SimpleIntegerProperty transactions;
    private SimpleDoubleProperty revenue;
    private SimpleDoubleProperty avgTransaction;
    
    public TransactionReport(String date, int transactions, double revenue, double avgTransaction) {
        this.date = new SimpleStringProperty(date);
        this.transactions = new SimpleIntegerProperty(transactions);
        this.revenue = new SimpleDoubleProperty(revenue);
        this.avgTransaction = new SimpleDoubleProperty(avgTransaction);
    }
    
    public TransactionReport(String string, int int1, double double1, int avgTransaction2) {
		// TODO Auto-generated constructor stub
	}

	public String getDate() { return date.get(); }
    public void setDate(String date) { this.date.set(date); }
    public SimpleStringProperty dateProperty() { return date; }
    
    public int getTransactions() { return transactions.get(); }
    public void setTransactions(int transactions) { this.transactions.set(transactions); }
    public SimpleIntegerProperty transactionsProperty() { return transactions; }
    
    public double getRevenue() { return revenue.get(); }
    public void setRevenue(double revenue) { this.revenue.set(revenue); }
    public SimpleDoubleProperty revenueProperty() { return revenue; }
    
    public double getAvgTransaction() { return avgTransaction.get(); }
    public void setAvgTransaction(double avgTransaction) { this.avgTransaction.set(avgTransaction); }
    public SimpleDoubleProperty avgTransactionProperty() { return avgTransaction; }
}