<%@ page import="com.mill.system.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
// Get user from session
User user = (User) session.getAttribute("user");

// If null, create minimal user from session attributes
if (user == null) {
    user = new User();
    user.setId((Integer) session.getAttribute("userId"));
    user.setUsername((String) session.getAttribute("username"));
    user.setFullName((String) session.getAttribute("fullName"));
    user.setEmail((String) session.getAttribute("email"));
    user.setUserType((String) session.getAttribute("userType"));
    
    // Optionally store back to session
    session.setAttribute("user", user);
}
    // Check authentication
    HttpSession userSession = request.getSession();
    String userType = (String) userSession.getAttribute("userType");
    if (!"admin".equals(userType)) {
        response.sendRedirect("index.html");
        return;
    }
    
    //User user = (User) userSession.getAttribute("user");
    String tab = request.getParameter("tab") != null ? request.getParameter("tab") : "dashboard";
    String message = request.getParameter("message");
    String error = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - Mill Management System</title>
    <style>
        :root {
            --primary-color: #2c3e50;
            --secondary-color: #3498db;
            --accent-color: #e74c3c;
            --success-color: #2ecc71;
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

        .sidebar-menu i {
            margin-right: 10px;
            width: 20px;
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

        .header h1 {
            font-size: 1.5rem;
            color: var(--primary-color);
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

        /* Cards */
        .stats-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1rem;
            margin-bottom: 2rem;
        }

        .stat-card {
            background: linear-gradient(135deg, var(--secondary-color), var(--primary-color));
            color: var(--text-light);
            padding: 1.5rem;
            border-radius: 8px;
            text-align: center;
            cursor: pointer;
            transition: transform 0.3s ease;
        }

        .stat-card:hover {
            transform: translateY(-5px);
        }

        .stat-card h3 {
            font-size: 2rem;
            margin-bottom: 0.5rem;
        }

        .stat-card p {
            opacity: 0.9;
        }

        /* Tables */
        .data-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }

        .data-table th,
        .data-table td {
            padding: 0.8rem;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }

        .data-table th {
            background-color: var(--primary-color);
            color: var(--text-light);
            position: sticky;
            top: 0;
        }

        .data-table tr:hover {
            background-color: #f5f5f5;
        }

        .dark-mode .data-table tr:hover {
            background-color: rgba(255,255,255,0.1);
        }

        /* Forms */
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

        .btn {
            padding: 0.5rem 1.5rem;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-weight: 500;
            transition: all 0.3s ease;
            text-decoration: none;
            display: inline-block;
        }

        .btn-primary {
            background-color: var(--secondary-color);
            color: var(--text-light);
        }

        .btn-success {
            background-color: var(--success-color);
            color: var(--text-light);
        }

        .btn-danger {
            background-color: var(--accent-color);
            color: var(--text-light);
        }

        .btn-sm {
            padding: 0.3rem 0.8rem;
            font-size: 0.9rem;
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }

        /* Actions */
        .action-buttons {
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
        }

        /* Status badges */
        .status-badge {
            padding: 0.3rem 0.8rem;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 500;
        }

        .status-active {
            background-color: #d4edda;
            color: #155724;
        }

        .status-pending {
            background-color: #fff3cd;
            color: #856404;
        }

        .status-inactive {
            background-color: #f8d7da;
            color: #721c24;
        }

        /* Tabs navigation within content */
        .content-tabs {
            display: flex;
            border-bottom: 2px solid #eee;
            margin-bottom: 1.5rem;
        }

        .content-tab {
            padding: 0.8rem 1.5rem;
            cursor: pointer;
            border: none;
            background: none;
            font-weight: 500;
            color: #666;
            position: relative;
        }

        .content-tab.active {
            color: var(--secondary-color);
        }

        .content-tab.active::after {
            content: '';
            position: absolute;
            bottom: -2px;
            left: 0;
            right: 0;
            height: 2px;
            background-color: var(--secondary-color);
        }

        /* Search and filter */
        .search-filter {
            display: flex;
            gap: 1rem;
            margin-bottom: 1rem;
            align-items: center;
        }

        .search-box {
            flex: 1;
            padding: 0.8rem;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 1rem;
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
        }

        .modal-content {
            background: var(--light-color);
            padding: 2rem;
            border-radius: 8px;
            max-width: 500px;
            width: 90%;
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
            
            .sidebar-menu i {
                margin-right: 0;
                font-size: 1.2rem;
            }

            .stats-cards {
                grid-template-columns: 1fr;
            }

            .search-filter {
                flex-direction: column;
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
                <p>Administrator</p>
            </div>
            
            <ul class="sidebar-menu">
                <li><a href="?tab=dashboard" class="<%= "dashboard".equals(tab) ? "active" : "" %>">
                    📊 Dashboard
                </a></li>
                <li><a href="?tab=products" class="<%= "products".equals(tab) ? "active" : "" %>">
                    🛒 Product Management
                </a></li>
                <li><a href="?tab=operators" class="<%= "operators".equals(tab) ? "active" : "" %>">
                    👨‍💼 Operator Management
                </a></li>
                <li><a href="?tab=customers" class="<%= "customers".equals(tab) ? "active" : "" %>">
                    👥 Customer Management
                </a></li>
                <li><a href="?tab=drivers" class="<%= "drivers".equals(tab) ? "active" : "" %>">
                    🚚 Driver Management
                </a></li>
                <li><a href="?tab=password-requests" class="<%= "password-requests".equals(tab) ? "active" : "" %>">
                    🔐 Password Requests
                </a></li>
                <li><a href="?tab=orders" class="<%= "orders".equals(tab) ? "active" : "" %>">
                    📦 Order Management
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
                <h1>Admin Dashboard</h1>
                <button class="theme-toggle" onclick="toggleDarkMode()">🌙 Dark Mode</button>
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
                <!-- Dashboard Tab -->
                <div id="dashboardTab" class="tab-content <%= "dashboard".equals(tab) ? "active" : "" %>">
                    <div class="stats-cards">
                        <div class="stat-card" onclick="window.location='?tab=products'">
                            <h3 id="totalProducts">0</h3>
                            <p>Total Products</p>
                        </div>
                        <div class="stat-card" onclick="window.location='?tab=customers'">
                            <h3 id="totalCustomers">0</h3>
                            <p>Total Customers</p>
                        </div>
                        <div class="stat-card" onclick="window.location='?tab=orders'">
                            <h3 id="totalOrders">0</h3>
                            <p>Total Orders</p>
                        </div>
                        <div class="stat-card" onclick="window.location='?tab=password-requests'">
                            <h3 id="pendingRequests">0</h3>
                            <p>Pending Requests</p>
                        </div>
                    </div>

                    <h2>Recent Orders</h2>
                    <div class="search-filter">
                        <input type="text" class="search-box" placeholder="Search orders..." onkeyup="searchOrders(this.value)">
                        <select class="search-box" onchange="filterOrders(this.value)">
                            <option value="all">All Status</option>
                            <option value="pending">Pending</option>
                            <option value="received">Received</option>
                            <option value="processing">Processing</option>
                            <option value="completed">Completed</option>
                            <option value="delivered">Delivered</option>
                        </select>
                    </div>
                    <table class="data-table" id="recentOrders">
                        <thead>
                            <tr>
                                <th>Order #</th>
                                <th>Customer</th>
                                <th>Product</th>
                                <th>Quantity</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <!-- Orders will be loaded dynamically -->
                        </tbody>
                    </table>
                </div>

                <!-- Products Tab -->
                <div id="productsTab" class="tab-content <%= "products".equals(tab) ? "active" : "" %>">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h2>Product Management</h2>
                        <div class="action-buttons">
                            <button class="btn btn-primary" onclick="openProductModal()">➕ Add Product</button>
                            <button class="btn btn-success" onclick="exportProducts()">📊 Export Products</button>
                        </div>
                    </div>

                    <div class="search-filter">
                        <input type="text" class="search-box" placeholder="Search products..." onkeyup="searchProducts(this.value)">
                        <select class="search-box" onchange="filterProducts(this.value)">
                            <option value="all">All Categories</option>
                            <option value="Grain">Grains</option>
                            <option value="Legume">Legumes</option>
                            <option value="Other">Other</option>
                        </select>
                        <select class="search-box" onchange="filterProductStatus(this.value)">
                            <option value="all">All Status</option>
                            <option value="posted">Posted Only</option>
                            <option value="not-posted">Not Posted</option>
                        </select>
                    </div>

                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Image</th>
                                <th>Name</th>
                                <th>Category</th>
                                <th>Purchase Price</th>
                                <th>Sell Price</th>
                                <th>Min Qty</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="productsTable">
                            <!-- Products will be loaded dynamically -->
                        </tbody>
                    </table>
                </div>

                <!-- Operators Tab -->
                <div id="operatorsTab" class="tab-content <%= "operators".equals(tab) ? "active" : "" %>">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h2>Operator Management</h2>
                        <button class="btn btn-primary" onclick="openUserModal('operator')">➕ Add Operator</button>
                    </div>

                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Phone</th>
                                <th>Status</th>
                                <th>Joined Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="operatorsTable">
                            <!-- Operators will be loaded from database -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT * FROM users WHERE user_type = 'operator' ORDER BY created_at DESC";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    while (rs.next()) {
                            %>
                                        <tr>
                                            <td><%= rs.getInt("id") %></td>
                                            <td><%= rs.getString("full_name") %></td>
                                            <td><%= rs.getString("email") %></td>
                                            <td><%= rs.getString("phone") != null ? rs.getString("phone") : "N/A" %></td>
                                            <td>
                                                <span class="status-badge <%= "active".equals(rs.getString("status")) ? "status-active" : "status-inactive" %>">
                                                    <%= rs.getString("status") %>
                                                </span>
                                            </td>
                                            <td><%= rs.getTimestamp("created_at") %></td>
                                            <td>
                                                <div class="action-buttons">
                                                    <button class="btn btn-primary btn-sm" 
                                                            onclick="editUser(<%= rs.getInt("id") %>, 'operator')">Edit</button>
                                                    <button class="btn btn-danger btn-sm" 
                                                            onclick="deleteUser(<%= rs.getInt("id") %>, 'operator')">Delete</button>
                                                    <% if ("active".equals(rs.getString("status"))) { %>
                                                        <button class="btn btn-danger btn-sm" 
                                                                onclick="toggleUserStatus(<%= rs.getInt("id") %>, 'inactive', 'operator')">Deactivate</button>
                                                    <% } else { %>
                                                        <button class="btn btn-success btn-sm" 
                                                                onclick="toggleUserStatus(<%= rs.getInt("id") %>, 'active', 'operator')">Activate</button>
                                                    <% } %>
                                                </div>
                                            </td>
                                        </tr>
                            <%
                                    }
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </tbody>
                    </table>
                </div>

                <!-- Customers Tab -->
                <div id="customersTab" class="tab-content <%= "customers".equals(tab) ? "active" : "" %>">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h2>Customer Management</h2>
                        <div class="action-buttons">
                            <button class="btn btn-primary" onclick="exportCustomers()">📊 Export Customers</button>
                        </div>
                    </div>

                    <div class="search-filter">
                        <input type="text" class="search-box" placeholder="Search customers..." onkeyup="searchCustomers(this.value)">
                        <select class="search-box" onchange="filterCustomerStatus(this.value)">
                            <option value="all">All Status</option>
                            <option value="active">Active Only</option>
                            <option value="inactive">Inactive</option>
                        </select>
                    </div>

                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Phone</th>
                                <th>Address</th>
                                <th>Payment Method</th>
                                <th>Status</th>
                                <th>Joined Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="customersTable">
                            <!-- Customers will be loaded from database -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT * FROM users WHERE user_type = 'customer' ORDER BY created_at DESC";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    while (rs.next()) {
                            %>
                                        <tr>
                                            <td><%= rs.getInt("id") %></td>
                                            <td><%= rs.getString("full_name") %></td>
                                            <td><%= rs.getString("email") %></td>
                                            <td><%= rs.getString("phone") != null ? rs.getString("phone") : "N/A" %></td>
                                            <td><%= rs.getString("address") != null ? (rs.getString("address").length() > 30 ? rs.getString("address").substring(0, 30) + "..." : rs.getString("address")) : "N/A" %></td>
                                            <td><%= rs.getString("payment_method") != null ? rs.getString("payment_method") : "N/A" %></td>
                                            <td>
                                                <span class="status-badge <%= "active".equals(rs.getString("status")) ? "status-active" : "status-inactive" %>">
                                                    <%= rs.getString("status") %>
                                                </span>
                                            </td>
                                            <td><%= rs.getTimestamp("created_at") %></td>
                                            <td>
                                                <div class="action-buttons">
                                                    <button class="btn btn-primary btn-sm" 
                                                            onclick="viewCustomerDetails(<%= rs.getInt("id") %>)">View</button>
                                                    <button class="btn btn-danger btn-sm" 
                                                            onclick="deleteUser(<%= rs.getInt("id") %>, 'customer')">Delete</button>
                                                    <% if ("active".equals(rs.getString("status"))) { %>
                                                        <button class="btn btn-danger btn-sm" 
                                                                onclick="toggleUserStatus(<%= rs.getInt("id") %>, 'inactive', 'customer')">Deactivate</button>
                                                    <% } else { %>
                                                        <button class="btn btn-success btn-sm" 
                                                                onclick="toggleUserStatus(<%= rs.getInt("id") %>, 'active', 'customer')">Activate</button>
                                                    <% } %>
                                                </div>
                                            </td>
                                        </tr>
                            <%
                                    }
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </tbody>
                    </table>
                </div>

                <!-- Drivers Tab -->
                <div id="driversTab" class="tab-content <%= "drivers".equals(tab) ? "active" : "" %>">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h2>Driver Management</h2>
                        <button class="btn btn-primary" onclick="openUserModal('driver')">➕ Add Driver</button>
                    </div>

                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Phone</th>
                                <th>Car Number</th>
                                <th>Car Type</th>
                                <th>License No.</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="driversTable">
                            <!-- Drivers will be loaded from database -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT u.*, d.car_number, d.car_type, d.license_number " +
                                                "FROM users u LEFT JOIN driver_details d ON u.id = d.driver_id " +
                                                "WHERE u.user_type = 'driver' ORDER BY u.created_at DESC";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    while (rs.next()) {
                            %>
                                        <tr>
                                            <td><%= rs.getInt("id") %></td>
                                            <td><%= rs.getString("full_name") %></td>
                                            <td><%= rs.getString("email") %></td>
                                            <td><%= rs.getString("phone") != null ? rs.getString("phone") : "N/A" %></td>
                                            <td><%= rs.getString("car_number") != null ? rs.getString("car_number") : "N/A" %></td>
                                            <td><%= rs.getString("car_type") != null ? rs.getString("car_type") : "N/A" %></td>
                                            <td><%= rs.getString("license_number") != null ? rs.getString("license_number") : "N/A" %></td>
                                            <td>
                                                <span class="status-badge <%= "active".equals(rs.getString("status")) ? "status-active" : "status-inactive" %>">
                                                    <%= rs.getString("status") %>
                                                </span>
                                            </td>
                                            <td>
                                                <div class="action-buttons">
                                                    <button class="btn btn-primary btn-sm" 
                                                            onclick="editUser(<%= rs.getInt("id") %>, 'driver')">Edit</button>
                                                    <button class="btn btn-danger btn-sm" 
                                                            onclick="deleteUser(<%= rs.getInt("id") %>, 'driver')">Delete</button>
                                                    <% if ("active".equals(rs.getString("status"))) { %>
                                                        <button class="btn btn-danger btn-sm" 
                                                                onclick="toggleUserStatus(<%= rs.getInt("id") %>, 'inactive', 'driver')">Deactivate</button>
                                                    <% } else { %>
                                                        <button class="btn btn-success btn-sm" 
                                                                onclick="toggleUserStatus(<%= rs.getInt("id") %>, 'active', 'driver')">Activate</button>
                                                    <% } %>
                                                </div>
                                            </td>
                                        </tr>
                            <%
                                    }
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </tbody>
                    </table>
                </div>

                <!-- Password Requests Tab -->
                <div id="passwordRequestsTab" class="tab-content <%= "password-requests".equals(tab) ? "active" : "" %>">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h2>Password Reset Requests</h2>
                        <button class="btn btn-primary" onclick="refreshRequests()">🔄 Refresh</button>
                    </div>

                    <div class="content-tabs">
                        <button class="content-tab active" onclick="showRequestTab('pending')">Pending</button>
                        <button class="content-tab" onclick="showRequestTab('approved')">Approved</button>
                        <button class="content-tab" onclick="showRequestTab('rejected')">Rejected</button>
                    </div>

                    <div id="pendingRequestsSection">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Request ID</th>
                                    <th>Customer Name</th>
                                    <th>Customer Email</th>
                                    <th>Request Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="pendingRequestsTable">
                                <!-- Pending requests will be loaded from database -->
                                <%
                                    try {
                                        Connection conn = DatabaseConnection.getConnection();
                                        String sql = "SELECT pr.*, u.full_name, u.email " +
                                                    "FROM password_requests pr " +
                                                    "JOIN users u ON pr.customer_id = u.id " +
                                                    "WHERE pr.status = 'pending' " +
                                                    "ORDER BY pr.request_date DESC";
                                        PreparedStatement stmt = conn.prepareStatement(sql);
                                        ResultSet rs = stmt.executeQuery();
                                        
                                        while (rs.next()) {
                                %>
                                            <tr>
                                                <td><%= rs.getInt("id") %></td>
                                                <td><%= rs.getString("full_name") %></td>
                                                <td><%= rs.getString("email") %></td>
                                                <td><%= rs.getTimestamp("request_date") %></td>
                                                <td>
                                                    <div class="action-buttons">
                                                        <button class="btn btn-success btn-sm" 
                                                                onclick="handlePasswordRequest(<%= rs.getInt("id") %>, 'approved')">Approve</button>
                                                        <button class="btn btn-danger btn-sm" 
                                                                onclick="handlePasswordRequest(<%= rs.getInt("id") %>, 'rejected')">Reject</button>
                                                    </div>
                                                </td>
                                            </tr>
                                <%
                                        }
                                        rs.close();
                                        stmt.close();
                                        conn.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                %>
                            </tbody>
                        </table>
                    </div>

                    <div id="approvedRequestsSection" style="display: none;">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Request ID</th>
                                    <th>Customer Name</th>
                                    <th>Approved By</th>
                                    <th>Request Date</th>
                                    <th>Approved Date</th>
                                </tr>
                            </thead>
                            <tbody id="approvedRequestsTable">
                                <!-- Approved requests will be loaded from database -->
                            </tbody>
                        </table>
                    </div>

                    <div id="rejectedRequestsSection" style="display: none;">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Request ID</th>
                                    <th>Customer Name</th>
                                    <th>Rejected By</th>
                                    <th>Request Date</th>
                                    <th>Rejected Date</th>
                                </tr>
                            </thead>
                            <tbody id="rejectedRequestsTable">
                                <!-- Rejected requests will be loaded from database -->
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Order Management Tab -->
                <div id="ordersTab" class="tab-content <%= "orders".equals(tab) ? "active" : "" %>">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h2>Order Management</h2>
                        <div class="action-buttons">
                            <button class="btn btn-primary" onclick="exportOrders()">📊 Export Orders</button>
                            <button class="btn btn-success" onclick="createOfflineOrder()">➕ Offline Order</button>
                        </div>
                    </div>

                    <div class="search-filter">
                        <input type="text" class="search-box" placeholder="Search orders..." onkeyup="searchAllOrders(this.value)">
                        <select class="search-box" onchange="filterOrderType(this.value)">
                            <option value="all">All Orders</option>
                            <option value="online">Online Orders</option>
                            <option value="offline">Offline Orders</option>
                            <option value="special">Special Orders</option>
                        </select>
                        <select class="search-box" onchange="filterOrderStatusAll(this.value)">
                            <option value="all">All Status</option>
                            <option value="pending">Pending</option>
                            <option value="received">Received</option>
                            <option value="processing">Processing</option>
                            <option value="completed">Completed</option>
                            <option value="delivered">Delivered</option>
                        </select>
                    </div>

                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Order #</th>
                                <th>Customer</th>
                                <th>Product</th>
                                <th>Quantity</th>
                                <th>Total Amount</th>
                                <th>Payment Status</th>
                                <th>Order Status</th>
                                <th>Assigned To</th>
                                <th>Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="allOrdersTable">
                            <!-- All orders will be loaded from database -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT o.*, u.full_name as customer_name, p.name as product_name, " +
                                                "op.full_name as operator_name, d.full_name as driver_name " +
                                                "FROM orders o " +
                                                "LEFT JOIN users u ON o.customer_id = u.id " +
                                                "LEFT JOIN products p ON o.product_id = p.id " +
                                                "LEFT JOIN users op ON o.assigned_operator = op.id " +
                                                "LEFT JOIN users d ON o.assigned_driver = d.id " +
                                                "ORDER BY o.order_date DESC LIMIT 50";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    while (rs.next()) {
                                        String orderStatus = rs.getString("order_status");
                                        String statusClass = "";
                                        switch(orderStatus) {
                                            case "pending": statusClass = "status-pending"; break;
                                            case "completed": 
                                            case "delivered": statusClass = "status-active"; break;
                                            default: statusClass = "status-pending";
                                        }
                            %>
                                        <tr>
                                            <td><%= rs.getString("order_number") %></td>
                                            <td><%= rs.getString("customer_name") %></td>
                                            <td><%= rs.getString("product_name") != null ? rs.getString("product_name") : "Special Order" %></td>
                                            <td><%= rs.getBigDecimal("quantity") %> kg</td>
                                            <td><%= rs.getBigDecimal("total_price") %> Birr</td>
                                            <td>
                                                <span class="status-badge <%= "paid".equals(rs.getString("payment_status")) ? "status-active" : "status-pending" %>">
                                                    <%= rs.getString("payment_status") %>
                                                </span>
                                            </td>
                                            <td>
                                                <span class="status-badge <%= statusClass %>">
                                                    <%= orderStatus %>
                                                </span>
                                            </td>
                                            <td>
                                                <% if (rs.getString("operator_name") != null) { %>
                                                    Operator: <%= rs.getString("operator_name") %><br>
                                                <% } %>
                                                <% if (rs.getString("driver_name") != null) { %>
                                                    Driver: <%= rs.getString("driver_name") %>
                                                <% } %>
                                            </td>
                                            <td><%= rs.getTimestamp("order_date") %></td>
                                            <td>
                                                <div class="action-buttons">
                                                    <button class="btn btn-primary btn-sm" 
                                                            onclick="viewOrderDetails(<%= rs.getInt("id") %>)">View</button>
                                                    <button class="btn btn-success btn-sm" 
                                                            onclick="updateOrderStatus(<%= rs.getInt("id") %>, 'processing')">Process</button>
                                                    <% if (!"delivered".equals(orderStatus)) { %>
                                                        <button class="btn btn-success btn-sm" 
                                                                onclick="markAsDelivered(<%= rs.getInt("id") %>)">Deliver</button>
                                                    <% } %>
                                                </div>
                                            </td>
                                        </tr>
                            <%
                                    }
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </tbody>
                    </table>
                </div>

                <!-- Settings Tab -->
                <div id="settingsTab" class="tab-content <%= "settings".equals(tab) ? "active" : "" %>">
                    <h2>Admin Settings</h2>
                    
                    <div class="content-tabs">
                        <button class="content-tab active" onclick="showSettingsTab('profile')">Profile</button>
                        <button class="content-tab" onclick="showSettingsTab('security')">Security</button>
                        <button class="content-tab" onclick="showSettingsTab('system')">System Settings</button>
                    </div>

                    <div id="profileSettings" class="settings-section">
                        <h3>Profile Settings</h3>
                        <form id="profileForm" action="admin" method="POST" enctype="multipart/form-data">
                            <input type="hidden" name="action" value="updateProfile">
                            <input type="hidden" name="userId" value="<%= user.getId() %>">
                            
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="profileImage">Profile Image</label>
                                    <div style="display: flex; align-items: center; gap: 1rem;">
                                        <img id="profilePreview" src="<%= user.getProfileImage() != null ? user.getProfileImage() : "https://ui-avatars.com/api/?name=" + user.getFullName() + "&background=3498db&color=fff" %>" 
                                             alt="Profile" style="width: 100px; height: 100px; border-radius: 50%; object-fit: cover;">
                                        <input type="file" id="profileImage" name="profileImage" accept="image/*" onchange="previewImage(this)">
                                    </div>
                                </div>
                            </div>

                            <div class="form-row">
                                <div class="form-group">
                                    <label for="fullName">Full Name</label>
                                    <input type="text" id="fullName" name="fullName" value="<%= user.getFullName() %>" required>
                                </div>
                                <div class="form-group">
                                    <label for="email">Email</label>
                                    <input type="email" id="email" name="email" value="<%= user.getEmail() %>" required>
                                </div>
                            </div>

                            <div class="form-row">
                                <div class="form-group">
                                    <label for="phone">Phone Number</label>
                                    <input type="tel" id="phone" name="phone" value="<%= user.getPhone() != null ? user.getPhone() : "" %>" 
                                           pattern="\+251[79]\d{8}" placeholder="+2519XXXXXXXX">
                                </div>
                            </div>

                            <div class="form-group">
                                <button type="submit" class="btn btn-primary">Update Profile</button>
                            </div>
                        </form>
                    </div>

                    <div id="securitySettings" class="settings-section" style="display: none;">
                        <h3>Security Settings</h3>
                        <form id="passwordForm" action="admin" method="POST">
                            <input type="hidden" name="action" value="changePassword">
                            <input type="hidden" name="userId" value="<%= user.getId() %>">
                            
                            <div class="form-group">
                                <label for="currentPassword">Current Password</label>
                                <input type="password" id="currentPassword" name="currentPassword" required>
                            </div>

                            <div class="form-row">
                                <div class="form-group">
                                    <label for="newPassword">New Password</label>
                                    <input type="password" id="newPassword" name="newPassword" required minlength="6">
                                </div>
                                <div class="form-group">
                                    <label for="confirmPassword">Confirm New Password</label>
                                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                                </div>
                            </div>

                            <div class="form-group">
                                <button type="submit" class="btn btn-primary">Change Password</button>
                            </div>
                        </form>
                    </div>

                    <div id="systemSettings" class="settings-section" style="display: none;">
                        <h3>System Settings</h3>
                        <form id="systemForm" action="admin" method="POST">
                            <input type="hidden" name="action" value="updateSystemSettings">
                            
                            <div class="form-group">
                                <label for="orderFee">Order Fee (Birr)</label>
                                <input type="number" id="orderFee" name="orderFee" value="20.00" step="0.01" min="0">
                            </div>

                            <div class="form-group">
                                <label for="minOrderAmount">Minimum Order Amount (Birr)</label>
                                <input type="number" id="minOrderAmount" name="minOrderAmount" value="100.00" step="0.01" min="0">
                            </div>

                            <div class="form-group">
                                <label for="deliveryRadius">Delivery Radius (km)</label>
                                <input type="number" id="deliveryRadius" name="deliveryRadius" value="20" min="1">
                            </div>

                            <div class="form-group">
                                <label for="businessHours">Business Hours</label>
                                <input type="text" id="businessHours" name="businessHours" value="8:00 AM - 8:00 PM">
                            </div>

                            <div class="form-group">
                                <label for="contactEmail">Contact Email</label>
                                <input type="email" id="contactEmail" name="contactEmail" value="admin@mill.com">
                            </div>

                            <div class="form-group">
                                <label for="contactPhone">Contact Phone</label>
                                <input type="tel" id="contactPhone" name="contactPhone" value="+251911223344">
                            </div>

                            <div class="form-group">
                                <button type="submit" class="btn btn-primary">Save System Settings</button>
                            </div>
                        </form>

                        <div style="margin-top: 2rem;">
                            <h4>System Maintenance</h4>
                            <div class="action-buttons">
                                <button class="btn btn-danger" onclick="backupDatabase()">Backup Database</button>
                                <button class="btn btn-danger" onclick="clearCache()">Clear Cache</button>
                                <button class="btn btn-danger" onclick="updateSystem()">Check for Updates</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Product Modal -->
    <div id="productModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 id="modalProductTitle">Add New Product</h2>
                <button class="close-modal" onclick="closeModal('productModal')">&times;</button>
            </div>
            <form id="productForm" action="admin" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="action" value="addProduct">
                <input type="hidden" id="productId" name="productId" value="">
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="productName">Product Name *</label>
                        <input type="text" id="productName" name="name" required>
                    </div>
                    <div class="form-group">
                        <label for="productCategory">Category *</label>
                        <select id="productCategory" name="category" required>
                            <option value="">Select Category</option>
                            <option value="Grain">Grain</option>
                            <option value="Legume">Legume</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="productDescription">Description</label>
                    <textarea id="productDescription" name="description" rows="3"></textarea>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="purchasePrice">Purchase Price (Birr/kg) *</label>
                        <input type="number" id="purchasePrice" name="purchasePrice" step="0.01" min="0" required>
                    </div>
                    <div class="form-group">
                        <label for="sellPrice">Sell Price (Birr/kg) *</label>
                        <input type="number" id="sellPrice" name="sellPrice" step="0.01" min="0" required>
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="millingPrice">Milling Price (Birr/kg) *</label>
                        <input type="number" id="millingPrice" name="millingPrice" step="0.01" min="0" required>
                    </div>
                    <div class="form-group">
                        <label for="minQuantity">Minimum Quantity (kg) *</label>
                        <input type="number" id="minQuantity" name="minQuantity" value="1" min="1" required>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="productImage">Product Image URL</label>
                    <input type="text" id="productImage" name="imageUrl" placeholder="Enter image URL or upload file">
                    <input type="file" id="imageUpload" name="imageFile" accept="image/*" style="margin-top: 0.5rem;">
                </div>
                
                <div class="form-group">
                    <div style="display: flex; gap: 1rem;">
                        <button type="submit" class="btn btn-primary">Save Product</button>
                        <button type="button" class="btn btn-success" onclick="saveAndPostProduct()">Save & Post</button>
                        <button type="button" class="btn" onclick="closeModal('productModal')">Cancel</button>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <!-- User Modal (for Operators/Drivers) -->
    <div id="userModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 id="modalUserTitle">Add New User</h2>
                <button class="close-modal" onclick="closeModal('userModal')">&times;</button>
            </div>
            <form id="userForm" action="admin" method="POST">
                <input type="hidden" name="action" value="addUser">
                <input type="hidden" id="userId" name="userId" value="">
                <input type="hidden" id="userTypeField" name="userType" value="">
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="userFullName">Full Name *</label>
                        <input type="text" id="userFullName" name="fullName" required>
                    </div>
                    <div class="form-group">
                        <label for="userEmail">Email *</label>
                        <input type="email" id="userEmail" name="email" required>
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="userUsername">Username *</label>
                        <input type="text" id="userUsername" name="username" required>
                    </div>
                    <div class="form-group">
                        <label for="userPassword">Password *</label>
                        <input type="password" id="userPassword" name="password" required minlength="6">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="userPhone">Phone Number</label>
                        <input type="tel" id="userPhone" name="phone" pattern="\+251[79]\d{8}" placeholder="+2519XXXXXXXX">
                    </div>
                </div>
                
                <!-- Driver-specific fields -->
                <div id="driverFields" style="display: none;">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="carNumber">Car Number *</label>
                            <input type="text" id="carNumber" name="carNumber">
                        </div>
                        <div class="form-group">
                            <label for="carType">Car Type *</label>
                            <input type="text" id="carType" name="carType">
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <label for="licenseNumber">License Number</label>
                        <input type="text" id="licenseNumber" name="licenseNumber">
                    </div>
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-primary">Save User</button>
                    <button type="button" class="btn" onclick="closeModal('userModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Customer Details Modal -->
    <div id="customerDetailsModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Customer Details</h2>
                <button class="close-modal" onclick="closeModal('customerDetailsModal')">&times;</button>
            </div>
            <div id="customerDetailsContent">
                <!-- Customer details will be loaded here -->
            </div>
        </div>
    </div>

    <!-- Order Details Modal -->
    <div id="orderDetailsModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Order Details</h2>
                <button class="close-modal" onclick="closeModal('orderDetailsModal')">&times;</button>
            </div>
            <div id="orderDetailsContent">
                <!-- Order details will be loaded here -->
            </div>
        </div>
    </div>

    <!-- Offline Order Modal -->
    <div id="offlineOrderModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Create Offline Order</h2>
                <button class="close-modal" onclick="closeModal('offlineOrderModal')">&times;</button>
            </div>
            <form id="offlineOrderForm" action="admin" method="POST">
                <input type="hidden" name="action" value="createOfflineOrder">
                
                <div class="form-group">
                    <label for="offlineCustomer">Customer</label>
                    <select id="offlineCustomer" name="customerId" required>
                        <option value="">Select Customer</option>
                        <!-- Customers will be loaded here -->
                    </select>
                </div>
                
                <div class="form-group">
                    <label for="offlineProduct">Product</label>
                    <select id="offlineProduct" name="productId" required>
                        <option value="">Select Product</option>
                        <!-- Products will be loaded here -->
                    </select>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="offlineQuantity">Quantity (kg)</label>
                        <input type="number" id="offlineQuantity" name="quantity" step="0.01" min="1" required>
                    </div>
                    <div class="form-group">
                        <label for="offlinePrice">Unit Price (Birr)</label>
                        <input type="number" id="offlinePrice" name="unitPrice" step="0.01" min="0" required>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="offlineAddress">Delivery Address</label>
                    <textarea id="offlineAddress" name="deliveryAddress" required></textarea>
                </div>
                
                <div class="form-group">
                    <label for="offlinePayment">Payment Method</label>
                    <select id="offlinePayment" name="paymentMethod" required>
                        <option value="cash">Cash</option>
                        <option value="CBE">CBE</option>
                        <option value="Telebirr">Telebirr</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-primary">Create Order</button>
                    <button type="button" class="btn" onclick="closeModal('offlineOrderModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        // Dark Mode Toggle
        function toggleDarkMode() {
            document.body.classList.toggle('dark-mode');
            const button = document.querySelector('.theme-toggle');
            button.textContent = document.body.classList.contains('dark-mode') ? '☀️ Light Mode' : '🌙 Dark Mode';
            localStorage.setItem('darkMode', document.body.classList.contains('dark-mode'));
            
            // Update system setting
            fetch('admin?action=toggleDarkMode&darkMode=' + document.body.classList.contains('dark-mode'));
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
       
     // Post product
        function postProduct(productId) {
            if (confirm('Post this product to make it visible to customers?')) {
                fetch('admin?action=postProduct&productId=' + productId, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Product posted successfully! Customers can now see it.');
                        window.location.reload();
                    } else {
                        alert('Error posting product');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error posting product');
                });
            }
        }

        // Handle password request
        function handlePasswordRequest(requestId, action) {
            if (confirm(action + ' this password reset request?')) {
                fetch('admin?action=handlePasswordRequest&requestId=' + requestId + '&action=' + action, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Request ' + action + ' successfully');
                        window.location.reload();
                    } else {
                        alert('Error handling request');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error handling request');
                });
            }
        }
        // Product Modal
        function openProductModal(productId = null) {
            const modal = document.getElementById('productModal');
            const title = document.getElementById('modalProductTitle');
            const form = document.getElementById('productForm');
            const productIdField = document.getElementById('productId');
            
            if (productId) {
                title.textContent = 'Edit Product';
                productIdField.value = productId;
                form.action = 'admin?action=updateProduct';
                // Load product data
                loadProductData(productId);
            } else {
                title.textContent = 'Add New Product';
                productIdField.value = '';
                form.action = 'admin?action=addProduct';
                form.reset();
            }
            
            modal.style.display = 'flex';
        }

        function saveAndPostProduct() {
            const form = document.getElementById('productForm');
            const hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = 'postProduct';
            hiddenInput.value = 'true';
            form.appendChild(hiddenInput);
            form.submit();
        }

        // User Modal
        function openUserModal(userType, userId = null) {
            const modal = document.getElementById('userModal');
            const title = document.getElementById('modalUserTitle');
            const form = document.getElementById('userForm');
            const userIdField = document.getElementById('userId');
            const userTypeField = document.getElementById('userTypeField');
            const driverFields = document.getElementById('driverFields');
            
            userTypeField.value = userType;
            
            if (userId) {
                title.textContent = 'Edit ' + (userType === 'driver' ? 'Driver' : 'Operator');
                userIdField.value = userId;
                form.action = 'admin?action=updateUser';
                // Load user data
                loadUserData(userId, userType);
            } else {
                title.textContent = 'Add New ' + (userType === 'driver' ? 'Driver' : 'Operator');
                userIdField.value = '';
                form.action = 'admin?action=addUser';
                form.reset();
            }
            
            // Show/hide driver fields
            if (userType === 'driver') {
                driverFields.style.display = 'block';
                document.getElementById('carNumber').required = true;
                document.getElementById('carType').required = true;
            } else {
                driverFields.style.display = 'none';
                document.getElementById('carNumber').required = false;
                document.getElementById('carType').required = false;
            }
            
            modal.style.display = 'flex';
        }

        // Load Dashboard Data
        function loadDashboardData() {
            fetch('admin?action=getStats')
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        document.getElementById('totalProducts').textContent = data.totalProducts || 0;
                        document.getElementById('totalCustomers').textContent = data.totalCustomers || 0;
                        document.getElementById('totalOrders').textContent = data.totalOrders || 0;
                        document.getElementById('pendingRequests').textContent = data.pendingRequests || 0;
                    }
                })
                .catch(error => console.error('Error loading dashboard data:', error));
        }

        // Load Products
        function loadProducts() {
            fetch('admin?action=getProducts')
                .then(response => response.text())
                .then(html => {
                    document.getElementById('productsTable').innerHTML = html;
                })
                .catch(error => console.error('Error loading products:', error));
        }

        // User Management Functions
        function editUser(userId, userType) {
            openUserModal(userType, userId);
        }

        function deleteUser(userId, userType) {
            if (confirm('Are you sure you want to delete this ' + userType + '?')) {
                fetch('admin?action=deleteUser&userId=' + userId + '&userType=' + userType, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert(userType + ' deleted successfully');
                        window.location.reload();
                    } else {
                        alert('Error deleting ' + userType);
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error deleting ' + userType);
                });
            }
        }

        function toggleUserStatus(userId, newStatus, userType) {
            fetch('admin?action=toggleUserStatus&userId=' + userId + '&status=' + newStatus + '&userType=' + userType, {
                method: 'POST'
            })
            .then(response => {
                if (response.ok) {
                    alert('Status updated successfully');
                    window.location.reload();
                } else {
                    alert('Error updating status');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error updating status');
            });
        }

        // Customer Functions
        function viewCustomerDetails(customerId) {
            fetch('admin?action=getCustomerDetails&customerId=' + customerId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('customerDetailsContent').innerHTML = html;
                    openModal('customerDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading customer details');
                });
        }

        // Password Request Functions
        function showRequestTab(tab) {
            // Hide all sections
            document.getElementById('pendingRequestsSection').style.display = 'none';
            document.getElementById('approvedRequestsSection').style.display = 'none';
            document.getElementById('rejectedRequestsSection').style.display = 'none';
            
            // Remove active class from all tabs
            document.querySelectorAll('.content-tab').forEach(t => t.classList.remove('active'));
            
            // Show selected section and activate tab
            document.getElementById(tab + 'RequestsSection').style.display = 'block';
            event.target.classList.add('active');
            
            // Load data for the tab if not loaded
            if (tab !== 'pending') {
                loadRequestData(tab);
            }
        }

        function loadRequestData(status) {
            fetch('admin?action=getPasswordRequests&status=' + status)
                .then(response => response.text())
                .then(html => {
                    document.getElementById(status + 'RequestsTable').innerHTML = html;
                })
                .catch(error => console.error('Error loading requests:', error));
        }

        function handlePasswordRequest(requestId, action) {
            if (confirm('Are you sure you want to ' + action + ' this password reset request?')) {
                fetch('admin?action=handlePasswordRequest&requestId=' + requestId + '&action=' + action, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Request ' + action + ' successfully');
                        window.location.reload();
                    } else {
                        alert('Error handling request');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error handling request');
                });
            }
        }

        function refreshRequests() {
            loadRequestData('pending');
            alert('Requests refreshed');
        }

        // Order Management Functions
        function showRequestTab(tab) {
            // Implementation for request tabs
            document.querySelectorAll('.content-tab').forEach(t => {
                t.classList.remove('active');
                document.getElementById(t.textContent.toLowerCase() + 'RequestsSection').style.display = 'none';
            });
            
            event.target.classList.add('active');
            document.getElementById(tab + 'RequestsSection').style.display = 'block';
        }

        function searchAllOrders(query) {
            const rows = document.querySelectorAll('#allOrdersTable tr');
            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(query.toLowerCase()) ? '' : 'none';
            });
        }

        function filterOrderType(type) {
            const rows = document.querySelectorAll('#allOrdersTable tr');
            rows.forEach(row => {
                const productCell = row.cells[2];
                if (productCell) {
                    const isSpecial = productCell.textContent.includes('Special Order');
                    let show = false;
                    
                    switch(type) {
                        case 'all': show = true; break;
                        case 'online': show = !isSpecial; break;
                        case 'offline': show = row.textContent.includes('Offline'); break;
                        case 'special': show = isSpecial; break;
                    }
                    
                    row.style.display = show ? '' : 'none';
                }
            });
        }

        function filterOrderStatusAll(status) {
            const rows = document.querySelectorAll('#allOrdersTable tr');
            rows.forEach(row => {
                const statusCell = row.cells[6];
                if (statusCell) {
                    const rowStatus = statusCell.textContent.toLowerCase().trim();
                    row.style.display = (status === 'all' || rowStatus === status) ? '' : 'none';
                }
            });
        }

        function viewOrderDetails(orderId) {
            fetch('admin?action=getOrderDetails&orderId=' + orderId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('orderDetailsContent').innerHTML = html;
                    openModal('orderDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading order details');
                });
        }

        function updateOrderStatus(orderId, newStatus) {
            if (confirm('Change order status to ' + newStatus + '?')) {
                fetch('admin?action=updateOrderStatus&orderId=' + orderId + '&status=' + newStatus, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Order status updated');
                        window.location.reload();
                    } else {
                        alert('Error updating order status');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error updating order status');
                });
            }
        }

        function markAsDelivered(orderId) {
            updateOrderStatus(orderId, 'delivered');
        }

        function createOfflineOrder() {
            // Load customers and products for dropdowns
            fetch('admin?action=getCustomersForDropdown')
                .then(response => response.json())
                .then(customers => {
                    const select = document.getElementById('offlineCustomer');
                    select.innerHTML = '<option value="">Select Customer</option>';
                    customers.forEach(customer => {
                        const option = document.createElement('option');
                        option.value = customer.id;
                        option.textContent = customer.full_name + ' (' + customer.phone + ')';
                        select.appendChild(option);
                    });
                });
            
            fetch('admin?action=getProductsForDropdown')
                .then(response => response.json())
                .then(products => {
                    const select = document.getElementById('offlineProduct');
                    select.innerHTML = '<option value="">Select Product</option>';
                    products.forEach(product => {
                        const option = document.createElement('option');
                        option.value = product.id;
                        option.textContent = product.name + ' - ' + product.sell_price + ' Birr/kg';
                        select.appendChild(option);
                    });
                });
            
            openModal('offlineOrderModal');
        }

        // Settings Functions
        function showSettingsTab(tab) {
            // Hide all sections
            document.querySelectorAll('.settings-section').forEach(section => {
                section.style.display = 'none';
            });
            
            // Remove active class from all tabs
            document.querySelectorAll('.content-tab').forEach(t => t.classList.remove('active'));
            
            // Show selected section and activate tab
            document.getElementById(tab + 'Settings').style.display = 'block';
            event.target.classList.add('active');
        }

        function previewImage(input) {
            if (input.files && input.files[0]) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    document.getElementById('profilePreview').src = e.target.result;
                };
                reader.readAsDataURL(input.files[0]);
            }
        }

        function backupDatabase() {
            if (confirm('This will create a backup of the database. Continue?')) {
                fetch('admin?action=backupDatabase', {
                    method: 'POST'
                })
                .then(response => response.blob())
                .then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'database-backup-' + new Date().toISOString().split('T')[0] + '.sql';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                    alert('Backup completed successfully');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error creating backup');
                });
            }
        }

        function clearCache() {
            if (confirm('Clear all cached data? This may improve system performance.')) {
                fetch('admin?action=clearCache', {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Cache cleared successfully');
                    } else {
                        alert('Error clearing cache');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error clearing cache');
                });
            }
        }

        function updateSystem() {
            alert('System update check initiated. This feature requires server-side implementation.');
            // In a real implementation, this would check for updates and apply them
        }

        // Export Functions
        function exportProducts() {
            window.open('admin?action=exportProducts', '_blank');
        }

        function exportCustomers() {
            window.open('admin?action=exportCustomers', '_blank');
        }

        function exportOrders() {
            window.open('admin?action=exportOrders', '_blank');
        }

        // Search and Filter Functions
        function searchProducts(query) {
            const rows = document.querySelectorAll('#productsTable tr');
            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(query.toLowerCase()) ? '' : 'none';
            });
        }

        function filterProducts(category) {
            const rows = document.querySelectorAll('#productsTable tr');
            rows.forEach(row => {
                const categoryCell = row.cells[3];
                if (categoryCell) {
                    row.style.display = (category === 'all' || categoryCell.textContent === category) ? '' : 'none';
                }
            });
        }

        function filterProductStatus(status) {
            const rows = document.querySelectorAll('#productsTable tr');
            rows.forEach(row => {
                const statusCell = row.cells[7];
                if (statusCell) {
                    const isPosted = statusCell.textContent.includes('Posted');
                    let show = false;
                    
                    switch(status) {
                        case 'all': show = true; break;
                        case 'posted': show = isPosted; break;
                        case 'not-posted': show = !isPosted; break;
                    }
                    
                    row.style.display = show ? '' : 'none';
                }
            });
        }

        function searchCustomers(query) {
            const rows = document.querySelectorAll('#customersTable tr');
            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(query.toLowerCase()) ? '' : 'none';
            });
        }

        function filterCustomerStatus(status) {
            const rows = document.querySelectorAll('#customersTable tr');
            rows.forEach(row => {
                const statusCell = row.cells[6];
                if (statusCell) {
                    const isActive = statusCell.textContent.includes('active');
                    let show = false;
                    
                    switch(status) {
                        case 'all': show = true; break;
                        case 'active': show = isActive; break;
                        case 'inactive': show = !isActive; break;
                    }
                    
                    row.style.display = show ? '' : 'none';
                }
            });
        }

        function searchOrders(query) {
            const rows = document.querySelectorAll('#recentOrders tbody tr');
            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(query.toLowerCase()) ? '' : 'none';
            });
        }

        function filterOrders(status) {
            const rows = document.querySelectorAll('#recentOrders tbody tr');
            rows.forEach(row => {
                const statusCell = row.cells[5];
                if (statusCell) {
                    const rowStatus = statusCell.textContent.toLowerCase().trim();
                    row.style.display = (status === 'all' || rowStatus === status) ? '' : 'none';
                }
            });
        }

        // Helper Functions
        function loadProductData(productId) {
            fetch('admin?action=getProductData&productId=' + productId)
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        document.getElementById('productName').value = data.name || '';
                        document.getElementById('productCategory').value = data.category || '';
                        document.getElementById('productDescription').value = data.description || '';
                        document.getElementById('purchasePrice').value = data.purchase_price || '';
                        document.getElementById('sellPrice').value = data.sell_price || '';
                        document.getElementById('millingPrice').value = data.milling_price || '';
                        document.getElementById('minQuantity').value = data.min_quantity || 1;
                        document.getElementById('productImage').value = data.image_url || '';
                    }
                })
                .catch(error => console.error('Error loading product data:', error));
        }

        function loadUserData(userId, userType) {
            fetch('admin?action=getUserData&userId=' + userId + '&userType=' + userType)
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        document.getElementById('userFullName').value = data.full_name || '';
                        document.getElementById('userEmail').value = data.email || '';
                        document.getElementById('userUsername').value = data.username || '';
                        document.getElementById('userPhone').value = data.phone || '';
                        
                        if (userType === 'driver') {
                            document.getElementById('carNumber').value = data.car_number || '';
                            document.getElementById('carType').value = data.car_type || '';
                            document.getElementById('licenseNumber').value = data.license_number || '';
                        }
                        
                        // Don't fill password field for security
                        document.getElementById('userPassword').required = false;
                        document.getElementById('userPassword').placeholder = 'Leave blank to keep current password';
                    }
                })
                .catch(error => console.error('Error loading user data:', error));
        }

        // Initialize based on current tab
        window.addEventListener('DOMContentLoaded', function() {
            const tab = '<%= tab %>';
            
            if (tab === 'dashboard') {
                loadDashboardData();
            } else if (tab === 'products') {
                loadProducts();
            }
            
            // Set up form validation
            setupFormValidation();
        });

        // Form Validation
        function setupFormValidation() {
            // Product form validation
            const productForm = document.getElementById('productForm');
            if (productForm) {
                productForm.addEventListener('submit', function(e) {
                    const sellPrice = parseFloat(document.getElementById('sellPrice').value);
                    const purchasePrice = parseFloat(document.getElementById('purchasePrice').value);
                    
                    if (sellPrice <= purchasePrice) {
                        e.preventDefault();
                        alert('Sell price must be greater than purchase price');
                        return;
                    }
                    
                    if (document.getElementById('millingPrice').value < 0) {
                        e.preventDefault();
                        alert('Milling price cannot be negative');
                        return;
                    }
                });
            }
            
            // User form validation
            const userForm = document.getElementById('userForm');
            if (userForm) {
                userForm.addEventListener('submit', function(e) {
                    const phone = document.getElementById('userPhone').value;
                    if (phone && !phone.match(/^\+251[79]\d{8}$/)) {
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
        }

        // Close modal when clicking outside
        window.onclick = function(event) {
            if (event.target.classList.contains('modal')) {
                event.target.style.display = 'none';
            }
        }

        // Auto-calculate sell price with margin
        document.getElementById('purchasePrice')?.addEventListener('change', function() {
            const purchasePrice = parseFloat(this.value);
            const sellPriceInput = document.getElementById('sellPrice');
            if (purchasePrice && !sellPriceInput.value) {
                // Auto-set sell price with 20% margin
                sellPriceInput.value = (purchasePrice * 1.2).toFixed(2);
            }
        });
    </script>
</body>
</html>