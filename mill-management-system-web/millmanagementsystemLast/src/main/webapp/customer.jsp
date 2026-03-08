<%@ page import="com.mill.system.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Debug information
    System.out.println("=== customer.jsp accessed ===");
    HttpSession userSession = request.getSession(false);
    System.out.println("Session exists: " + (userSession != null));
    
    if (userSession != null) {
        System.out.println("Session ID: " + userSession.getId());
        System.out.println("userId: " + userSession.getAttribute("userId"));
        System.out.println("userType: " + userSession.getAttribute("userType"));
        System.out.println("username: " + userSession.getAttribute("username"));
        System.out.println("user object: " + userSession.getAttribute("user"));
    }
    
    // Check authentication
    String userType = (String) userSession.getAttribute("userType");
    if (!"customer".equals(userType)) {
        System.out.println("Access denied - userType is: " + userType);
        response.sendRedirect("index.html?error=Access denied. User type: " + (userType != null ? userType : "null"));
        return;
    }
    
    // Get user from session
    User user = (User) userSession.getAttribute("user");
    
    // If user object is null, create a minimal one from session attributes
    if (user == null) {
        System.out.println("WARNING: User object is null in session, creating from attributes");
        user = new User();
        user.setId((Integer) userSession.getAttribute("userId"));
        user.setUsername((String) userSession.getAttribute("username"));
        user.setFullName((String) userSession.getAttribute("fullName"));
        user.setEmail((String) userSession.getAttribute("email"));
        user.setUserType((String) userSession.getAttribute("userType"));
        
        // Store it back to session for future use
        userSession.setAttribute("user", user);
    }
    
    int userId = user.getId();
    String tab = request.getParameter("tab") != null ? request.getParameter("tab") : "products";
    String message = request.getParameter("message");
    String error = request.getParameter("error");
    
    // Get cart count
    int cartCount = 0;
    try {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT COUNT(*) as count FROM cart WHERE customer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            cartCount = rs.getInt("count");
        }
        rs.close();
        stmt.close();
        conn.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    System.out.println("User object ready: " + user.getFullName() + ", ID: " + user.getId());
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Customer Dashboard - Mill Management System</title>
    <style>
        :root {
            --primary-color: #2c3e50;
            --secondary-color: #3498db;
            --accent-color: #e74c3c;
            --success-color: #2ecc71;
            --warning-color: #f39c12;
            --light-color: #ecf0f1;
            --dark-color: #2c3e50;
            --text-color: #333;
            --text-light: #fff;
            --sidebar-width: 250px;
        }

        .dark-mode {
            --primary-color: #1a1a2e;
            --secondary-color: #16213e;
            --accent-color: #0f3460;
            --success-color: #27ae60;
            --warning-color: #d35400;
            --light-color: #1a1a2e;
            --dark-color: #0f3460;
            --text-color: #fff;
            --text-light: #fff;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Arial', sans-serif;
            background-color: var(--light-color);
            color: var(--text-color);
            transition: all 0.3s ease;
        }

        .dashboard-container {
            display: flex;
            min-height: 100vh;
        }

        /* Sidebar */
        .sidebar {
            width: var(--sidebar-width);
            background-color: var(--primary-color);
            color: var(--text-light);
            position: fixed;
            height: 100vh;
            overflow-y: auto;
        }

        .sidebar-header {
            padding: 1.5rem;
            text-align: center;
            border-bottom: 1px solid rgba(255,255,255,0.1);
        }

        .sidebar-header h2 {
            font-size: 1.2rem;
            margin-bottom: 0.5rem;
        }

        .profile-img {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            object-fit: cover;
            margin-bottom: 1rem;
            border: 3px solid var(--secondary-color);
        }

        .sidebar-menu {
            list-style: none;
            padding: 1rem 0;
        }

        .sidebar-menu li {
            margin-bottom: 0.5rem;
            position: relative;
        }

        .sidebar-menu a {
            display: block;
            padding: 0.8rem 1.5rem;
            color: var(--text-light);
            text-decoration: none;
            transition: all 0.3s ease;
        }

        .sidebar-menu a:hover,
        .sidebar-menu a.active {
            background-color: var(--secondary-color);
            border-left: 4px solid var(--accent-color);
        }

        .cart-count {
            position: absolute;
            right: 1rem;
            top: 50%;
            transform: translateY(-50%);
            background-color: var(--accent-color);
            color: white;
            font-size: 0.8rem;
            padding: 0.2rem 0.5rem;
            border-radius: 10px;
            min-width: 20px;
            text-align: center;
        }

        /* Main Content */
        .main-content {
            flex: 1;
            margin-left: var(--sidebar-width);
            padding: 1rem;
        }

        .header {
            background-color: var(--text-light);
            padding: 1rem;
            border-radius: 8px;
            margin-bottom: 1rem;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .header-left h1 {
            font-size: 1.5rem;
            color: var(--primary-color);
            margin-bottom: 0.5rem;
        }

        .header-left p {
            color: #666;
            font-size: 0.9rem;
        }

        .header-right {
            display: flex;
            gap: 1rem;
            align-items: center;
        }

        .theme-toggle {
            background: var(--secondary-color);
            color: var(--text-light);
            border: none;
            padding: 0.5rem 1rem;
            border-radius: 4px;
            cursor: pointer;
        }

        /* Messages */
        .alert {
            padding: 1rem;
            border-radius: 4px;
            margin-bottom: 1rem;
        }

        .alert-success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }

        .alert-error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }

        .alert-info {
            background-color: #d1ecf1;
            color: #0c5460;
            border: 1px solid #bee5eb;
        }

        /* Content Area */
        .content-area {
            background-color: var(--text-light);
            border-radius: 8px;
            padding: 1.5rem;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            min-height: calc(100vh - 120px);
        }

        /* Tabs */
        .tab-content {
            display: none;
        }

        .tab-content.active {
            display: block;
            animation: fadeIn 0.5s ease;
        }

        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }

        /* Search and Filter */
        .search-filter {
            display: flex;
            gap: 1rem;
            margin-bottom: 1.5rem;
            align-items: center;
            flex-wrap: wrap;
        }

        .search-box {
            flex: 1;
            min-width: 200px;
            padding: 0.8rem;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 1rem;
        }

        .category-filter {
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
            margin-bottom: 1rem;
        }

        .category-btn {
            padding: 0.5rem 1rem;
            border: 1px solid #ddd;
            background: white;
            border-radius: 20px;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .category-btn.active {
            background-color: var(--secondary-color);
            color: white;
            border-color: var(--secondary-color);
        }

        .category-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        /* Product Grid */
        .products-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 1.5rem;
            margin-top: 1rem;
        }

        .product-card {
            background: white;
            border-radius: 10px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            transition: all 0.3s ease;
            border: 1px solid #eee;
        }

        .product-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 15px rgba(0,0,0,0.2);
        }

        .product-image {
            width: 100%;
            height: 200px;
            object-fit: cover;
        }

        .product-content {
            padding: 1.5rem;
        }

        .product-category {
            display: inline-block;
            padding: 0.3rem 0.8rem;
            background-color: #e8f4fc;
            color: var(--secondary-color);
            border-radius: 15px;
            font-size: 0.8rem;
            margin-bottom: 0.8rem;
        }

        .product-title {
            font-size: 1.2rem;
            margin-bottom: 0.5rem;
            color: var(--primary-color);
        }

        .product-description {
            color: #666;
            font-size: 0.9rem;
            margin-bottom: 1rem;
            line-height: 1.5;
        }

        .product-price {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1rem;
        }

        .price-tag {
            font-size: 1.5rem;
            font-weight: bold;
            color: var(--success-color);
        }

        .price-tag span {
            font-size: 0.9rem;
            color: #999;
            font-weight: normal;
        }

        .milling-price {
            color: var(--warning-color);
            font-size: 0.9rem;
        }

        .product-meta {
            display: flex;
            justify-content: space-between;
            font-size: 0.9rem;
            color: #666;
            margin-bottom: 1rem;
        }

        .product-actions {
            display: flex;
            gap: 0.5rem;
        }

        .btn {
            padding: 0.5rem 1.5rem;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-weight: 500;
            transition: all 0.3s ease;
            text-decoration: none;
            display: inline-block;
            text-align: center;
        }

        .btn-primary {
            background-color: var(--secondary-color);
            color: var(--text-light);
        }

        .btn-success {
            background-color: var(--success-color);
            color: var(--text-light);
        }

        .btn-warning {
            background-color: var(--warning-color);
            color: var(--text-light);
        }

        .btn-danger {
            background-color: var(--accent-color);
            color: var(--text-light);
        }

        .btn-outline {
            background-color: transparent;
            border: 1px solid var(--secondary-color);
            color: var(--secondary-color);
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }

        .btn-full {
            width: 100%;
        }

        /* Cart Page */
        .cart-items {
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }

        .cart-item {
            display: flex;
            gap: 1rem;
            padding: 1rem;
            background: white;
            border-radius: 8px;
            border: 1px solid #eee;
            align-items: center;
        }

        .cart-item-image {
            width: 100px;
            height: 100px;
            object-fit: cover;
            border-radius: 8px;
        }

        .cart-item-details {
            flex: 1;
        }

        .cart-item-title {
            font-weight: bold;
            margin-bottom: 0.5rem;
        }

        .cart-item-price {
            color: var(--success-color);
            font-weight: bold;
        }

        .cart-item-quantity {
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .quantity-btn {
            width: 30px;
            height: 30px;
            border: 1px solid #ddd;
            background: white;
            border-radius: 4px;
            cursor: pointer;
            font-size: 1.2rem;
        }

        .quantity-input {
            width: 50px;
            text-align: center;
            padding: 0.3rem;
            border: 1px solid #ddd;
            border-radius: 4px;
        }

        .cart-summary {
            background: white;
            padding: 1.5rem;
            border-radius: 8px;
            border: 1px solid #eee;
            margin-top: 2rem;
        }

        .summary-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 1rem;
            padding-bottom: 0.5rem;
            border-bottom: 1px solid #eee;
        }

        .summary-total {
            font-size: 1.2rem;
            font-weight: bold;
            color: var(--primary-color);
        }

        /* Orders Page */
        .orders-list {
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }

        .order-card {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            border: 1px solid #eee;
            transition: all 0.3s ease;
        }

        .order-card:hover {
            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
        }

        .order-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1rem;
            padding-bottom: 1rem;
            border-bottom: 1px solid #eee;
        }

        .order-number {
            font-weight: bold;
            color: var(--primary-color);
        }

        .order-date {
            color: #666;
            font-size: 0.9rem;
        }

        .order-status {
            padding: 0.3rem 0.8rem;
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 500;
        }

        .status-pending {
            background-color: #fff3cd;
            color: #856404;
        }

        .status-received {
            background-color: #d1ecf1;
            color: #0c5460;
        }

        .status-processing {
            background-color: #cce5ff;
            color: #004085;
        }

        .status-completed {
            background-color: #d4edda;
            color: #155724;
        }

        .status-delivered {
            background-color: #d4edda;
            color: #155724;
        }

        .order-details {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1rem;
            margin-bottom: 1rem;
        }

        .detail-item {
            display: flex;
            flex-direction: column;
        }

        .detail-label {
            font-size: 0.8rem;
            color: #666;
            margin-bottom: 0.3rem;
        }

        .detail-value {
            font-weight: 500;
        }

        /* Driver Page */
        .driver-info {
            background: white;
            border-radius: 8px;
            padding: 2rem;
            text-align: center;
            border: 1px solid #eee;
        }

        .driver-avatar {
            width: 120px;
            height: 120px;
            border-radius: 50%;
            object-fit: cover;
            margin-bottom: 1rem;
            border: 5px solid var(--secondary-color);
        }

        .driver-name {
            font-size: 1.5rem;
            margin-bottom: 0.5rem;
            color: var(--primary-color);
        }

        .driver-phone {
            color: #666;
            margin-bottom: 1rem;
            font-size: 1.1rem;
        }

        .driver-vehicle {
            display: inline-block;
            padding: 0.5rem 1rem;
            background-color: #e8f4fc;
            color: var(--secondary-color);
            border-radius: 20px;
            font-size: 0.9rem;
        }

        /* Settings Page */
        .settings-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 2rem;
        }

        .settings-card {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            border: 1px solid #eee;
        }

        .settings-card h3 {
            margin-bottom: 1rem;
            padding-bottom: 1rem;
            border-bottom: 1px solid #eee;
            color: var(--primary-color);
        }

        .form-group {
            margin-bottom: 1rem;
        }

        .form-group label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 500;
        }

        .form-group input,
        .form-group select,
        .form-group textarea {
            width: 100%;
            padding: 0.8rem;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 1rem;
        }

        .form-row {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1rem;
        }

        /* Special Order Page */
        .special-order-form {
            max-width: 600px;
            margin: 0 auto;
            background: white;
            padding: 2rem;
            border-radius: 8px;
            border: 1px solid #eee;
        }

        /* Modal */
        .modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.5);
            z-index: 1000;
            align-items: center;
            justify-content: center;
            padding: 1rem;
        }

        .modal-content {
            background: var(--light-color);
            padding: 2rem;
            border-radius: 8px;
            max-width: 500px;
            width: 100%;
            max-height: 80vh;
            overflow-y: auto;
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
        }

        .close-modal {
            background: none;
            border: none;
            font-size: 1.5rem;
            cursor: pointer;
            color: var(--text-color);
        }

        /* Progress Bar */
        .progress-bar {
            height: 6px;
            background-color: #eee;
            border-radius: 3px;
            overflow: hidden;
            margin-bottom: 1rem;
        }

        .progress {
            height: 100%;
            background-color: var(--success-color);
            transition: width 0.3s ease;
        }

        /* Empty States */
        .empty-state {
            text-align: center;
            padding: 3rem;
            color: #666;
        }

        .empty-state-icon {
            font-size: 4rem;
            margin-bottom: 1rem;
            opacity: 0.3;
        }

        /* Responsive */
        @media (max-width: 768px) {
            .sidebar {
                width: 60px;
            }
            
            .sidebar-header h2,
            .sidebar-menu span {
                display: none;
            }
            
            .main-content {
                margin-left: 60px;
            }
            
            .sidebar-menu a {
                text-align: center;
                padding: 0.8rem;
            }
            
            .cart-count {
                right: 0.3rem;
                font-size: 0.7rem;
                padding: 0.1rem 0.3rem;
                min-width: 16px;
            }
            
            .products-grid {
                grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
            }
            
            .cart-item {
                flex-direction: column;
                text-align: center;
            }
            
            .cart-item-image {
                width: 150px;
                height: 150px;
            }
            
            .search-filter {
                flex-direction: column;
            }
            
            .header {
                flex-direction: column;
                gap: 1rem;
                text-align: center;
            }
        }

        @media (max-width: 480px) {
            .products-grid {
                grid-template-columns: 1fr;
            }
            
            .order-details {
                grid-template-columns: 1fr;
            }
            
            .product-actions {
                flex-direction: column;
            }
            
            .btn {
                width: 100%;
            }
        }
    </style>
