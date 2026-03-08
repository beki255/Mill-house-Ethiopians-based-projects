package com.mill.system;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.*;
import java.util.*;

@WebServlet("/operator")
@MultipartConfig
public class OperatorServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String userType = (String) session.getAttribute("userType");
        
        if (!"operator".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("getOrders".equals(action)) {
            getOrders(request, response);
        } else if ("getAvailableDrivers".equals(action)) {
            getAvailableDrivers(request, response);
        } else if ("getOrderDetails".equals(action)) {
            getOrderDetails(request, response);
        } else if ("getOrderTimeline".equals(action)) {
            getOrderTimeline(request, response);
        } else if ("getStats".equals(action)) {
            getDashboardStats(request, response);
        } else if ("getProducts".equals(action)) {
            getProducts(request, response);
        } else if ("getCustomers".equals(action)) {
            getCustomers(request, response);
        } else if ("getNotifications".equals(action)) {
            getNotifications(request, response);
        } else if ("getNotificationCount".equals(action)) {
            getNotificationCount(request, response);
        } else if ("getOfflineCount".equals(action)) {
            getOfflineCount(request, response);
        } else if ("getReportData".equals(action)) {
            getReportData(request, response);
        } else if ("checkNewOrders".equals(action)) {
            checkNewOrders(request, response);
        } else if ("getReceipt".equals(action)) {
            getReceipt(request, response);
        } else if ("getPendingOrders".equals(action)) {
            getPendingOrders(request, response);
        } else if ("getDriverPerformance".equals(action)) {
            getDriverPerformance(request, response);
        } else if ("getStockHistory".equals(action)) {
            getStockHistory(request, response);
        } else if ("getCurrentStock".equals(action)) {
            getCurrentStock(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String userType = (String) session.getAttribute("userType");
        
        if (!"operator".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        int operatorId = (Integer) session.getAttribute("userId");
        
        if ("updateOrderStatus".equals(action)) {
            updateOrderStatus(request, response, operatorId);
        } else if ("assignDriver".equals(action)) {
            assignDriver(request, response, operatorId);
        } else if ("processOfflineOrder".equals(action)) {
            processOfflineOrder(request, response, operatorId);
        } else if ("updateProfile".equals(action)) {
            updateProfile(request, response);
        } else if ("updatePaymentStatus".equals(action)) {
            updatePaymentStatus(request, response, operatorId);
        } else if ("addOrderNotes".equals(action)) {
            addOrderNotes(request, response, operatorId);
        } else if ("assignToMe".equals(action)) {
            assignToMe(request, response, operatorId);
        } else if ("markPaymentPaid".equals(action)) {
            markPaymentPaid(request, response, operatorId);
        } else if ("voidOrder".equals(action)) {
            voidOrder(request, response, operatorId);
        } else if ("toggleAvailability".equals(action)) {
            toggleAvailability(request, response);
        } else if ("updateStock".equals(action)) {
            updateStock(request, response, operatorId);
        } else if ("changePassword".equals(action)) {
            changePassword(request, response, operatorId);
        } else if ("updateWorkingHours".equals(action)) {
            updateWorkingHours(request, response, operatorId);
        } else if ("createOfflineOrder".equals(action)) {
            createOfflineOrder(request, response, operatorId);
        }
    }
    
    // OPERATOR SEES ORDERS - Fetch from database
    private void getOrders(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String statusFilter = request.getParameter("status");
            if (statusFilter == null) statusFilter = "pending";
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT o.*, " +
                        "u.full_name as customer_name, u.phone as customer_phone, u.address as customer_address, " +
                        "p.name as product_name, p.image_url, p.sell_price, p.milling_price, " +
                        "op.full_name as operator_name, " +
                        "d.full_name as driver_name, d.phone as driver_phone, " +
                        "dd.car_number, dd.car_type, dd.license_number " +
                        "FROM orders o " +
                        "LEFT JOIN users u ON o.customer_id = u.id " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "LEFT JOIN users op ON o.assigned_operator = op.id " +
                        "LEFT JOIN users d ON o.assigned_driver = d.id " +
                        "LEFT JOIN driver_details dd ON d.id = dd.driver_id ";
            
            if (!statusFilter.equals("all")) {
                sql += "WHERE o.order_status = ? ";
            }
            
            sql += "ORDER BY o.order_date DESC";
            
            stmt = conn.prepareStatement(sql);
            if (!statusFilter.equals("all")) {
                stmt.setString(1, statusFilter);
            }
            
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td>" + rs.getString("order_number") + "</td>");
                out.println("<td>");
                out.println("<div class='customer-info'>");
                out.println("<strong>" + rs.getString("customer_name") + "</strong><br>");
                out.println("<small>" + rs.getString("customer_phone") + "</small><br>");
                out.println("<small>" + rs.getString("customer_address") + "</small>");
                out.println("</div>");
                out.println("</td>");
                out.println("<td>");
                out.println("<div class='product-info'>");
                out.println("<img src='" + rs.getString("image_url") + "' width='40' height='40'>");
                out.println("<span>" + rs.getString("product_name") + "</span>");
                out.println("</div>");
                out.println("</td>");
                out.println("<td>" + rs.getBigDecimal("quantity") + " kg</td>");
                out.println("<td>" + rs.getBigDecimal("total_price") + " Birr</td>");
                out.println("<td>");
                out.println("<span class='status-badge status-" + rs.getString("order_status") + "'>");
                out.println(rs.getString("order_status").toUpperCase());
                out.println("</span>");
                out.println("</td>");
                out.println("<td>");
                out.println("<span class='payment-status payment-" + rs.getString("payment_status") + "'>");
                out.println(rs.getString("payment_status").toUpperCase());
                out.println("</span>");
                out.println("</td>");
                out.println("<td>" + rs.getTimestamp("order_date") + "</td>");
                out.println("<td>");
                out.println("<div class='action-buttons'>");
                
                // Status update buttons
                String currentStatus = rs.getString("order_status");
                if ("pending".equals(currentStatus)) {
                    out.println("<button class='btn btn-success btn-sm' onclick=\"updateOrderStatus(" + rs.getInt("id") + ", 'processing')\">Start Processing</button>");
                } else if ("processing".equals(currentStatus)) {
                    out.println("<button class='btn btn-primary btn-sm' onclick=\"assignDriver(" + rs.getInt("id") + ")\">Assign Driver</button>");
                } else if ("assigned".equals(currentStatus)) {
                    out.println("<button class='btn btn-info btn-sm' onclick=\"updateOrderStatus(" + rs.getInt("id") + ", 'out_for_delivery')\">Out for Delivery</button>");
                } else if ("out_for_delivery".equals(currentStatus)) {
                    out.println("<button class='btn btn-success btn-sm' onclick=\"updateOrderStatus(" + rs.getInt("id") + ", 'delivered')\">Mark Delivered</button>");
                }
                
                // Payment status buttons
                if ("pending".equals(rs.getString("payment_status"))) {
                    out.println("<button class='btn btn-warning btn-sm' onclick=\"updatePaymentStatus(" + rs.getInt("id") + ", 'paid')\">Mark Paid</button>");
                }
                
                // View details button
                out.println("<button class='btn btn-info btn-sm' onclick=\"viewOrderDetails(" + rs.getInt("id") + ")\">View</button>");
                
                // Add notes button
                out.println("<button class='btn btn-secondary btn-sm' onclick=\"addOrderNotes(" + rs.getInt("id") + ")\">Add Notes</button>");
                
                out.println("</div>");
                out.println("</td>");
                out.println("</tr>");
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
    
    // GET AVAILABLE DRIVERS
    private void getAvailableDrivers(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            
            String sql = "SELECT u.id, u.full_name, u.phone, dd.car_number, dd.car_type, " +
                        "(SELECT COUNT(*) FROM orders WHERE assigned_driver = u.id AND order_status IN ('assigned', 'out_for_delivery')) as active_orders " +
                        "FROM users u " +
                        "LEFT JOIN driver_details dd ON u.id = dd.driver_id " +
                        "WHERE u.user_type = 'driver' AND u.status = 'active' " +
                        "ORDER BY active_orders ASC";
            
            rs = stmt.executeQuery(sql);
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            out.println("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.println(",");
                first = false;
                out.println("{");
                out.println("\"id\": " + rs.getInt("id") + ",");
                out.println("\"full_name\": \"" + rs.getString("full_name") + "\",");
                out.println("\"phone\": \"" + rs.getString("phone") + "\",");
                out.println("\"car_number\": \"" + rs.getString("car_number") + "\",");
                out.println("\"car_type\": \"" + rs.getString("car_type") + "\",");
                out.println("\"active_orders\": " + rs.getInt("active_orders"));
                out.println("}");
            }
            out.println("]");
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // OPERATOR ASSIGNS DRIVER - Save to database
    private void assignDriver(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int driverId = Integer.parseInt(request.getParameter("driverId"));
            
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Verify order exists and is in processing status
            String verifySql = "SELECT id, customer_id FROM orders WHERE id = ? AND order_status = 'processing'";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setInt(1, orderId);
            ResultSet rs = verifyStmt.executeQuery();
            
            if (!rs.next()) {
                response.sendRedirect("operator.jsp?tab=orders&error=Order not found or not ready for driver assignment");
                return;
            }
            int customerId = rs.getInt("customer_id");
            rs.close();
            verifyStmt.close();
            
            // Verify driver exists and is active
            String driverSql = "SELECT id FROM users WHERE id = ? AND user_type = 'driver' AND status = 'active'";
            PreparedStatement driverStmt = conn.prepareStatement(driverSql);
            driverStmt.setInt(1, driverId);
            rs = driverStmt.executeQuery();
            
            if (!rs.next()) {
                response.sendRedirect("operator.jsp?tab=orders&error=Driver not found or not active");
                return;
            }
            rs.close();
            driverStmt.close();
            
            // Assign driver to order
            String updateSql = "UPDATE orders SET assigned_driver = ?, assigned_operator = ?, " +
                              "order_status = 'assigned', updated_at = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, driverId);
            stmt.setInt(2, operatorId);
            stmt.setInt(3, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Log the assignment
                String logSql = "INSERT INTO order_logs (order_id, customer_id, driver_id, operator_id, action, details) " +
                               "VALUES (?, ?, ?, ?, 'assign_driver', ?)";
                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, orderId);
                logStmt.setInt(2, customerId);
                logStmt.setInt(3, driverId);
                logStmt.setInt(4, operatorId);
                logStmt.setString(5, "Driver assigned to order #" + orderId);
                logStmt.executeUpdate();
                logStmt.close();
                
                conn.commit(); // Commit transaction
                
                response.sendRedirect("operator.jsp?tab=orders&message=Driver assigned successfully! Driver and customer have been notified.");
            } else {
                conn.rollback();
                response.sendRedirect("operator.jsp?tab=orders&error=Failed to assign driver");
            }
            
        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=orders&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // UPDATE ORDER STATUS
    private void updateOrderStatus(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String newStatus = request.getParameter("status");
            
            // Validate status transition
            if (!isValidStatusTransition(orderId, newStatus)) {
                response.sendRedirect("operator.jsp?tab=orders&error=Invalid status transition");
                return;
            }
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "UPDATE orders SET order_status = ?, assigned_operator = ?, updated_at = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, operatorId);
            stmt.setInt(3, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Log the status change
                String logSql = "INSERT INTO order_logs (order_id, operator_id, action, details) " +
                               "VALUES (?, ?, 'update_status', ?)";
                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, orderId);
                logStmt.setInt(2, operatorId);
                logStmt.setString(3, "Order status changed to: " + newStatus);
                logStmt.executeUpdate();
                logStmt.close();
                
                response.sendRedirect("operator.jsp?tab=orders&message=Order status updated to " + newStatus);
            } else {
                response.sendRedirect("operator.jsp?tab=orders&error=Order not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=orders&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // UPDATE PAYMENT STATUS
    private void updatePaymentStatus(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String paymentStatus = request.getParameter("status");
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE orders SET payment_status = ?, updated_at = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, paymentStatus);
            stmt.setInt(2, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Log the payment status change
                String logSql = "INSERT INTO payment_logs (order_id, operator_id, action, amount, details) " +
                               "VALUES (?, ?, 'update_payment', " +
                               "(SELECT total_price FROM orders WHERE id = ?), ?)";
                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, orderId);
                logStmt.setInt(2, operatorId);
                logStmt.setInt(3, orderId);
                logStmt.setString(4, "Payment status changed to: " + paymentStatus);
                logStmt.executeUpdate();
                logStmt.close();
                
                response.sendRedirect("operator.jsp?tab=orders&message=Payment status updated to " + paymentStatus);
            } else {
                response.sendRedirect("operator.jsp?tab=orders&error=Order not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=orders&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET ORDER DETAILS
    private void getOrderDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT o.*, " +
                        "u.full_name as customer_name, u.phone as customer_phone, u.email as customer_email, u.address as customer_address, " +
                        "p.name as product_name, p.image_url, p.description as product_description, " +
                        "p.sell_price, p.milling_price, p.min_quantity, " +
                        "op.full_name as operator_name, op.phone as operator_phone, " +
                        "d.full_name as driver_name, d.phone as driver_phone, " +
                        "dd.car_number, dd.car_type, dd.license_number " +
                        "FROM orders o " +
                        "LEFT JOIN users u ON o.customer_id = u.id " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "LEFT JOIN users op ON o.assigned_operator = op.id " +
                        "LEFT JOIN users d ON o.assigned_driver = d.id " +
                        "LEFT JOIN driver_details dd ON d.id = dd.driver_id " +
                        "WHERE o.id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            if (rs.next()) {
                out.println("<div class='order-details-container'>");
                out.println("<h3>Order #" + rs.getString("order_number") + "</h3>");
                out.println("<div class='detail-section'>");
                out.println("<h4>Customer Information</h4>");
                out.println("<p><strong>Name:</strong> " + rs.getString("customer_name") + "</p>");
                out.println("<p><strong>Phone:</strong> " + rs.getString("customer_phone") + "</p>");
                out.println("<p><strong>Email:</strong> " + rs.getString("customer_email") + "</p>");
                out.println("<p><strong>Address:</strong> " + rs.getString("customer_address") + "</p>");
                out.println("</div>");
                
                out.println("<div class='detail-section'>");
                out.println("<h4>Order Information</h4>");
                out.println("<p><strong>Product:</strong> " + rs.getString("product_name") + "</p>");
                out.println("<p><strong>Quantity:</strong> " + rs.getBigDecimal("quantity") + " kg</p>");
                out.println("<p><strong>Total Price:</strong> " + rs.getBigDecimal("total_price") + " Birr</p>");
                out.println("<p><strong>Order Status:</strong> <span class='status-badge status-" + rs.getString("order_status") + "'>" + 
                           rs.getString("order_status").toUpperCase() + "</span></p>");
                out.println("<p><strong>Payment Status:</strong> <span class='payment-status payment-" + rs.getString("payment_status") + "'>" + 
                           rs.getString("payment_status").toUpperCase() + "</span></p>");
                out.println("<p><strong>Order Date:</strong> " + rs.getTimestamp("order_date") + "</p>");
                out.println("</div>");
                
                if (rs.getString("driver_name") != null) {
                    out.println("<div class='detail-section'>");
                    out.println("<h4>Driver Information</h4>");
                    out.println("<p><strong>Name:</strong> " + rs.getString("driver_name") + "</p>");
                    out.println("<p><strong>Phone:</strong> " + rs.getString("driver_phone") + "</p>");
                    out.println("<p><strong>Vehicle:</strong> " + rs.getString("car_type") + " - " + rs.getString("car_number") + "</p>");
                    out.println("</div>");
                }
                
                if (rs.getString("operator_name") != null) {
                    out.println("<div class='detail-section'>");
                    out.println("<h4>Operator Information</h4>");
                    out.println("<p><strong>Name:</strong> " + rs.getString("operator_name") + "</p>");
                    out.println("<p><strong>Phone:</strong> " + rs.getString("operator_phone") + "</p>");
                    out.println("</div>");
                }
                out.println("</div>");
            } else {
                out.println("<p class='error'>Order not found</p>");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("<p class='error'>Error: " + e.getMessage() + "</p>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // PROCESS OFFLINE ORDER
    private void processOfflineOrder(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String customerPhone = request.getParameter("customerPhone");
            String customerName = request.getParameter("customerName");
            String customerEmail = request.getParameter("customerEmail");
            String customerAddress = request.getParameter("customerAddress");
            int productId = Integer.parseInt(request.getParameter("productId"));
            double quantity = Double.parseDouble(request.getParameter("quantity"));
            String deliveryAddress = request.getParameter("deliveryAddress");
            String paymentMethod = request.getParameter("paymentMethod");
            String notes = request.getParameter("notes");
            
            // Get product details
            Product product = getProductById(productId);
            if (product == null) {
                response.sendRedirect("operator.jsp?tab=offline-orders&error=Product not found");
                return;
            }
            
            // Validate quantity
            if (quantity < product.getMinQuantity()) {
                response.sendRedirect("operator.jsp?tab=offline-orders&error=Quantity must be at least " + product.getMinQuantity() + " kg");
                return;
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Check if customer exists, if not create
            int customerId = getOrCreateCustomer(customerPhone, customerName, customerEmail, customerAddress, conn);
            
            // Generate order number
            String orderNumber = "OFF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Calculate total price
            double sellPrice = quantity * product.getSellPrice().doubleValue();
            double millingCharge = quantity * product.getMillingPrice().doubleValue();
            double orderFee = 20.00;
            double totalPrice = sellPrice + millingCharge + orderFee;
            
            String sql = "INSERT INTO orders (order_number, customer_id, product_id, quantity, " +
                        "sell_price, milling_charge, order_fee, total_price, " +
                        "delivery_address, payment_method, notes, order_status, payment_status, assigned_operator) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'processing', 'paid', ?)";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, orderNumber);
            stmt.setInt(2, customerId);
            stmt.setInt(3, productId);
            stmt.setDouble(4, quantity);
            stmt.setDouble(5, sellPrice);
            stmt.setDouble(6, millingCharge);
            stmt.setDouble(7, orderFee);
            stmt.setDouble(8, totalPrice);
            stmt.setString(9, deliveryAddress != null ? deliveryAddress : customerAddress);
            stmt.setString(10, paymentMethod);
            stmt.setString(11, notes);
            stmt.setInt(12, operatorId);
            
            stmt.executeUpdate();
            
            response.sendRedirect("operator.jsp?tab=offline&message=Offline order placed successfully and marked as paid.");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=offline&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET DASHBOARD STATS
    private void getDashboardStats(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            StringBuilder stats = new StringBuilder();
            stats.append("{");
            
            // Today's orders
            String sql1 = "SELECT COUNT(*) as count FROM orders WHERE DATE(order_date) = CURDATE()";
            PreparedStatement stmt1 = conn.prepareStatement(sql1);
            ResultSet rs1 = stmt1.executeQuery();
            if (rs1.next()) {
                stats.append("\"todayOrders\":").append(rs1.getInt("count")).append(",");
            }
            rs1.close();
            stmt1.close();
            
            // Pending orders
            String sql2 = "SELECT COUNT(*) as count FROM orders WHERE order_status = 'pending'";
            PreparedStatement stmt2 = conn.prepareStatement(sql2);
            ResultSet rs2 = stmt2.executeQuery();
            if (rs2.next()) {
                stats.append("\"pendingOrders\":").append(rs2.getInt("count")).append(",");
            }
            rs2.close();
            stmt2.close();
            
            // Processing orders
            String sql3 = "SELECT COUNT(*) as count FROM orders WHERE order_status = 'processing'";
            PreparedStatement stmt3 = conn.prepareStatement(sql3);
            ResultSet rs3 = stmt3.executeQuery();
            if (rs3.next()) {
                stats.append("\"processingOrders\":").append(rs3.getInt("count")).append(",");
            }
            rs3.close();
            stmt3.close();
            
            // Today's revenue
            String sql4 = "SELECT SUM(total_price) as revenue FROM orders WHERE DATE(order_date) = CURDATE() AND payment_status = 'paid'";
            PreparedStatement stmt4 = conn.prepareStatement(sql4);
            ResultSet rs4 = stmt4.executeQuery();
            if (rs4.next()) {
                double revenue = rs4.getDouble("revenue");
                stats.append("\"todayRevenue\":").append(revenue);
            }
            rs4.close();
            stmt4.close();
            
            stats.append("}");
            
            response.setContentType("application/json");
            response.getWriter().write(stats.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET PRODUCTS FOR OFFLINE ORDER
    private void getProducts(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT * FROM products WHERE is_posted = TRUE ORDER BY name ASC";
            rs = stmt.executeQuery(sql);
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            out.println("<select class='form-select' name='productId' required>");
            out.println("<option value=''>Select a product</option>");
            
            while (rs.next()) {
                out.println("<option value='" + rs.getInt("id") + "' " +
                           "data-price='" + rs.getBigDecimal("sell_price") + "' " +
                           "data-milling='" + rs.getBigDecimal("milling_price") + "' " +
                           "data-min='" + rs.getInt("min_quantity") + "'>");
                out.println(rs.getString("name") + " - " + rs.getBigDecimal("sell_price") + " Birr/kg" +
                           " (Milling: " + rs.getBigDecimal("milling_price") + " Birr/kg, Min: " + 
                           rs.getInt("min_quantity") + " kg)");
                out.println("</option>");
            }
            
            out.println("</select>");
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET CUSTOMERS FOR SEARCH
    private void getCustomers(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String searchTerm = request.getParameter("search");
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT id, full_name, phone, email, address FROM users " +
                        "WHERE user_type = 'customer' AND (full_name LIKE ? OR phone LIKE ? OR email LIKE ?) " +
                        "ORDER BY full_name LIMIT 10";
            
            stmt = conn.prepareStatement(sql);
            String likeTerm = "%" + searchTerm + "%";
            stmt.setString(1, likeTerm);
            stmt.setString(2, likeTerm);
            stmt.setString(3, likeTerm);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            out.println("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.println(",");
                first = false;
                out.println("{");
                out.println("\"id\": " + rs.getInt("id") + ",");
                out.println("\"name\": \"" + rs.getString("full_name") + "\",");
                out.println("\"phone\": \"" + rs.getString("phone") + "\",");
                out.println("\"email\": \"" + rs.getString("email") + "\",");
                out.println("\"address\": \"" + rs.getString("address") + "\"");
                out.println("}");
            }
            out.println("]");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // ADD ORDER NOTES
    private void addOrderNotes(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String notes = request.getParameter("notes");
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE orders SET notes = CONCAT(COALESCE(notes, ''), '\n', ?), updated_at = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, "[" + new java.util.Date() + "] Operator: " + notes);
            stmt.setInt(2, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("operator.jsp?tab=orders&message=Notes added successfully");
            } else {
                response.sendRedirect("operator.jsp?tab=orders&error=Order not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=orders&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // UPDATE PROFILE
    private void updateProfile(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int operatorId = (Integer) request.getSession().getAttribute("userId");
            String username = request.getParameter("username");
            String email = request.getParameter("email");
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String fullName = request.getParameter("fullName");
            String phone = request.getParameter("phone");
            
            conn = DatabaseConnection.getConnection();
            
            // Verify current password if changing password
            if (newPassword != null && !newPassword.isEmpty()) {
                String verifySql = "SELECT password FROM users WHERE id = ?";
                PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
                verifyStmt.setInt(1, operatorId);
                ResultSet rs = verifyStmt.executeQuery();
                
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (!storedPassword.equals(hashPassword(currentPassword))) {
                        response.sendRedirect("operator.jsp?tab=profile&error=Current password is incorrect");
                        return;
                    }
                }
                rs.close();
                verifyStmt.close();
            }
            
            // Update profile
            String sql = "UPDATE users SET username = ?, email = ?, full_name = ?, phone = ?";
            if (newPassword != null && !newPassword.isEmpty()) {
                sql += ", password = ?";
            }
            sql += " WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, fullName);
            stmt.setString(4, phone);
            
            int paramIndex = 5;
            if (newPassword != null && !newPassword.isEmpty()) {
                stmt.setString(paramIndex++, hashPassword(newPassword));
            }
            stmt.setInt(paramIndex, operatorId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Update session attributes
                HttpSession session = request.getSession();
                session.setAttribute("username", username);
                session.setAttribute("fullName", fullName);
                session.setAttribute("email", email);
                session.setAttribute("phone", phone);
                
                response.sendRedirect("operator.jsp?tab=profile&message=Profile updated successfully");
            } else {
                response.sendRedirect("operator.jsp?tab=profile&error=Profile update failed");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=profile&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // ASSIGN ORDER TO ME
    private void assignToMe(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE orders SET assigned_operator = ?, updated_at = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, operatorId);
            stmt.setInt(2, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("operator.jsp?tab=orders&message=Order assigned to you");
            } else {
                response.sendRedirect("operator.jsp?tab=orders&error=Order not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=orders&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // MARK PAYMENT AS PAID
    private void markPaymentPaid(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE orders SET payment_status = 'paid', updated_at = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("operator.jsp?tab=orders&message=Payment marked as paid");
            } else {
                response.sendRedirect("operator.jsp?tab=orders&error=Order not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=orders&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // VOID ORDER
    private void voidOrder(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE orders SET order_status = 'cancelled', updated_at = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("operator.jsp?tab=orders&message=Order voided successfully");
            } else {
                response.sendRedirect("operator.jsp?tab=orders&error=Order not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=orders&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET ORDER TIMELINE
    private void getOrderTimeline(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT * FROM order_logs WHERE order_id = ? ORDER BY created_at DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            out.println("<div class='timeline'>");
            
            while (rs.next()) {
                out.println("<div class='timeline-item'>");
                out.println("<div class='timeline-content'>");
                out.println("<strong>" + rs.getString("action") + "</strong>");
                out.println("<p>" + rs.getString("details") + "</p>");
                out.println("<div class='timeline-date'>" + rs.getTimestamp("created_at") + "</div>");
                out.println("</div>");
                out.println("</div>");
            }
            
            out.println("</div>");
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("<p>Error loading timeline: " + e.getMessage() + "</p>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET NOTIFICATIONS
    private void getNotifications(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int userId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT * FROM notifications WHERE user_id = ? OR user_type = 'operator' ORDER BY created_at DESC LIMIT 10";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            if (!rs.isBeforeFirst()) {
                out.println("<p>No notifications</p>");
                return;
            }
            
            while (rs.next()) {
                out.println("<div style='padding: 0.5rem; border-bottom: 1px solid #eee;'>");
                out.println("<strong>" + rs.getString("title") + "</strong><br>");
                out.println("<small>" + rs.getString("message") + "</small><br>");
                out.println("<small style='color: #666;'>" + rs.getTimestamp("created_at") + "</small>");
                out.println("</div>");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("<p>Error loading notifications</p>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET NOTIFICATION COUNT
    private void getNotificationCount(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int userId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT COUNT(*) as count FROM notifications WHERE (user_id = ? OR user_type = 'operator') AND is_read = FALSE";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            if (rs.next()) {
                out.println("{\"count\": " + rs.getInt("count") + "}");
            } else {
                out.println("{\"count\": 0}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("{\"count\": 0}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET OFFLINE COUNT
    private void getOfflineCount(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int userId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT COUNT(*) as count FROM orders WHERE assigned_operator = ? AND DATE(order_date) = CURDATE()";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            if (rs.next()) {
                out.println("{\"count\": " + rs.getInt("count") + "}");
            } else {
                out.println("{\"count\": 0}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("{\"count\": 0}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET REPORT DATA
    private void getReportData(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            out.println("{");
            
            // Today's orders
            String sql1 = "SELECT COUNT(*) as todayOrders FROM orders WHERE DATE(order_date) = CURDATE()";
            stmt = conn.prepareStatement(sql1);
            rs = stmt.executeQuery();
            if (rs.next()) {
                out.println("\"todayOrders\": " + rs.getInt("todayOrders") + ",");
            }
            rs.close();
            stmt.close();
            
            // Today's revenue
            String sql2 = "SELECT SUM(total_price) as todayRevenue FROM orders WHERE DATE(order_date) = CURDATE() AND payment_status = 'paid'";
            stmt = conn.prepareStatement(sql2);
            rs = stmt.executeQuery();
            if (rs.next()) {
                out.println("\"todayRevenue\": " + rs.getDouble("todayRevenue") + ",");
            }
            rs.close();
            stmt.close();
            
            // Average processing time
            String sql3 = "SELECT AVG(TIMESTAMPDIFF(MINUTE, order_date, updated_at)) as avgProcessing FROM orders WHERE order_status = 'delivered'";
            stmt = conn.prepareStatement(sql3);
            rs = stmt.executeQuery();
            if (rs.next()) {
                out.println("\"avgProcessing\": " + rs.getInt("avgProcessing") + ",");
            }
            rs.close();
            stmt.close();
            
            // Completion rate
            String sql4 = "SELECT (COUNT(CASE WHEN order_status = 'delivered' THEN 1 END) * 100.0 / COUNT(*)) as completionRate FROM orders";
            stmt = conn.prepareStatement(sql4);
            rs = stmt.executeQuery();
            if (rs.next()) {
                out.println("\"completionRate\": " + rs.getDouble("completionRate"));
            }
            rs.close();
            stmt.close();
            
            out.println("}");
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("{\"todayOrders\": 0, \"todayRevenue\": 0, \"avgProcessing\": 0, \"completionRate\": 0}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // CHECK NEW ORDERS
    private void checkNewOrders(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int userId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT COUNT(*) as newOrders FROM orders WHERE assigned_operator = ? AND order_status = 'pending' AND DATE(order_date) = CURDATE()";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            if (rs.next()) {
                out.println("{\"newOrders\": " + rs.getInt("newOrders") + "}");
            } else {
                out.println("{\"newOrders\": 0}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("{\"newOrders\": 0}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET RECEIPT
    private void getReceipt(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT o.*, u.full_name as customer_name, u.phone as customer_phone, " +
                        "p.name as product_name, p.sell_price, p.milling_price " +
                        "FROM orders o " +
                        "LEFT JOIN users u ON o.customer_id = u.id " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "WHERE o.id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            if (rs.next()) {
                out.println("<div style='font-family: Arial, sans-serif; padding: 20px;'>");
                out.println("<div style='text-align: center; margin-bottom: 20px;'>");
                out.println("<h2>Mill Management System</h2>");
                out.println("<p>Order Receipt</p>");
                out.println("</div>");
                
                out.println("<div style='margin-bottom: 20px;'>");
                out.println("<p><strong>Receipt #:</strong> " + rs.getString("order_number") + "</p>");
                out.println("<p><strong>Date:</strong> " + rs.getTimestamp("order_date") + "</p>");
                out.println("<p><strong>Customer:</strong> " + rs.getString("customer_name") + "</p>");
                out.println("<p><strong>Phone:</strong> " + rs.getString("customer_phone") + "</p>");
                out.println("</div>");
                
                out.println("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
                out.println("<tr>");
                out.println("<th style='border: 1px solid #ddd; padding: 8px; text-align: left;'>Item</th>");
                out.println("<th style='border: 1px solid #ddd; padding: 8px; text-align: left;'>Quantity</th>");
                out.println("<th style='border: 1px solid #ddd; padding: 8px; text-align: left;'>Price</th>");
                out.println("<th style='border: 1px solid #ddd; padding: 8px; text-align: left;'>Total</th>");
                out.println("</tr>");
                
                out.println("<tr>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getString("product_name") + "</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getBigDecimal("quantity") + " kg</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getBigDecimal("sell_price") + " Birr/kg</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getBigDecimal("total_price") + " Birr</td>");
                out.println("</tr>");
                out.println("</table>");
                
                out.println("<div style='text-align: right;'>");
                out.println("<p><strong>Total Amount: " + rs.getBigDecimal("total_price") + " Birr</strong></p>");
                out.println("<p><strong>Payment Method: " + rs.getString("payment_method") + "</strong></p>");
                out.println("<p><strong>Status: " + rs.getString("payment_status") + "</strong></p>");
                out.println("</div>");
                
                out.println("<div style='margin-top: 30px; text-align: center;'>");
                out.println("<p>Thank you for your business!</p>");
                out.println("<p>Mill Management System</p>");
                out.println("</div>");
                out.println("</div>");
            } else {
                out.println("<p>Order not found</p>");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("<p>Error generating receipt: " + e.getMessage() + "</p>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET PENDING ORDERS
    private void getPendingOrders(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            
            String sql = "SELECT o.id, u.full_name as customer_name, p.name as product_name, o.quantity " +
                        "FROM orders o " +
                        "JOIN users u ON o.customer_id = u.id " +
                        "JOIN products p ON o.product_id = p.id " +
                        "WHERE o.order_status = 'pending' " +
                        "ORDER BY o.order_date LIMIT 10";
            
            rs = stmt.executeQuery(sql);
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            out.println("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.println(",");
                first = false;
                out.println("{");
                out.println("\"id\": " + rs.getInt("id") + ",");
                out.println("\"customer_name\": \"" + rs.getString("customer_name") + "\",");
                out.println("\"product_name\": \"" + rs.getString("product_name") + "\",");
                out.println("\"quantity\": " + rs.getBigDecimal("quantity"));
                out.println("}");
            }
            out.println("]");
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("[]");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET DRIVER PERFORMANCE
    private void getDriverPerformance(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int driverId = Integer.parseInt(request.getParameter("driverId"));
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT " +
                        "COUNT(*) as total_orders, " +
                        "SUM(CASE WHEN order_status = 'delivered' THEN 1 ELSE 0 END) as delivered_orders, " +
                        "AVG(TIMESTAMPDIFF(MINUTE, order_date, updated_at)) as avg_delivery_time " +
                        "FROM orders WHERE assigned_driver = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, driverId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            if (rs.next()) {
                out.println("<div class='performance-stats'>");
                out.println("<p><strong>Total Orders Assigned:</strong> " + rs.getInt("total_orders") + "</p>");
                out.println("<p><strong>Delivered Orders:</strong> " + rs.getInt("delivered_orders") + "</p>");
                out.println("<p><strong>Average Delivery Time:</strong> " + rs.getInt("avg_delivery_time") + " minutes</p>");
                out.println("<p><strong>Delivery Success Rate:</strong> " + 
                           (rs.getInt("total_orders") > 0 ? 
                           (rs.getInt("delivered_orders") * 100 / rs.getInt("total_orders")) : 0) + "%</p>");
                out.println("</div>");
            } else {
                out.println("<p>No performance data available</p>");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("<p>Error loading performance data</p>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET STOCK HISTORY
    private void getStockHistory(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT il.*, u.full_name as operator_name " +
                        "FROM inventory_log il " +
                        "LEFT JOIN users u ON il.operator_id = u.id " +
                        "WHERE il.product_id = ? " +
                        "ORDER BY il.created_at DESC LIMIT 20";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            out.println("<table style='width: 100%; border-collapse: collapse;'>");
            out.println("<tr>");
            out.println("<th style='border: 1px solid #ddd; padding: 8px;'>Date</th>");
            out.println("<th style='border: 1px solid #ddd; padding: 8px;'>Type</th>");
            out.println("<th style='border: 1px solid #ddd; padding: 8px;'>Quantity</th>");
            out.println("<th style='border: 1px solid #ddd; padding: 8px;'>Reason</th>");
            out.println("<th style='border: 1px solid #ddd; padding: 8px;'>Operator</th>");
            out.println("<th style='border: 1px solid #ddd; padding: 8px;'>Notes</th>");
            out.println("</tr>");
            
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getTimestamp("created_at") + "</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getString("type") + "</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getBigDecimal("quantity") + " kg</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getString("reason") + "</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getString("operator_name") + "</td>");
                out.println("<td style='border: 1px solid #ddd; padding: 8px;'>" + rs.getString("notes") + "</td>");
                out.println("</tr>");
            }
            out.println("</table>");
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("<p>Error loading stock history</p>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET CURRENT STOCK
    private void getCurrentStock(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT " +
                        "(SELECT SUM(quantity) FROM inventory_log WHERE product_id = ? AND type = 'in') as total_in, " +
                        "(SELECT SUM(quantity) FROM inventory_log WHERE product_id = ? AND type = 'out') as total_out";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            stmt.setInt(2, productId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            if (rs.next()) {
                BigDecimal totalIn = rs.getBigDecimal("total_in");
                BigDecimal totalOut = rs.getBigDecimal("total_out");
                
                if (totalIn == null) totalIn = BigDecimal.ZERO;
                if (totalOut == null) totalOut = BigDecimal.ZERO;
                
                BigDecimal currentStock = totalIn.subtract(totalOut);
                out.println("{\"stock\": " + currentStock + "}");
            } else {
                out.println("{\"stock\": 0}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("{\"stock\": 0}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // TOGGLE AVAILABILITY
    private void toggleAvailability(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int userId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            // Get current status
            String currentStatus = "active";
            String checkSql = "SELECT status FROM users WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, userId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                currentStatus = rs.getString("status");
            }
            rs.close();
            checkStmt.close();
            
            // Toggle status
            String newStatus = "active".equals(currentStatus) ? "unavailable" : "active";
            
            String updateSql = "UPDATE users SET status = ? WHERE id = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, userId);
            
            int rows = stmt.executeUpdate();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            if (rows > 0) {
                out.println("{\"status\": \"" + newStatus + "\"}");
            } else {
                out.println("{\"status\": \"" + currentStatus + "\"}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.println("{\"status\": \"error\"}");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // UPDATE STOCK
    private void updateStock(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            String type = request.getParameter("type");
            BigDecimal quantity = new BigDecimal(request.getParameter("quantity"));
            String reason = request.getParameter("reason");
            String notes = request.getParameter("notes");
            
            conn = DatabaseConnection.getConnection();
            
            String sql = "INSERT INTO inventory_log (product_id, type, quantity, reason, notes, operator_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            stmt.setString(2, type);
            stmt.setBigDecimal(3, quantity);
            stmt.setString(4, reason);
            stmt.setString(5, notes);
            stmt.setInt(6, operatorId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("operator.jsp?tab=inventory&message=Stock updated successfully");
            } else {
                response.sendRedirect("operator.jsp?tab=inventory&error=Failed to update stock");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=inventory&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // CHANGE PASSWORD
    private void changePassword(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            
            if (!newPassword.equals(confirmPassword)) {
                response.sendRedirect("operator.jsp?tab=settings&error=New passwords do not match");
                return;
            }
            
            if (newPassword.length() < 6) {
                response.sendRedirect("operator.jsp?tab=settings&error=Password must be at least 6 characters long");
                return;
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Verify current password
            String verifySql = "SELECT password FROM users WHERE id = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setInt(1, operatorId);
            ResultSet rs = verifyStmt.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (!storedPassword.equals(hashPassword(currentPassword))) {
                    response.sendRedirect("operator.jsp?tab=settings&error=Current password is incorrect");
                    return;
                }
            }
            rs.close();
            verifyStmt.close();
            
            // Update password
            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, hashPassword(newPassword));
            stmt.setInt(2, operatorId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("operator.jsp?tab=settings&message=Password changed successfully");
            } else {
                response.sendRedirect("operator.jsp?tab=settings&error=Failed to change password");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=settings&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // UPDATE WORKING HOURS
    private void updateWorkingHours(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String shiftStart = request.getParameter("shiftStart");
            String shiftEnd = request.getParameter("shiftEnd");
            String[] workingDays = request.getParameterValues("workingDays");
            
            String workingDaysStr = workingDays != null ? String.join(",", workingDays) : "";
            
            conn = DatabaseConnection.getConnection();
            
            // Check if record exists
            String checkSql = "SELECT id FROM operator_settings WHERE operator_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, operatorId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // Update existing
                String updateSql = "UPDATE operator_settings SET shift_start = ?, shift_end = ?, working_days = ? WHERE operator_id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setString(1, shiftStart);
                stmt.setString(2, shiftEnd);
                stmt.setString(3, workingDaysStr);
                stmt.setInt(4, operatorId);
            } else {
                // Insert new
                String insertSql = "INSERT INTO operator_settings (operator_id, shift_start, shift_end, working_days) VALUES (?, ?, ?, ?)";
                stmt = conn.prepareStatement(insertSql);
                stmt.setInt(1, operatorId);
                stmt.setString(2, shiftStart);
                stmt.setString(3, shiftEnd);
                stmt.setString(4, workingDaysStr);
            }
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("operator.jsp?tab=settings&message=Working hours updated successfully");
            } else {
                response.sendRedirect("operator.jsp?tab=settings&error=Failed to update working hours");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=settings&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // CREATE OFFLINE ORDER
    private void createOfflineOrder(HttpServletRequest request, HttpServletResponse response, int operatorId) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String customerPhone = request.getParameter("customerPhone");
            String customerName = request.getParameter("customerName");
            int productId = Integer.parseInt(request.getParameter("productId"));
            BigDecimal quantity = new BigDecimal(request.getParameter("quantity"));
            String paymentMethod = request.getParameter("paymentMethod");
            String orderType = request.getParameter("orderType");
            
            conn = DatabaseConnection.getConnection();
            
            // Get product details
            Product product = getProductById(productId);
            if (product == null) {
                response.sendRedirect("operator.jsp?tab=offline&error=Product not found");
                return;
            }
            
            // Validate quantity
            if (quantity.compareTo(new BigDecimal(product.getMinQuantity())) < 0) {
                response.sendRedirect("operator.jsp?tab=offline&error=Quantity must be at least " + product.getMinQuantity() + " kg");
                return;
            }
            
            // Get or create customer
            int customerId = getOrCreateCustomer(customerPhone, customerName, customerPhone + "@email.com", "Walk-in", conn);
            
            // Generate order number
            String orderNumber = "OFF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Calculate prices
            BigDecimal sellPrice = quantity.multiply(product.getSellPrice());
            BigDecimal millingCharge = quantity.multiply(product.getMillingPrice());
            BigDecimal orderFee = new BigDecimal("20.00");
            BigDecimal totalPrice = sellPrice.add(millingCharge).add(orderFee);
            
            // Adjust based on order type
            if ("takeaway".equals(orderType)) {
                millingCharge = BigDecimal.ZERO;
            } else if ("milling".equals(orderType)) {
                sellPrice = BigDecimal.ZERO;
            }
            
            totalPrice = sellPrice.add(millingCharge).add(orderFee);
            
            String sql = "INSERT INTO orders (order_number, customer_id, product_id, quantity, " +
                        "sell_price, milling_charge, order_fee, total_price, " +
                        "delivery_address, payment_method, order_status, payment_status, assigned_operator) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'processing', 'paid', ?)";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, orderNumber);
            stmt.setInt(2, customerId);
            stmt.setInt(3, productId);
            stmt.setBigDecimal(4, quantity);
            stmt.setBigDecimal(5, sellPrice);
            stmt.setBigDecimal(6, millingCharge);
            stmt.setBigDecimal(7, orderFee);
            stmt.setBigDecimal(8, totalPrice);
            stmt.setString(9, "Walk-in");
            stmt.setString(10, paymentMethod);
            stmt.setInt(11, operatorId);
            
            stmt.executeUpdate();
            
            response.sendRedirect("operator.jsp?tab=offline&message=Offline order created successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("operator.jsp?tab=offline&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // HELPER METHODS
    private Product getProductById(int productId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM products WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("id"));
                product.setName(rs.getString("name"));
                product.setSellPrice(rs.getBigDecimal("sell_price"));
                product.setMillingPrice(rs.getBigDecimal("milling_price"));
                product.setMinQuantity(rs.getInt("min_quantity"));
                return product;
            }
            return null;
            
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    private int getOrCreateCustomer(String phone, String name, String email, String address, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // Check if customer exists by phone
            String checkSql = "SELECT id FROM users WHERE phone = ? AND user_type = 'customer'";
            stmt = conn.prepareStatement(checkSql);
            stmt.setString(1, phone);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                int customerId = rs.getInt("id");
                // Update customer info
                String updateSql = "UPDATE users SET full_name = ?, email = ?, address = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, name);
                updateStmt.setString(2, email != null ? email : phone + "@temp.com");
                updateStmt.setString(3, address);
                updateStmt.setInt(4, customerId);
                updateStmt.executeUpdate();
                updateStmt.close();
                return customerId;
            }
            rs.close();
            stmt.close();
            
            // Create new customer
            String username = "customer_" + phone;
            String customerEmail = email != null ? email : phone + "@temp.com";
            String password = hashPassword("temp123"); // Default password
            
            String insertSql = "INSERT INTO users (username, email, password, full_name, phone, address, user_type) " +
                              "VALUES (?, ?, ?, ?, ?, ?, 'customer')";
            stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, customerEmail);
            stmt.setString(3, password);
            stmt.setString(4, name);
            stmt.setString(5, phone);
            stmt.setString(6, address);
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            throw new SQLException("Failed to create customer");
            
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        }
    }
    
    private boolean isValidStatusTransition(int orderId, String newStatus) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT order_status FROM orders WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String currentStatus = rs.getString("order_status");
                
                // Define valid transitions
                Map<String, List<String>> validTransitions = new HashMap<>();
                validTransitions.put("pending", Arrays.asList("processing", "cancelled"));
                validTransitions.put("processing", Arrays.asList("assigned", "cancelled"));
                validTransitions.put("assigned", Arrays.asList("out_for_delivery", "cancelled"));
                validTransitions.put("out_for_delivery", Arrays.asList("delivered", "cancelled"));
                validTransitions.put("delivered", Arrays.asList()); // No further transitions
                validTransitions.put("cancelled", Arrays.asList()); // No further transitions
                
                return validTransitions.containsKey(currentStatus) && 
                       validTransitions.get(currentStatus).contains(newStatus);
            }
            return false;
            
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
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
            return password;
        }
    }
}