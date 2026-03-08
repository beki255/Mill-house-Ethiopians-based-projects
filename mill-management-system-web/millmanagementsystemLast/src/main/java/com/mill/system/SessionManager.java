package com.mill.system;

import jakarta.servlet.http.*;
import java.sql.*;
import java.util.UUID;

public class SessionManager {
    
    public static String createSession(HttpServletRequest request, HttpServletResponse response, User user) 
            throws SQLException {
        
        // Generate unique session ID
        String sessionId = UUID.randomUUID().toString();
        
        // Store in database
        Connection conn = DatabaseConnection.getConnection();
        String sql = "INSERT INTO user_sessions (user_id, session_id, ip_address, user_agent) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, user.getId());
        stmt.setString(2, sessionId);
        stmt.setString(3, request.getRemoteAddr());
        stmt.setString(4, request.getHeader("User-Agent"));
        stmt.executeUpdate();
        
        stmt.close();
        conn.close();
        
        // Create HTTP session
        HttpSession session = request.getSession();
        session.setAttribute("sessionId", sessionId);
        session.setAttribute("user", user);
        session.setAttribute("userType", user.getUserType());
        session.setAttribute("userId", user.getId());
        
        // Set session timeout (30 minutes)
        session.setMaxInactiveInterval(30 * 60);
        
        return sessionId;
    }
    
    public static boolean validateSession(HttpServletRequest request) throws SQLException {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String sessionId = (String) session.getAttribute("sessionId");
        if (sessionId == null) return false;
        
        // Check in database
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT * FROM user_sessions WHERE session_id = ? AND is_active = TRUE";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, sessionId);
        ResultSet rs = stmt.executeQuery();
        
        boolean isValid = rs.next();
        
        rs.close();
        stmt.close();
        conn.close();
        
        return isValid;
    }
    
    public static void invalidateSession(HttpServletRequest request) throws SQLException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionId = (String) session.getAttribute("sessionId");
            
            if (sessionId != null) {
                // Mark as inactive in database
                Connection conn = DatabaseConnection.getConnection();
                String sql = "UPDATE user_sessions SET is_active = FALSE, logout_time = NOW() WHERE session_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, sessionId);
                stmt.executeUpdate();
                stmt.close();
                conn.close();
            }
            
            // Invalidate HTTP session
            session.invalidate();
        }
    }
    
    public static void cleanupExpiredSessions() throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "UPDATE user_sessions SET is_active = FALSE WHERE login_time < DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
        conn.close();
    }
}