</head>
<body>
    <div class="dashboard-container">
        <!-- Sidebar -->
        <div class="sidebar">
            <div class="sidebar-header">
                <img src="<%= user.getProfileImage() != null ? user.getProfileImage() : "https://ui-avatars.com/api/?name=" + user.getFullName() + "&background=3498db&color=fff" %>" 
                     alt="Profile" class="profile-img">
                <h2><%= user.getFullName() %></h2>
                <p>Customer</p>
            </div>
            
            <ul class="sidebar-menu">
                <li><a href="?tab=products" class="<%= "products".equals(tab) ? "active" : "" %>">
                    🛒 Products
                </a></li>
                <li><a href="?tab=cart" class="<%= "cart".equals(tab) ? "active" : "" %>">
                    🛍️ My Cart
                    <% if (cartCount > 0) { %>
                        <span class="cart-count"><%= cartCount %></span>
                    <% } %>
                </a></li>
                <li><a href="?tab=orders" class="<%= "orders".equals(tab) ? "active" : "" %>">
                    📦 My Orders
                </a></li>
                <li><a href="?tab=special-order" class="<%= "special-order".equals(tab) ? "active" : "" %>">
                    ⚙️ Special Order
                </a></li>
                <li><a href="?tab=driver" class="<%= "driver".equals(tab) ? "active" : "" %>">
                    🚚 My Driver
                </a></li>
                <li><a href="?tab=settings" class="<%= "settings".equals(tab) ? "active" : "" %>">
                    ⚙️ Settings
                </a></li>
                <li><a href="logout.jsp">🚪 Logout</a></li>
            </ul>
        </div>

        <!-- Main Content -->
        <div class="main-content">
            <div class="header">
                <div class="header-left">
                    <h1>Welcome, <%= user.getFullName() %></h1>
                    <p>Your one-stop shop for quality grains and milling services</p>
                </div>
                <div class="header-right">
                    <button class="theme-toggle" onclick="toggleDarkMode()">🌙 Dark Mode</button>
                    <span class="balance">Balance: <strong>0.00 Birr</strong></span>
                </div>
            </div>

            <!-- Messages -->
            <% if (message != null) { %>
                <div class="alert alert-success">
                    <%= message %>
                </div>
            <% } %>
            
            <% if (error != null) { %>
                <div class="alert alert-error">
                    <%= error %>
                </div>
            <% } %>

            <div class="content-area">
                <!-- Products Tab -->
                <div id="productsTab" class="tab-content <%= "products".equals(tab) ? "active" : "" %>">
                    <h2>Browse Products</h2>
                    
                    <div class="search-filter">
                        <input type="text" class="search-box" id="productSearch" 
                               placeholder="Search products..." onkeyup="searchProducts()">
                        <select class="search-box" id="categoryFilter" onchange="filterProducts()">
                            <option value="all">All Categories</option>
                            <option value="Grain">Grains</option>
                            <option value="Legume">Legumes</option>
                            <option value="Other">Other</option>
                        </select>
                        <select class="search-box" id="sortBy" onchange="sortProducts()">
                            <option value="name">Sort by Name</option>
                            <option value="price_asc">Price: Low to High</option>
                            <option value="price_desc">Price: High to Low</option>
                            <option value="date">Newest First</option>
                        </select>
                    </div>

                    <div class="category-filter">
                        <button class="category-btn active" data-category="all" onclick="filterByCategory('all')">All</button>
                        <button class="category-btn" data-category="Teff" onclick="filterByCategory('Teff')">Teff</button>
                        <button class="category-btn" data-category="Barley" onclick="filterByCategory('Barley')">Barley</button>
                        <button class="category-btn" data-category="Wheat" onclick="filterByCategory('Wheat')">Wheat</button>
                        <button class="category-btn" data-category="Millet" onclick="filterByCategory('Millet')">Millet</button>
                        <button class="category-btn" data-category="Sorghum" onclick="filterByCategory('Sorghum')">Sorghum</button>
                        <button class="category-btn" data-category="Peas" onclick="filterByCategory('Peas')">Peas</button>
                        <button class="category-btn" data-category="Beans" onclick="filterByCategory('Beans')">Beans</button>
                        <button class="category-btn" data-category="Lentils" onclick="filterByCategory('Lentils')">Lentils</button>
                    </div>

                    <div class="products-grid" id="productsGrid">
                        <!-- Products will be loaded from database -->
                        <%
                            try {
                                Connection conn = DatabaseConnection.getConnection();
                                String sql = "SELECT * FROM products WHERE is_posted = true ORDER BY name";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                ResultSet rs = stmt.executeQuery();
                                
                                while (rs.next()) {
                                    String imageUrl = rs.getString("image_url");
                                    if (imageUrl == null || imageUrl.isEmpty()) {
                                        imageUrl = "https://images.unsplash.com/photo-1595341888016-a392ef81b7de?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=80";
                                    }
                        %>
                                    <div class="product-card" data-category="<%= rs.getString("category") %>" 
                                         data-name="<%= rs.getString("name").toLowerCase() %>"
                                         data-price="<%= rs.getBigDecimal("sell_price") %>">
                                        <img src="<%= imageUrl %>" alt="<%= rs.getString("name") %>" class="product-image">
                                        <div class="product-content">
                                            <span class="product-category"><%= rs.getString("category") %></span>
                                            <h3 class="product-title"><%= rs.getString("name") %></h3>
                                            <p class="product-description">
                                                <%= rs.getString("description") != null && rs.getString("description").length() > 100 
                                                    ? rs.getString("description").substring(0, 100) + "..." 
                                                    : rs.getString("description") != null ? rs.getString("description") : "High quality product" %>
                                            </p>
                                            
                                            <div class="product-price">
                                                <div class="price-tag">
                                                    <%= rs.getBigDecimal("sell_price") %> Birr
                                                    <span>/kg</span>
                                                </div>
                                                <div class="milling-price">
                                                    Milling: <%= rs.getBigDecimal("milling_price") %> Birr/kg
                                                </div>
                                            </div>
                                            
                                            <div class="product-meta">
                                                <span>Min. Quantity: <%= rs.getInt("min_quantity") %> kg</span>
                                                <span>Stock: Available</span>
                                            </div>
                                            
                                            <div class="product-actions">
                                                <button class="btn btn-primary btn-full" 
                                                        onclick="addToCart(<%= rs.getInt("id") %>, <%= rs.getInt("min_quantity") %>)">
                                                    🛒 Add to Cart
                                                </button>
                                                <button class="btn btn-success btn-full" 
                                                        onclick="buyNow(<%= rs.getInt("id") %>)">
                                                    🛍️ Buy Now
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                        <%
                                }
                                
                                if (!rs.isBeforeFirst()) {
                        %>
                                    <div class="empty-state">
                                        <div class="empty-state-icon">🛒</div>
                                        <h3>No Products Available</h3>
                                        <p>Check back later for new products.</p>
                                    </div>
                        <%
                                }
                                
                                rs.close();
                                stmt.close();
                                conn.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                        %>
                                <div class="alert alert-error">
                                    Error loading products. Please try again later.
                                </div>
                        <%
                            }
                        %>
                    </div>
                </div>

                <!-- Cart Tab -->
                <div id="cartTab" class="tab-content <%= "cart".equals(tab) ? "active" : "" %>">
                    <h2>My Shopping Cart</h2>
                    
                    <div class="cart-items" id="cartItems">
                        <!-- Cart items will be loaded from database -->
                        <%
                            try {
                                Connection conn = DatabaseConnection.getConnection();
                                String sql = "SELECT c.*, p.name, p.image_url, p.sell_price, p.milling_price, p.min_quantity " +
                                            "FROM cart c JOIN products p ON c.product_id = p.id " +
                                            "WHERE c.customer_id = ?";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                stmt.setInt(1, userId);
                                ResultSet rs = stmt.executeQuery();
                                
                                BigDecimal totalPrice = BigDecimal.ZERO;
                                int itemCount = 0;
                                
                                while (rs.next()) {
                                    itemCount++;
                                    String imageUrl = rs.getString("image_url");
                                    if (imageUrl == null || imageUrl.isEmpty()) {
                                        imageUrl = "https://images.unsplash.com/photo-1595341888016-a392ef81b7de?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=80";
                                    }
                                    
                                    BigDecimal price = rs.getBigDecimal("sell_price");
                                    int quantity = rs.getInt("quantity");
                                    BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));
                                    totalPrice = totalPrice.add(itemTotal);
                        %>
                                    <div class="cart-item" id="cartItem-<%= rs.getInt("id") %>">
                                        <img src="<%= imageUrl %>" alt="<%= rs.getString("name") %>" class="cart-item-image">
                                        <div class="cart-item-details">
                                            <h4 class="cart-item-title"><%= rs.getString("name") %></h4>
                                            <p class="cart-item-price"><%= price %> Birr/kg</p>
                                            <p>Milling: <%= rs.getBigDecimal("milling_price") %> Birr/kg</p>
                                        </div>
                                        <div class="cart-item-quantity">
                                            <button class="quantity-btn" onclick="updateCartQuantity(<%= rs.getInt("id") %>, <%= quantity - 1 %>)">-</button>
                                            <input type="number" class="quantity-input" value="<%= quantity %>" min="<%= rs.getInt("min_quantity") %>" 
                                                   onchange="updateCartQuantity(<%= rs.getInt("id") %>, this.value)">
                                            <button class="quantity-btn" onclick="updateCartQuantity(<%= rs.getInt("id") %>, <%= quantity + 1 %>)">+</button>
                                        </div>
                                        <div class="cart-item-total">
                                            <strong><%= itemTotal %> Birr</strong>
                                        </div>
                                        <button class="btn btn-danger" onclick="removeFromCart(<%= rs.getInt("id") %>)">Remove</button>
                                    </div>
                        <%
                                }
                                
                                if (itemCount == 0) {
                        %>
                                    <div class="empty-state">
                                        <div class="empty-state-icon">🛒</div>
                                        <h3>Your Cart is Empty</h3>
                                        <p>Add some products to get started!</p>
                                        <a href="?tab=products" class="btn btn-primary" style="margin-top: 1rem;">Browse Products</a>
                                    </div>
                        <%
                                }
                                
                                rs.close();
                                stmt.close();
                                conn.close();
                                
                                // Add order fee
                                BigDecimal orderFee = new BigDecimal("20.00");
                                BigDecimal grandTotal = totalPrice.add(orderFee);
                                
                                if (itemCount > 0) {
                        %>
                                    <div class="cart-summary">
                                        <div class="summary-row">
                                            <span>Subtotal:</span>
                                            <span><%= totalPrice %> Birr</span>
                                        </div>
                                        <div class="summary-row">
                                            <span>Order Fee:</span>
                                            <span><%= orderFee %> Birr</span>
                                        </div>
                                        <div class="summary-row summary-total">
                                            <span>Total:</span>
                                            <span><%= grandTotal %> Birr</span>
                                        </div>
                                        
                                        <div style="margin-top: 1.5rem;">
                                            <button class="btn btn-success btn-full" onclick="checkout()">
                                                🛒 Proceed to Checkout
                                            </button>
                                            <button class="btn btn-outline btn-full" onclick="clearCart()" style="margin-top: 0.5rem;">
                                                Clear Cart
                                            </button>
                                        </div>
                                    </div>
                        <%
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                        %>
                                <div class="alert alert-error">
                                    Error loading cart items. Please try again later.
                                </div>
                        <%
                            }
                        %>
                    </div>
                </div>

