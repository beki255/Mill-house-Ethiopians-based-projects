package com.mill.system;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.util.UUID;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        System.out.println("AuthServlet: Action = " + action);
        
        if ("login".equals(action)) {
            loginUser(request, response);
        } else if ("register".equals(action)) {
            registerUser(request, response);
        } else if ("forgot-password".equals(action)) {
            handleForgotPassword(request, response);
        }
    }
    
    // User login method
    private void loginUser(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            
            System.out.println("Login attempt: email = " + email);
            
            User user = authenticateUser(email, password);
            
            if (user != null) {
                System.out.println("User authenticated: " + user.getUsername() + ", type: " + user.getUserType());
                
                // Create HTTP session
                HttpSession session = request.getSession();
                
                // Set individual attributes (for convenience)
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("fullName", user.getFullName());
                session.setAttribute("userType", user.getUserType());
                session.setAttribute("email", user.getEmail());
                
                // CRITICAL: Set the FULL User object
                session.setAttribute("user", user);
                
                // Set session timeout (30 minutes)
                session.setMaxInactiveInterval(30 * 60);
                
                System.out.println("Session created: " + session.getId());
                System.out.println("Full User object stored in session: " + user);
                
                // Redirect based on user type
                String redirectUrl = "";
                switch(user.getUserType()) {
                    case "admin":
                        redirectUrl = "admin.jsp";
                        break;
                    case "customer":
                        redirectUrl = "customer.jsp";
                        break;
                    case "operator":
                        redirectUrl = "operator.jsp";
                        break;
                    case "driver":
                        redirectUrl = "driver.jsp";
                        break;
                    default:
                        redirectUrl = "index.html?error=Invalid user type";
                }
                
                System.out.println("Redirecting to: " + redirectUrl);
                response.sendRedirect(redirectUrl);
            } else {
                System.out.println("Authentication failed for: " + email);
                response.sendRedirect("index.html?error=Invalid email or password");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("index.html?error=Database error: " + e.getMessage());
        }
    }
    
    // User registration method
    private void registerUser(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String username = request.getParameter("username");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String fullName = request.getParameter("fullName");
            String phone = request.getParameter("phone");
            String userType = "customer"; // Default user type for registration
            String address = request.getParameter("address");
            
            // Hash the password for security
            String hashedPassword = hashPassword(password);
            
            conn = DatabaseConnection.getConnection();
            
            // Check if email already exists
            String checkEmailSql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkEmailSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                response.sendRedirect("register.html?error=Email already registered");
                return;
            }
            rs.close();
            checkStmt.close();
            
            // Insert new user into database
            String sql = "INSERT INTO users (username, email, password, full_name, phone, user_type, address, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 'active')";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, fullName);
            stmt.setString(5, phone);
            stmt.setString(6, userType);
            stmt.setString(7, address);
            
            int rowsInserted = stmt.executeUpdate();
            
            if (rowsInserted > 0) {
                // Automatically log in the user after registration
                User user = authenticateUser(email, password);
                if (user != null) {
                    HttpSession session = request.getSession();
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("fullName", user.getFullName());
                    session.setAttribute("userType", user.getUserType());
                    session.setAttribute("email", user.getEmail());
                    session.setAttribute("user", user);
                    session.setMaxInactiveInterval(30 * 60);
                    
                    response.sendRedirect("customer.jsp?message=Registration successful");
                } else {
                    response.sendRedirect("index.html?message=Registration successful. Please login.");
                }
            } else {
                response.sendRedirect("register.html?error=Registration failed");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("register.html?error=Database error: " + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Handle forgot password request from customer
    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String email = request.getParameter("email");
            
            conn = DatabaseConnection.getConnection();
            
            // Find customer by email
            String findUserSql = "SELECT id FROM users WHERE email = ? AND user_type = 'customer'";
            PreparedStatement findStmt = conn.prepareStatement(findUserSql);
            findStmt.setString(1, email);
            ResultSet rs = findStmt.executeQuery();
            
            if (!rs.next()) {
                response.sendRedirect("index.html?error=Email not found");
                return;
            }
            
            int customerId = rs.getInt("id");
            rs.close();
            findStmt.close();
            
            // Check if there's already a pending request
            String checkSql = "SELECT id FROM password_requests WHERE customer_id = ? AND status = 'pending'";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, customerId);
            rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                response.sendRedirect("index.html?message=Password reset request already submitted. Please wait for admin approval.");
                return;
            }
            rs.close();
            checkStmt.close();
            
            // Create password reset request
            String sql = "INSERT INTO password_requests (customer_id, status) VALUES (?, 'pending')";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerId);
            stmt.executeUpdate();
            
            response.sendRedirect("index.html?message=Password reset request submitted. Admin will contact you soon.");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("index.html?error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Authenticate user credentials
    private User authenticateUser(String email, String password) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE email = ? AND status = 'active'";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String hashedInput = hashPassword(password);
                
                System.out.println("Stored password hash: " + storedPassword);
                System.out.println("Input password hash: " + hashedInput);
                
                // For testing, you can use plain text comparison temporarily
                // In production, always use proper password hashing
                if (storedPassword.equals(hashedInput) || storedPassword.equals(password)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));
                    user.setPhone(rs.getString("phone"));
                    user.setUserType(rs.getString("user_type"));
                    user.setStatus(rs.getString("status"));
                    user.setAddress(rs.getString("address"));
                    return user;
                }
            }
            return null;
            
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password; // fallback for debugging
        }
    }
    
    // Helper method for logout (can be called from other servlets)
    public static void logoutUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
    
    // Helper method to check if user is authenticated
    public static boolean isAuthenticated(HttpServletRequest request, String requiredUserType) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        
        String userType = (String) session.getAttribute("userType");
        return userType != null && userType.equals(requiredUserType);
    }
    
    // Helper method to get current user from session
    public static User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}

 