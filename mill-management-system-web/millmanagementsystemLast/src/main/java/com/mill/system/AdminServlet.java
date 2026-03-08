package com.mill.system;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet("/admin")
@MultipartConfig
public class AdminServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String userType = (String) session.getAttribute("userType");
        
        if (!"admin".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("getProducts".equals(action)) {
            getProducts(request, response);
        } else if ("getUsers".equals(action)) {
            getUsers(request, response);
        } else if ("getPasswordRequests".equals(action)) {
            getPasswordRequests(request, response);
        } else if ("getOrders".equals(action)) {
            getAllOrders(request, response);
        } else if ("getProductData".equals(action)) {
            getProductData(request, response);
        } else if ("getUserData".equals(action)) {
            getUserData(request, response);
        } else if ("getStats".equals(action)) {
            getDashboardStats(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String userType = (String) session.getAttribute("userType");
        
        if (!"admin".equals(userType)) {
            response.sendRedirect("index.html");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("addProduct".equals(action)) {
            addProduct(request, response);
        } else if ("updateProduct".equals(action)) {
            updateProduct(request, response);
        } else if ("deleteProduct".equals(action)) {
            deleteProduct(request, response);
        } else if ("postProduct".equals(action)) {
            postProduct(request, response);
        } else if ("addUser".equals(action)) {
            addUser(request, response);
        } else if ("updateUser".equals(action)) {
            updateUser(request, response);
        } else if ("deleteUser".equals(action)) {
            deleteUser(request, response);
        } else if ("handlePasswordRequest".equals(action)) {
            handlePasswordRequest(request, response);
        } else if ("updateProfile".equals(action)) {
            updateProfile(request, response);
        }
    }
    
    // 1. ADMIN ADDS PRODUCT - Save to database
    private void addProduct(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String name = request.getParameter("name");
            String category = request.getParameter("category");
            String description = request.getParameter("description");
            double purchasePrice = Double.parseDouble(request.getParameter("purchasePrice"));
            double sellPrice = Double.parseDouble(request.getParameter("sellPrice"));
            double millingPrice = Double.parseDouble(request.getParameter("millingPrice"));
            int minQuantity = Integer.parseInt(request.getParameter("minQuantity"));
            String imageUrl = request.getParameter("imageUrl");
            int addedBy = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO products (name, category, description, purchase_price, " +
                        "sell_price, milling_price, min_quantity, image_url, added_by, is_posted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setString(3, description);
            stmt.setDouble(4, purchasePrice);
            stmt.setDouble(5, sellPrice);
            stmt.setDouble(6, millingPrice);
            stmt.setInt(7, minQuantity);
            stmt.setString(8, imageUrl);
            stmt.setInt(9, addedBy);
            
            stmt.executeUpdate();
            response.sendRedirect("admin.jsp?tab=products&message=Product saved successfully. Post it to make visible.");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=products&error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Get products for admin view
    private void getProducts(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT * FROM products ORDER BY is_posted, added_date DESC";
            rs = stmt.executeQuery(sql);
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td>" + rs.getInt("id") + "</td>");
                out.println("<td><img src='" + rs.getString("image_url") + "' width='50' height='50'></td>");
                out.println("<td>" + rs.getString("name") + "</td>");
                out.println("<td>" + rs.getString("category") + "</td>");
                out.println("<td>" + rs.getBigDecimal("purchase_price") + "</td>");
                out.println("<td>" + rs.getBigDecimal("sell_price") + "</td>");
                out.println("<td>" + rs.getInt("min_quantity") + "</td>");
                
                if (rs.getBoolean("is_posted")) {
                    out.println("<td><span class='status-badge status-active'>Posted</span></td>");
                } else {
                    out.println("<td><span class='status-badge status-pending'>Not Posted</span></td>");
                }
                
                out.println("<td>");
                out.println("<div class='action-buttons'>");
                out.println("<button class='btn btn-primary btn-sm' onclick=\"openProductModal(" + rs.getInt("id") + ")\">Edit</button>");
                
                if (!rs.getBoolean("is_posted")) {
                    out.println("<button class='btn btn-success btn-sm' onclick=\"postProduct(" + rs.getInt("id") + ")\">Post</button>");
                }
                
                out.println("<button class='btn btn-danger btn-sm' onclick=\"deleteProduct(" + rs.getInt("id") + ")\">Delete</button>");
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
    
    // Get product data for editing
    private void getProductData(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM products WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            if (rs.next()) {
                out.println("{");
                out.println("\"id\": " + rs.getInt("id") + ",");
                out.println("\"name\": \"" + rs.getString("name") + "\",");
                out.println("\"category\": \"" + rs.getString("category") + "\",");
                out.println("\"description\": \"" + rs.getString("description") + "\",");
                out.println("\"purchasePrice\": " + rs.getBigDecimal("purchase_price") + ",");
                out.println("\"sellPrice\": " + rs.getBigDecimal("sell_price") + ",");
                out.println("\"millingPrice\": " + rs.getBigDecimal("milling_price") + ",");
                out.println("\"minQuantity\": " + rs.getInt("min_quantity") + ",");
                out.println("\"imageUrl\": \"" + rs.getString("image_url") + "\",");
                out.println("\"isPosted\": " + rs.getBoolean("is_posted"));
                out.println("}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Update product
    private void updateProduct(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            String name = request.getParameter("name");
            String category = request.getParameter("category");
            String description = request.getParameter("description");
            BigDecimal purchasePrice = new BigDecimal(request.getParameter("purchasePrice"));
            BigDecimal sellPrice = new BigDecimal(request.getParameter("sellPrice"));
            BigDecimal millingPrice = new BigDecimal(request.getParameter("millingPrice"));
            int minQuantity = Integer.parseInt(request.getParameter("minQuantity"));
            String imageUrl = request.getParameter("imageUrl");
            
            // Handle file upload
            Part imagePart = request.getPart("imageFile");
            if (imagePart != null && imagePart.getSize() > 0) {
                String fileName = UUID.randomUUID().toString() + "_" + getFileName(imagePart);
                String uploadPath = getServletContext().getRealPath("") + "uploads" + File.separator;
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) uploadDir.mkdir();
                
                imagePart.write(uploadPath + fileName);
                imageUrl = "uploads/" + fileName;
            }
            
            // Validate prices
            if (sellPrice.compareTo(purchasePrice) <= 0) {
                response.sendRedirect("admin.jsp?tab=products&error=Sell price must be greater than purchase price");
                return;
            }
            
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE products SET name = ?, category = ?, description = ?, " +
                        "purchase_price = ?, sell_price = ?, milling_price = ?, " +
                        "min_quantity = ?, image_url = COALESCE(?, image_url) " +
                        "WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setString(3, description);
            stmt.setBigDecimal(4, purchasePrice);
            stmt.setBigDecimal(5, sellPrice);
            stmt.setBigDecimal(6, millingPrice);
            stmt.setInt(7, minQuantity);
            stmt.setString(8, imageUrl);
            stmt.setInt(9, productId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                response.sendRedirect("admin.jsp?tab=products&message=Product updated successfully");
            } else {
                response.sendRedirect("admin.jsp?tab=products&error=Product not found");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=products&error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Delete product
    private void deleteProduct(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            
            conn = DatabaseConnection.getConnection();
            
            // Check if product has associated orders
            String checkSql = "SELECT COUNT(*) as order_count FROM orders WHERE product_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, productId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt("order_count") > 0) {
                // Soft delete instead of hard delete
                String softDeleteSql = "UPDATE products SET is_posted = FALSE, name = CONCAT(name, ' (Deleted)') WHERE id = ?";
                stmt = conn.prepareStatement(softDeleteSql);
                stmt.setInt(1, productId);
                stmt.executeUpdate();
                
                response.sendRedirect("admin.jsp?tab=products&message=Product marked as deleted (has associated orders)");
            } else {
                // Hard delete
                String deleteSql = "DELETE FROM products WHERE id = ?";
                stmt = conn.prepareStatement(deleteSql);
                stmt.setInt(1, productId);
                int rows = stmt.executeUpdate();
                
                if (rows > 0) {
                    response.sendRedirect("admin.jsp?tab=products&message=Product deleted successfully");
                } else {
                    response.sendRedirect("admin.jsp?tab=products&error=Product not found");
                }
            }
            
            rs.close();
            checkStmt.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=products&error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Post product - make it visible to customers
    private void postProduct(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            int adminId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            // First, check if product exists
            String checkSql = "SELECT * FROM products WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, productId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                response.sendRedirect("admin.jsp?tab=products&error=Product not found");
                return;
            }
            
            // Post the product (make it visible to customers)
            String sql = "UPDATE products SET is_posted = TRUE, added_date = NOW() WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Log the posting activity
                String logSql = "INSERT INTO admin_logs (admin_id, action, details) VALUES (?, 'post_product', ?)";
                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, adminId);
                logStmt.setString(2, "Posted product ID: " + productId);
                logStmt.executeUpdate();
                logStmt.close();
                
                response.sendRedirect("admin.jsp?tab=products&message=Product posted successfully! Customers can now see it.");
            } else {
                response.sendRedirect("admin.jsp?tab=products&error=Failed to post product");
            }
            
            rs.close();
            checkStmt.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=products&error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Get users
    private void getUsers(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String userTypeFilter = request.getParameter("type");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE user_type = ? ORDER BY created_at DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, userTypeFilter);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td>" + rs.getInt("id") + "</td>");
                out.println("<td>" + rs.getString("username") + "</td>");
                out.println("<td>" + rs.getString("email") + "</td>");
                out.println("<td>" + rs.getString("full_name") + "</td>");
                out.println("<td>" + rs.getString("phone") + "</td>");
                
                if ("active".equals(rs.getString("status"))) {
                    out.println("<td><span class='status-badge status-active'>Active</span></td>");
                } else {
                    out.println("<td><span class='status-badge status-inactive'>Inactive</span></td>");
                }
                
                out.println("<td>" + rs.getTimestamp("created_at") + "</td>");
                out.println("<td>");
                out.println("<div class='action-buttons'>");
                out.println("<button class='btn btn-primary btn-sm' onclick=\"openUserModal(" + rs.getInt("id") + ")\">Edit</button>");
                out.println("<button class='btn btn-danger btn-sm' onclick=\"deleteUser(" + rs.getInt("id") + ", '" + userTypeFilter + "')\">Delete</button>");
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
    
    // Get user data for editing
    private void getUserData(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        PreparedStatement driverStmt = null;
        ResultSet driverRs = null;
        
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            
            if (rs.next()) {
                out.println("{");
                out.println("\"id\": " + rs.getInt("id") + ",");
                out.println("\"username\": \"" + rs.getString("username") + "\",");
                out.println("\"email\": \"" + rs.getString("email") + "\",");
                out.println("\"fullName\": \"" + rs.getString("full_name") + "\",");
                out.println("\"phone\": \"" + rs.getString("phone") + "\",");
                out.println("\"userType\": \"" + rs.getString("user_type") + "\",");
                out.println("\"status\": \"" + rs.getString("status") + "\"");
                
                // If driver, get driver details
                if ("driver".equals(rs.getString("user_type"))) {
                    String driverSql = "SELECT * FROM driver_details WHERE driver_id = ?";
                    driverStmt = conn.prepareStatement(driverSql);
                    driverStmt.setInt(1, userId);
                    driverRs = driverStmt.executeQuery();
                    
                    if (driverRs.next()) {
                        out.println(",");
                        out.println("\"carNumber\": \"" + driverRs.getString("car_number") + "\",");
                        out.println("\"carType\": \"" + driverRs.getString("car_type") + "\",");
                        out.println("\"licenseNumber\": \"" + driverRs.getString("license_number") + "\"");
                    }
                }
                
                out.println("}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (driverRs != null) driverRs.close(); } catch (SQLException e) {}
            try { if (driverStmt != null) driverStmt.close(); } catch (SQLException e) {}
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Add user
    private void addUser(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        PreparedStatement checkStmt = null;
        ResultSet rs = null;
        
        try {
            String username = request.getParameter("username");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String fullName = request.getParameter("fullName");
            String phone = request.getParameter("phone");
            String userType = request.getParameter("userType");
            String carNumber = request.getParameter("carNumber");
            String carType = request.getParameter("carType");
            String licenseNumber = request.getParameter("licenseNumber");
            
            conn = DatabaseConnection.getConnection();
            
            // Check for duplicate username/email
            String checkSql = "SELECT COUNT(*) as count FROM users WHERE username = ? OR email = ?";
            checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt("count") > 0) {
                response.sendRedirect("admin.jsp?tab=" + userType + "s&error=Username or email already exists");
                return;
            }
            rs.close();
            checkStmt.close();
            
            // Hash password
            String hashedPassword = hashPassword(password);
            
            // Add user
            String sql = "INSERT INTO users (username, email, password, full_name, phone, user_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'active')";
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, fullName);
            stmt.setString(5, phone);
            stmt.setString(6, userType);
            
            int rows = stmt.executeUpdate();
            
            if (rows == 0) {
                response.sendRedirect("admin.jsp?tab=" + userType + "s&error=Failed to add user");
                return;
            }
            
            // If driver, add driver details
            if ("driver".equals(userType)) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int driverId = generatedKeys.getInt(1);
                    
                    String driverSql = "INSERT INTO driver_details (driver_id, car_number, car_type, license_number) " +
                                     "VALUES (?, ?, ?, ?)";
                    stmt2 = conn.prepareStatement(driverSql);
                    stmt2.setInt(1, driverId);
                    stmt2.setString(2, carNumber);
                    stmt2.setString(3, carType);
                    stmt2.setString(4, licenseNumber);
                    stmt2.executeUpdate();
                }
                generatedKeys.close();
            }
            
            response.sendRedirect("admin.jsp?tab=" + userType + "s&message=User added successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg.contains("Duplicate entry")) {
                response.sendRedirect("admin.jsp?tab=" + request.getParameter("userType") + "s&error=Username or email already exists");
            } else {
                response.sendRedirect("admin.jsp?tab=" + request.getParameter("userType") + "s&error=" + URLEncoder.encode(errorMsg, "UTF-8"));
            }
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (checkStmt != null) checkStmt.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (stmt2 != null) stmt2.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Update user
    private void updateUser(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement driverStmt = null;
        
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String username = request.getParameter("username");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String fullName = request.getParameter("fullName");
            String phone = request.getParameter("phone");
            String userType = request.getParameter("userType");
            
            conn = DatabaseConnection.getConnection();
            
            // Check if username or email already exists (excluding current user)
            String checkSql = "SELECT COUNT(*) as count FROM users WHERE (username = ? OR email = ?) AND id != ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            checkStmt.setInt(3, userId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt("count") > 0) {
                response.sendRedirect("admin.jsp?tab=" + userType + "s&error=Username or email already exists");
                return;
            }
            rs.close();
            checkStmt.close();
            
            // Update user
            String sql = "UPDATE users SET username = ?, email = ?, full_name = ?, phone = ?";
            if (password != null && !password.isEmpty()) {
                sql += ", password = ?";
            }
            sql += " WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, fullName);
            stmt.setString(4, phone);
            
            int paramIndex = 5;
            if (password != null && !password.isEmpty()) {
                stmt.setString(paramIndex++, hashPassword(password));
            }
            stmt.setInt(paramIndex, userId);
            
            stmt.executeUpdate();
            
            // If driver, update driver details
            if ("driver".equals(userType)) {
                String carNumber = request.getParameter("carNumber");
                String carType = request.getParameter("carType");
                String licenseNumber = request.getParameter("licenseNumber");
                
                // Check if driver details exist
                String checkDriverSql = "SELECT COUNT(*) as count FROM driver_details WHERE driver_id = ?";
                PreparedStatement checkDriverStmt = conn.prepareStatement(checkDriverSql);
                checkDriverStmt.setInt(1, userId);
                ResultSet driverRs = checkDriverStmt.executeQuery();
                
                if (driverRs.next() && driverRs.getInt("count") > 0) {
                    // Update existing
                    String driverSql = "UPDATE driver_details SET car_number = ?, car_type = ?, license_number = ? " +
                                     "WHERE driver_id = ?";
                    driverStmt = conn.prepareStatement(driverSql);
                    driverStmt.setString(1, carNumber);
                    driverStmt.setString(2, carType);
                    driverStmt.setString(3, licenseNumber);
                    driverStmt.setInt(4, userId);
                } else {
                    // Insert new
                    String driverSql = "INSERT INTO driver_details (driver_id, car_number, car_type, license_number) " +
                                     "VALUES (?, ?, ?, ?)";
                    driverStmt = conn.prepareStatement(driverSql);
                    driverStmt.setInt(1, userId);
                    driverStmt.setString(2, carNumber);
                    driverStmt.setString(3, carType);
                    driverStmt.setString(4, licenseNumber);
                }
                
                driverStmt.executeUpdate();
                driverRs.close();
                checkDriverStmt.close();
            }
            
            response.sendRedirect("admin.jsp?tab=" + userType + "s&message=User updated successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=users&error=" + e.getMessage());
        } finally {
            try { if (driverStmt != null) driverStmt.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Delete user
    private void deleteUser(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String userType = request.getParameter("userType");
            
            conn = DatabaseConnection.getConnection();
            
            // Check if user has associated data
            String checkSql = "";
            if ("customer".equals(userType)) {
                checkSql = "SELECT COUNT(*) as count FROM orders WHERE customer_id = ?";
            } else if ("driver".equals(userType)) {
                checkSql = "SELECT COUNT(*) as count FROM orders WHERE assigned_driver = ?";
            } else if ("operator".equals(userType)) {
                checkSql = "SELECT COUNT(*) as count FROM orders WHERE assigned_operator = ?";
            }
            
            int associatedData = 0;
            if (!checkSql.isEmpty()) {
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, userId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    associatedData = rs.getInt("count");
                }
                rs.close();
                checkStmt.close();
            }
            
            if (associatedData > 0) {
                // Soft delete - mark as inactive
                String softDeleteSql = "UPDATE users SET status = 'inactive' WHERE id = ?";
                stmt = conn.prepareStatement(softDeleteSql);
                stmt.setInt(1, userId);
                stmt.executeUpdate();
                
                response.sendRedirect("admin.jsp?tab=" + userType + "s&message=User marked as inactive (has associated data)");
            } else {
                // Hard delete
                // First delete from driver_details if driver
                if ("driver".equals(userType)) {
                    String deleteDriverSql = "DELETE FROM driver_details WHERE driver_id = ?";
                    PreparedStatement driverStmt = conn.prepareStatement(deleteDriverSql);
                    driverStmt.setInt(1, userId);
                    driverStmt.executeUpdate();
                    driverStmt.close();
                }
                
                // Delete user
                String deleteSql = "DELETE FROM users WHERE id = ?";
                stmt = conn.prepareStatement(deleteSql);
                stmt.setInt(1, userId);
                int rows = stmt.executeUpdate();
                
                if (rows > 0) {
                    response.sendRedirect("admin.jsp?tab=" + userType + "s&message=User deleted successfully");
                } else {
                    response.sendRedirect("admin.jsp?tab=" + userType + "s&error=User not found");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=users&error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Get password requests
    private void getPasswordRequests(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String status = request.getParameter("status");
            if (status == null) status = "pending";
            
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT pr.*, u.full_name, u.email, u2.full_name as handled_by_name " +
                        "FROM password_requests pr " +
                        "JOIN users u ON pr.customer_id = u.id " +
                        "LEFT JOIN users u2 ON pr.handled_by = u2.id " +
                        "WHERE pr.status = ? " +
                        "ORDER BY pr.request_date DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            rs = stmt.executeQuery();
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td>" + rs.getInt("id") + "</td>");
                out.println("<td>" + rs.getString("full_name") + "</td>");
                
                if ("pending".equals(status)) {
                    out.println("<td>" + rs.getString("email") + "</td>");
                    out.println("<td>" + rs.getTimestamp("request_date") + "</td>");
                    out.println("<td>");
                    out.println("<div class='action-buttons'>");
                    out.println("<button class='btn btn-success btn-sm' onclick=\"handlePasswordRequest(" + rs.getInt("id") + ", 'approved')\">Approve</button>");
                    out.println("<button class='btn btn-danger btn-sm' onclick=\"handlePasswordRequest(" + rs.getInt("id") + ", 'rejected')\">Reject</button>");
                    out.println("</div>");
                    out.println("</td>");
                } else {
                    out.println("<td>" + rs.getString("handled_by_name") + "</td>");
                    out.println("<td>" + rs.getTimestamp("request_date") + "</td>");
                    out.println("<td>" + rs.getTimestamp("handled_date") + "</td>");
                }
                
                out.println("</tr>");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("error.html");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Handle password request
    private void handlePasswordRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int requestId = Integer.parseInt(request.getParameter("requestId"));
            String action = request.getParameter("action");
            int adminId = (Integer) request.getSession().getAttribute("userId");
            
            conn = DatabaseConnection.getConnection();
            
            // Get customer ID from request
            String getSql = "SELECT customer_id FROM password_requests WHERE id = ? AND status = 'pending'";
            stmt = conn.prepareStatement(getSql);
            stmt.setInt(1, requestId);
            ResultSet rs = stmt.executeQuery();
            
            if (!rs.next()) {
                response.sendRedirect("admin.jsp?tab=password-requests&error=Request not found or already handled");
                return;
            }
            
            int customerId = rs.getInt("customer_id");
            rs.close();
            stmt.close();
            
            if ("approved".equals(action)) {
                // Generate new temporary password
                String tempPassword = generateTemporaryPassword();
                String hashedPassword = hashPassword(tempPassword);
                
                // Update user password
                String updatePasswordSql = "UPDATE users SET password = ? WHERE id = ?";
                stmt = conn.prepareStatement(updatePasswordSql);
                stmt.setString(1, hashedPassword);
                stmt.setInt(2, customerId);
                stmt.executeUpdate();
                stmt.close();
                
                // TODO: Send email to customer with temporary password
                System.out.println("Password reset for customer ID " + customerId + 
                                 ". Temporary password: " + tempPassword);
            }
            
            // Update request status
            String updateRequestSql = "UPDATE password_requests SET status = ?, handled_by = ?, handled_date = NOW() WHERE id = ?";
            stmt = conn.prepareStatement(updateRequestSql);
            stmt.setString(1, action);
            stmt.setInt(2, adminId);
            stmt.setInt(3, requestId);
            stmt.executeUpdate();
            
            response.sendRedirect("admin.jsp?tab=password-requests&message=Password request " + action + " successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=password-requests&error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Get all orders
    private void getAllOrders(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT o.*, u.full_name as customer_name, p.name as product_name " +
                        "FROM orders o " +
                        "LEFT JOIN users u ON o.customer_id = u.id " +
                        "LEFT JOIN products p ON o.product_id = p.id " +
                        "ORDER BY o.order_date DESC";
            
            rs = stmt.executeQuery(sql);
            
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td>" + rs.getString("order_number") + "</td>");
                out.println("<td>" + rs.getString("customer_name") + "</td>");
                out.println("<td>" + rs.getString("product_name") + "</td>");
                out.println("<td>" + rs.getBigDecimal("quantity") + "</td>");
                out.println("<td>" + rs.getBigDecimal("total_price") + "</td>");
                out.println("<td>" + rs.getString("payment_status") + "</td>");
                out.println("<td>" + rs.getString("order_status") + "</td>");
                out.println("<td>" + rs.getTimestamp("order_date") + "</td>");
                out.println("<td>");
                out.println("<button class='btn btn-primary btn-sm' onclick=\"viewOrderDetails(" + rs.getInt("id") + ")\">View</button>");
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
    
    // Get dashboard statistics
    private void getDashboardStats(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            StringBuilder stats = new StringBuilder();
            stats.append("{");
            
            // Total products
            String sql1 = "SELECT COUNT(*) as count FROM products";
            PreparedStatement stmt1 = conn.prepareStatement(sql1);
            ResultSet rs1 = stmt1.executeQuery();
            if (rs1.next()) {
                stats.append("\"totalProducts\":").append(rs1.getInt("count")).append(",");
            }
            rs1.close();
            stmt1.close();
            
            // Total customers
            String sql2 = "SELECT COUNT(*) as count FROM users WHERE user_type = 'customer'";
            PreparedStatement stmt2 = conn.prepareStatement(sql2);
            ResultSet rs2 = stmt2.executeQuery();
            if (rs2.next()) {
                stats.append("\"totalCustomers\":").append(rs2.getInt("count")).append(",");
            }
            rs2.close();
            stmt2.close();
            
            // Total orders
            String sql3 = "SELECT COUNT(*) as count FROM orders";
            PreparedStatement stmt3 = conn.prepareStatement(sql3);
            ResultSet rs3 = stmt3.executeQuery();
            if (rs3.next()) {
                stats.append("\"totalOrders\":").append(rs3.getInt("count")).append(",");
            }
            rs3.close();
            stmt3.close();
            
            // Pending password requests
            String sql4 = "SELECT COUNT(*) as count FROM password_requests WHERE status = 'pending'";
            PreparedStatement stmt4 = conn.prepareStatement(sql4);
            ResultSet rs4 = stmt4.executeQuery();
            if (rs4.next()) {
                stats.append("\"pendingRequests\":").append(rs4.getInt("count"));
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
    
    // Update admin profile
    private void updateProfile(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            int adminId = (Integer) request.getSession().getAttribute("userId");
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
                verifyStmt.setInt(1, adminId);
                ResultSet rs = verifyStmt.executeQuery();
                
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (!storedPassword.equals(hashPassword(currentPassword))) {
                        response.sendRedirect("admin.jsp?tab=profile&error=Current password is incorrect");
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
            stmt.setInt(paramIndex, adminId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Update session attributes
                HttpSession session = request.getSession();
                session.setAttribute("username", username);
                session.setAttribute("fullName", fullName);
                
                response.sendRedirect("admin.jsp?tab=profile&message=Profile updated successfully");
            } else {
                response.sendRedirect("admin.jsp?tab=profile&error=Profile update failed");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.jsp?tab=profile&error=" + e.getMessage());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    
    // Helper methods
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        String[] tokens = contentDisposition.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
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
            return password; // fallback (not secure for production)
        }
    }
    
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }
}