<div id="ordersTab" class="tab-content <%= "orders".equals(tab) ? "active" : "" %>">
    <h2>My Orders</h2>
    
    <div class="search-filter">
        <input type="text" class="search-box" id="orderSearch" placeholder="Search orders..." onkeyup="filterOrders()">
        <select class="search-box" id="orderStatusFilter" onchange="filterOrders()">
            <option value="all">All Status</option>
            <option value="pending">Pending</option>
            <option value="received">Received</option>
            <option value="processing">Processing</option>
            <option value="completed">Completed</option>
            <option value="delivered">Delivered</option>
            <option value="cancelled">Cancelled</option>
        </select>
    </div>

    <div class="orders-list" id="ordersList">
        <!-- Regular Orders and Special Orders will be loaded from database -->
        <%
            try {
                Connection conn = DatabaseConnection.getConnection();
                int totalOrderCount = 0;
                
                // 1. REGULAR ORDERS from orders table
                String regularSql = "SELECT o.*, p.name as product_name, p.image_url, " +
                                  "op.full_name as operator_name, d.full_name as driver_name " +
                                  "FROM orders o " +
                                  "LEFT JOIN products p ON o.product_id = p.id " +
                                  "LEFT JOIN users op ON o.assigned_operator = op.id " +
                                  "LEFT JOIN users d ON o.assigned_driver = d.id " +
                                  "WHERE o.customer_id = ? " +
                                  "ORDER BY o.order_date DESC";
                PreparedStatement regularStmt = conn.prepareStatement(regularSql);
                regularStmt.setInt(1, userId);
                ResultSet regularRs = regularStmt.executeQuery();
                
                while (regularRs.next()) {
                    totalOrderCount++;
                    String status = regularRs.getString("order_status");
                    String statusClass = "status-pending";
                    
                    switch(status) {
                        case "pending": statusClass = "status-pending"; break;
                        case "received": statusClass = "status-received"; break;
                        case "processing": statusClass = "status-processing"; break;
                        case "completed": statusClass = "status-completed"; break;
                        case "delivered": statusClass = "status-delivered"; break;
                        case "cancelled": statusClass = "status-pending"; break; // Use pending style for cancelled
                    }
                    
                    String productName = regularRs.getString("product_name");
                    if (productName == null && regularRs.getBoolean("is_special_order")) {
                        productName = "Special Order";
                    }
        %>
                    <div class="order-card" data-order-type="regular" data-status="<%= status.toLowerCase() %>" 
                         data-order-number="<%= regularRs.getString("order_number") %>"
                         data-product="<%= productName.toLowerCase() %>">
                        <div class="order-header">
                            <div>
                                <span class="order-number">Order #<%= regularRs.getString("order_number") %></span>
                                <span class="order-date"><%= regularRs.getTimestamp("order_date") %></span>
                            </div>
                            <span class="order-status <%= statusClass %>"><%= status %></span>
                        </div>
                        
                        <div class="order-details">
                            <div class="detail-item">
                                <span class="detail-label">Product</span>
                                <span class="detail-value"><%= productName %></span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Quantity</span>
                                <span class="detail-value"><%= regularRs.getBigDecimal("quantity") %> kg</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Total Amount</span>
                                <span class="detail-value"><%= regularRs.getBigDecimal("total_price") %> Birr</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Payment Status</span>
                                <span class="detail-value <%= "paid".equals(regularRs.getString("payment_status")) ? "status-completed" : "status-pending" %>">
                                    <%= regularRs.getString("payment_status") %>
                                </span>
                            </div>
                        </div>
                        
                        <div class="order-details">
                            <div class="detail-item">
                                <span class="detail-label">Delivery Address</span>
                                <span class="detail-value"><%= regularRs.getString("delivery_address") %></span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Payment Method</span>
                                <span class="detail-value"><%= regularRs.getString("payment_method") %></span>
                            </div>
                            <% if (regularRs.getString("operator_name") != null) { %>
                            <div class="detail-item">
                                <span class="detail-label">Assigned Operator</span>
                                <span class="detail-value"><%= regularRs.getString("operator_name") %></span>
                            </div>
                            <% } %>
                            <% if (regularRs.getString("driver_name") != null) { %>
                            <div class="detail-item">
                                <span class="detail-label">Assigned Driver</span>
                                <span class="detail-value"><%= regularRs.getString("driver_name") %></span>
                            </div>
                            <% } %>
                        </div>
                        
                        <% if ("pending".equals(status) && "pending".equals(regularRs.getString("payment_status"))) { %>
                        <div style="margin-top: 1rem; text-align: right;">
                            <button class="btn btn-success" onclick="payOrder(<%= regularRs.getInt("id") %>)">Pay Now</button>
                            <button class="btn btn-danger" onclick="cancelOrder(<%= regularRs.getInt("id") %>)">Cancel Order</button>
                        </div>
                        <% } %>
                        
                        <% if ("delivered".equals(status)) { %>
                        <div style="margin-top: 1rem;">
                            <button class="btn btn-primary" onclick="downloadInvoice(<%= regularRs.getInt("id") %>)">Download Invoice</button>
                        </div>
                        <% } %>
                    </div>
        <%
                }
                regularRs.close();
                regularStmt.close();
                
                // 2. SPECIAL ORDERS from special_orders table
                String specialSql = "SELECT * FROM special_orders WHERE customer_id = ? ORDER BY created_at DESC";
                PreparedStatement specialStmt = conn.prepareStatement(specialSql);
                specialStmt.setInt(1, userId);
                ResultSet specialRs = specialStmt.executeQuery();
                
                while (specialRs.next()) {
                    totalOrderCount++;
                    String status = specialRs.getString("status");
                    String statusClass = "status-pending";
                    
                    switch(status) {
                        case "pending": statusClass = "status-pending"; break;
                        case "received": statusClass = "status-received"; break;
                        case "processing": statusClass = "status-processing"; break;
                        case "completed": statusClass = "status-completed"; break;
                        case "delivered": statusClass = "status-delivered"; break;
                        case "cancelled": statusClass = "status-pending"; break; // Use pending style for cancelled
                    }
                    
                    // Get payment preference
                    String pickupPref = specialRs.getString("pickup_preference");
                    String paymentMethod = "Cash";
                    if ("pickup".equals(pickupPref)) {
                        paymentMethod = "Cash at Mill";
                    }
        %>
                    <div class="order-card" data-order-type="special" data-status="<%= status.toLowerCase() %>" 
                         data-order-number="SP<%= specialRs.getInt("id") %>"
                         data-product="<%= specialRs.getString("grain_type").toLowerCase() %>">
                        <div class="order-header">
                            <div>
                                <span class="order-number">Special Order #SP<%= specialRs.getInt("id") %></span>
                                <span class="order-date"><%= specialRs.getTimestamp("created_at") %></span>
                            </div>
                            <span class="order-status <%= statusClass %>"><%= status %></span>
                        </div>
                        
                        <div class="order-details">
                            <div class="detail-item">
                                <span class="detail-label">Grain Type</span>
                                <span class="detail-value"><%= specialRs.getString("grain_type") %></span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Description</span>
                                <span class="detail-value"><%= specialRs.getString("description") %></span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Quantity</span>
                                <span class="detail-value"><%= specialRs.getBigDecimal("quantity") %> kg</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Total Amount</span>
                                <span class="detail-value"><%= specialRs.getBigDecimal("total_price") %> Birr</span>
                            </div>
                        </div>
                        
                        <div class="order-details">
                            <div class="detail-item">
                                <span class="detail-label">Instructions</span>
                                <span class="detail-value"><%= specialRs.getString("instructions") != null && !specialRs.getString("instructions").isEmpty() ? specialRs.getString("instructions") : "None" %></span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Pickup/Delivery</span>
                                <span class="detail-value">
                                    <%= "delivery".equals(pickupPref) ? "Delivery to Address" : "Customer Pickup" %>
                                </span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Delivery Address</span>
                                <span class="detail-value"><%= specialRs.getString("delivery_address") %></span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Payment Method</span>
                                <span class="detail-value"><%= paymentMethod %></span>
                            </div>
                        </div>
                        
                        <% if ("pending".equals(status)) { %>
                        <div style="margin-top: 1rem; text-align: right;">
                            <button class="btn btn-danger" onclick="cancelSpecialOrder(<%= specialRs.getInt("id") %>)">Cancel Order</button>
                        </div>
                        <% } %>
                        
                        <% if ("delivered".equals(status)) { %>
                        <div style="margin-top: 1rem;">
                            <button class="btn btn-primary" onclick="downloadSpecialInvoice(<%= specialRs.getInt("id") %>)">Download Receipt</button>
                        </div>
                        <% } %>
                    </div>
        <%
                }
                specialRs.close();
                specialStmt.close();
                
                if (totalOrderCount == 0) {
        %>
                    <div class="empty-state">
                        <div class="empty-state-icon">📦</div>
                        <h3>No Orders Yet</h3>
                        <p>Start shopping to see your orders here!</p>
                        <a href="?tab=products" class="btn btn-primary" style="margin-top: 1rem;">Browse Products</a>
                    </div>
        <%
                }
                
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
        %>
                <div class="alert alert-error">
                    Error loading orders. Please try again later.
                </div>
        <%
            }
        %>
    </div>
