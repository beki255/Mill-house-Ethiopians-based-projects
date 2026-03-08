package fxml;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomerSession {
    // Thread-safe session storage
    private static final ThreadLocal<CustomerSession> currentSession = 
        new ThreadLocal<>();
    
    // Session token to session map for lookup
    private static final Map<String, CustomerSession> activeSessions = 
        new HashMap<>();
    
    private String sessionToken;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private double creditBalance;
    private long lastAccessTime;
    
    private CustomerSession() {
        // Private constructor - use factory method
    }
    
    // FIXED: This was empty! Now properly creates a session
    public static void setCurrentCustomer(int id, String name, String phone, double balance) {
        CustomerSession session = new CustomerSession();
        session.sessionToken = UUID.randomUUID().toString();
        session.customerId = id;
        session.customerName = name;
        session.customerPhone = phone;
        session.creditBalance = balance;
        session.lastAccessTime = System.currentTimeMillis();
        
        // Store in active sessions
        synchronized (activeSessions) {
            activeSessions.put(session.sessionToken, session);
        }
        
        // Set for current thread
        currentSession.set(session);
        
        System.out.println("=== CUSTOMER SESSION CREATED ===");
        System.out.println("Token: " + session.sessionToken);
        System.out.println("ID: " + session.customerId);
        System.out.println("Name: " + session.customerName);
        System.out.println("Phone: " + session.customerPhone);
        System.out.println("Balance: " + session.creditBalance);
        System.out.println("===============================");
    }
    
    // Alternative method without balance
    public static void setCurrentCustomer(int id, String name, String phone) {
        setCurrentCustomer(id, name, phone, 0.0);
    }
    
    public static CustomerSession createSession(int id, String name, String phone, double balance) {
        setCurrentCustomer(id, name, phone, balance);
        return getCurrentSession();
    }
    
    public static CustomerSession getCurrentSession() {
        CustomerSession session = currentSession.get();
        if (session != null) {
            session.lastAccessTime = System.currentTimeMillis();
        }
        return session;
    }
    
    public static CustomerSession getSessionByToken(String token) {
        synchronized (activeSessions) {
            CustomerSession session = activeSessions.get(token);
            if (session != null && 
                System.currentTimeMillis() - session.lastAccessTime < 3600000) { // 1 hour timeout
                session.lastAccessTime = System.currentTimeMillis();
                currentSession.set(session);
                return session;
            }
            return null;
        }
    }
    
    public static boolean isLoggedIn() {
        CustomerSession session = getCurrentSession();
        boolean loggedIn = session != null && session.customerId > 0;
        
        System.out.println("=== CUSTOMER SESSION CHECK ===");
        System.out.println("Session exists: " + (session != null));
        if (session != null) {
            System.out.println("Customer ID: " + session.customerId);
            System.out.println("Customer Name: " + session.customerName);
            System.out.println("Session Token: " + session.sessionToken);
        }
        System.out.println("Is logged in: " + loggedIn);
        System.out.println("=============================");
        
        return loggedIn;
    }
    
    public static int getCustomerId() {
        CustomerSession session = getCurrentSession();
        int id = session != null ? session.customerId : 0;
        System.out.println("Getting customer ID: " + id);
        return id;
    }
    
    public static String getCustomerName() {
        CustomerSession session = getCurrentSession();
        String name = session != null ? session.customerName : "";
        System.out.println("Getting customer name: " + name);
        return name;
    }
    
    public static String getCustomerPhone() {
        CustomerSession session = getCurrentSession();
        String phone = session != null ? session.customerPhone : "";
        System.out.println("Getting customer phone: " + phone);
        return phone;
    }
    
    public static double getCreditBalance() {
        CustomerSession session = getCurrentSession();
        double balance = session != null ? session.creditBalance : 0.0;
        System.out.println("Getting customer balance: " + balance);
        return balance;
    }
    
    public static void clearSession() {
        CustomerSession session = currentSession.get();
        if (session != null) {
            System.out.println("Clearing session for: " + session.customerName);
            synchronized (activeSessions) {
                activeSessions.remove(session.sessionToken);
            }
        } else {
            System.out.println("No active session to clear");
        }
        currentSession.remove();
        
        // Also clean up old sessions (optional)
        cleanOldSessions();
    }
    
    private static void cleanOldSessions() {
        long now = System.currentTimeMillis();
        synchronized (activeSessions) {
            activeSessions.entrySet().removeIf(entry -> 
                now - entry.getValue().lastAccessTime > 3600000); // 1 hour
        }
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    // Helper methods for debugging
    public static void printActiveSessions() {
        System.out.println("=== ACTIVE CUSTOMER SESSIONS ===");
        synchronized (activeSessions) {
            for (Map.Entry<String, CustomerSession> entry : activeSessions.entrySet()) {
                CustomerSession session = entry.getValue();
                System.out.println("Token: " + entry.getKey() + 
                                 " | Customer: " + session.customerName + 
                                 " | Last Access: " + 
                                 (System.currentTimeMillis() - session.lastAccessTime) / 1000 + "s ago");
            }
            System.out.println("Total active sessions: " + activeSessions.size());
        }
        System.out.println("===============================");
    }
    
    // Test method
    public static void main(String[] args) {
        // Test the session
        setCurrentCustomer(1, "Test Customer", "0911111111", 500.0);
        
        System.out.println("Is logged in: " + isLoggedIn());
        System.out.println("Customer name: " + getCustomerName());
        System.out.println("Customer ID: " + getCustomerId());
        
        clearSession();
        
        System.out.println("After clear - Is logged in: " + isLoggedIn());
    }
}