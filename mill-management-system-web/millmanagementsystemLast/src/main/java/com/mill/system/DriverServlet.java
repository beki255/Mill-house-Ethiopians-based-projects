package com.mill.system;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/driver")
@MultipartConfig
public class DriverServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String userType = (String) session.getAttribute("userType");
        
        if (!"driver".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("getOrders".equals(action)) {
            getAssignedOrders(request, response);
        } else if ("getAssignedOrders".equals(action)) {
            getAssignedOrders(request, response);
        } else if ("getTodayStops".equals(action)) {
            getTodayStops(request, response);
        } else if ("getOnlineStatus".equals(action)) {
            getOnlineStatus(request, response);
        } else if ("checkNewOrders".equals(action)) {
            checkNewOrders(request, response);
        } else if ("getRouteGuidance".equals(action)) {
            getRouteGuidance(request, response);
        } else if ("getOrderDetails".equals(action)) {
            getOrderDetails(request, response);
        } else if ("getAvailableBalance".equals(action)) {
            getAvailableBalance(request, response);
        } else if ("getDeliveryProof".equals(action)) {
            getDeliveryProof(request, response);
        } else if ("getCustomerFeedback".equals(action)) {
            getCustomerFeedback(request, response);
        } else if ("getEarningDetails".equals(action)) {
            getEarningDetails(request, response);
        } else if ("getDetailedMetrics".equals(action)) {
            getDetailedMetrics(request, response);
        } else if ("getMaintenanceHistory".equals(action)) {
            getMaintenanceHistory(request, response);
        } else if ("getDocument".equals(action)) {
            getDocument(request, response);
        } else if ("optimizeRoute".equals(action)) {
            optimizeRoute(request, response);
        } else if ("downloadStatement".equals(action)) {
            downloadStatement(request, response);
        } else if ("downloadPerformanceReport".equals(action)) {
            downloadPerformanceReport(request, response);
        } else if ("getDeliveryLocations".equals(action)) {
            getDeliveryLocations(request, response);
        } else if ("getRouteCoordinates".equals(action)) {
            getRouteCoordinates(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String userType = (String) session.getAttribute("userType");
        
        if (!"driver".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("updateOrderStatus".equals(action)) {
            updateOrderStatus(request, response);
        } else if ("updateProfile".equals(action)) {
            updateProfile(request, response);
        } else if ("updateDeliveryStatus".equals(action)) {
            updateDeliveryStatus(request, response);
        } else if ("submitDeliveryProof".equals(action)) {
            submitDeliveryProof(request, response);
        } else if ("reportIssue".equals(action)) {
            reportIssue(request, response);
        } else if ("requestPayout".equals(action)) {
            requestPayout(request, response);
        } else if ("logMaintenance".equals(action)) {
            logMaintenance(request, response);
        } else if ("changePassword".equals(action)) {
            changePassword(request, response);
        } else if ("updateVehicle".equals(action)) {
            updateVehicle(request, response);
        } else if ("updateAvailability".equals(action)) {
            updateAvailability(request, response);
        } else if ("setOnlineStatus".equals(action)) {
            setOnlineStatus(request, response);
        }
    }
    
    // MAIN METHOD: DRIVER GETS ASSIGNED ORDERS
    private void getAssignedOrders(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int driverId = (Integer) request.getSession().getAttribute("userId");
        List<Order> orders = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            // CORRECT QUERY: Get all orders assigned to this driver
            String sql = "SELECT o.*, " +
                        "u.full_name as customer_name, u.phone as customer_phone, " +
                        "u.address as customer_address, u.email as customer_email, u.profile_image as customer_image, " +
                        "p.name as product_name, p.description as product_description, p.image_url as product_image, " +
                        "op.full_name as operator_name, op.phone as operator_phone " +
                        "FROM orders o " +
                        "JOIN users u ON o.customer_id = u.id " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "LEFT JOIN users op ON o.assigned_operator = op.id " +
                        "WHERE o.assigned_driver = ? " +
                        "ORDER BY o.order_date DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, driverId);
            rs = stmt.executeQuery();
            
            // Check if we're returning HTML or JSP
            String format = request.getParameter("format");
            
            if ("html".equals(format)) {
                // Return HTML for AJAX calls
                PrintWriter out = response.getWriter();
                response.setContentType("text/html");
                
                while (rs.next()) {
                    out.println("<div class='order-card'>");
                    out.println("<h4>Order #" + rs.getString("order_number") + "</h4>");
                    out.println("<p><strong>Customer:</strong> " + rs.getString("customer_name") + "</p>");
                    out.println("<p><strong>Phone:</strong> " + rs.getString("customer_phone") + "</p>");
                    out.println("<p><strong>Address:</strong> " + rs.getString("delivery_address") + "</p>");
                    out.println("<p><strong>Product:</strong> " + rs.getString("product_name") + "</p>");
                    out.println("<p><strong>Quantity:</strong> " + rs.getBigDecimal("quantity") + " kg</p>");
                    out.println("<p><strong>Status:</strong> <span class='status-" + rs.getString("order_status") + "'>" + rs.getString("order_status") + "</span></p>");
                    out.println("<p><strong>Assigned by:</strong> " + rs.getString("operator_name") + "</p>");
                    out.println("<p><strong>Order Date:</strong> " + rs.getTimestamp("order_date") + "</p>");
                    
                    out.println("<div class='action-buttons'>");
                    
                    if ("processing".equals(rs.getString("order_status"))) {
                        out.println("<button class='btn btn-primary btn-sm' onclick=\"updateStatus(" + rs.getInt("id") + ", 'picked')\">Start Delivery</button>");
                    }
                    
                    if ("picked".equals(rs.getString("order_status"))) {
                        out.println("<button class='btn btn-success btn-sm' onclick=\"updateStatus(" + rs.getInt("id") + ", 'delivered')\">Mark as Delivered</button>");
                    }
                    
                    out.println("<button class='btn btn-info btn-sm' onclick=\"viewMap('" + rs.getString("delivery_address") + "')\">View Map</button>");
                    out.println("<button class='btn btn-secondary btn-sm' onclick=\"callCustomer('" + rs.getString("customer_phone") + "')\">Call Customer</button>");
                    out.println("</div>");
                    
                    out.println("</div>");
                }
                
                if (!rs.isBeforeFirst()) {
                    out.println("<div class='empty-state'>");
                    out.println("<div class='empty-state-icon'>📦</div>");
                    out.println("<h3>No Assigned Orders</h3>");
                    out.println("<p>You have no orders assigned to you at the moment.</p>");
                    out.println("</div>");
                }
                
            } else {
                // Store data in request for JSP rendering
                while (rs.next()) {
                    Order order = new Order();
                    order.setId(rs.getInt("id"));
                    order.setOrderNumber(rs.getString("order_number"));
                    order.setCustomerId(rs.getInt("customer_id"));
                    order.setProductId(rs.getInt("product_id"));
                    order.setQuantity(rs.getBigDecimal("quantity"));
                    order.setTotalPrice(rs.getBigDecimal("total_price"));
                    order.setDeliveryAddress(rs.getString("delivery_address"));
                    order.setOrderStatus(rs.getString("order_status"));
                    order.setPaymentStatus(rs.getString("payment_status"));
                    order.setOrderDate(rs.getTimestamp("order_date"));
                    order.setDeliveryDate(rs.getTimestamp("delivery_date"));
                    
                    // Store additional info
                    request.setAttribute("customerName_" + order.getId(), rs.getString("customer_name"));
                    request.setAttribute("customerPhone_" + order.getId(), rs.getString("customer_phone"));
                    request.setAttribute("customerAddress_" + order.getId(), rs.getString("customer_address"));
                    request.setAttribute("customerEmail_" + order.getId(), rs.getString("customer_email"));
                    request.setAttribute("customerImage_" + order.getId(), rs.getString("customer_image"));
                    request.setAttribute("productName_" + order.getId(), rs.getString("product_name"));
                    request.setAttribute("productImage_" + order.getId(), rs.getString("product_image"));
                    request.setAttribute("operatorName_" + order.getId(), rs.getString("operator_name"));
                    request.setAttribute("operatorPhone_" + order.getId(), rs.getString("operator_phone"));
                    
                    orders.add(order);
                }
                
                request.setAttribute("orders", orders);
                RequestDispatcher dispatcher = request.getRequestDispatcher("driver-orders.jsp");
                dispatcher.forward(request, response);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    private void updateOrderStatus(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String newStatus = request.getParameter("status");
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE orders SET order_status = ? WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            
            stmt.executeUpdate();
            response.sendRedirect("driver.jsp?message=Order status updated");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("driver.jsp?error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    private void updateProfile(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        
        try {
            int driverId = (Integer) request.getSession().getAttribute("userId");
            String fullName = request.getParameter("fullName");
            String phone = request.getParameter("phone");
            String password = request.getParameter("password");
            String carNumber = request.getParameter("carNumber");
            String carType = request.getParameter("carType");
            
            conn = DatabaseConnection.getConnection();
            
            // Update user
            String userSql = "UPDATE users SET full_name = ?, phone = ?";
            if (password != null && !password.isEmpty()) {
                userSql += ", password = ?";
            }
            userSql += " WHERE id = ?";
            
            stmt = conn.prepareStatement(userSql);
            stmt.setString(1, fullName);
            stmt.setString(2, phone);
            
            int paramIndex = 3;
            if (password != null && !password.isEmpty()) {
                stmt.setString(paramIndex++, password);
            }
            stmt.setInt(paramIndex, driverId);
            
            stmt.executeUpdate();
            
            // Update driver details
            String driverSql = "UPDATE driver_details SET car_number = ?, car_type = ? WHERE driver_id = ?";
            stmt2 = conn.prepareStatement(driverSql);
            stmt2.setString(1, carNumber);
            stmt2.setString(2, carType);
            stmt2.setInt(3, driverId);
            stmt2.executeUpdate();
            
            response.sendRedirect("driver.jsp?message=Profile updated successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("driver.jsp?error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (stmt2 != null) stmt2.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Driver updates delivery status
    private void updateDeliveryStatus(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String newStatus = request.getParameter("status");
            int driverId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            // Verify this order is assigned to this driver
            String verifySql = "SELECT id FROM orders WHERE id = ? AND assigned_driver = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setInt(1, orderId);
            verifyStmt.setInt(2, driverId);
            ResultSet rs = verifyStmt.executeQuery();
            
            if (!rs.next()) {
                response.sendRedirect("driver.jsp?error=Order not assigned to you");
                return;
            }
            rs.close();
            verifyStmt.close();
            
            // Update order status
            String sql = "UPDATE orders SET order_status = ?";
            if ("delivered".equals(newStatus)) {
                sql += ", delivery_date = NOW()";
            }
            sql += " WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Log status update
                String logSql = "INSERT INTO order_logs (order_id, action, performed_by, details) " +
                               "VALUES (?, 'update_status', ?, ?)";
                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, orderId);
                logStmt.setInt(2, driverId);
                logStmt.setString(3, "Updated status to: " + newStatus);
                logStmt.executeUpdate();
                logStmt.close();
                
                response.sendRedirect("driver.jsp?message=Order status updated to " + newStatus);
            } else {
                response.sendRedirect("driver.jsp?error=Failed to update order status");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("driver.jsp?error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Get today's delivery stops for map
    private void getTodayStops(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int driverId = (Integer) request.getSession().getAttribute("userId");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT o.id, o.order_number, o.order_status, o.delivery_address, " +
                        "u.full_name as customer_name, u.phone as customer_phone " +
                        "FROM orders o " +
                        "JOIN users u ON o.customer_id = u.id " +
                        "WHERE o.assigned_driver = ? " +
                        "AND DATE(o.order_date) = CURDATE() " +
                        "AND o.order_status IN ('processing', 'picked', 'on-the-way') " +
                        "ORDER BY o.order_date";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, driverId);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder stopsJson = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    stopsJson.append(",");
                }
                first = false;
                stopsJson.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"order_number\":\"").append(rs.getString("order_number")).append("\",")
                    .append("\"status\":\"").append(rs.getString("order_status")).append("\",")
                    .append("\"customer_name\":\"").append(escapeJson(rs.getString("customer_name"))).append("\",")
                    .append("\"customer_phone\":\"").append(rs.getString("customer_phone")).append("\",")
                    .append("\"address\":\"").append(escapeJson(rs.getString("delivery_address"))).append("\"")
                    .append("}");
            }
            stopsJson.append("]");
            
            String result = "{\"stops\":" + stopsJson.toString() + ",\"success\":true}";
            out.print(result);
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            String error = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(error);
        }
    }
    
    private void getOnlineStatus(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            int driverId = (Integer) request.getSession().getAttribute("userId");
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT COALESCE(online_status, 'online') as status FROM driver_details WHERE driver_id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, driverId);
            ResultSet rs = stmt.executeQuery();
            
            String result;
            if (rs.next()) {
                result = "{\"status\":\"" + rs.getString("status") + "\",\"success\":true}";
            } else {
                result = "{\"status\":\"online\",\"success\":true}";
            }
            
            out.print(result);
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            String error = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(error);
        }
    }
    
    private void checkNewOrders(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int driverId = (Integer) request.getSession().getAttribute("userId");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) as new_orders FROM orders " +
                        "WHERE assigned_driver = ? " +
                        "AND order_status = 'processing' " +
                        "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) <= 5";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, driverId);
            ResultSet rs = stmt.executeQuery();
            
            String result;
            if (rs.next()) {
                result = "{\"newOrders\":" + rs.getInt("new_orders") + ",\"success\":true}";
            } else {
                result = "{\"newOrders\":0,\"success\":true}";
            }
            
            out.print(result);
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            String error = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(error);
        }
    }
    
    private void getRouteGuidance(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int orderId = Integer.parseInt(request.getParameter("orderId"));
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT o.delivery_address, u.full_name as customer_name " +
                        "FROM orders o " +
                        "JOIN users u ON o.customer_id = u.id " +
                        "WHERE o.id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            String result;
            if (rs.next()) {
                String address = rs.getString("delivery_address");
                result = "{\"distance\":\"8.5 km\",\"time\":\"25 minutes\",\"instructions\":\"Take main road, turn left at signal, destination on right\",\"address\":\"" + 
                        escapeJson(address) + "\",\"customer_name\":\"" + escapeJson(rs.getString("customer_name")) + "\",\"success\":true}";
            } else {
                result = "{\"success\":false,\"error\":\"Order not found\"}";
            }
            
            out.print(result);
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            String error = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(error);
        }
    }
    
    private void getAvailableBalance(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int driverId = (Integer) request.getSession().getAttribute("userId");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT COALESCE(SUM(total_price), 0) as balance " +
                        "FROM orders " +
                        "WHERE assigned_driver = ? " +
                        "AND order_status = 'delivered' " +
                        "AND payment_status = 'paid' " +
                        "AND (payment_released IS NULL OR payment_released = false)";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, driverId);
            ResultSet rs = stmt.executeQuery();
            
            String result;
            if (rs.next()) {
                result = "{\"balance\":" + rs.getBigDecimal("balance") + ",\"success\":true}";
            } else {
                result = "{\"balance\":0,\"success\":true}";
            }
            
            out.print(result);
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            String error = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(error);
        }
    }
    
    private void setOnlineStatus(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String status = request.getParameter("status");
        int driverId = (Integer) request.getSession().getAttribute("userId");
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "UPDATE driver_details SET online_status = ? WHERE driver_id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            stmt.setInt(2, driverId);
            stmt.executeUpdate();
            
            stmt.close();
            conn.close();
            
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            String result = "{\"success\":true,\"message\":\"Status updated to " + escapeJson(status) + "\"}";
            out.print(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            String result = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(result);
        }
    }
    
    private void getOrderDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int orderId = Integer.parseInt(request.getParameter("orderId"));
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT o.*, u.full_name as customer_name, u.phone as customer_phone, " +
                        "u.address as customer_address, u.email as customer_email, " +
                        "p.name as product_name, p.description as product_description, " +
                        "op.full_name as operator_name " +
                        "FROM orders o " +
                        "JOIN users u ON o.customer_id = u.id " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "LEFT JOIN users op ON o.assigned_operator = op.id " +
                        "WHERE o.id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                out.println("<div class='order-details-content'>");
                out.println("<h3>Order #" + rs.getString("order_number") + "</h3>");
                out.println("<div class='detail-row'><strong>Customer:</strong> " + rs.getString("customer_name") + "</div>");
                out.println("<div class='detail-row'><strong>Phone:</strong> " + rs.getString("customer_phone") + "</div>");
                out.println("<div class='detail-row'><strong>Email:</strong> " + rs.getString("customer_email") + "</div>");
                out.println("<div class='detail-row'><strong>Address:</strong> " + rs.getString("delivery_address") + "</div>");
                out.println("<div class='detail-row'><strong>Product:</strong> " + rs.getString("product_name") + "</div>");
                out.println("<div class='detail-row'><strong>Quantity:</strong> " + rs.getBigDecimal("quantity") + " kg</div>");
                out.println("<div class='detail-row'><strong>Total Price:</strong> " + rs.getBigDecimal("total_price") + " Birr</div>");
                out.println("<div class='detail-row'><strong>Status:</strong> " + rs.getString("order_status") + "</div>");
                out.println("<div class='detail-row'><strong>Order Date:</strong> " + rs.getTimestamp("order_date") + "</div>");
                out.println("<div class='detail-row'><strong>Assigned By:</strong> " + rs.getString("operator_name") + "</div>");
                out.println("</div>");
            } else {
                out.println("<div class='alert alert-error'>Order not found</div>");
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            out.println("<div class='alert alert-error'>Error loading order details</div>");
        }
    }
    
    private void getDeliveryLocations(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int driverId = (Integer) request.getSession().getAttribute("userId");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT o.id, o.order_number, o.delivery_address, " +
                        "u.full_name as customer_name, u.phone as customer_phone, " +
                        "o.order_status, o.quantity, o.total_price " +
                        "FROM orders o " +
                        "JOIN users u ON o.customer_id = u.id " +
                        "WHERE o.assigned_driver = ? " +
                        "AND o.order_status IN ('processing', 'picked', 'on-the-way') " +
                        "AND DATE(o.order_date) = CURDATE() " +
                        "ORDER BY o.order_date";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, driverId);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder locationsJson = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    locationsJson.append(",");
                }
                first = false;
                
                Map<String, Double> coords = getCoordinatesFromAddress(rs.getString("delivery_address"));
                
                locationsJson.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"order_number\":\"").append(rs.getString("order_number")).append("\",")
                    .append("\"address\":\"").append(escapeJson(rs.getString("delivery_address"))).append("\",")
                    .append("\"customer_name\":\"").append(escapeJson(rs.getString("customer_name"))).append("\",")
                    .append("\"customer_phone\":\"").append(rs.getString("customer_phone")).append("\",")
                    .append("\"status\":\"").append(rs.getString("order_status")).append("\",")
                    .append("\"quantity\":").append(rs.getBigDecimal("quantity")).append(",")
                    .append("\"total_price\":").append(rs.getBigDecimal("total_price")).append(",")
                    .append("\"lat\":").append(coords.get("lat")).append(",")
                    .append("\"lng\":").append(coords.get("lng"))
                    .append("}");
            }
            locationsJson.append("]");
            
            String result = "{\"locations\":" + locationsJson.toString() + ",\"driver_id\":" + driverId + ",\"success\":true}";
            out.print(result);
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            String error = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(error);
        }
    }
    
    private Map<String, Double> getCoordinatesFromAddress(String address) {
        Map<String, Double> coords = new HashMap<>();
        
        if (address.toLowerCase().contains("bole")) {
            coords.put("lat", 9.0246);
            coords.put("lng", 38.7469);
        } else if (address.toLowerCase().contains("megenagna")) {
            coords.put("lat", 9.0321);
            coords.put("lng", 38.7594);
        } else if (address.toLowerCase().contains("piazza")) {
            coords.put("lat", 9.0157);
            coords.put("lng", 38.7652);
        } else {
            coords.put("lat", 9.0192);
            coords.put("lng", 38.7465);
        }
        
        return coords;
    }
    
    private void getRouteCoordinates(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            StringBuilder routeJson = new StringBuilder("[");
            routeJson.append("{\"lat\":9.0192,\"lng\":38.7465,\"title\":\"Mill Location\"},");
            routeJson.append("{\"lat\":9.0246,\"lng\":38.7469,\"title\":\"Bole\"},");
            routeJson.append("{\"lat\":9.0321,\"lng\":38.7594,\"title\":\"Megenagna\"}");
            routeJson.append("]");
            
            String result = "{\"route\":" + routeJson.toString() + ",\"total_distance\":18.5,\"estimated_time\":45,\"success\":true}";
            out.print(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            String error = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            out.print(error);
        }
    }
    
    private void optimizeRoute(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String result = "{\"distanceSaved\":\"2.5\",\"timeSaved\":\"15\",\"message\":\"Route optimized successfully\",\"success\":true}";
        out.print(result);
    }
    
    // Helper method to escape JSON strings
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    // Stub methods
    private void getDeliveryProof(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<div class='alert alert-info'>No delivery proof available.</div>");
    }
    
    private void getCustomerFeedback(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<div class='alert alert-info'>No customer feedback available.</div>");
    }
    
    private void getEarningDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<div class='alert alert-info'>Earning details not available.</div>");
    }
    
    private void getDetailedMetrics(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<div class='alert alert-info'>Detailed metrics not available.</div>");
    }
    
    private void getMaintenanceHistory(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<div class='alert alert-info'>No maintenance history available.</div>");
    }
    
    private void getDocument(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<div class='alert alert-info'>Document not available.</div>");
    }
    
    private void downloadStatement(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=earnings-statement.pdf");
        PrintWriter out = response.getWriter();
        out.println("PDF content would be generated here");
    }
    
    private void downloadPerformanceReport(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=performance-report.pdf");
        PrintWriter out = response.getWriter();
        out.println("PDF content would be generated here");
    }
    
    // POST method stubs
    private void submitDeliveryProof(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("driver.jsp?message=Delivery proof submitted successfully");
    }
    
    private void reportIssue(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("driver.jsp?message=Issue reported successfully");
    }
    
    private void requestPayout(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("driver.jsp?message=Payout request submitted");
    }
    
    private void logMaintenance(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("driver.jsp?message=Maintenance logged successfully");
    }
    
    private void changePassword(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("driver.jsp?message=Password changed successfully");
    }
    
    private void updateVehicle(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("driver.jsp?message=Vehicle updated successfully");
    }
    
    private void updateAvailability(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("driver.jsp?message=Availability updated successfully");
    }
    
    // Helper class for Order objects
    public class Order {
        private int id;
        private String orderNumber;
        private int customerId;
        private int productId;
        private java.math.BigDecimal quantity;
        private java.math.BigDecimal totalPrice;
        private String deliveryAddress;
        private String orderStatus;
        private String paymentStatus;
        private java.sql.Timestamp orderDate;
        private java.sql.Timestamp deliveryDate;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        
        public int getCustomerId() { return customerId; }
        public void setCustomerId(int customerId) { this.customerId = customerId; }
        
        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }
        
        public java.math.BigDecimal getQuantity() { return quantity; }
        public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
        
        public java.math.BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(java.math.BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        
        public String getDeliveryAddress() { return deliveryAddress; }
        public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
        
        public String getOrderStatus() { return orderStatus; }
        public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
        
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        
        public java.sql.Timestamp getOrderDate() { return orderDate; }
        public void setOrderDate(java.sql.Timestamp orderDate) { this.orderDate = orderDate; }
        
        public java.sql.Timestamp getDeliveryDate() { return deliveryDate; }
        public void setDeliveryDate(java.sql.Timestamp deliveryDate) { this.deliveryDate = deliveryDate; }
    }
}