</div>

                <!-- Special Order Tab -->
                <div id="specialOrderTab" class="tab-content <%= "special-order".equals(tab) ? "active" : "" %>">
                    <h2>Special Milling Order</h2>
                    <p class="alert alert-info">Have your own grains? Use our milling service!</p>
                    
                    <div class="special-order-form">
                        <form id="specialOrderForm" action="customer" method="POST">
                            <input type="hidden" name="action" value="placeSpecialOrder">
                            
                            <div class="form-group">
                                <label for="specialDescription">Describe your grains *</label>
                                <textarea id="specialDescription" name="description" rows="4" 
                                          placeholder="e.g., 20kg of red teff for milling" required></textarea>
                            </div>
                            
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="specialQuantity">Quantity (kg) *</label>
                                    <input type="number" id="specialQuantity" name="quantity" 
                                           min="1" step="0.5" required>
                                </div>
                                <div class="form-group">
                                    <label for="specialType">Grain Type *</label>
                                    <select id="specialType" name="grainType" required>
                                        <option value="">Select Type</option>
                                        <option value="Teff">Teff</option>
                                        <option value="Barley">Barley</option>
                                        <option value="Wheat">Wheat</option>
                                        <option value="Millet">Millet</option>
                                        <option value="Sorghum">Sorghum</option>
                                        <option value="Peas">Peas</option>
                                        <option value="Beans">Beans</option>
                                        <option value="Lentils">Lentils</option>
                                        <option value="Other">Other</option>
                                    </select>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <label for="specialInstructions">Special Instructions</label>
                                <textarea id="specialInstructions" name="instructions" rows="3" 
                                          placeholder="Any special milling requirements?"></textarea>
                            </div>
                            
                            <div class="form-group">
                                <label for="specialPickup">Pickup/Delivery Preference</label>
                                <select id="specialPickup" name="pickupPreference">
                                    <option value="delivery">Delivery to my address</option>
                                    <option value="pickup">I will bring to mill</option>
                                </select>
                            </div>
                            
                            <div class="form-group" id="specialAddressSection">
                                <label for="specialAddress">Delivery Address *</label>
                                <textarea id="specialAddress" name="deliveryAddress" 
                                          placeholder="Enter delivery address" required></textarea>
                            </div>
                            
                            <div class="form-group">
                                <label>Estimated Cost</label>
                                <div class="alert alert-info" style="margin-top: 0.5rem;">
                                    <strong>Milling Fee:</strong> 10 Birr/kg<br>
                                    <strong>Order Fee:</strong> 20 Birr<br>
                                    <strong>Total:</strong> <span id="specialTotal">0</span> Birr
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <button type="submit" class="btn btn-success btn-full">Place Special Order</button>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Driver Tab -->
                <div id="driverTab" class="tab-content <%= "driver".equals(tab) ? "active" : "" %>">
                    <h2>My Assigned Driver</h2>
                    
                    <div id="driverInfo">
                        <!-- Driver info will be loaded from database -->
                        <%
                            try {
                                Connection conn = DatabaseConnection.getConnection();
                                String sql = "SELECT u.*, d.car_number, d.car_type " +
                                            "FROM users u " +
                                            "LEFT JOIN driver_details d ON u.id = d.driver_id " +
                                            "WHERE u.id = (SELECT assigned_driver FROM orders WHERE customer_id = ? AND assigned_driver IS NOT NULL LIMIT 1) " +
                                            "LIMIT 1";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                stmt.setInt(1, userId);
                                ResultSet rs = stmt.executeQuery();
                                
                                if (rs.next()) {
                        %>
                                    <div class="driver-info">
                                        <img src="<%= rs.getString("profile_image") != null ? rs.getString("profile_image") : "https://ui-avatars.com/api/?name=" + rs.getString("full_name") + "&background=3498db&color=fff" %>" 
                                             alt="Driver" class="driver-avatar">
                                        <h3 class="driver-name"><%= rs.getString("full_name") %></h3>
                                        <p class="driver-phone">📞 <%= rs.getString("phone") %></p>
                                        <p class="driver-vehicle">
                                            🚗 <%= rs.getString("car_type") %> - <%= rs.getString("car_number") %>
                                        </p>
                                        <div style="margin-top: 1.5rem;">
                                            <button class="btn btn-primary" onclick="callDriver('<%= rs.getString("phone") %>')">
                                                📞 Call Driver
                                            </button>
                                            <button class="btn btn-success" onclick="messageDriver('<%= rs.getString("phone") %>')" style="margin-left: 0.5rem;">
                                                💬 Message
                                            </button>
                                        </div>
                                    </div>
                                    
                                    <div style="margin-top: 2rem;">
                                        <h3>Current Delivery</h3>
                                        <div class="order-card">
                                            <!-- Current delivery info would go here -->
                                            <p>Your driver is currently assigned to deliver your latest order.</p>
                                            <p>Estimated delivery time: 2-3 hours</p>
                                            <div class="progress-bar">
                                                <div class="progress" style="width: 60%;"></div>
                                            </div>
                                            <p>Status: On the way to your location</p>
                                        </div>
                                    </div>
                        <%
                                } else {
                        %>
                                    <div class="empty-state">
                                        <div class="empty-state-icon">🚚</div>
                                        <h3>No Driver Assigned</h3>
                                        <p>Once an order is placed and assigned, your driver will appear here.</p>
                                        <a href="?tab=products" class="btn btn-primary" style="margin-top: 1rem;">Place an Order</a>
                                    </div>
                        <%
                                }
                                
                                rs.close();
                                stmt.close();
                                conn.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                        %>
                                <div class="alert alert-error">
                                    Error loading driver information. Please try again later.
                                </div>
                        <%
                            }
                        %>
                    </div>
                </div>

                <!-- Settings Tab -->
                <div id="settingsTab" class="tab-content <%= "settings".equals(tab) ? "active" : "" %>">
                    <h2>Account Settings</h2>
                    
                    <div class="settings-grid">
                        <div class="settings-card">
                            <h3>Profile Information</h3>
                            <form id="profileForm" action="customer" method="POST">
                                <input type="hidden" name="action" value="updateProfile">
                                
                                <div class="form-group">
                                    <label for="profileImage">Profile Picture</label>
                                    <div style="display: flex; align-items: center; gap: 1rem;">
                                        <img id="profilePreview" src="<%= user.getProfileImage() != null ? user.getProfileImage() : "https://ui-avatars.com/api/?name=" + user.getFullName() + "&background=3498db&color=fff" %>" 
                                             alt="Profile" style="width: 80px; height: 80px; border-radius: 50%; object-fit: cover;">
                                        <input type="file" id="profileImage" name="profileImage" accept="image/*" onchange="previewImage(this)">
                                    </div>
                                </div>
                                
                                <div class="form-row">
                                    <div class="form-group">
                                        <label for="fullName">Full Name *</label>
                                        <input type="text" id="fullName" name="fullName" value="<%= user.getFullName() %>" required>
                                    </div>
                                    <div class="form-group">
                                        <label for="email">Email *</label>
                                        <input type="email" id="email" name="email" value="<%= user.getEmail() %>" required>
                                    </div>
                                </div>
                                
                                <div class="form-row">
                                    <div class="form-group">
                                        <label for="phone">Phone Number *</label>
                                        <input type="tel" id="phone" name="phone" value="<%= user.getPhone() != null ? user.getPhone() : "" %>" 
                                               pattern="\+251[79]\d{8}" placeholder="+2519XXXXXXXX" required>
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <label for="address">Address *</label>
                                    <textarea id="address" name="address" rows="3" required><%= user.getAddress() != null ? user.getAddress() : "" %></textarea>
                                </div>
                                
                                <div class="form-group">
                                    <button type="submit" class="btn btn-primary">Update Profile</button>
                                </div>
                            </form>
                        </div>
                        
                        <div class="settings-card">
                            <h3>Security Settings</h3>
                            <form id="passwordForm" action="customer" method="POST">
                                <input type="hidden" name="action" value="changePassword">
                                
                                <div class="form-group">
                                    <label for="currentPassword">Current Password *</label>
                                    <input type="password" id="currentPassword" name="currentPassword" required>
                                </div>
                                
                                <div class="form-row">
                                    <div class="form-group">
                                        <label for="newPassword">New Password *</label>
                                        <input type="password" id="newPassword" name="newPassword" required minlength="6">
                                    </div>
                                    <div class="form-group">
                                        <label for="confirmPassword">Confirm New Password *</label>
                                        <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <button type="submit" class="btn btn-primary">Change Password</button>
                                </div>
                            </form>
                            
                            <div style="margin-top: 2rem;">
                                <h4>Forgot Password?</h4>
                                <p>Request admin to reset your password.</p>
                                <button class="btn btn-warning" onclick="requestPasswordReset()">Request Password Reset</button>
                            </div>
                        </div>
                        
                        <div class="settings-card">
                            <h3>Payment Settings</h3>
                            <form id="paymentForm" action="customer" method="POST">
                                <input type="hidden" name="action" value="updatePayment">
                                
                                <div class="form-group">
                                    <label for="paymentMethod">Preferred Payment Method *</label>
                                    <select id="paymentMethod" name="paymentMethod" required>
                                        <option value="">Select Method</option>
                                        <option value="CBE" <%= "CBE".equals(user.getPaymentMethod()) ? "selected" : "" %>>CBE</option>
                                        <option value="Telebirr" <%= "Telebirr".equals(user.getPaymentMethod()) ? "selected" : "" %>>Telebirr</option>
                                        <option value="Cash" <%= "Cash".equals(user.getPaymentMethod()) ? "selected" : "" %>>Cash on Delivery</option>
                                    </select>
                                </div>
                                
                                <div class="form-group">
                                    <label for="paymentAccount">Account Number *</label>
                                    <input type="text" id="paymentAccount" name="paymentAccount" 
                                           value="<%= user.getPaymentAccount() != null ? user.getPaymentAccount() : "" %>"
                                           pattern="(1000\d+|\\+2519\d{8}|CASH)" required>
                                    <small style="color: #666; font-size: 0.8rem;">
                                        For CBE: Start with 1000<br>
                                        For Telebirr: Phone number starting with +2519<br>
                                        For Cash: Enter "CASH"
                                    </small>
                                </div>
                                
                                <div class="form-group">
                                    <button type="submit" class="btn btn-primary">Update Payment Info</button>
                                </div>
                            </form>
                            
                            <div style="margin-top: 2rem;">
                                <h4>Order History</h4>
                                <p>Total Orders: <strong id="totalOrdersCount">0</strong></p>
                                <p>Total Spent: <strong id="totalSpent">0 Birr</strong></p>
                                <button class="btn btn-outline" onclick="downloadOrderHistory()">Download History</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Buy Now Modal -->
    <div id="buyNowModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Buy Now</h2>
                <button class="close-modal" onclick="closeModal('buyNowModal')">&times;</button>
            </div>
            <form id="buyNowForm" action="customer" method="POST">
                <input type="hidden" name="action" value="placeOrder">
                <input type="hidden" id="buyProductId" name="productId">
                
                <div id="productSummary">
                    <!-- Product summary will be loaded here -->
                </div>
                
                <div class="form-group">
                    <label for="orderQuantity">Quantity (kg) *</label>
                    <input type="number" id="orderQuantity" name="quantity" min="1" step="0.5" required>
                    <small id="minQuantityInfo" style="color: #666; font-size: 0.8rem;"></small>
                </div>
                
                <div class="form-group">
                    <label for="deliveryAddress">Delivery Address *</label>
                    <textarea id="deliveryAddress" name="deliveryAddress" rows="3" required><%= user.getAddress() != null ? user.getAddress() : "" %></textarea>
                </div>
                
                <div class="form-group">
                    <label for="orderPaymentMethod">Payment Method *</label>
                    <select id="orderPaymentMethod" name="paymentMethod" required>
                        <option value="">Select Method</option>
                        <option value="CBE" <%= "CBE".equals(user.getPaymentMethod()) ? "selected" : "" %>>CBE</option>
                        <option value="Telebirr" <%= "Telebirr".equals(user.getPaymentMethod()) ? "selected" : "" %>>Telebirr</option>
                        <option value="Cash">Cash on Delivery</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label>Order Summary</label>
                    <div class="cart-summary" style="margin-top: 0.5rem;">
                        <div class="summary-row">
                            <span>Product Price:</span>
                            <span id="productPrice">0 Birr</span>
                        </div>
                        <div class="summary-row">
                            <span>Milling Charge:</span>
                            <span id="millingCharge">0 Birr</span>
                        </div>
                        <div class="summary-row">
                            <span>Order Fee:</span>
                            <span>20 Birr</span>
                        </div>
                        <div class="summary-row summary-total">
                            <span>Total:</span>
                            <span id="orderTotal">0 Birr</span>
                        </div>
                    </div>
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-success btn-full">Place Order</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Checkout Modal -->
    <div id="checkoutModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Checkout</h2>
                <button class="close-modal" onclick="closeModal('checkoutModal')">&times;</button>
            </div>
            <form id="checkoutForm" action="customer" method="POST">
                <input type="hidden" name="action" value="checkout">
                
                <div class="form-group">
                    <h3>Delivery Information</h3>
                    <p><strong>Name:</strong> <%= user.getFullName() %></p>
                    <p><strong>Phone:</strong> <%= user.getPhone() != null ? user.getPhone() : "Not provided" %></p>
                    
                    <label for="checkoutAddress">Delivery Address *</label>
                    <textarea id="checkoutAddress" name="deliveryAddress" rows="3" required><%= user.getAddress() != null ? user.getAddress() : "" %></textarea>
                </div>
                
                <div class="form-group">
                    <h3>Payment Method</h3>
                    <select id="checkoutPaymentMethod" name="paymentMethod" required>
                        <option value="">Select Method</option>
                        <option value="CBE" <%= "CBE".equals(user.getPaymentMethod()) ? "selected" : "" %>>CBE</option>
                        <option value="Telebirr" <%= "Telebirr".equals(user.getPaymentMethod()) ? "selected" : "" %>>Telebirr</option>
                        <option value="Cash">Cash on Delivery</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <h3>Order Summary</h3>
                    <div id="checkoutSummary">
                        <!-- Checkout summary will be loaded here -->
                    </div>
                </div>
                
                <div class="form-group">
                    <div class="form-check" style="margin-bottom: 1rem;">
                        <input type="checkbox" id="termsAgreement" required>
                        <label for="termsAgreement">I agree to the terms and conditions</label>
                    </div>
                    
                    <button type="submit" class="btn btn-success btn-full">Confirm Order</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Payment Modal -->
    <div id="paymentModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Make Payment</h2>
                <button class="close-modal" onclick="closeModal('paymentModal')">&times;</button>
            </div>
            <div id="paymentContent">
                <!-- Payment options will be loaded here -->
            </div>
        </div>
    </div>

    <script>
        // Dark Mode Toggle
        function toggleDarkMode() {
            document.body.classList.toggle('dark-mode');
            const button = document.querySelector('.theme-toggle');
            button.textContent = document.body.classList.contains('dark-mode') ? '☀️ Light Mode' : '🌙 Dark Mode';
            localStorage.setItem('darkMode', document.body.classList.contains('dark-mode'));
        }

        // Check for saved dark mode preference
        if (localStorage.getItem('darkMode') === 'true') {
            document.body.classList.add('dark-mode');
            document.querySelector('.theme-toggle').textContent = '☀️ Light Mode';
        }

        // Modal Functions
        function openModal(modalId) {
            document.getElementById(modalId).style.display = 'flex';
        }

        function closeModal(modalId) {
            document.getElementById(modalId).style.display = 'none';
        }

        // Product Functions
        function searchProducts() {
            const searchTerm = document.getElementById('productSearch').value.toLowerCase();
            const categoryFilter = document.getElementById('categoryFilter').value;
            const products = document.querySelectorAll('.product-card');
            
            products.forEach(product => {
                const name = product.getAttribute('data-name');
                const category = product.getAttribute('data-category');
                const matchesSearch = name.includes(searchTerm);
                const matchesCategory = categoryFilter === 'all' || category === categoryFilter;
                
                product.style.display = (matchesSearch && matchesCategory) ? 'block' : 'none';
            });
        }

        function filterByCategory(category) {
            // Update active button
            document.querySelectorAll('.category-btn').forEach(btn => {
                btn.classList.remove('active');
            });
            event.target.classList.add('active');
            
            const products = document.querySelectorAll('.product-card');
            
            products.forEach(product => {
                const productName = product.querySelector('.product-title').textContent.toLowerCase();
                const matchesCategory = category === 'all' || productName.includes(category.toLowerCase());
                product.style.display = matchesCategory ? 'block' : 'none';
            });
        }

        function filterProducts() {
            searchProducts(); // Reuse search function
        }

        function sortProducts() {
            const sortBy = document.getElementById('sortBy').value;
            const container = document.getElementById('productsGrid');
            const products = Array.from(container.querySelectorAll('.product-card'));
            
            products.sort((a, b) => {
                switch(sortBy) {
                    case 'name':
                        return a.querySelector('.product-title').textContent.localeCompare(b.querySelector('.product-title').textContent);
                    case 'price_asc':
                        return parseFloat(a.getAttribute('data-price')) - parseFloat(b.getAttribute('data-price'));
                    case 'price_desc':
                        return parseFloat(b.getAttribute('data-price')) - parseFloat(a.getAttribute('data-price'));
                    case 'date':
                        return 0; // Would need date attribute
                    default:
                        return 0;
                }
            });
            
            // Reorder products
            products.forEach(product => container.appendChild(product));
        }
        function cancelSpecialOrder(specialOrderId) {
            if (confirm('Are you sure you want to cancel this special order?')) {
                const formData = new FormData();
                formData.append('action', 'cancelSpecialOrder');
                formData.append('specialOrderId', specialOrderId);
                
                fetch('customer', {
                    method: 'POST',
                    body: formData
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        alert(data.message);
                        window.location.reload();
                    } else {
                        throw new Error(data.error || 'Error cancelling order');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error cancelling order: ' + error.message);
                });
            }
        }
        // Cart Functions
