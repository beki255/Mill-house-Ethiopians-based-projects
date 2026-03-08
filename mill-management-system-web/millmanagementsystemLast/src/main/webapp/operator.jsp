<%@ page import="com.mill.system.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.math.BigDecimal" %>
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
    if (!"operator".equals(userType)) {
        response.sendRedirect("index.html");
        return;
    }
    
    //User user = (User) userSession.getAttribute("user");
    int userId = user.getId();
    String tab = request.getParameter("tab") != null ? request.getParameter("tab") : "orders";
    String statusFilter = request.getParameter("status") != null ? request.getParameter("status") : "pending";
    String message = request.getParameter("message");
    String error = request.getParameter("error");
    
    // Get statistics
    int pendingOrders = 0;
    int processingOrders = 0;
    int completedToday = 0;
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        // Pending orders
        String sql = "SELECT COUNT(*) as count FROM orders WHERE assigned_operator = ? AND order_status = 'pending'";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) pendingOrders = rs.getInt("count");
        rs.close();
        stmt.close();
        
        // Processing orders
        sql = "SELECT COUNT(*) as count FROM orders WHERE assigned_operator = ? AND order_status = 'processing'";
        stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        rs = stmt.executeQuery();
        if (rs.next()) processingOrders = rs.getInt("count");
        rs.close();
        stmt.close();
        
        // Completed today
        sql = "SELECT COUNT(*) as count FROM orders WHERE assigned_operator = ? AND order_status = 'completed' AND DATE(order_date) = CURDATE()";
        stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        rs = stmt.executeQuery();
        if (rs.next()) completedToday = rs.getInt("count");
        rs.close();
        stmt.close();
        
        conn.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Operator Dashboard - Mill Management System</title>
    <style>
        :root {
            --primary-color: #2c3e50;
            --secondary-color: #3498db;
            --accent-color: #e74c3c;
            --success-color: #2ecc71;
            --warning-color: #f39c12;
            --info-color: #17a2b8;
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
            --info-color: #148ea1;
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

        .sidebar-menu .badge {
            float: right;
            background: var(--accent-color);
            color: white;
            padding: 0.2rem 0.5rem;
            border-radius: 10px;
            font-size: 0.8rem;
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

        .notification-bell {
            position: relative;
            cursor: pointer;
            font-size: 1.2rem;
        }

        .notification-count {
            position: absolute;
            top: -8px;
            right: -8px;
            background: var(--accent-color);
            color: white;
            font-size: 0.7rem;
            padding: 0.2rem 0.4rem;
            border-radius: 50%;
            min-width: 18px;
            text-align: center;
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

        .alert-warning {
            background-color: #fff3cd;
            color: #856404;
            border: 1px solid #ffeaa7;
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

        /* Stats Cards */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1rem;
            margin-bottom: 2rem;
        }

        .stat-card {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            text-align: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            transition: all 0.3s ease;
            border-top: 4px solid var(--secondary-color);
            cursor: pointer;
        }

        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.1);
        }

        .stat-card.pending {
            border-top-color: var(--warning-color);
        }

        .stat-card.processing {
            border-top-color: var(--info-color);
        }

        .stat-card.completed {
            border-top-color: var(--success-color);
        }

        .stat-card.delivered {
            border-top-color: var(--secondary-color);
        }

        .stat-number {
            font-size: 2.5rem;
            font-weight: bold;
            color: var(--primary-color);
            margin-bottom: 0.5rem;
        }

        .stat-label {
            color: #666;
            font-size: 0.9rem;
        }

        /* Orders Grid */
        .orders-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
            gap: 1.5rem;
            margin-top: 1rem;
        }

        .order-card {
            background: white;
            border-radius: 10px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            border: 1px solid #eee;
            transition: all 0.3s ease;
        }

        .order-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 6px 12px rgba(0,0,0,0.15);
        }

        .order-header {
            padding: 1rem;
            background: linear-gradient(135deg, var(--secondary-color), var(--primary-color));
            color: white;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .order-number {
            font-weight: bold;
            font-size: 1.1rem;
        }

        .order-date {
            font-size: 0.85rem;
            opacity: 0.9;
        }

        .order-body {
            padding: 1.5rem;
        }

        .customer-info {
            display: flex;
            align-items: center;
            gap: 1rem;
            margin-bottom: 1rem;
            padding-bottom: 1rem;
            border-bottom: 1px solid #eee;
        }

        .customer-avatar {
            width: 50px;
            height: 50px;
            border-radius: 50%;
            object-fit: cover;
            border: 2px solid var(--secondary-color);
        }

        .customer-details h4 {
            margin-bottom: 0.3rem;
            color: var(--primary-color);
        }

        .customer-details p {
            color: #666;
            font-size: 0.9rem;
        }

        .order-details {
            margin-bottom: 1.5rem;
        }

        .detail-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 0.8rem;
            font-size: 0.9rem;
        }

        .detail-label {
            color: #666;
        }

        .detail-value {
            font-weight: 500;
            color: var(--primary-color);
        }

        .order-status {
            display: inline-block;
            padding: 0.3rem 0.8rem;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 500;
            margin-bottom: 1rem;
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

        .payment-status {
            display: inline-block;
            padding: 0.3rem 0.8rem;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 500;
            margin-left: 0.5rem;
        }

        .payment-pending {
            background-color: #fff3cd;
            color: #856404;
        }

        .payment-paid {
            background-color: #d4edda;
            color: #155724;
        }

        .payment-failed {
            background-color: #f8d7da;
            color: #721c24;
        }

        .order-actions {
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
        }

        .btn {
            padding: 0.5rem 1rem;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-weight: 500;
            transition: all 0.3s ease;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
        }

        .btn-sm {
            padding: 0.3rem 0.8rem;
            font-size: 0.85rem;
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

        .btn-info {
            background-color: var(--info-color);
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
            justify-content: center;
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

        .status-tabs {
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
            margin-bottom: 1rem;
            border-bottom: 2px solid #eee;
            padding-bottom: 1rem;
        }

        .status-tab {
            padding: 0.5rem 1rem;
            background: white;
            border: 1px solid #ddd;
            border-radius: 20px;
            cursor: pointer;
            font-size: 0.9rem;
            transition: all 0.3s ease;
        }

        .status-tab.active {
            background-color: var(--secondary-color);
            color: white;
            border-color: var(--secondary-color);
        }

        .status-tab:hover:not(.active) {
            background-color: #f8f9fa;
        }

        /* Offline Orders */
        .offline-orders {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            margin-top: 2rem;
            border: 1px solid #eee;
        }

        .offline-form {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1rem;
            margin-top: 1rem;
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

        /* Inventory Table */
        .inventory-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }

        .inventory-table th,
        .inventory-table td {
            padding: 0.8rem;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }

        .inventory-table th {
            background-color: var(--primary-color);
            color: white;
            font-weight: 500;
        }

        .inventory-table tr:hover {
            background-color: #f8f9fa;
        }

        .stock-low {
            color: var(--accent-color);
            font-weight: bold;
        }

        .stock-ok {
            color: var(--success-color);
        }

        /* Settings */
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
            max-width: 600px;
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

        .modal-header h2 {
            color: var(--primary-color);
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
            margin: 0.5rem 0;
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
            grid-column: 1 / -1;
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
            
            .sidebar-menu .badge {
                display: none;
            }
            
            .orders-grid {
                grid-template-columns: 1fr;
            }
            
            .stats-grid {
                grid-template-columns: 1fr;
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
            .order-actions {
                flex-direction: column;
            }
            
            .btn {
                width: 100%;
                justify-content: center;
            }
            
            .modal-content {
                padding: 1rem;
            }
        }

        /* Timeline */
        .timeline {
            position: relative;
            padding-left: 2rem;
            margin: 1.5rem 0;
        }

        .timeline::before {
            content: '';
            position: absolute;
            left: 7px;
            top: 0;
            bottom: 0;
            width: 2px;
            background: var(--secondary-color);
        }

        .timeline-item {
            position: relative;
            margin-bottom: 1rem;
            padding-bottom: 1rem;
        }

        .timeline-item:last-child {
            margin-bottom: 0;
            padding-bottom: 0;
        }

        .timeline-item::before {
            content: '';
            position: absolute;
            left: -2rem;
            top: 5px;
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: var(--secondary-color);
            border: 2px solid white;
        }

        .timeline-content {
            background: #f8f9fa;
            padding: 0.8rem;
            border-radius: 6px;
            border-left: 3px solid var(--secondary-color);
        }

        .timeline-date {
            font-size: 0.8rem;
            color: #666;
            margin-top: 0.3rem;
        }

        /* Quick Actions */
        .quick-actions {
            display: flex;
            gap: 1rem;
            margin-bottom: 1.5rem;
            flex-wrap: wrap;
        }

        .action-btn {
            flex: 1;
            min-width: 200px;
            padding: 1rem;
            background: white;
            border: 2px solid var(--secondary-color);
            border-radius: 8px;
            text-align: center;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .action-btn:hover {
            background: var(--secondary-color);
            color: white;
            transform: translateY(-3px);
        }

        .action-btn i {
            font-size: 2rem;
            margin-bottom: 0.5rem;
            display: block;
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
                <p>Operator</p>
            </div>
            
            <ul class="sidebar-menu">
                <li><a href="?tab=orders&status=pending" class="<%= "orders".equals(tab) ? "active" : "" %>">
                    📦 Orders
                    <% if (pendingOrders > 0) { %>
                        <span class="badge"><%= pendingOrders %></span>
                    <% } %>
                </a></li>
                <li><a href="?tab=offline" class="<%= "offline".equals(tab) ? "active" : "" %>">
                    🛒 Offline Orders
                </a></li>
                <li><a href="?tab=inventory" class="<%= "inventory".equals(tab) ? "active" : "" %>">
                    📊 Inventory
                </a></li>
                <li><a href="?tab=drivers" class="<%= "drivers".equals(tab) ? "active" : "" %>">
                    🚚 Drivers
                </a></li>
                <li><a href="?tab=reports" class="<%= "reports".equals(tab) ? "active" : "" %>">
                    📈 Reports
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
                    <h1>Operator Dashboard</h1>
                    <p>Manage orders, inventory, and customer requests</p>
                </div>
                <div class="header-right">
                    <div class="notification-bell" onclick="showNotifications()">
                        🔔
                        <span class="notification-count" id="notificationCount">0</span>
                    </div>
                    <button class="theme-toggle" onclick="toggleDarkMode()">🌙 Dark Mode</button>
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
                <!-- Orders Tab -->
                <div id="ordersTab" class="tab-content <%= "orders".equals(tab) ? "active" : "" %>">
                    <h2>Order Management</h2>
                    
                    <div class="stats-grid">
                        <div class="stat-card pending" onclick="window.location='?tab=orders&status=pending'">
                            <div class="stat-number"><%= pendingOrders %></div>
                            <div class="stat-label">Pending Orders</div>
                        </div>
                        <div class="stat-card processing" onclick="window.location='?tab=orders&status=processing'">
                            <div class="stat-number"><%= processingOrders %></div>
                            <div class="stat-label">Processing</div>
                        </div>
                        <div class="stat-card completed" onclick="window.location='?tab=orders&status=completed'">
                            <div class="stat-number"><%= completedToday %></div>
                            <div class="stat-label">Completed Today</div>
                        </div>
                        <div class="stat-card" onclick="window.location='?tab=offline'">
                            <div class="stat-number" id="offlineCount">0</div>
                            <div class="stat-label">Offline Today</div>
                        </div>
                    </div>

                    <div class="search-filter">
                        <input type="text" class="search-box" id="orderSearch" 
                               placeholder="Search by order number, customer name..." onkeyup="searchOrders()">
                        <select class="search-box" id="dateFilter" onchange="filterByDate()">
                            <option value="all">All Dates</option>
                            <option value="today">Today</option>
                            <option value="week">This Week</option>
                            <option value="month">This Month</option>
                        </select>
                    </div>

                    <div class="status-tabs">
                        <button class="status-tab <%= "pending".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=pending'">Pending</button>
                        <button class="status-tab <%= "received".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=received'">Received</button>
                        <button class="status-tab <%= "processing".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=processing'">Processing</button>
                        <button class="status-tab <%= "completed".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=completed'">Completed</button>
                        <button class="status-tab <%= "delivered".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=delivered'">Delivered</button>
                    </div>

                    <div class="orders-grid" id="ordersGrid">
                        <!-- Orders will be loaded from database -->
                        <%
                            try {
                                Connection conn = DatabaseConnection.getConnection();
                                String sql = "SELECT o.*, u.full_name as customer_name, u.phone as customer_phone, " +
                                            "u.address as customer_address, u.profile_image, " +
                                            "p.name as product_name, p.image_url as product_image, " +
                                            "d.full_name as driver_name, d.phone as driver_phone " +
                                            "FROM orders o " +
                                            "JOIN users u ON o.customer_id = u.id " +
                                            "LEFT JOIN products p ON o.product_id = p.id " +
                                            "LEFT JOIN users d ON o.assigned_driver = d.id " +
                                            "WHERE (o.assigned_operator = ? OR o.assigned_operator IS NULL) " +
                                            "AND o.order_status = ? " +
                                            "ORDER BY o.order_date DESC LIMIT 20";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                stmt.setInt(1, userId);
                                stmt.setString(2, statusFilter);
                                ResultSet rs = stmt.executeQuery();
                                
                                int orderCount = 0;
                                
                                while (rs.next()) {
                                    orderCount++;
                                    String status = rs.getString("order_status");
                                    String paymentStatus = rs.getString("payment_status");
                                    boolean isSpecialOrder = rs.getBoolean("is_special_order");
                        %>
                                    <div class="order-card" data-order-id="<%= rs.getInt("id") %>">
                                        <div class="order-header">
                                            <div>
                                                <div class="order-number">#<%= rs.getString("order_number") %></div>
                                                <div class="order-date"><%= rs.getTimestamp("order_date") %></div>
                                            </div>
                                            <div>
                                                <span class="order-status status-<%= status %>"><%= status %></span>
                                                <span class="payment-status payment-<%= paymentStatus %>"><%= paymentStatus %></span>
                                            </div>
                                        </div>
                                        
                                        <div class="order-body">
                                            <div class="customer-info">
                                                <img src="<%= rs.getString("profile_image") != null ? rs.getString("profile_image") : "https://ui-avatars.com/api/?name=" + rs.getString("customer_name") + "&background=3498db&color=fff" %>" 
                                                     alt="Customer" class="customer-avatar">
                                                <div class="customer-details">
                                                    <h4><%= rs.getString("customer_name") %></h4>
                                                    <p>📞 <%= rs.getString("customer_phone") %></p>
                                                    <p>📍 <%= rs.getString("customer_address") %></p>
                                                </div>
                                            </div>
                                            
                                            <div class="order-details">
                                                <div class="detail-row">
                                                    <span class="detail-label">Product:</span>
                                                    <span class="detail-value">
                                                        <%= isSpecialOrder ? "Special Order" : rs.getString("product_name") %>
                                                        <% if (isSpecialOrder && rs.getString("special_description") != null) { %>
                                                            <br><small><%= rs.getString("special_description") %></small>
                                                        <% } %>
                                                    </span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">Quantity:</span>
                                                    <span class="detail-value"><%= rs.getBigDecimal("quantity") %> kg</span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">Total Amount:</span>
                                                    <span class="detail-value"><%= rs.getBigDecimal("total_price") %> Birr</span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">Payment Method:</span>
                                                    <span class="detail-value"><%= rs.getString("payment_method") %></span>
                                                </div>
                                                <% if (rs.getString("driver_name") != null) { %>
                                                <div class="detail-row">
                                                    <span class="detail-label">Driver:</span>
                                                    <span class="detail-value">
                                                        <%= rs.getString("driver_name") %>
                                                        <br><small>📞 <%= rs.getString("driver_phone") %></small>
                                                    </span>
                                                </div>
                                                <% } %>
                                            </div>
                                            
                                            <div class="order-actions">
                                                <% if ("pending".equals(status)) { %>
                                                    <button class="btn btn-success btn-sm" 
                                                            onclick="updateOrderStatus(<%= rs.getInt("id") %>, 'received')">
                                                        ✅ Mark as Received
                                                    </button>
                                                    <button class="btn btn-primary btn-sm" 
                                                            onclick="assignToMe(<%= rs.getInt("id") %>)">
                                                        👤 Assign to Me
                                                    </button>
                                                <% } else if ("received".equals(status)) { %>
                                                    <button class="btn btn-warning btn-sm" 
                                                            onclick="updateOrderStatus(<%= rs.getInt("id") %>, 'processing')">
                                                        ⚙️ Start Processing
                                                    </button>
                                                    <button class="btn btn-info btn-sm" 
                                                            onclick="openAssignDriverModal(<%= rs.getInt("id") %>)">
                                                        🚚 Assign Driver
                                                    </button>
                                                <% } else if ("processing".equals(status)) { %>
                                                    <button class="btn btn-success btn-sm" 
                                                            onclick="updateOrderStatus(<%= rs.getInt("id") %>, 'completed')">
                                                        ✅ Mark Complete
                                                    </button>
                                                    <button class="btn btn-info btn-sm" 
                                                            onclick="viewOrderTimeline(<%= rs.getInt("id") %>)">
                                                        📋 View Timeline
                                                    </button>
                                                <% } else if ("completed".equals(status)) { %>
                                                    <button class="btn btn-success btn-sm" 
                                                            onclick="updateOrderStatus(<%= rs.getInt("id") %>, 'delivered')">
                                                        🚚 Mark Delivered
                                                    </button>
                                                <% } %>
                                                
                                                <button class="btn btn-outline btn-sm" 
                                                        onclick="viewOrderDetails(<%= rs.getInt("id") %>)">
                                                    👁️ View Details
                                                </button>
                                                
                                                <% if ("pending".equals(paymentStatus)) { %>
                                                    <button class="btn btn-danger btn-sm" 
                                                            onclick="markPaymentPaid(<%= rs.getInt("id") %>)">
                                                        💰 Mark Paid
                                                    </button>
                                                <% } %>
                                            </div>
                                        </div>
                                    </div>
                        <%
                                }
                                
                                if (orderCount == 0) {
                        %>
                                    <div class="empty-state">
                                        <div class="empty-state-icon">📦</div>
                                        <h3>No <%= statusFilter %> Orders</h3>
                                        <p>All <%= statusFilter %> orders are handled. Check other status tabs.</p>
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
                                    Error loading orders. Please try again later.
                                </div>
                        <%
                            }
                        %>
                    </div>
                </div>

                <!-- Offline Orders Tab -->
                <div id="offlineTab" class="tab-content <%= "offline".equals(tab) ? "active" : "" %>">
                    <h2>Offline Order Processing</h2>
                    <p class="alert alert-info">Record in-person orders and update inventory accordingly.</p>
                    
                    <div class="quick-actions">
                        <div class="action-btn" onclick="openOfflineOrderModal()">
                            <div>🛒</div>
                            <div>New Offline Order</div>
                        </div>
                        <div class="action-btn" onclick="openInventoryUpdateModal()">
                            <div>📊</div>
                            <div>Update Inventory</div>
                        </div>
                        <div class="action-btn" onclick="window.location='?tab=offline&view=history'">
                            <div>📋</div>
                            <div>View History</div>
                        </div>
                    </div>
                    
                    <div class="offline-orders">
                        <h3>Today's Offline Orders</h3>
                        
                        <table class="inventory-table">
                            <thead>
                                <tr>
                                    <th>Time</th>
                                    <th>Customer</th>
                                    <th>Product</th>
                                    <th>Quantity</th>
                                    <th>Amount</th>
                                    <th>Payment</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="offlineOrdersTable">
                                <!-- Offline orders will be loaded from database -->
                                <%
                                    try {
                                        Connection conn = DatabaseConnection.getConnection();
                                        String sql = "SELECT o.*, u.full_name as customer_name, p.name as product_name " +
                                                    "FROM orders o " +
                                                    "LEFT JOIN users u ON o.customer_id = u.id " +
                                                    "LEFT JOIN products p ON o.product_id = p.id " +
                                                    "WHERE o.is_special_order = false " +
                                                    "AND o.assigned_operator = ? " +
                                                    "AND DATE(o.order_date) = CURDATE() " +
                                                    "ORDER BY o.order_date DESC";
                                        PreparedStatement stmt = conn.prepareStatement(sql);
                                        stmt.setInt(1, userId);
                                        ResultSet rs = stmt.executeQuery();
                                        
                                        int offlineCount = 0;
                                        
                                        while (rs.next()) {
                                            offlineCount++;
                                %>
                                            <tr>
                                                <td><%= rs.getTimestamp("order_date").toString().substring(11, 16) %></td>
                                                <td>
                                                    <%= rs.getString("customer_name") != null ? rs.getString("customer_name") : "Walk-in Customer" %>
                                                    <% if (rs.getString("customer_name") == null) { %>
                                                        <br><small>📞 <%= rs.getString("delivery_address") %></small>
                                                    <% } %>
                                                </td>
                                                <td><%= rs.getString("product_name") %></td>
                                                <td><%= rs.getBigDecimal("quantity") %> kg</td>
                                                <td><%= rs.getBigDecimal("total_price") %> Birr</td>
                                                <td>
                                                    <span class="payment-status payment-<%= rs.getString("payment_status") %>">
                                                        <%= rs.getString("payment_status") %>
                                                    </span>
                                                </td>
                                                <td>
                                                    <button class="btn btn-sm btn-success" 
                                                            onclick="printReceipt(<%= rs.getInt("id") %>)">🖨️</button>
                                                    <button class="btn btn-sm btn-danger" 
                                                            onclick="voidOrder(<%= rs.getInt("id") %>)">❌</button>
                                                </td>
                                            </tr>
                                <%
                                        }
                                        
                                        if (offlineCount == 0) {
                                %>
                                            <tr>
                                                <td colspan="7" style="text-align: center; padding: 2rem;">
                                                    No offline orders today. Start recording orders!
                                                </td>
                                            </tr>
                                <%
                                        }
                                        
                                        rs.close();
                                        stmt.close();
                                        conn.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                %>
                                        <tr>
                                            <td colspan="7" style="text-align: center; color: #e74c3c;">
                                                Error loading offline orders
                                            </td>
                                        </tr>
                                <%
                                    }
                                %>
                            </tbody>
                        </table>
                    </div>
                    
                    <!-- Quick Order Form -->
                    <div class="offline-orders" style="margin-top: 2rem;">
                        <h3>Quick Order Entry</h3>
                        <form id="quickOrderForm" action="operator" method="POST" onsubmit="return submitQuickOrder()">
                            <input type="hidden" name="action" value="createOfflineOrder">
                            
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="customerName">Customer Name</label>
                                    <input type="text" id="customerName" name="customerName" 
                                           placeholder="Enter name or 'Walk-in'">
                                </div>
                                <div class="form-group">
                                    <label for="customerPhone">Phone Number</label>
                                    <input type="tel" id="customerPhone" name="customerPhone" 
                                           pattern="\+251[79]\d{8}" placeholder="+2519XXXXXXXX">
                                </div>
                            </div>
                            
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="productSelect">Product *</label>
                                    <select id="productSelect" name="productId" required onchange="updateProductPrice()">
                                        <option value="">Select Product</option>
                                        <%
                                            try {
                                                Connection conn = DatabaseConnection.getConnection();
                                                String sql = "SELECT id, name, sell_price, milling_price FROM products WHERE is_posted = true ORDER BY name";
                                                PreparedStatement stmt = conn.prepareStatement(sql);
                                                ResultSet rs = stmt.executeQuery();
                                                
                                                while (rs.next()) {
                                        %>
                                                    <option value="<%= rs.getInt("id") %>" 
                                                            data-price="<%= rs.getBigDecimal("sell_price") %>"
                                                            data-milling="<%= rs.getBigDecimal("milling_price") %>">
                                                        <%= rs.getString("name") %> - <%= rs.getBigDecimal("sell_price") %> Birr/kg
                                                    </option>
                                        <%
                                                }
                                                
                                                rs.close();
                                                stmt.close();
                                                conn.close();
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            }
                                        %>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label for="quantity">Quantity (kg) *</label>
                                    <input type="number" id="quantity" name="quantity" 
                                           min="1" step="0.5" required oninput="calculateTotal()">
                                </div>
                            </div>
                            
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="paymentMethod">Payment Method *</label>
                                    <select id="paymentMethod" name="paymentMethod" required>
                                        <option value="">Select Method</option>
                                        <option value="cash">Cash</option>
                                        <option value="CBE">CBE</option>
                                        <option value="Telebirr">Telebirr</option>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label for="orderType">Order Type</label>
                                    <select id="orderType" name="orderType">
                                        <option value="takeaway">Takeaway</option>
                                        <option value="milling">Milling Only</option>
                                        <option value="both">Product + Milling</option>
                                    </select>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <label>Order Summary</label>
                                <div class="cart-summary" style="margin-top: 0.5rem;">
                                    <div class="summary-row">
                                        <span>Product Price:</span>
                                        <span id="summaryProductPrice">0 Birr</span>
                                    </div>
                                    <div class="summary-row">
                                        <span>Milling Charge:</span>
                                        <span id="summaryMillingCharge">0 Birr</span>
                                    </div>
                                    <div class="summary-row">
                                        <span>Order Fee:</span>
                                        <span>20 Birr</span>
                                    </div>
                                    <div class="summary-row summary-total">
                                        <span>Total:</span>
                                        <span id="summaryTotal">0 Birr</span>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <button type="submit" class="btn btn-success">Create Order</button>
                                <button type="button" class="btn btn-primary" onclick="printQuickReceipt()">Print Receipt</button>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Inventory Tab -->
                <div id="inventoryTab" class="tab-content <%= "inventory".equals(tab) ? "active" : "" %>">
                    <h2>Inventory Management</h2>
                    
                    <div class="search-filter">
                        <input type="text" class="search-box" id="inventorySearch" 
                               placeholder="Search products..." onkeyup="searchInventory()">
                        <select class="search-box" id="categoryFilter" onchange="filterInventory()">
                            <option value="all">All Categories</option>
                            <option value="Grain">Grains</option>
                            <option value="Legume">Legumes</option>
                            <option value="Other">Other</option>
                        </select>
                        <button class="btn btn-primary" onclick="openStockUpdateModal()">Update Stock</button>
                    </div>
                    
                    <table class="inventory-table">
                        <thead>
                            <tr>
                                <th>Product</th>
                                <th>Category</th>
                                <th>Current Stock</th>
                                <th>Min Stock</th>
                                <th>Status</th>
                                <th>Price (Birr/kg)</th>
                                <th>Last Updated</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="inventoryTable">
                            <!-- Inventory will be loaded from database -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT p.*, " +
                                                "(SELECT SUM(quantity) FROM inventory_log WHERE product_id = p.id AND type = 'in') as total_in, " +
                                                "(SELECT SUM(quantity) FROM inventory_log WHERE product_id = p.id AND type = 'out') as total_out " +
                                                "FROM products p " +
                                                "WHERE p.is_posted = true " +
                                                "ORDER BY p.name";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    while (rs.next()) {
                                        BigDecimal totalIn = rs.getBigDecimal("total_in");
                                        BigDecimal totalOut = rs.getBigDecimal("total_out");
                                        
                                        if (totalIn == null) totalIn = BigDecimal.ZERO;
                                        if (totalOut == null) totalOut = BigDecimal.ZERO;
                                        
                                        BigDecimal currentStock = totalIn.subtract(totalOut);
                                        int minStock = 50; // Default minimum stock
                                        String stockStatus = currentStock.compareTo(BigDecimal.valueOf(minStock)) <= 0 ? "stock-low" : "stock-ok";
                                        String statusText = currentStock.compareTo(BigDecimal.valueOf(minStock)) <= 0 ? "Low Stock" : "In Stock";
                            %>
                                        <tr>
                                            <td>
                                                <strong><%= rs.getString("name") %></strong>
                                                <br><small><%= rs.getString("description") != null && rs.getString("description").length() > 50 
                                                    ? rs.getString("description").substring(0, 50) + "..." 
                                                    : rs.getString("description") != null ? rs.getString("description") : "" %></small>
                                            </td>
                                            <td><%= rs.getString("category") %></td>
                                            <td class="<%= stockStatus %>"><%= currentStock %> kg</td>
                                            <td><%= minStock %> kg</td>
                                            <td>
                                                <span class="order-status <%= stockStatus.equals("stock-low") ? "status-pending" : "status-completed" %>">
                                                    <%= statusText %>
                                                </span>
                                            </td>
                                            <td>
                                                Sell: <%= rs.getBigDecimal("sell_price") %><br>
                                                Milling: <%= rs.getBigDecimal("milling_price") %>
                                            </td>
                                            <td>
                                                <%= rs.getTimestamp("added_date") %>
                                            </td>
                                            <td>
                                                <button class="btn btn-sm btn-primary" 
                                                        onclick="updateProductStock(<%= rs.getInt("id") %>, '<%= rs.getString("name") %>')">
                                                    📝 Update
                                                </button>
                                                <button class="btn btn-sm btn-info" 
                                                        onclick="viewStockHistory(<%= rs.getInt("id") %>, '<%= rs.getString("name") %>')">
                                                    📋 History
                                                </button>
                                            </td>
                                        </tr>
                            <%
                                    }
                                    
                                    if (!rs.isBeforeFirst()) {
                            %>
                                        <tr>
                                            <td colspan="8" style="text-align: center; padding: 2rem;">
                                                No products in inventory. Contact admin to add products.
                                            </td>
                                        </tr>
                            <%
                                    }
                                    
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                            %>
                                    <tr>
                                        <td colspan="8" style="text-align: center; color: #e74c3c;">
                                            Error loading inventory
                                        </td>
                                    </tr>
                            <%
                                }
                            %>
                        </tbody>
                    </table>
                    
                    <div class="offline-orders" style="margin-top: 2rem;">
                        <h3>Low Stock Alert</h3>
                        <div id="lowStockAlert">
                            <!-- Low stock items will be loaded here -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT p.name, p.category, " +
                                                "(SELECT SUM(quantity) FROM inventory_log WHERE product_id = p.id AND type = 'in') as total_in, " +
                                                "(SELECT SUM(quantity) FROM inventory_log WHERE product_id = p.id AND type = 'out') as total_out " +
                                                "FROM products p " +
                                                "HAVING (total_in - total_out) <= 50 OR total_in IS NULL OR total_out IS NULL " +
                                                "ORDER BY (total_in - total_out)";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    int lowStockCount = 0;
                            %>
                                    <div class="alert alert-warning">
                                        <strong>Attention!</strong> The following products are running low:
                                        <ul style="margin-top: 0.5rem; margin-left: 1.5rem;">
                            <%
                                    while (rs.next()) {
                                        lowStockCount++;
                                        BigDecimal totalIn = rs.getBigDecimal("total_in");
                                        BigDecimal totalOut = rs.getBigDecimal("total_out");
                                        
                                        if (totalIn == null) totalIn = BigDecimal.ZERO;
                                        if (totalOut == null) totalOut = BigDecimal.ZERO;
                                        
                                        BigDecimal currentStock = totalIn.subtract(totalOut);
                            %>
                                            <li><%= rs.getString("name") %> (<%= rs.getString("category") %>) - Only <%= currentStock %> kg left</li>
                            <%
                                    }
                                    
                                    if (lowStockCount == 0) {
                            %>
                                            <li>All products have sufficient stock.</li>
                            <%
                                    }
                                    
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                            %>
                                        </ul>
                                    </div>
                            <%
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </div>
                    </div>
                </div>

                <!-- Drivers Tab -->
                <div id="driversTab" class="tab-content <%= "drivers".equals(tab) ? "active" : "" %>">
                    <h2>Driver Management</h2>
                    
                    <div class="search-filter">
                        <input type="text" class="search-box" id="driverSearch" 
                               placeholder="Search drivers..." onkeyup="searchDrivers()">
                        <select class="search-box" id="driverStatusFilter" onchange="filterDrivers()">
                            <option value="all">All Drivers</option>
                            <option value="available">Available Now</option>
                            <option value="busy">Currently Busy</option>
                        </select>
                    </div>
                    
                    <div class="orders-grid" id="driversGrid">
                        <!-- Drivers will be loaded from database -->
                        <%
                            try {
                                Connection conn = DatabaseConnection.getConnection();
                                String sql = "SELECT u.*, d.car_number, d.car_type, d.license_number, " +
                                            "(SELECT COUNT(*) FROM orders WHERE assigned_driver = u.id AND order_status IN ('processing', 'completed')) as active_deliveries " +
                                            "FROM users u " +
                                            "JOIN driver_details d ON u.id = d.driver_id " +
                                            "WHERE u.user_type = 'driver' AND u.status = 'active' " +
                                            "ORDER BY u.full_name";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                ResultSet rs = stmt.executeQuery();
                                
                                int driverCount = 0;
                                
                                while (rs.next()) {
                                    driverCount++;
                                    int activeDeliveries = rs.getInt("active_deliveries");
                                    String statusClass = activeDeliveries > 0 ? "status-processing" : "status-completed";
                                    String statusText = activeDeliveries > 0 ? "Busy (" + activeDeliveries + " deliveries)" : "Available";
                        %>
                                    <div class="order-card">
                                        <div class="order-header">
                                            <div class="order-number"><%= rs.getString("full_name") %></div>
                                            <span class="order-status <%= statusClass %>"><%= statusText %></span>
                                        </div>
                                        
                                        <div class="order-body">
                                            <div class="customer-info">
                                                <img src="<%= rs.getString("profile_image") != null ? rs.getString("profile_image") : "https://ui-avatars.com/api/?name=" + rs.getString("full_name") + "&background=3498db&color=fff" %>" 
                                                     alt="Driver" class="customer-avatar">
                                                <div class="customer-details">
                                                    <h4>📞 <%= rs.getString("phone") %></h4>
                                                    <p>🚗 <%= rs.getString("car_type") %> - <%= rs.getString("car_number") %></p>
                                                    <p>📋 License: <%= rs.getString("license_number") %></p>
                                                </div>
                                            </div>
                                            
                                            <div class="order-details">
                                                <div class="detail-row">
                                                    <span class="detail-label">Email:</span>
                                                    <span class="detail-value"><%= rs.getString("email") %></span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">Status:</span>
                                                    <span class="detail-value"><%= rs.getString("status") %></span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">Active Deliveries:</span>
                                                    <span class="detail-value"><%= activeDeliveries %></span>
                                                </div>
                                            </div>
                                            
                                            <div class="order-actions">
                                                <button class="btn btn-primary btn-sm" 
                                                        onclick="assignDriverToOrder(<%= rs.getInt("id") %>, '<%= rs.getString("full_name") %>')">
                                                    🚚 Assign to Order
                                                </button>
                                                <button class="btn btn-info btn-sm" 
                                                        onclick="contactDriver('<%= rs.getString("phone") %>', '<%= rs.getString("full_name") %>')">
                                                    📞 Contact
                                                </button>
                                                <button class="btn btn-outline btn-sm" 
                                                        onclick="viewDriverPerformance(<%= rs.getInt("id") %>, '<%= rs.getString("full_name") %>')">
                                                    📊 Performance
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                        <%
                                }
                                
                                if (driverCount == 0) {
                        %>
                                    <div class="empty-state">
                                        <div class="empty-state-icon">🚚</div>
                                        <h3>No Drivers Available</h3>
                                        <p>Contact admin to add drivers to the system.</p>
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
                                    Error loading drivers. Please try again later.
                                </div>
                        <%
                            }
                        %>
                    </div>
                    
                    <div class="offline-orders" style="margin-top: 2rem;">
                        <h3>Driver Assignments</h3>
                        <div id="driverAssignments">
                            <!-- Current driver assignments will be loaded here -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT o.order_number, u.full_name as customer_name, d.full_name as driver_name, " +
                                                "o.order_status, o.delivery_address " +
                                                "FROM orders o " +
                                                "JOIN users u ON o.customer_id = u.id " +
                                                "JOIN users d ON o.assigned_driver = d.id " +
                                                "WHERE o.order_status IN ('processing', 'completed') " +
                                                "AND o.assigned_operator = ? " +
                                                "ORDER BY o.order_date DESC LIMIT 5";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    stmt.setInt(1, userId);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    int assignmentCount = 0;
                            %>
                                    <table class="inventory-table">
                                        <thead>
                                            <tr>
                                                <th>Order</th>
                                                <th>Customer</th>
                                                <th>Driver</th>
                                                <th>Status</th>
                                                <th>Address</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                            <%
                                    while (rs.next()) {
                                        assignmentCount++;
                            %>
                                            <tr>
                                                <td><%= rs.getString("order_number") %></td>
                                                <td><%= rs.getString("customer_name") %></td>
                                                <td><%= rs.getString("driver_name") %></td>
                                                <td>
                                                    <span class="order-status status-<%= rs.getString("order_status") %>">
                                                        <%= rs.getString("order_status") %>
                                                    </span>
                                                </td>
                                                <td><%= rs.getString("delivery_address") %></td>
                                            </tr>
                            <%
                                    }
                                    
                                    if (assignmentCount == 0) {
                            %>
                                            <tr>
                                                <td colspan="5" style="text-align: center; padding: 1rem;">
                                                    No active driver assignments
                                                </td>
                                            </tr>
                            <%
                                    }
                                    
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                            %>
                                        </tbody>
                                    </table>
                            <%
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </div>
                    </div>
                </div>

                <!-- Reports Tab -->
                <div id="reportsTab" class="tab-content <%= "reports".equals(tab) ? "active" : "" %>">
                    <h2>Reports & Analytics</h2>
                    
                    <div class="quick-actions">
                        <div class="action-btn" onclick="generateDailyReport()">
                            <div>📅</div>
                            <div>Daily Report</div>
                        </div>
                        <div class="action-btn" onclick="generateWeeklyReport()">
                            <div>📊</div>
                            <div>Weekly Report</div>
                        </div>
                        <div class="action-btn" onclick="generateInventoryReport()">
                            <div>📦</div>
                            <div>Inventory Report</div>
                        </div>
                        <div class="action-btn" onclick="generatePerformanceReport()">
                            <div>⭐</div>
                            <div>Performance Report</div>
                        </div>
                    </div>
                    
                    <div class="stats-grid" style="margin-top: 2rem;">
                        <div class="stat-card">
                            <div class="stat-number" id="todayOrders">0</div>
                            <div class="stat-label">Orders Today</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="todayRevenue">0</div>
                            <div class="stat-label">Revenue Today</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="avgProcessing">0</div>
                            <div class="stat-label">Avg. Processing Time</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="completionRate">0%</div>
                            <div class="stat-label">Completion Rate</div>
                        </div>
                    </div>
                    
                    <div class="offline-orders" style="margin-top: 2rem;">
                        <h3>Today's Activity</h3>
                        <div id="todayActivity">
                            <!-- Today's activity will be loaded here -->
                            <div class="timeline" id="activityTimeline">
                                <!-- Timeline items will be added by JavaScript -->
                            </div>
                        </div>
                    </div>
                    
                    <div class="offline-orders" style="margin-top: 2rem;">
                        <h3>Top Products</h3>
                        <table class="inventory-table">
                            <thead>
                                <tr>
                                    <th>Product</th>
                                    <th>Quantity Sold</th>
                                    <th>Revenue</th>
                                    <th>Trend</th>
                                </tr>
                            </thead>
                            <tbody id="topProductsTable">
                                <!-- Top products will be loaded here -->
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Settings Tab -->
                <div id="settingsTab" class="tab-content <%= "settings".equals(tab) ? "active" : "" %>">
                    <h2>Operator Settings</h2>
                    
                    <div class="settings-grid">
                        <div class="settings-card">
                            <h3>Profile Settings</h3>
                            <form id="profileForm" action="operator" method="POST">
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
                                    <button type="submit" class="btn btn-primary">Update Profile</button>
                                </div>
                            </form>
                        </div>
                        
                        <div class="settings-card">
                            <h3>Security Settings</h3>
                            <form id="passwordForm" action="operator" method="POST">
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
                        </div>
                        
                        <div class="settings-card">
                            <h3>Working Hours</h3>
                            <form id="workingHoursForm" action="operator" method="POST">
                                <input type="hidden" name="action" value="updateWorkingHours">
                                
                                <div class="form-row">
                                    <div class="form-group">
                                        <label for="shiftStart">Shift Start *</label>
                                        <input type="time" id="shiftStart" name="shiftStart" value="08:00" required>
                                    </div>
                                    <div class="form-group">
                                        <label for="shiftEnd">Shift End *</label>
                                        <input type="time" id="shiftEnd" name="shiftEnd" value="17:00" required>
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <label for="workingDays">Working Days</label>
                                    <div style="margin-top: 0.5rem;">
                                        <label style="display: inline-block; margin-right: 1rem;">
                                            <input type="checkbox" name="workingDays" value="mon" checked> Mon
                                        </label>
                                        <label style="display: inline-block; margin-right: 1rem;">
                                            <input type="checkbox" name="workingDays" value="tue" checked> Tue
                                        </label>
                                        <label style="display: inline-block; margin-right: 1rem;">
                                            <input type="checkbox" name="workingDays" value="wed" checked> Wed
                                        </label>
                                        <label style="display: inline-block; margin-right: 1rem;">
                                            <input type="checkbox" name="workingDays" value="thu" checked> Thu
                                        </label>
                                        <label style="display: inline-block; margin-right: 1rem;">
                                            <input type="checkbox" name="workingDays" value="fri" checked> Fri
                                        </label>
                                        <label style="display: inline-block; margin-right: 1rem;">
                                            <input type="checkbox" name="workingDays" value="sat"> Sat
                                        </label>
                                        <label style="display: inline-block;">
                                            <input type="checkbox" name="workingDays" value="sun"> Sun
                                        </label>
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <button type="submit" class="btn btn-primary">Save Schedule</button>
                                </div>
                            </form>
                            
                            <div style="margin-top: 2rem;">
                                <h4>Current Status</h4>
                                <p id="currentStatus">Status: <span class="order-status status-completed">Active</span></p>
                                <button class="btn btn-warning" onclick="toggleAvailability()">Toggle Availability</button>
                            </div>
                        </div>
                    </div>
                </div>
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

    <!-- Assign Driver Modal -->
    <div id="assignDriverModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Assign Driver</h2>
                <button class="close-modal" onclick="closeModal('assignDriverModal')">&times;</button>
            </div>
            <form id="assignDriverForm" action="operator" method="POST">
                <input type="hidden" name="action" value="assignDriver">
                <input type="hidden" id="assignOrderId" name="orderId">
                
                <div class="form-group">
                    <label for="driverSelect">Select Driver *</label>
                    <select id="driverSelect" name="driverId" required>
                        <option value="">Choose a driver...</option>
                        <!-- Drivers will be loaded here -->
                    </select>
                </div>
                
                <div class="form-group">
                    <label for="driverInstructions">Special Instructions</label>
                    <textarea id="driverInstructions" name="instructions" rows="3" 
                              placeholder="Any special instructions for the driver..."></textarea>
                </div>
                
                <div class="form-group">
                    <label for="estimatedDelivery">Estimated Delivery Time</label>
                    <input type="datetime-local" id="estimatedDelivery" name="estimatedDelivery">
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-success">Assign Driver</button>
                    <button type="button" class="btn" onclick="closeModal('assignDriverModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Stock Update Modal -->
    <div id="stockUpdateModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Update Stock</h2>
                <button class="close-modal" onclick="closeModal('stockUpdateModal')">&times;</button>
            </div>
            <form id="stockUpdateForm" action="operator" method="POST">
                <input type="hidden" name="action" value="updateStock">
                <input type="hidden" id="stockProductId" name="productId">
                
                <div class="form-group">
                    <h3 id="stockProductName">Product Name</h3>
                    <p>Current Stock: <span id="currentStockValue">0</span> kg</p>
                </div>
                
                <div class="form-group">
                    <label for="stockType">Transaction Type *</label>
                    <select id="stockType" name="type" required>
                        <option value="">Select Type</option>
                        <option value="in">Stock In (Add)</option>
                        <option value="out">Stock Out (Deduct)</option>
                        <option value="adjust">Adjustment</option>
                    </select>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="stockQuantity">Quantity (kg) *</label>
                        <input type="number" id="stockQuantity" name="quantity" step="0.01" min="0.01" required>
                    </div>
                    <div class="form-group">
                        <label for="stockReason">Reason</label>
                        <select id="stockReason" name="reason">
                            <option value="">Select Reason</option>
                            <option value="purchase">New Purchase</option>
                            <option value="sale">Sale</option>
                            <option value="damage">Damaged Goods</option>
                            <option value="expired">Expired</option>
                            <option value="adjustment">Stock Adjustment</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="stockNotes">Notes</label>
                    <textarea id="stockNotes" name="notes" rows="3" 
                              placeholder="Additional notes about this transaction..."></textarea>
                </div>
                
                <div class="form-group">
                    <label>New Stock After Update: <span id="newStockValue">0</span> kg</label>
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-success">Update Stock</button>
                    <button type="button" class="btn" onclick="closeModal('stockUpdateModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Stock History Modal -->
    <div id="stockHistoryModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Stock History</h2>
                <button class="close-modal" onclick="closeModal('stockHistoryModal')">&times;</button>
            </div>
            <div id="stockHistoryContent">
                <!-- Stock history will be loaded here -->
            </div>
        </div>
    </div>

    <!-- Offline Order Modal -->
    <div id="offlineOrderModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>New Offline Order</h2>
                <button class="close-modal" onclick="closeModal('offlineOrderModal')">&times;</button>
            </div>
            <form id="offlineOrderForm" action="operator" method="POST">
                <input type="hidden" name="action" value="createOfflineOrder">
                <!-- Form content similar to quick order form -->
            </form>
        </div>
    </div>

    <!-- Print Receipt Modal -->
    <div id="printReceiptModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Print Receipt</h2>
                <button class="close-modal" onclick="closeModal('printReceiptModal')">&times;</button>
            </div>
            <div id="receiptContent">
                <!-- Receipt content will be loaded here -->
            </div>
            <div class="form-group" style="margin-top: 1rem;">
                <button class="btn btn-primary" onclick="printReceiptNow()">🖨️ Print</button>
                <button class="btn" onclick="closeModal('printReceiptModal')">Close</button>
            </div>
        </div>
    </div>

    <!-- Notifications Modal -->
    <div id="notificationsModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Notifications</h2>
                <button class="close-modal" onclick="closeModal('notificationsModal')">&times;</button>
            </div>
            <div id="notificationsContent">
                <!-- Notifications will be loaded here -->
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
     
     // Assign driver to order
        function assignDriver(orderId) {
            fetch('operator?action=getAvailableDrivers')
                .then(response => response.text())
                .then(html => {
                    document.getElementById('driversList').innerHTML = html;
                    document.getElementById('assignOrderId').value = orderId;
                    openModal('assignDriverModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading drivers');
                });
        }

        function confirmDriverAssignment() {
            const orderId = document.getElementById('assignOrderId').value;
            const driverId = document.getElementById('selectedDriver').value;
            
            if (!driverId) {
                alert('Please select a driver');
                return;
            }
            
            fetch('operator?action=assignDriver&orderId=' + orderId + '&driverId=' + driverId, {
                method: 'POST'
            })
            .then(response => {
                if (response.ok) {
                    alert('Driver assigned successfully!');
                    closeModal('assignDriverModal');
                    window.location.reload();
                } else {
                    alert('Error assigning driver');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error assigning driver');
            });
        }

        // Update order status
        function updateOrderStatus(orderId, status) {
            if (confirm('Change order status to ' + status + '?')) {
                fetch('operator?action=updateOrderStatus&orderId=' + orderId + '&status=' + status, {
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
        // Order Management Functions
        function searchOrders() {
            const searchTerm = document.getElementById('orderSearch').value.toLowerCase();
            const orderCards = document.querySelectorAll('.order-card');
            
            orderCards.forEach(card => {
                const text = card.textContent.toLowerCase();
                card.style.display = text.includes(searchTerm) ? 'block' : 'none';
            });
        }

        function filterByDate() {
            const filter = document.getElementById('dateFilter').value;
            const orderCards = document.querySelectorAll('.order-card');
            const today = new Date();
            
            orderCards.forEach(card => {
                const dateText = card.querySelector('.order-date').textContent;
                const orderDate = new Date(dateText);
                let show = true;
                
                switch(filter) {
                    case 'today':
                        show = orderDate.toDateString() === today.toDateString();
                        break;
                    case 'week':
                        const weekStart = new Date(today);
                        weekStart.setDate(today.getDate() - today.getDay());
                        show = orderDate >= weekStart;
                        break;
                    case 'month':
                        show = orderDate.getMonth() === today.getMonth() && 
                               orderDate.getFullYear() === today.getFullYear();
                        break;
                }
                
                card.style.display = show ? 'block' : 'none';
            });
        }

        function updateOrderStatus(orderId, newStatus) {
            if (confirm('Change order status to ' + newStatus + '?')) {
                fetch('operator?action=updateOrderStatus&orderId=' + orderId + '&status=' + newStatus, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Order status updated successfully');
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

        function assignToMe(orderId) {
            fetch('operator?action=assignToMe&orderId=' + orderId, {
                method: 'POST'
            })
            .then(response => {
                if (response.ok) {
                    alert('Order assigned to you');
                    window.location.reload();
                } else {
                    alert('Error assigning order');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error assigning order');
            });
        }

        function openAssignDriverModal(orderId) {
            document.getElementById('assignOrderId').value = orderId;
            
            // Load available drivers
            fetch('operator?action=getAvailableDrivers')
                .then(response => response.json())
                .then(drivers => {
                    const select = document.getElementById('driverSelect');
                    select.innerHTML = '<option value="">Choose a driver...</option>';
                    drivers.forEach(driver => {
                        const option = document.createElement('option');
                        option.value = driver.id;
                        option.textContent = driver.full_name + ' - ' + driver.car_type + ' (' + driver.car_number + ')';
                        select.appendChild(option);
                    });
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading drivers');
                });
            
            openModal('assignDriverModal');
        }

        function viewOrderDetails(orderId) {
            fetch('operator?action=getOrderDetails&orderId=' + orderId)
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

        function viewOrderTimeline(orderId) {
            fetch('operator?action=getOrderTimeline&orderId=' + orderId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('orderDetailsContent').innerHTML = html;
                    openModal('orderDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading order timeline');
                });
        }

        function markPaymentPaid(orderId) {
            if (confirm('Mark payment as paid?')) {
                fetch('operator?action=markPaymentPaid&orderId=' + orderId, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Payment marked as paid');
                        window.location.reload();
                    } else {
                        alert('Error updating payment status');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error updating payment status');
                });
            }
        }

        // Offline Order Functions
        function openOfflineOrderModal() {
            // Similar to buyNow modal implementation
            alert('Open offline order form');
        }

        function updateProductPrice() {
            const select = document.getElementById('productSelect');
            const selectedOption = select.options[select.selectedIndex];
            const price = selectedOption.getAttribute('data-price') || 0;
            const milling = selectedOption.getAttribute('data-milling') || 0;
            
            document.getElementById('summaryProductPrice').textContent = '0 Birr';
            document.getElementById('summaryMillingCharge').textContent = '0 Birr';
            calculateTotal();
        }

        function calculateTotal() {
            const quantity = parseFloat(document.getElementById('quantity').value) || 0;
            const select = document.getElementById('productSelect');
            const selectedOption = select.options[select.selectedIndex];
            const price = parseFloat(selectedOption.getAttribute('data-price')) || 0;
            const milling = parseFloat(selectedOption.getAttribute('data-milling')) || 0;
            const orderType = document.getElementById('orderType').value;
            
            let productTotal = 0;
            let millingTotal = 0;
            
            if (orderType === 'takeaway' || orderType === 'both') {
                productTotal = quantity * price;
            }
            
            if (orderType === 'milling' || orderType === 'both') {
                millingTotal = quantity * milling;
            }
            
            const orderFee = 20;
            const total = productTotal + millingTotal + orderFee;
            
            document.getElementById('summaryProductPrice').textContent = productTotal.toFixed(2) + ' Birr';
            document.getElementById('summaryMillingCharge').textContent = millingTotal.toFixed(2) + ' Birr';
            document.getElementById('summaryTotal').textContent = total.toFixed(2) + ' Birr';
        }

        function submitQuickOrder() {
            // Validate form
            const customerName = document.getElementById('customerName').value;
            const productId = document.getElementById('productSelect').value;
            
            if (!customerName.trim()) {
                alert('Please enter customer name');
                return false;
            }
            
            if (!productId) {
                alert('Please select a product');
                return false;
            }
            
            return true;
        }

        function printQuickReceipt() {
            // Generate receipt for preview
            alert('Print receipt functionality');
        }

        function printReceipt(orderId) {
            fetch('operator?action=getReceipt&orderId=' + orderId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('receiptContent').innerHTML = html;
                    openModal('printReceiptModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading receipt');
                });
        }

    //    function printReceiptNow() {
         //   const receiptContent = document.getElementById('receiptContent').innerHTML;
      //      const printWindow = window.open('', '_blank');
        //    printWindow.document.close();
        //}

        function voidOrder(orderId) {
            if (confirm('Void this order? This action cannot be undone.')) {
                fetch('operator?action=voidOrder&orderId=' + orderId, {
                    method: 'POST'
                })
                .then(response => {
                    if (response.ok) {
                        alert('Order voided successfully');
                        window.location.reload();
                    } else {
                        alert('Error voiding order');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error voiding order');
                });
            }
        }

        // Inventory Functions
        function searchInventory() {
            const searchTerm = document.getElementById('inventorySearch').value.toLowerCase();
            const rows = document.querySelectorAll('#inventoryTable tr');
            
            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(searchTerm) ? '' : 'none';
            });
        }

        function filterInventory() {
            const category = document.getElementById('categoryFilter').value;
            const rows = document.querySelectorAll('#inventoryTable tr');
            
            rows.forEach(row => {
                if (row.cells[1]) {
                    const rowCategory = row.cells[1].textContent;
                    row.style.display = (category === 'all' || rowCategory === category) ? '' : 'none';
                }
            });
        }

        function updateProductStock(productId, productName) {
            document.getElementById('stockProductId').value = productId;
            document.getElementById('stockProductName').textContent = productName;
            
            // Get current stock
            fetch('operator?action=getCurrentStock&productId=' + productId)
                .then(response => response.json())
                .then(data => {
                    document.getElementById('currentStockValue').textContent = data.stock || 0;
                    updateNewStockValue();
                })
                .catch(error => {
                    console.error('Error:', error);
                    document.getElementById('currentStockValue').textContent = '0';
                });
            
            openModal('stockUpdateModal');
        }

        function updateNewStockValue() {
            const currentStock = parseFloat(document.getElementById('currentStockValue').textContent) || 0;
            const quantity = parseFloat(document.getElementById('stockQuantity').value) || 0;
            const type = document.getElementById('stockType').value;
            
            let newStock = currentStock;
            if (type === 'in') {
                newStock = currentStock + quantity;
            } else if (type === 'out') {
                newStock = currentStock - quantity;
            }
            
            document.getElementById('newStockValue').textContent = newStock.toFixed(2);
        }

        // Add event listeners for stock calculation
        document.getElementById('stockQuantity')?.addEventListener('input', updateNewStockValue);
        document.getElementById('stockType')?.addEventListener('change', updateNewStockValue);

        function viewStockHistory(productId, productName) {
            fetch('operator?action=getStockHistory&productId=' + productId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('stockHistoryContent').innerHTML = '<h3>Stock History: ' + productName + '</h3>' + html;
                    openModal('stockHistoryModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading stock history');
                });
        }

        function openStockUpdateModal() {
            alert('Open bulk stock update modal');
        }

        // Driver Functions
        function searchDrivers() {
            const searchTerm = document.getElementById('driverSearch').value.toLowerCase();
            const driverCards = document.querySelectorAll('#driversGrid .order-card');
            
            driverCards.forEach(card => {
                const text = card.textContent.toLowerCase();
                card.style.display = text.includes(searchTerm) ? 'block' : 'none';
            });
        }

        function filterDrivers() {
            const status = document.getElementById('driverStatusFilter').value;
            const driverCards = document.querySelectorAll('#driversGrid .order-card');
            
            driverCards.forEach(card => {
                const statusElement = card.querySelector('.order-status');
                const statusText = statusElement.textContent.toLowerCase();
                let show = true;
                
                if (status === 'available') {
                    show = statusText.includes('available');
                } else if (status === 'busy') {
                    show = statusText.includes('busy');
                }
                
                card.style.display = show ? 'block' : 'none';
            });
        }

        function assignDriverToOrder(driverId, driverName) {
            // Get pending orders for assignment
            fetch('operator?action=getPendingOrders')
                .then(response => response.json())
                .then(orders => {
                    if (orders.length === 0) {
                        alert('No pending orders to assign');
                        return;
                    }
                    
                    let orderList = 'Select an order to assign:\\n\\n';
                    orders.forEach(order => {
                        orderList += order.id + '. ' + order.customer_name + ' - ' + order.product_name + ' (' + order.quantity + 'kg)\\n';
                    });
                    
                    const orderId = prompt(orderList + '\\nEnter order ID:');
                    if (orderId) {
                        fetch('operator?action=assignDriver&orderId=' + orderId + '&driverId=' + driverId, {
                            method: 'POST'
                        })
                        .then(response => {
                            if (response.ok) {
                                alert('Driver ' + driverName + ' assigned to order #' + orderId);
                                window.location.reload();
                            } else {
                                alert('Error assigning driver');
                            }
                        })
                        .catch(error => {
                            console.error('Error:', error);
                            alert('Error assigning driver');
                        });
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading pending orders');
                });
        }

        function contactDriver(phone, name) {
            if (confirm('Contact driver ' + name + ' at ' + phone + '?')) {
                window.location.href = 'tel:' + phone;
            }
        }

        function viewDriverPerformance(driverId, driverName) {
            fetch('operator?action=getDriverPerformance&driverId=' + driverId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('orderDetailsContent').innerHTML = '<h3>Performance: ' + driverName + '</h3>' + html;
                    openModal('orderDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading driver performance');
                });
        }

        // Reports Functions
        function generateDailyReport() {
            fetch('operator?action=generateReport&type=daily')
                .then(response => response.blob())
                .then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'daily-report-' + new Date().toISOString().split('T')[0] + '.pdf';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error generating report');
                });
        }

        function generateWeeklyReport() {
            fetch('operator?action=generateReport&type=weekly')
                .then(response => response.blob())
                .then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'weekly-report-' + new Date().toISOString().split('T')[0] + '.pdf';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error generating report');
                });
        }

        function generateInventoryReport() {
            window.open('operator?action=inventoryReport', '_blank');
        }

        function generatePerformanceReport() {
            window.open('operator?action=performanceReport', '_blank');
        }

        // Load report data on page load
        function loadReportData() {
            fetch('operator?action=getReportData')
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        document.getElementById('todayOrders').textContent = data.todayOrders || 0;
                        document.getElementById('todayRevenue').textContent = (data.todayRevenue || 0) + ' Birr';
                        document.getElementById('avgProcessing').textContent = (data.avgProcessing || 0) + ' mins';
                        document.getElementById('completionRate').textContent = (data.completionRate || 0) + '%';
                        
                        // Load top products
                        if (data.topProducts && data.topProducts.length > 0) {
                            const table = document.getElementById('topProductsTable');
                            table.innerHTML = '';
                            data.topProducts.forEach(product => {
                                const row = document.createElement('tr');
                                row.innerHTML = '<td>' + product.name + '</td><td>' + product.quantity + ' kg</td><td>' + product.revenue + ' Birr</td><td>' + product.trend + '</td>';
                                table.appendChild(row);
                            });
                        }
                        
                        // Load activity timeline
                        if (data.timeline && data.timeline.length > 0) {
                            const timeline = document.getElementById('activityTimeline');
                            timeline.innerHTML = '';
                            data.timeline.forEach(item => {
                                const timelineItem = document.createElement('div');
                                timelineItem.className = 'timeline-item';
                                timelineItem.innerHTML = '<div class="timeline-content"><strong>' + item.action + '</strong><p>' + item.details + '</p><div class="timeline-date">' + item.time + '</div></div>';
                                timeline.appendChild(timelineItem);
                            });
                        }
                    }
                })
                .catch(error => console.error('Error loading report data:', error));
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

        function toggleAvailability() {
            fetch('operator?action=toggleAvailability', {
                method: 'POST'
            })
            .then(response => response.json())
            .then(data => {
                if (data) {
                    const statusElement = document.getElementById('currentStatus');
                    const statusClass = data.status === 'available' ? 'completed' : 'pending';
                    statusElement.innerHTML = 'Status: <span class="order-status status-' + statusClass + '">' + data.status + '</span>';
                    alert('Availability set to: ' + data.status);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error toggling availability');
            });
        }

        // Notifications
        function showNotifications() {
            fetch('operator?action=getNotifications')
                .then(response => response.text())
                .then(html => {
                    document.getElementById('notificationsContent').innerHTML = html;
                    openModal('notificationsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading notifications');
                });
        }

        function loadNotificationCount() {
            fetch('operator?action=getNotificationCount')
                .then(response => response.json())
                .then(data => {
                    const countElement = document.getElementById('notificationCount');
                    if (data.count > 0) {
                        countElement.textContent = data.count;
                        countElement.style.display = 'block';
                    } else {
                        countElement.style.display = 'none';
                    }
                })
                .catch(error => console.error('Error loading notification count:', error));
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
            
            // Load report data if on reports tab
            if (window.location.href.includes('tab=reports')) {
                loadReportData();
            }
            
            // Load offline orders count
            loadOfflineCount();
            
            // Load notification count
            loadNotificationCount();
            
            // Set up quantity calculation for offline orders
            const quantityInput = document.getElementById('quantity');
            const orderTypeSelect = document.getElementById('orderType');
            const productSelect = document.getElementById('productSelect');
            
            if (quantityInput) quantityInput.addEventListener('input', calculateTotal);
            if (orderTypeSelect) orderTypeSelect.addEventListener('change', calculateTotal);
            if (productSelect) productSelect.addEventListener('change', calculateTotal);
        });

        function loadOfflineCount() {
            fetch('operator?action=getOfflineCount')
                .then(response => response.json())
                .then(data => {
                    document.getElementById('offlineCount').textContent = data.count || 0;
                })
                .catch(error => console.error('Error loading offline count:', error));
        }

        // Auto-refresh data
        setInterval(() => {
            if (window.location.href.includes('tab=orders')) {
                // Check for new orders
                fetch('operator?action=checkNewOrders')
                    .then(response => response.json())
                    .then(data => {
                        if (data.newOrders > 0) {
                            const notification = new Notification('New Orders', {
                                body: 'You have ' + data.newOrders + ' new order(s)',
                                icon: '/favicon.ico'
                            });
                            
                            // Update badge in sidebar
                            const badge = document.querySelector('.sidebar-menu a[href*="tab=orders"] .badge');
                            if (badge) {
                                const currentCount = parseInt(badge.textContent) || 0;
                                badge.textContent = currentCount + data.newOrders;
                            }
                        }
                    });
            }
            
            // Update notification count
            loadNotificationCount();
        }, 60000); // Check every minute

        // Close modal when clicking outside
        window.onclick = function(event) {
            if (event.target.classList.contains('modal')) {
                event.target.style.display = 'none';
            }
        }

        // Request notification permission
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }
    </script>
</body>
</html>