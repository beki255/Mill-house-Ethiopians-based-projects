// TestOrderClass.java
package fxml;

public class TestOrderClass {
    public static void main(String[] args) {
        System.out.println("Testing Order class getters...");
        
        Order order = new Order();
        order.setId(1);
        order.setOrderType("Milling Only");
        order.setOrderSource("Online");
        order.setAssignedDriver("Driver 1");
        order.setAssignedTo("Mill 1");
        
        System.out.println("ID: " + order.getId());
        System.out.println("Order Type: " + order.getOrderType());
        System.out.println("Order Source: " + order.getOrderSource());
        System.out.println("Assigned Driver: " + order.getAssignedDriver());
        System.out.println("Assigned To: " + order.getAssignedTo());
        
        // Test if PropertyValueFactory can access them
        try {
            java.lang.reflect.Method method = Order.class.getMethod("getOrderSource");
            System.out.println("✓ getOrderSource() method exists!");
        } catch (NoSuchMethodException e) {
            System.out.println("✗ getOrderSource() method missing!");
        }
    }
}