// Update cart quantity function
function updateCartQuantity(cartItemId, newQuantity) {
    if (newQuantity < 1) {
        removeFromCart(cartItemId);
        return;
    }
    
    const formData = new FormData();
    formData.append('action', 'updateCart');
    formData.append('cartItemId', cartItemId);
    formData.append('quantity', newQuantity);
    
    fetch('customer', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            window.location.reload();
        } else {
            throw new Error(data.error || 'Error updating quantity');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error updating quantity: ' + error.message);
    });
}
//Remove from cart function
function removeFromCart(cartItemId) {
    if (!confirm('Remove item from cart?')) return;
    
    const formData = new FormData();
    formData.append('action', 'removeFromCart');
    formData.append('cartItemId', cartItemId);
    
    fetch('customer', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            window.location.reload();
        } else {
            throw new Error(data.error || 'Error removing item');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error removing item: ' + error.message);
    });
}
// Helper function to update cart summary
function updateCartSummary() {
    const cartItems = document.querySelectorAll('.cart-item');
    let subtotal = 0;
    
    cartItems.forEach(item => {
        const priceText = item.querySelector('.cart-item-price').textContent;
        const quantity = parseInt(item.querySelector('.quantity-input').value);
        const price = parseFloat(priceText.replace(/[^\d.]/g, ''));
        subtotal += price * quantity;
    });
    
    const orderFee = 20;
    const total = subtotal + orderFee;
    
    // Update summary if it exists
    const summaryRows = document.querySelectorAll('.summary-row');
    if (summaryRows.length > 0) {
        if (summaryRows[0]) summaryRows[0].querySelector('span:last-child').textContent = subtotal.toFixed(2) + ' Birr';
        if (summaryRows[2]) summaryRows[2].querySelector('span:last-child').textContent = total.toFixed(2) + ' Birr';
    }
    
    updateCartCount();
}
 
     // Clear cart function
        function clearCart() {
            if (!confirm('Clear all items from cart?')) return;
            
            const formData = new FormData();
            formData.append('action', 'clearCart');
            
            fetch('customer', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert(data.message);
                    window.location.reload();
                } else {
                    throw new Error(data.error || 'Error clearing cart');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error clearing cart: ' + error.message);
            });
        }

        function checkout() {
            // Load cart summary
            fetch('customer?action=getCartSummary')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to load cart summary: ' + response.status);
                    }
                    return response.text();
                })
                .then(html => {
                    document.getElementById('checkoutSummary').innerHTML = html;
                    openModal('checkoutModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading checkout: ' + error.message);
                });
        }

        // Buy Now Function
