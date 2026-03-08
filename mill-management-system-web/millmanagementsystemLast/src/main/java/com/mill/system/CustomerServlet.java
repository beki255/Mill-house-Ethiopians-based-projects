package com.mill.system;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.net.URLEncoder;
import java.sql.*;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Date;

@WebServlet("/customer")
@MultipartConfig
public class CustomerServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("index.html");
            return;
        }
        
        String userType = (String) session.getAttribute("userType");
        if (!"customer".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("getProductDetails".equals(action)) {
            getProductDetails(request, response);
        } else if ("getCartSummary".equals(action)) {
            getCartSummary(request, response);
        } else if ("getCartCount".equals(action)) {
            getCartCount(request, response);
        } else if ("getCart".equals(action)) {
            getCart(request, response);
        } else if ("getDriverInfo".equals(action)) {
            getDriverInfo(request, response);
        } else if ("getOrderPayment".equals(action)) {
            getOrderPayment(request, response);
        } else if ("downloadInvoice".equals(action)) {
            downloadInvoice(request, response);
        } else if ("downloadOrderHistory".equals(action)) {
            downloadOrderHistory(request, response);
        } else {
            response.sendRedirect("customer.jsp");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("index.html");
            return;
        }
        
        String userType = (String) session.getAttribute("userType");
        if (!"customer".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("placeOrder".equals(action)) {
            placeOrder(request, response);
        } else if ("addToCart".equals(action)) {
            addToCart(request, response);
        } else if ("updateCart".equals(action)) {
            updateCart(request, response);
        } else if ("removeFromCart".equals(action)) {
            removeFromCart(request, response);
        } else if ("clearCart".equals(action)) {
            clearCart(request, response);
        } else if ("checkout".equals(action)) {
            checkoutCart(request, response);
        } else if ("placeSpecialOrder".equals(action)) {
            placeSpecialOrder(request, response);
        } else if ("updateProfile".equals(action)) {
            updateProfile(request, response);
        } else if ("changePassword".equals(action)) {
            changePassword(request, response);
        } else if ("updatePayment".equals(action)) {
            updatePayment(request, response);
        } else if ("requestPasswordReset".equals(action)) {
            requestPasswordReset(request, response);
        } else if ("cancelOrder".equals(action)) {
            cancelOrder(request, response);
        } else if ("payOrder".equals(action)) {
            payOrder(request, response);
        } else if ("cancelSpecialOrder".equals(action)) {
            cancelSpecialOrder(request, response);
        }else {
            response.sendRedirect("customer.jsp");
        }
    }
    
    // GET PRODUCT DETAILS FOR BUY NOW MODAL
    private void getProductDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM products WHERE id = ? AND is_posted = true";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            
            if (rs.next()) {
                String imageUrl = rs.getString("image_url");
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = "https://images.unsplash.com/photo-1595341888016-a392ef81b7de?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=80";
                }
                
                // Build JSON manually
                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"id\":").append(rs.getInt("id")).append(",");
                json.append("\"name\":\"").append(escapeJson(rs.getString("name"))).append("\",");
                json.append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",");
                json.append("\"sell_price\":").append(rs.getBigDecimal("sell_price")).append(",");
                json.append("\"milling_price\":").append(rs.getBigDecimal("milling_price")).append(",");
                json.append("\"min_quantity\":").append(rs.getInt("min_quantity")).append(",");
                json.append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",");
                json.append("\"image_url\":\"").append(escapeJson(imageUrl)).append("\"");
                json.append("}");
                
                out.write(json.toString());
            } else {
                out.write("{\"error\":\"Product not found\"}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET CART COUNT
    private void getCartCount(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) as count FROM cart WHERE customer_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            
            if (rs.next()) {
                out.write("{\"count\":" + rs.getInt("count") + "}");
            } else {
                out.write("{\"count\":0}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"count\":0,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET CART (returns JSON with cart items)
    private void getCart(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT c.*, p.name, p.sell_price, p.milling_price, p.min_quantity, p.image_url, p.category " +
                        "FROM cart c " +
                        "JOIN products p ON c.product_id = p.id " +
                        "WHERE c.customer_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            
            StringBuilder json = new StringBuilder();
            json.append("{\"items\":[");
            
            boolean firstItem = true;
            BigDecimal totalPrice = BigDecimal.ZERO;
            
            while (rs.next()) {
                if (!firstItem) {
                    json.append(",");
                }
                firstItem = false;
                
                int cartId = rs.getInt("id");
                String productName = rs.getString("name");
                BigDecimal sellPrice = rs.getBigDecimal("sell_price");
                BigDecimal millingPrice = rs.getBigDecimal("milling_price");
                int quantity = rs.getInt("quantity");
                int minQuantity = rs.getInt("min_quantity");
                String imageUrl = rs.getString("image_url");
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = "https://images.unsplash.com/photo-1595341888016-a392ef81b7de?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=80";
                }
                String category = rs.getString("category");
                
                BigDecimal itemTotal = sellPrice.add(millingPrice).multiply(BigDecimal.valueOf(quantity));
                totalPrice = totalPrice.add(itemTotal);
                
                json.append("{");
                json.append("\"cart_id\":").append(cartId).append(",");
                json.append("\"product_id\":").append(rs.getInt("product_id")).append(",");
                json.append("\"name\":\"").append(escapeJson(productName)).append("\",");
                json.append("\"category\":\"").append(escapeJson(category)).append("\",");
                json.append("\"image_url\":\"").append(escapeJson(imageUrl)).append("\",");
                json.append("\"sell_price\":").append(sellPrice).append(",");
                json.append("\"milling_price\":").append(millingPrice).append(",");
                json.append("\"quantity\":").append(quantity).append(",");
                json.append("\"min_quantity\":").append(minQuantity).append(",");
                json.append("\"item_total\":").append(itemTotal);
                json.append("}");
            }
            
            json.append("],\"summary\":{");
            json.append("\"item_count\":").append(firstItem ? 0 : 1).append(","); // Simplified count
            json.append("\"subtotal\":").append(totalPrice).append(",");
            json.append("\"order_fee\":20.00,");
            json.append("\"total\":").append(totalPrice.add(new BigDecimal("20.00")));
            json.append("}}");
            
            out.write(json.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"" + escapeJson(e.getMessage()) + "\",\"items\":[]}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET CART SUMMARY FOR CHECKOUT
    private void getCartSummary(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT c.*, p.name, p.sell_price, p.milling_price, p.image_url " +
                        "FROM cart c " +
                        "JOIN products p ON c.product_id = p.id " +
                        "WHERE c.customer_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            
            BigDecimal totalPrice = BigDecimal.ZERO;
            BigDecimal totalMilling = BigDecimal.ZERO;
            int itemCount = 0;
            
            out.println("<div class='cart-summary'>");
            
            // Show cart items
            out.println("<div class='cart-items-preview'>");
            while (rs.next()) {
                itemCount++;
                BigDecimal sellPrice = rs.getBigDecimal("sell_price");
                BigDecimal millingPrice = rs.getBigDecimal("milling_price");
                BigDecimal quantity = BigDecimal.valueOf(rs.getDouble("quantity"));
                
                BigDecimal itemSellTotal = sellPrice.multiply(quantity);
                BigDecimal itemMillingTotal = millingPrice.multiply(quantity);
                BigDecimal itemTotal = itemSellTotal.add(itemMillingTotal);
                
                totalPrice = totalPrice.add(itemSellTotal);
                totalMilling = totalMilling.add(itemMillingTotal);
                
                String imageUrl = rs.getString("image_url");
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = "https://images.unsplash.com/photo-1595341888016-a392ef81b7de?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=80";
                }
                
                out.println("<div class='cart-preview-item'>");
                out.println("<img src='" + imageUrl + "' alt='" + rs.getString("name") + "' style='width: 50px; height: 50px; border-radius: 5px;'>");
                out.println("<div>");
                out.println("<p><strong>" + rs.getString("name") + "</strong></p>");
                out.println("<p>" + quantity + " kg × " + sellPrice + " Birr = " + itemSellTotal + " Birr</p>");
                out.println("</div>");
                out.println("</div>");
            }
            out.println("</div>");
            
            if (itemCount == 0) {
                out.println("<div class='empty-state'>Your cart is empty</div>");
            } else {
                BigDecimal orderFee = new BigDecimal("20.00");
                BigDecimal grandTotal = totalPrice.add(totalMilling).add(orderFee);
                
                out.println("<div class='summary-row'>");
                out.println("<span>Product Total (" + itemCount + " items):</span>");
                out.println("<span>" + totalPrice + " Birr</span>");
                out.println("</div>");
                
                out.println("<div class='summary-row'>");
                out.println("<span>Milling Total:</span>");
                out.println("<span>" + totalMilling + " Birr</span>");
                out.println("</div>");
                
                out.println("<div class='summary-row'>");
                out.println("<span>Order Fee:</span>");
                out.println("<span>" + orderFee + " Birr</span>");
                out.println("</div>");
                
                out.println("<div class='summary-row summary-total'>");
                out.println("<span>Grand Total:</span>");
                out.println("<span>" + grandTotal + " Birr</span>");
                out.println("</div>");
            }
            out.println("</div>");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("<div class='alert alert-error'>Error loading cart summary: " + e.getMessage() + "</div>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // ADD TO CART
    private void addToCart(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int productId = Integer.parseInt(request.getParameter("productId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            // Check if product exists and is available
            conn = DatabaseConnection.getConnection();
            String checkSql = "SELECT id, min_quantity FROM products WHERE id = ? AND is_posted = true";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, productId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                throw new Exception("Product not available");
            }
            
            int minQuantity = rs.getInt("min_quantity");
            if (quantity < minQuantity) {
                throw new Exception("Minimum quantity is " + minQuantity + " kg");
            }
            
            // Check if already in cart
            String findSql = "SELECT id, quantity FROM cart WHERE customer_id = ? AND product_id = ?";
            PreparedStatement findStmt = conn.prepareStatement(findSql);
            findStmt.setInt(1, userId);
            findStmt.setInt(2, productId);
            ResultSet cartRs = findStmt.executeQuery();
            
            if (cartRs.next()) {
                // Update quantity
                String updateSql = "UPDATE cart SET quantity = quantity + ? WHERE id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setInt(1, quantity);
                stmt.setInt(2, cartRs.getInt("id"));
            } else {
                // Insert new
                String insertSql = "INSERT INTO cart (customer_id, product_id, quantity) VALUES (?, ?, ?)";
                stmt = conn.prepareStatement(insertSql);
                stmt.setInt(1, userId);
                stmt.setInt(2, productId);
                stmt.setInt(3, quantity);
            }
            
            stmt.executeUpdate();
            
            // Redirect with success message
            response.sendRedirect("customer.jsp?tab=cart&message=Product added to cart successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=products&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // UPDATE CART QUANTITY
    private void updateCart(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            // Check minimum quantity from product
            conn = DatabaseConnection.getConnection();
            String checkSql = "SELECT p.min_quantity FROM cart c " +
                            "JOIN products p ON c.product_id = p.id " +
                            "WHERE c.id = ? AND c.customer_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, cartItemId);
            checkStmt.setInt(2, userId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                throw new Exception("Cart item not found");
            }
            
            int minQuantity = rs.getInt("min_quantity");
            if (quantity < minQuantity) {
                throw new Exception("Minimum quantity is " + minQuantity + " kg");
            }
            
            // Update quantity
            String updateSql = "UPDATE cart SET quantity = ? WHERE id = ? AND customer_id = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, quantity);
            stmt.setInt(2, cartItemId);
            stmt.setInt(3, userId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Quantity updated\"}");
            } else {
                throw new Exception("Failed to update quantity");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // REMOVE FROM CART
    private void removeFromCart(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "DELETE FROM cart WHERE id = ? AND customer_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cartItemId);
            stmt.setInt(2, userId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Item removed from cart\"}");
            } else {
                throw new Exception("Failed to remove item");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // CLEAR CART
    private void clearCart(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "DELETE FROM cart WHERE customer_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Cart cleared successfully\"}");
            } else {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Cart is already empty\"}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // PLACE ORDER (BUY NOW)
    private void placeOrder(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int productId = Integer.parseInt(request.getParameter("productId"));
            BigDecimal quantity = new BigDecimal(request.getParameter("quantity"));
            String deliveryAddress = request.getParameter("deliveryAddress");
            String paymentMethod = request.getParameter("paymentMethod");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                throw new Exception("Delivery address is required");
            }
            
            // Get product details
            conn = DatabaseConnection.getConnection();
            String productSql = "SELECT * FROM products WHERE id = ? AND is_posted = true";
            PreparedStatement productStmt = conn.prepareStatement(productSql);
            productStmt.setInt(1, productId);
            ResultSet rs = productStmt.executeQuery();
            
            if (!rs.next()) {
                throw new Exception("Product not available");
            }
            
            BigDecimal sellPrice = rs.getBigDecimal("sell_price");
            BigDecimal millingPrice = rs.getBigDecimal("milling_price");
            int minQuantity = rs.getInt("min_quantity");
            
            if (quantity.compareTo(BigDecimal.valueOf(minQuantity)) < 0) {
                throw new Exception("Minimum quantity is " + minQuantity + " kg");
            }
            
            // Calculate totals
            BigDecimal productTotal = sellPrice.multiply(quantity);
            BigDecimal millingTotal = millingPrice.multiply(quantity);
            BigDecimal orderFee = new BigDecimal("20.00");
            BigDecimal totalPrice = productTotal.add(millingTotal).add(orderFee);
            
            // Generate order number
            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Insert order
            String orderSql = "INSERT INTO orders (order_number, customer_id, product_id, quantity, " +
                            "sell_price, milling_charge, order_fee, total_price, " +
                            "delivery_address, payment_method, order_status, payment_status, order_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', 'pending', NOW())";
            
            stmt = conn.prepareStatement(orderSql);
            stmt.setString(1, orderNumber);
            stmt.setInt(2, userId);
            stmt.setInt(3, productId);
            stmt.setBigDecimal(4, quantity);
            stmt.setBigDecimal(5, productTotal);
            stmt.setBigDecimal(6, millingTotal);
            stmt.setBigDecimal(7, orderFee);
            stmt.setBigDecimal(8, totalPrice);
            stmt.setString(9, deliveryAddress);
            stmt.setString(10, paymentMethod);
            
            stmt.executeUpdate();
            
            response.sendRedirect("customer.jsp?tab=orders&message=Order placed successfully. Order #" + orderNumber);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=products&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // CHECKOUT CART
    private void checkoutCart(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            String deliveryAddress = request.getParameter("deliveryAddress");
            String paymentMethod = request.getParameter("paymentMethod");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                throw new Exception("Delivery address is required");
            }
            
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Get cart items
            String cartSql = "SELECT c.*, p.name, p.sell_price, p.milling_price, p.min_quantity " +
                            "FROM cart c " +
                            "JOIN products p ON c.product_id = p.id " +
                            "WHERE c.customer_id = ?";
            stmt = conn.prepareStatement(cartSql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            boolean hasItems = false;
            int orderCount = 0;
            StringBuilder orderNumbers = new StringBuilder();
            
            while (rs.next()) {
                hasItems = true;
                orderCount++;
                
                BigDecimal quantity = BigDecimal.valueOf(rs.getDouble("quantity"));
                BigDecimal sellPrice = rs.getBigDecimal("sell_price");
                BigDecimal millingPrice = rs.getBigDecimal("milling_price");
                int minQuantity = rs.getInt("min_quantity");
                
                // Validate quantity
                if (quantity.compareTo(BigDecimal.valueOf(minQuantity)) < 0) {
                    throw new Exception("Product '" + rs.getString("name") + "' requires minimum " + minQuantity + " kg");
                }
                
                // Calculate totals
                BigDecimal productTotal = sellPrice.multiply(quantity);
                BigDecimal millingTotal = millingPrice.multiply(quantity);
                BigDecimal orderFee = new BigDecimal("20.00");
                BigDecimal totalPrice = productTotal.add(millingTotal).add(orderFee);
                
                // Generate order number
                String orderNumber = "CART-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                if (orderNumbers.length() > 0) {
                    orderNumbers.append(", ");
                }
                orderNumbers.append(orderNumber);
                
                // Insert order
                String orderSql = "INSERT INTO orders (order_number, customer_id, product_id, quantity, " +
                                "sell_price, milling_charge, order_fee, total_price, " +
                                "delivery_address, payment_method, order_status, payment_status, order_date) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', 'pending', NOW())";
                
                PreparedStatement orderStmt = conn.prepareStatement(orderSql);
                orderStmt.setString(1, orderNumber);
                orderStmt.setInt(2, userId);
                orderStmt.setInt(3, rs.getInt("product_id"));
                orderStmt.setBigDecimal(4, quantity);
                orderStmt.setBigDecimal(5, productTotal);
                orderStmt.setBigDecimal(6, millingTotal);
                orderStmt.setBigDecimal(7, orderFee);
                orderStmt.setBigDecimal(8, totalPrice);
                orderStmt.setString(9, deliveryAddress);
                orderStmt.setString(10, paymentMethod);
                orderStmt.executeUpdate();
                orderStmt.close();
            }
            
            if (!hasItems) {
                throw new Exception("Your cart is empty");
            }
            
            // Clear cart after successful order placement
            String clearSql = "DELETE FROM cart WHERE customer_id = ?";
            PreparedStatement clearStmt = conn.prepareStatement(clearSql);
            clearStmt.setInt(1, userId);
            clearStmt.executeUpdate();
            clearStmt.close();
            
            conn.commit();
            
            response.sendRedirect("customer.jsp?tab=orders&message=" + orderCount + " order(s) placed successfully. Order #" + orderNumbers.toString());
            
        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=cart&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // PLACE SPECIAL ORDER
    private void placeSpecialOrder(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            String description = request.getParameter("description");
            String grainType = request.getParameter("grainType");
            BigDecimal quantity = new BigDecimal(request.getParameter("quantity"));
            String instructions = request.getParameter("instructions");
            String pickupPreference = request.getParameter("pickupPreference");
            String deliveryAddress = request.getParameter("deliveryAddress");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            if (quantity.compareTo(BigDecimal.ONE) < 0) {
                throw new Exception("Quantity must be at least 1 kg");
            }
            
            // Calculate price: 10 Birr/kg for milling + 20 Birr order fee
            BigDecimal millingFee = new BigDecimal("10.00");
            BigDecimal orderFee = new BigDecimal("20.00");
            BigDecimal totalPrice = quantity.multiply(millingFee).add(orderFee);
            
            // Generate order number
            String orderNumber = "SPC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // First, ensure special_orders table exists
            createSpecialOrdersTableIfNotExists();
            
            conn = DatabaseConnection.getConnection();
            
            // Insert into special_orders table
            String sql = "INSERT INTO special_orders (order_number, customer_id, description, grain_type, " +
                        "quantity, instructions, pickup_preference, delivery_address, " +
                        "total_price, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', NOW())";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, orderNumber);
            stmt.setInt(2, userId);
            stmt.setString(3, description);
            stmt.setString(4, grainType);
            stmt.setBigDecimal(5, quantity);
            stmt.setString(6, instructions);
            stmt.setString(7, pickupPreference);
            stmt.setString(8, deliveryAddress);
            stmt.setBigDecimal(9, totalPrice);
            
            stmt.executeUpdate();
            
            response.sendRedirect("customer.jsp?tab=orders&message=Special order placed successfully. Order #" + orderNumber);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=special-order&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
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
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            String address = request.getParameter("address");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, address = ? WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, address);
            stmt.setInt(5, userId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                // Update session
                HttpSession session = request.getSession();
                session.setAttribute("fullName", fullName);
                session.setAttribute("email", email);
                session.setAttribute("phone", phone);
                session.setAttribute("address", address);
                
                // Update user object in session if it exists
                User user = (User) session.getAttribute("user");
                if (user != null) {
                    user.setFullName(fullName);
                    user.setEmail(email);
                    user.setPhone(phone);
                    user.setAddress(address);
                }
                
                response.sendRedirect("customer.jsp?tab=settings&message=Profile updated successfully");
            } else {
                throw new Exception("Failed to update profile");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=settings&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // CHANGE PASSWORD
    private void changePassword(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            if (!newPassword.equals(confirmPassword)) {
                throw new Exception("New passwords do not match");
            }
            
            if (newPassword.length() < 6) {
                throw new Exception("Password must be at least 6 characters long");
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Verify current password
            String verifySql = "SELECT password FROM users WHERE id = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setInt(1, userId);
            rs = verifyStmt.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // Simple comparison (in production, use proper password hashing)
                if (!storedPassword.equals(currentPassword)) {
                    throw new Exception("Current password is incorrect");
                }
            } else {
                throw new Exception("User not found");
            }
            
            // Update password
            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, newPassword);
            stmt.setInt(2, userId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                response.sendRedirect("customer.jsp?tab=settings&message=Password changed successfully");
            } else {
                throw new Exception("Failed to change password");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=settings&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // UPDATE PAYMENT INFO
    private void updatePayment(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            String paymentMethod = request.getParameter("paymentMethod");
            String paymentAccount = request.getParameter("paymentAccount");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            // Validate payment info
            if ("CBE".equals(paymentMethod) && !paymentAccount.matches("^1000\\d+$")) {
                throw new Exception("CBE account must start with 1000");
            }
            
            if ("Telebirr".equals(paymentMethod) && !paymentAccount.matches("^\\+2519\\d{8}$")) {
                throw new Exception("Telebirr must be a phone number starting with +2519");
            }
            
            if ("Cash".equals(paymentMethod) && !"CASH".equals(paymentAccount)) {
                throw new Exception("For Cash, enter 'CASH' in account field");
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Check if column exists in users table, if not add it
            try {
                // Try to update users table directly
                String updateUserSql = "UPDATE users SET payment_method = ?, payment_account = ? WHERE id = ?";
                stmt = conn.prepareStatement(updateUserSql);
                stmt.setString(1, paymentMethod);
                stmt.setString(2, paymentAccount);
                stmt.setInt(3, userId);
                
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    // Update session
                    HttpSession session = request.getSession();
                    session.setAttribute("paymentMethod", paymentMethod);
                    
                    // Update user object in session if it exists
                    User user = (User) session.getAttribute("user");
                    if (user != null) {
                        user.setPaymentMethod(paymentMethod);
                        user.setPaymentAccount(paymentAccount);
                    }
                    
                    response.sendRedirect("customer.jsp?tab=settings&message=Payment information updated successfully");
                } else {
                    throw new Exception("Failed to update payment information");
                }
            } catch (SQLException e) {
                // If column doesn't exist, alter the table and try again
                if (e.getMessage().contains("Unknown column")) {
                    // Add columns to users table
                    Statement alterStmt = conn.createStatement();
                    alterStmt.executeUpdate("ALTER TABLE users ADD COLUMN payment_method VARCHAR(50)");
                    alterStmt.executeUpdate("ALTER TABLE users ADD COLUMN payment_account VARCHAR(100)");
                    alterStmt.close();
                    
                    // Now try the update again
                    String updateUserSql = "UPDATE users SET payment_method = ?, payment_account = ? WHERE id = ?";
                    stmt = conn.prepareStatement(updateUserSql);
                    stmt.setString(1, paymentMethod);
                    stmt.setString(2, paymentAccount);
                    stmt.setInt(3, userId);
                    
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        // Update session
                        HttpSession session = request.getSession();
                        session.setAttribute("paymentMethod", paymentMethod);
                        
                        response.sendRedirect("customer.jsp?tab=settings&message=Payment information updated successfully");
                    } else {
                        throw new Exception("Failed to update payment information");
                    }
                } else {
                    throw e;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=settings&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // REQUEST PASSWORD RESET
    private void requestPasswordReset(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Create password_reset_requests table if it doesn't exist
            try {
                Statement checkStmt = conn.createStatement();
                checkStmt.executeUpdate("CREATE TABLE IF NOT EXISTS password_reset_requests (" +
                                      "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                      "user_id INT NOT NULL, " +
                                      "status VARCHAR(20) DEFAULT 'pending', " +
                                      "request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                      "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
                checkStmt.close();
            } catch (SQLException e) {
                // Table might already exist
            }
            
            // Insert password reset request
            String sql = "INSERT INTO password_reset_requests (user_id) VALUES (?)";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            
            stmt.executeUpdate();
            
            response.sendRedirect("customer.jsp?tab=settings&message=Password reset request sent to admin. You will be notified when processed.");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("customer.jsp?tab=settings&error=" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET DRIVER INFO
    private void getDriverInfo(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Get driver assigned to latest order
            String sql = "SELECT u.*, d.car_number, d.car_type " +
                        "FROM users u " +
                        "LEFT JOIN driver_details d ON u.id = d.driver_id " +
                        "WHERE u.id = (SELECT assigned_driver FROM orders WHERE customer_id = ? AND assigned_driver IS NOT NULL ORDER BY order_date DESC LIMIT 1) " +
                        "LIMIT 1";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            
            if (rs.next()) {
                String profileImage = rs.getString("profile_image");
                if (profileImage == null || profileImage.isEmpty()) {
                    profileImage = "https://ui-avatars.com/api/?name=" + rs.getString("full_name") + "&background=3498db&color=fff";
                }
                
                out.println("<div class='driver-info'>");
                out.println("<img src='" + profileImage + "' alt='Driver' class='driver-avatar'>");
                out.println("<h3 class='driver-name'>" + rs.getString("full_name") + "</h3>");
                out.println("<p class='driver-phone'>📞 " + rs.getString("phone") + "</p>");
                out.println("<p class='driver-vehicle'>🚗 " + rs.getString("car_type") + " - " + rs.getString("car_number") + "</p>");
                out.println("<div style='margin-top: 1.5rem;'>");
                out.println("<button class='btn btn-primary' onclick=\"callDriver('" + rs.getString("phone") + "')\">📞 Call Driver</button>");
                out.println("<button class='btn btn-success' onclick=\"messageDriver('" + rs.getString("phone") + "')\" style='margin-left: 0.5rem;'>💬 Message</button>");
                out.println("</div>");
                out.println("</div>");
            } else {
                out.println("<div class='empty-state'>");
                out.println("<div class='empty-state-icon'>🚚</div>");
                out.println("<h3>No Driver Assigned</h3>");
                out.println("<p>Once an order is placed and assigned, your driver will appear here.</p>");
                out.println("<a href='?tab=products' class='btn btn-primary' style='margin-top: 1rem;'>Place an Order</a>");
                out.println("</div>");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("<div class='alert alert-error'>Error loading driver information: " + e.getMessage() + "</div>");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // GET ORDER PAYMENT INFO
    private void getOrderPayment(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        out.println("<div class='payment-options'>");
        out.println("<h3>Payment Options</h3>");
        out.println("<div class='payment-method'>");
        out.println("<h4>CBE</h4>");
        out.println("<p>Account: 1000XXXXXX</p>");
        out.println("<p>Name: MILL SYSTEM</p>");
        out.println("<button class='btn btn-primary' onclick=\"alert('Send payment to CBE account: 1000XXXXXX\\nReference: Your Order Number')\">Pay with CBE</button>");
        out.println("</div>");
        out.println("<div class='payment-method'>");
        out.println("<h4>Telebirr</h4>");
        out.println("<p>Phone: +2519XXXXXXXX</p>");
        out.println("<p>Name: MILL SYSTEM</p>");
        out.println("<button class='btn btn-primary' onclick=\"alert('Send payment to Telebirr: +2519XXXXXXXX\\nReference: Your Order Number')\">Pay with Telebirr</button>");
        out.println("</div>");
        out.println("<div class='payment-method'>");
        out.println("<h4>Cash on Delivery</h4>");
        out.println("<p>Pay when your order is delivered</p>");
        out.println("<button class='btn btn-primary' onclick=\"confirmCashPayment()\">Select Cash Payment</button>");
        out.println("</div>");
        out.println("</div>");
    }
    
    // DOWNLOAD INVOICE
    private void downloadInvoice(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT o.*, p.name as product_name, u.full_name as customer_name, u.phone, u.address " +
                        "FROM orders o " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "LEFT JOIN users u ON o.customer_id = u.id " +
                        "WHERE o.id = ? AND o.customer_id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                response.setContentType("text/html");
                response.setHeader("Content-Disposition", "attachment; filename=invoice_" + rs.getString("order_number") + ".html");
                
                PrintWriter out = response.getWriter();
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head>");
                out.println("<title>Invoice - " + rs.getString("order_number") + "</title>");
                out.println("<style>");
                out.println("body { font-family: Arial, sans-serif; margin: 40px; }");
                out.println(".invoice-header { border-bottom: 2px solid #333; margin-bottom: 30px; padding-bottom: 10px; }");
                out.println(".invoice-details { margin-bottom: 30px; }");
                out.println(".invoice-table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
                out.println(".invoice-table th, .invoice-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
                out.println(".invoice-table th { background-color: #f2f2f2; }");
                out.println(".total-section { text-align: right; margin-top: 20px; }");
                out.println(".footer { margin-top: 50px; font-size: 12px; color: #666; }");
                out.println("</style>");
                out.println("</head>");
                out.println("<body>");
                
                out.println("<div class='invoice-header'>");
                out.println("<h1>MILL SYSTEM INVOICE</h1>");
                out.println("<h2>Order #" + rs.getString("order_number") + "</h2>");
                out.println("<p>Date: " + rs.getTimestamp("order_date") + "</p>");
                out.println("</div>");
                
                out.println("<div class='invoice-details'>");
                out.println("<h3>Customer Details</h3>");
                out.println("<p><strong>Name:</strong> " + rs.getString("customer_name") + "</p>");
                out.println("<p><strong>Phone:</strong> " + rs.getString("phone") + "</p>");
                out.println("<p><strong>Delivery Address:</strong> " + rs.getString("delivery_address") + "</p>");
                out.println("</div>");
                
                out.println("<table class='invoice-table'>");
                out.println("<thead>");
                out.println("<tr>");
                out.println("<th>Description</th>");
                out.println("<th>Quantity</th>");
                out.println("<th>Unit Price</th>");
                out.println("<th>Total</th>");
                out.println("</tr>");
                out.println("</thead>");
                out.println("<tbody>");
                
                if (rs.getString("product_name") != null) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getString("product_name") + "</td>");
                    out.println("<td>" + rs.getBigDecimal("quantity") + " kg</td>");
                    out.println("<td>" + rs.getBigDecimal("sell_price").divide(rs.getBigDecimal("quantity"), 2, BigDecimal.ROUND_HALF_UP) + " Birr</td>");
                    out.println("<td>" + rs.getBigDecimal("sell_price") + " Birr</td>");
                    out.println("</tr>");
                }
                
                out.println("<tr>");
                out.println("<td>Milling Service</td>");
                out.println("<td>" + rs.getBigDecimal("quantity") + " kg</td>");
                out.println("<td>" + rs.getBigDecimal("milling_charge").divide(rs.getBigDecimal("quantity"), 2, BigDecimal.ROUND_HALF_UP) + " Birr</td>");
                out.println("<td>" + rs.getBigDecimal("milling_charge") + " Birr</td>");
                out.println("</tr>");
                
                out.println("<tr>");
                out.println("<td>Order Fee</td>");
                out.println("<td>-</td>");
                out.println("<td>-</td>");
                out.println("<td>" + rs.getBigDecimal("order_fee") + " Birr</td>");
                out.println("</tr>");
                
                out.println("</tbody>");
                out.println("</table>");
                
                out.println("<div class='total-section'>");
                out.println("<h3>Total Amount: " + rs.getBigDecimal("total_price") + " Birr</h3>");
                out.println("<p><strong>Payment Method:</strong> " + rs.getString("payment_method") + "</p>");
                out.println("<p><strong>Payment Status:</strong> " + rs.getString("payment_status") + "</p>");
                out.println("<p><strong>Order Status:</strong> " + rs.getString("order_status") + "</p>");
                out.println("</div>");
                
                out.println("<div class='footer'>");
                out.println("<p>Thank you for your business!</p>");
                out.println("<p>Mill System - Quality Grains & Milling Services</p>");
                out.println("<p>Contact: +2519XXXXXXXX | Email: info@millsystem.com</p>");
                out.println("</div>");
                
                out.println("</body>");
                out.println("</html>");
            } else {
                response.getWriter().write("Invoice not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("Error generating invoice: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // DOWNLOAD ORDER HISTORY
    private void downloadOrderHistory(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT o.*, p.name as product_name " +
                        "FROM orders o " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "WHERE o.customer_id = ? " +
                        "ORDER BY o.order_date DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=order_history.csv");
            
            PrintWriter out = response.getWriter();
            
            // Write CSV header
            out.println("Order Number,Product,Quantity,Total Price,Order Date,Status,Payment Status,Delivery Address,Payment Method");
            
            // Write data rows
            while (rs.next()) {
                String orderNumber = rs.getString("order_number");
                String productName = rs.getString("product_name") != null ? rs.getString("product_name") : "Special Order";
                String quantity = rs.getBigDecimal("quantity").toString();
                String totalPrice = rs.getBigDecimal("total_price").toString();
                String orderDate = rs.getTimestamp("order_date").toString();
                String orderStatus = rs.getString("order_status");
                String paymentStatus = rs.getString("payment_status");
                String deliveryAddress = escapeCsv(rs.getString("delivery_address"));
                String paymentMethod = rs.getString("payment_method");
                
                out.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    orderNumber, productName, quantity, totalPrice, orderDate,
                    orderStatus, paymentStatus, deliveryAddress, paymentMethod));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("Error downloading order history: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // CANCEL ORDER
    private void cancelOrder(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Check if order belongs to user and is cancellable
            String checkSql = "SELECT order_status FROM orders WHERE id = ? AND customer_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, orderId);
            checkStmt.setInt(2, userId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                throw new Exception("Order not found");
            }
            
            String status = rs.getString("order_status");
            if (!"pending".equals(status) && !"received".equals(status)) {
                throw new Exception("Order cannot be cancelled. Current status: " + status);
            }
            
            // Cancel order
            String updateSql = "UPDATE orders SET order_status = 'cancelled', payment_status = 'cancelled' WHERE id = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, orderId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Order cancelled successfully\"}");
            } else {
                throw new Exception("Failed to cancel order");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // PAY ORDER
    private void payOrder(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Check if order belongs to user and is payable
            String checkSql = "SELECT payment_status FROM orders WHERE id = ? AND customer_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, orderId);
            checkStmt.setInt(2, userId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                throw new Exception("Order not found");
            }
            
            String paymentStatus = rs.getString("payment_status");
            if (!"pending".equals(paymentStatus)) {
                throw new Exception("Payment already " + paymentStatus);
            }
            
            // Mark as paid
            String updateSql = "UPDATE orders SET payment_status = 'paid' WHERE id = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, orderId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Payment marked as paid\"}");
            } else {
                throw new Exception("Failed to update payment status");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // HELPER METHOD: Create special_orders table if not exists
    private void createSpecialOrdersTableIfNotExists() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            // Check if table exists
            String checkSql = "SHOW TABLES LIKE 'special_orders'";
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(checkSql);
            
            if (!rs.next()) {
                // Create the table
                String createSql = "CREATE TABLE special_orders (" +
                                 "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                 "order_number VARCHAR(50) NOT NULL UNIQUE, " +
                                 "customer_id INT NOT NULL, " +
                                 "description TEXT NOT NULL, " +
                                 "grain_type VARCHAR(50) NOT NULL, " +
                                 "quantity DECIMAL(10,2) NOT NULL, " +
                                 "instructions TEXT, " +
                                 "pickup_preference ENUM('delivery', 'pickup') DEFAULT 'delivery', " +
                                 "delivery_address TEXT, " +
                                 "total_price DECIMAL(10,2) NOT NULL, " +
                                 "status ENUM('pending', 'received', 'processing', 'completed', 'delivered', 'cancelled') DEFAULT 'pending', " +
                                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                 "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                                 "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE)";
                
                stmt.executeUpdate(createSql);
                System.out.println("special_orders table created successfully");
            }
            
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // HELPER METHOD: Escape JSON strings
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
 // CANCEL SPECIAL ORDER
    private void cancelSpecialOrder(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            int specialOrderId = Integer.parseInt(request.getParameter("specialOrderId"));
            
            if (userId == null) {
                throw new Exception("User not authenticated");
            }
            
            conn = DatabaseConnection.getConnection();
            
            // Check if order belongs to user and is cancellable
            String checkSql = "SELECT status FROM special_orders WHERE id = ? AND customer_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, specialOrderId);
            checkStmt.setInt(2, userId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                throw new Exception("Special order not found");
            }
            
            String status = rs.getString("status");
            if (!"pending".equals(status)) {
                throw new Exception("Special order cannot be cancelled. Current status: " + status);
            }
            
            // Cancel special order
            String updateSql = "UPDATE special_orders SET status = 'cancelled' WHERE id = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, specialOrderId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Special order cancelled successfully\"}");
            } else {
                throw new Exception("Failed to cancel special order");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    // HELPER METHOD: Escape CSV strings
    private String escapeCsv(String input) {
        if (input == null) return "";
        // If the string contains comma, quote, or newline, wrap it in quotes and escape quotes
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
}