function buyNow(productId) {
    fetch('customer?action=getProductDetails&productId=' + productId)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.status);
            }
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                return response.text().then(text => {
                    console.error('Expected JSON but got:', text);
                    throw new Error('Expected JSON response');
                });
            }
            return response.json();
        })
        .then(data => {
            if (data) {
                document.getElementById('buyProductId').value = productId;
                
                const productSummary = document.getElementById('productSummary');
                productSummary.innerHTML = `
                    <div style="display: flex; gap: 1rem; margin-bottom: 1rem;">
                        <img src="${data.image_url || 'https://images.unsplash.com/photo-1595341888016-a392ef81b7de?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=80'}" 
                             alt="${data.name}" style="width: 100px; height: 100px; object-fit: cover; border-radius: 8px;">
                        <div>
                            <h4>${data.name}</h4>
                            <p>Price: ${data.sell_price} Birr/kg</p>
                            <p>Milling: ${data.milling_price} Birr/kg</p>
                        </div>
                    </div>
                `;
                
                document.getElementById('minQuantityInfo').textContent = `Minimum: ${data.min_quantity} kg`;
                document.getElementById('orderQuantity').min = data.min_quantity;
                document.getElementById('orderQuantity').value = data.min_quantity;
                
                // Set initial prices
                updateOrderTotal();
                
                openModal('buyNowModal');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading product details: ' + error.message);
            // Fallback: Extract from DOM
            extractProductFromDOM(productId);
        });
}

// Fallback function to extract product from DOM
function extractProductFromDOM(productId) {
    // Find product card with this product ID
    const productCard = document.querySelector(`[onclick*="buyNow(${productId})"]`)?.closest('.product-card');
    if (productCard) {
        const name = productCard.querySelector('.product-title').textContent;
        const priceText = productCard.querySelector('.price-tag').textContent;
        const millingText = productCard.querySelector('.milling-price').textContent;
        const minQuantityText = productCard.querySelector('.product-meta span:nth-child(1)').textContent;
        const imageSrc = productCard.querySelector('.product-image').src;
        
        const price = parseFloat(priceText.replace(/[^\d.]/g, ''));
        const millingPrice = parseFloat(millingText.replace(/[^\d.]/g, ''));
        const minQuantity = parseInt(minQuantityText.replace(/[^\d]/g, ''));
        
        document.getElementById('buyProductId').value = productId;
        
        const productSummary = document.getElementById('productSummary');
        productSummary.innerHTML = `
            <div style="display: flex; gap: 1rem; margin-bottom: 1rem;">
                <img src="${imageSrc}" 
                     alt="${name}" style="width: 100px; height: 100px; object-fit: cover; border-radius: 8px;">
                <div>
                    <h4>${name}</h4>
                    <p>Price: ${price} Birr/kg</p>
                    <p>Milling: ${millingPrice} Birr/kg</p>
                </div>
            </div>
        `;
        
        document.getElementById('minQuantityInfo').textContent = `Minimum: ${minQuantity} kg`;
        document.getElementById('orderQuantity').min = minQuantity;
        document.getElementById('orderQuantity').value = minQuantity;
        
        updateOrderTotal();
        openModal('buyNowModal');
    }
}
        function updateOrderTotal() {
            const quantity = parseFloat(document.getElementById('orderQuantity').value) || 0;
            const productPrice = parseFloat(document.querySelector('#productSummary p:nth-child(2)').textContent.replace('Price: ', '').replace(' Birr/kg', '')) || 0;
            const millingPrice = parseFloat(document.querySelector('#productSummary p:nth-child(3)').textContent.replace('Milling: ', '').replace(' Birr/kg', '')) || 0;
            
            const productTotal = quantity * productPrice;
            const millingTotal = quantity * millingPrice;
            const orderFee = 20;
            const total = productTotal + millingTotal + orderFee;
            
            document.getElementById('productPrice').textContent = productTotal.toFixed(2) + ' Birr';
            document.getElementById('millingCharge').textContent = millingTotal.toFixed(2) + ' Birr';
            document.getElementById('orderTotal').textContent = total.toFixed(2) + ' Birr';
        }

        // Add event listener for quantity change
        document.getElementById('orderQuantity')?.addEventListener('input', updateOrderTotal);

        // Special Order Functions
        document.getElementById('specialPickup')?.addEventListener('change', function() {
            const addressSection = document.getElementById('specialAddressSection');
            const addressField = document.getElementById('specialAddress');
            
            if (this.value === 'pickup') {
                addressSection.style.display = 'none';
                addressField.required = false;
                addressField.value = 'I will bring to the mill house';
            } else {
                addressSection.style.display = 'block';
                addressField.required = true;
                addressField.value = '<%= user.getAddress() != null ? user.getAddress() : "" %>';
            }
        });

        document.getElementById('specialQuantity')?.addEventListener('input', function() {
            const quantity = parseFloat(this.value) || 0;
            const millingFee = 10; // Birr/kg
            const orderFee = 20; // Birr
            const total = (quantity * millingFee) + orderFee;
            
            document.getElementById('specialTotal').textContent = total.toFixed(2);
        });

        // Driver Functions
        function callDriver(phoneNumber) {
            if (confirm(`Call driver at ${phoneNumber}?`)) {
                window.location.href = 'tel:' + phoneNumber;
            }
        }

        function messageDriver(phoneNumber) {
            const message = prompt('Enter your message to the driver:');
            if (message) {
                window.location.href = 'sms:' + phoneNumber + '?body=' + encodeURIComponent(message);
            }
        }

        // Order Functions
        function filterOrders() {
            const searchTerm = document.getElementById('orderSearch').value.toLowerCase();
            const statusFilter = document.getElementById('orderStatusFilter').value;
            const orders = document.querySelectorAll('.order-card');
            
            orders.forEach(order => {
                const text = order.textContent.toLowerCase();
                const status = order.querySelector('.order-status').textContent.toLowerCase();
                
                const matchesSearch = text.includes(searchTerm);
                const matchesStatus = statusFilter === 'all' || status === statusFilter;
                
                order.style.display = (matchesSearch && matchesStatus) ? 'block' : 'none';
            });
        }

     // Pay order function
        function payOrder(orderId) {
            const formData = new FormData();
            formData.append('action', 'payOrder');
            formData.append('orderId', orderId);
            
            fetch('customer', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert(data.message);
                    window.location.reload();
                } else {
                    throw new Error(data.error || 'Error processing payment');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error processing payment: ' + error.message);
            });
        }
     // Cancel order function
        function cancelOrder(orderId) {
            if (confirm('Are you sure you want to cancel this order?')) {
                const formData = new FormData();
                formData.append('action', 'cancelOrder');
                formData.append('orderId', orderId);
                
                fetch('customer', {
                    method: 'POST',
                    body: formData
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        alert(data.message);
                        window.location.reload();
                    } else {
                        throw new Error(data.error || 'Error cancelling order');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error cancelling order: ' + error.message);
                });
            }
        }
        function downloadInvoice(orderId) {
            window.open('customer?action=downloadInvoice&orderId=' + orderId, '_blank');
        }

        // Settings Functions
        function previewImage(input) {
            if (input.files && input.files[0]) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    document.getElementById('profilePreview').src = e.target.result;
                };
                reader.readAsDataURL(input.files[0]);
            }
        }

        function requestPasswordReset() {
            if (confirm('Request admin to reset your password?')) {
                fetch('customer?action=requestPasswordReset', {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Password reset request sent to admin');
                    } else {
                        alert('Error sending request');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error sending request');
                });
            }
        }

        function downloadOrderHistory() {
            window.open('customer?action=downloadOrderHistory', '_blank');
        }

        // Form Validation
        document.addEventListener('DOMContentLoaded', function() {
            // Profile form validation
            const profileForm = document.getElementById('profileForm');
            if (profileForm) {
                profileForm.addEventListener('submit', function(e) {
                    const phone = document.getElementById('phone').value;
                    if (!phone.match(/^\+251[79]\d{8}$/)) {
                        e.preventDefault();
                        alert('Phone must be in format +2519XXXXXXXX or +2517XXXXXXXX');
                        return;
                    }
                });
            }
            
            // Password form validation
            const passwordForm = document.getElementById('passwordForm');
            if (passwordForm) {
                passwordForm.addEventListener('submit', function(e) {
                    const newPassword = document.getElementById('newPassword').value;
                    const confirmPassword = document.getElementById('confirmPassword').value;
                    
                    if (newPassword !== confirmPassword) {
                        e.preventDefault();
                        alert('New password and confirmation do not match');
                        return;
                    }
                    
                    if (newPassword.length < 6) {
                        e.preventDefault();
                        alert('Password must be at least 6 characters long');
                        return;
                    }
                });
            }
            
            // Payment form validation
            const paymentForm = document.getElementById('paymentForm');
            if (paymentForm) {
                paymentForm.addEventListener('submit', function(e) {
                    const method = document.getElementById('paymentMethod').value;
                    const account = document.getElementById('paymentAccount').value;
                    
                    if (method === 'CBE' && !account.match(/^1000\d+$/)) {
                        e.preventDefault();
                        alert('CBE account must start with 1000');
                        return;
                    }
                    
                    if (method === 'Telebirr' && !account.match(/^\+2519\d{8}$/)) {
                        e.preventDefault();
                        alert('Telebirr must be a phone number starting with +2519');
                        return;
                    }
                    
                    if (method === 'Cash' && account !== 'CASH') {
                        e.preventDefault();
                        alert('For Cash, enter "CASH" in account field');
                        return;
                    }
                });
            }
            
            // Special order form validation
            const specialOrderForm = document.getElementById('specialOrderForm');
            if (specialOrderForm) {
                specialOrderForm.addEventListener('submit', function(e) {
                    const quantity = document.getElementById('specialQuantity').value;
                    if (quantity < 1) {
                        e.preventDefault();
                        alert('Quantity must be at least 1 kg');
                        return;
                    }
                });
            }
            
            // Calculate total orders and spent amount
            calculateOrderStats();
        });

        function calculateOrderStats() {
            const orderCards = document.querySelectorAll('.order-card');
            let totalOrders = orderCards.length;
            let totalSpent = 0;
            
            orderCards.forEach(card => {
                const totalText = card.querySelector('.detail-item:nth-child(3) .detail-value').textContent;
                const amount = parseFloat(totalText.replace(' Birr', '')) || 0;
                totalSpent += amount;
            });
            
            document.getElementById('totalOrdersCount').textContent = totalOrders;
            document.getElementById('totalSpent').textContent = totalSpent.toFixed(2) + ' Birr';
        }

        // Close modal when clicking outside
        window.onclick = function(event) {
            if (event.target.classList.contains('modal')) {
                event.target.style.display = 'none';
            }
        }

        // Auto-update cart count in sidebar
function updateCartCount() {
    fetch('customer?action=getCartCount')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.status);
            }
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return response.json();
            } else {
                return response.text().then(text => {
                    console.warn('Expected JSON but got text:', text);
                    // Try to parse as JSON anyway
                    try {
                        return JSON.parse(text);
                    } catch (e) {
                        return { count: 0 }; // Default value
                    }
                });
            }
        })
        .then(data => {
            const cartCountElement = document.querySelector('.cart-count');
            if (data && typeof data.count !== 'undefined') {
                if (data.count > 0) {
                    if (!cartCountElement) {
                        const cartLink = document.querySelector('a[href*="tab=cart"]');
                        if (cartLink) {
                            const countSpan = document.createElement('span');
                            countSpan.className = 'cart-count';
                            countSpan.textContent = data.count;
                            cartLink.appendChild(countSpan);
                        }
                    } else {
                        cartCountElement.textContent = data.count;
                    }
                } else if (cartCountElement) {
                    cartCountElement.remove();
                }
            }
        })
        .catch(error => {
            console.error('Error updating cart count:', error);
            // Don't show alert for this error as it's background
        });
}

        // Update cart count periodically
        setInterval(updateCartCount, 30000); // Every 30 seconds

        // Initialize
        window.addEventListener('load', function() {
            // Set active category button based on URL
            const urlParams = new URLSearchParams(window.location.search);
            const category = urlParams.get('category');
            if (category) {
                const btn = document.querySelector(`.category-btn[data-category="${category}"]`);
                if (btn) {
                    filterByCategory(category);
                }
            }
            
            // Auto-refresh driver info if on driver tab
            if (window.location.href.includes('tab=driver')) {
                setInterval(() => {
                    fetch('customer?action=getDriverInfo')
                        .then(response => response.text())
                        .then(html => {
                            document.getElementById('driverInfo').innerHTML = html;
                        });
                }, 60000); // Every minute
            }
        });
    </script>
</body>
</html>