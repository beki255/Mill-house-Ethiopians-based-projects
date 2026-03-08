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
    if (!"driver".equals(userType)) {
        response.sendRedirect("index.html");
        return;
    }
    
    //User user = (User) userSession.getAttribute("user");
    int driverId = user.getId();
    String tab = request.getParameter("tab") != null ? request.getParameter("tab") : "orders";
    String statusFilter = request.getParameter("status") != null ? request.getParameter("status") : "assigned";
    String message = request.getParameter("message");
    String error = request.getParameter("error");
    
    // Get driver statistics
    int assignedOrders = 0;
    int deliveredToday = 0;
    int totalDeliveries = 0;
    BigDecimal totalEarnings = BigDecimal.ZERO;
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        // Assigned orders
        String sql = "SELECT COUNT(*) as count FROM orders WHERE assigned_driver = ? AND order_status IN ('processing', 'completed')";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, driverId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) assignedOrders = rs.getInt("count");
        rs.close();
        stmt.close();
        
        // Delivered today
        sql = "SELECT COUNT(*) as count FROM orders WHERE assigned_driver = ? AND order_status = 'delivered' AND DATE(order_date) = CURDATE()";
        stmt = conn.prepareStatement(sql);
        stmt.setInt(1, driverId);
        rs = stmt.executeQuery();
        if (rs.next()) deliveredToday = rs.getInt("count");
        rs.close();
        stmt.close();
        
        // Total deliveries and earnings
        sql = "SELECT COUNT(*) as count, SUM(total_price) as earnings FROM orders WHERE assigned_driver = ? AND order_status = 'delivered'";
        stmt = conn.prepareStatement(sql);
        stmt.setInt(1, driverId);
        rs = stmt.executeQuery();
        if (rs.next()) {
            totalDeliveries = rs.getInt("count");
            totalEarnings = rs.getBigDecimal("earnings") != null ? rs.getBigDecimal("earnings") : BigDecimal.ZERO;
        }
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
    <title>Driver Dashboard - Mill Management System</title>
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

        .driver-vehicle {
            font-size: 0.9rem;
            color: #aaa;
            margin-top: 0.5rem;
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

        .sidebar-menu .badge {
            position: absolute;
            right: 1rem;
            top: 50%;
            transform: translateY(-50%);
            background: var(--accent-color);
            color: white;
            padding: 0.2rem 0.5rem;
            border-radius: 10px;
            font-size: 0.8rem;
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

        .driver-status {
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .status-indicator {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background-color: var(--success-color);
        }

        .status-indicator.offline {
            background-color: var(--accent-color);
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

        .stat-card.assigned {
            border-top-color: var(--warning-color);
        }

        .stat-card.delivered {
            border-top-color: var(--success-color);
        }

        .stat-card.earnings {
            border-top-color: var(--info-color);
        }

        .stat-card.rating {
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
            position: relative;
        }

        .order-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 6px 12px rgba(0,0,0,0.15);
        }

        .order-priority {
            position: absolute;
            top: 1rem;
            right: 1rem;
            padding: 0.3rem 0.8rem;
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 500;
            z-index: 1;
        }

        .priority-high {
            background-color: #f8d7da;
            color: #721c24;
        }

        .priority-medium {
            background-color: #fff3cd;
            color: #856404;
        }

        .priority-low {
            background-color: #d1ecf1;
            color: #0c5460;
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

        .status-assigned {
            background-color: #cce5ff;
            color: #004085;
        }

        .status-picked {
            background-color: #d1ecf1;
            color: #0c5460;
        }

        .status-on-the-way {
            background-color: #fff3cd;
            color: #856404;
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

        /* Delivery Map */
        .delivery-map {
            height: 300px;
            background: #f8f9fa;
            border-radius: 8px;
            margin: 1.5rem 0;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 2px dashed #ddd;
        }

        .map-placeholder {
            text-align: center;
            color: #666;
        }

        .map-placeholder i {
            font-size: 3rem;
            margin-bottom: 1rem;
            display: block;
        }

        /* Delivery Timeline */
        .delivery-timeline {
            position: relative;
            padding-left: 2rem;
            margin: 1.5rem 0;
        }

        .delivery-timeline::before {
            content: '';
            position: absolute;
            left: 7px;
            top: 0;
            bottom: 0;
            width: 2px;
            background: var(--secondary-color);
        }

        .timeline-step {
            position: relative;
            margin-bottom: 1.5rem;
            padding-bottom: 1.5rem;
        }

        .timeline-step:last-child {
            margin-bottom: 0;
            padding-bottom: 0;
        }

        .timeline-step::before {
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

        .timeline-step.completed::before {
            background: var(--success-color);
        }

        .timeline-step.current::before {
            background: var(--warning-color);
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { transform: scale(1); }
            50% { transform: scale(1.2); }
            100% { transform: scale(1); }
        }

        .timeline-content {
            background: #f8f9fa;
            padding: 0.8rem;
            border-radius: 6px;
            border-left: 3px solid var(--secondary-color);
        }

        .timeline-step.completed .timeline-content {
            border-left-color: var(--success-color);
        }

        .timeline-step.current .timeline-content {
            border-left-color: var(--warning-color);
        }

        .timeline-time {
            font-size: 0.8rem;
            color: #666;
            margin-top: 0.3rem;
        }

        /* Navigation Panel */
        .navigation-panel {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            margin-top: 1.5rem;
            border: 1px solid #eee;
        }

        .navigation-controls {
            display: flex;
            gap: 1rem;
            margin-top: 1rem;
            flex-wrap: wrap;
        }

        .nav-btn {
            flex: 1;
            min-width: 120px;
            padding: 1rem;
            background: white;
            border: 2px solid var(--secondary-color);
            border-radius: 8px;
            text-align: center;
            cursor: pointer;
            transition: all 0.3s ease;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 0.5rem;
        }

        .nav-btn:hover {
            background: var(--secondary-color);
            color: white;
            transform: translateY(-3px);
        }

        .nav-btn i {
            font-size: 1.5rem;
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

        /* Earnings Table */
        .earnings-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }

        .earnings-table th,
        .earnings-table td {
            padding: 0.8rem;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }

        .earnings-table th {
            background-color: var(--primary-color);
            color: white;
            font-weight: 500;
        }

        .earnings-table tr:hover {
            background-color: #f8f9fa;
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

        /* Delivery Proof */
        .delivery-proof {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 1rem;
            margin-top: 1rem;
        }

        .proof-image {
            width: 100%;
            height: 150px;
            object-fit: cover;
            border-radius: 8px;
            border: 2px solid #ddd;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .proof-image:hover {
            transform: scale(1.05);
            border-color: var(--secondary-color);
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
            .sidebar-menu span,
            .driver-vehicle {
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
                right: 0.3rem;
                font-size: 0.7rem;
                padding: 0.1rem 0.3rem;
                min-width: 16px;
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
            
            .nav-btn {
                min-width: 100%;
            }
            
            .modal-content {
                padding: 1rem;
            }
        }

        /* Rating Stars */
        .rating-stars {
            display: flex;
            gap: 0.2rem;
            margin-top: 0.5rem;
        }

        .star {
            color: #ddd;
            font-size: 1.2rem;
        }

        .star.filled {
            color: #f39c12;
        }

        /* Route Summary */
        .route-summary {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            margin-top: 1rem;
            border: 1px solid #eee;
        }

        .route-stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 1rem;
            margin-top: 1rem;
        }

        .route-stat {
            text-align: center;
            padding: 1rem;
            background: #f8f9fa;
            border-radius: 8px;
        }

        .route-stat-value {
            font-size: 1.5rem;
            font-weight: bold;
            color: var(--primary-color);
            margin-bottom: 0.3rem;
        }

        .route-stat-label {
            font-size: 0.9rem;
            color: #666;
        }

        /* Vehicle Info */
        .vehicle-info {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            margin-top: 1rem;
            border: 1px solid #eee;
            display: flex;
            align-items: center;
            gap: 1.5rem;
        }

        .vehicle-icon {
            font-size: 3rem;
            color: var(--secondary-color);
        }

        .vehicle-details h4 {
            margin-bottom: 0.5rem;
            color: var(--primary-color);
        }

        /* Performance Chart */
        .performance-chart {
            height: 200px;
            background: #f8f9fa;
            border-radius: 8px;
            margin-top: 1rem;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 1px solid #eee;
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
                <p>Driver</p>
                <div class="driver-vehicle">
                    <%
                        try {
                            Connection conn = DatabaseConnection.getConnection();
                            String sql = "SELECT car_number, car_type FROM driver_details WHERE driver_id = ?";
                            PreparedStatement stmt = conn.prepareStatement(sql);
                            stmt.setInt(1, driverId);
                            ResultSet rs = stmt.executeQuery();
                            if (rs.next()) {
                    %>
                                🚗 <%= rs.getString("car_type") %> - <%= rs.getString("car_number") %>
                    <%
                            }
                            rs.close();
                            stmt.close();
                            conn.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    %>
                </div>
            </div>
            
            <ul class="sidebar-menu">
                <li><a href="?tab=orders&status=assigned" class="<%= "orders".equals(tab) ? "active" : "" %>">
                    📦 Orders
                    <% if (assignedOrders > 0) { %>
                        <span class="badge"><%= assignedOrders %></span>
                    <% } %>
                </a></li>
                <li><a href="?tab=map" class="<%= "map".equals(tab) ? "active" : "" %>">
                    🗺️ Delivery Map
                </a></li>
                <li><a href="?tab=earnings" class="<%= "earnings".equals(tab) ? "active" : "" %>">
                    💰 Earnings
                </a></li>
                <li><a href="?tab=performance" class="<%= "performance".equals(tab) ? "active" : "" %>">
                    📊 Performance
                </a></li>
                <li><a href="?tab=vehicle" class="<%= "vehicle".equals(tab) ? "active" : "" %>">
                    🚗 Vehicle
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
                    <h1>Driver Dashboard</h1>
                    <p>Deliver orders efficiently and track your performance</p>
                </div>
                <div class="header-right">
                    <div class="driver-status">
                        <span class="status-indicator" id="statusIndicator"></span>
                        <span id="statusText">Online</span>
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
                    <h2>Delivery Orders</h2>
                    
                    <div class="stats-grid">
                        <div class="stat-card assigned" onclick="window.location='?tab=orders&status=assigned'">
                            <div class="stat-number"><%= assignedOrders %></div>
                            <div class="stat-label">Assigned Orders</div>
                        </div>
                        <div class="stat-card delivered" onclick="window.location='?tab=orders&status=delivered'">
                            <div class="stat-number"><%= deliveredToday %></div>
                            <div class="stat-label">Delivered Today</div>
                        </div>
                        <div class="stat-card earnings" onclick="window.location='?tab=earnings'">
                            <div class="stat-number"><%= totalEarnings %> Birr</div>
                            <div class="stat-label">Total Earnings</div>
                        </div>
                        <div class="stat-card rating">
                            <div class="stat-number">4.8</div>
                            <div class="stat-label">Customer Rating</div>
                        </div>
                    </div>

                    <div class="search-filter">
                        <input type="text" class="search-box" id="orderSearch" 
                               placeholder="Search by order number, customer name..." onkeyup="searchOrders()">
                        <select class="search-box" id="priorityFilter" onchange="filterByPriority()">
                            <option value="all">All Priorities</option>
                            <option value="high">High Priority</option>
                            <option value="medium">Medium Priority</option>
                            <option value="low">Low Priority</option>
                        </select>
                    </div>

                    <div class="status-tabs">
                        <button class="status-tab <%= "assigned".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=assigned'">Assigned</button>
                        <button class="status-tab <%= "picked".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=picked'">Picked Up</button>
                        <button class="status-tab <%= "on-the-way".equals(statusFilter) ? "active" : "" %>" 
                                onclick="window.location='?tab=orders&status=on-the-way'">On The Way</button>
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
                                            "op.full_name as operator_name, op.phone as operator_phone " +
                                            "FROM orders o " +
                                            "JOIN users u ON o.customer_id = u.id " +
                                            "LEFT JOIN products p ON o.product_id = p.id " +
                                            "LEFT JOIN users op ON o.assigned_operator = op.id " +
                                            "WHERE o.assigned_driver = ? " +
                                            "AND o.order_status = ? " +
                                            "ORDER BY o.order_date DESC";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                stmt.setInt(1, driverId);
                                stmt.setString(2, statusFilter.equals("assigned") ? "processing" : statusFilter);
                                ResultSet rs = stmt.executeQuery();
                                
                                int orderCount = 0;
                                
                                while (rs.next()) {
                                    orderCount++;
                                    String status = rs.getString("order_status");
                                    String paymentStatus = rs.getString("payment_status");
                                    boolean isSpecialOrder = rs.getBoolean("is_special_order");
                                    String priority = determinePriority(rs.getTimestamp("order_date"), rs.getString("delivery_address"));
                        %>
                                    <div class="order-card" data-order-id="<%= rs.getInt("id") %>" data-priority="<%= priority %>">
                                        <div class="order-priority priority-<%= priority %>">
                                            <%= priority.toUpperCase() %> PRIORITY
                                        </div>
                                        
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
                                                    <p>📍 <%= rs.getString("delivery_address") %></p>
                                                </div>
                                            </div>
                                            
                                            <div class="order-details">
                                                <div class="detail-row">
                                                    <span class="detail-label">Product:</span>
                                                    <span class="detail-value">
                                                        <%= isSpecialOrder ? "Special Order" : rs.getString("product_name") %>
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
                                                    <span class="detail-label">Assigned By:</span>
                                                    <span class="detail-value">
                                                        <%= rs.getString("operator_name") %>
                                                        <% if (rs.getString("operator_phone") != null) { %>
                                                            <br><small>📞 <%= rs.getString("operator_phone") %></small>
                                                        <% } %>
                                                    </span>
                                                </div>
                                            </div>
                                            
                                            <% if ("processing".equals(status)) { %>
                                            <div class="delivery-timeline">
                                                <div class="timeline-step completed">
                                                    <div class="timeline-content">
                                                        <strong>Order Assigned</strong>
                                                        <p>You have been assigned this delivery</p>
                                                        <div class="timeline-time"><%= rs.getTimestamp("order_date") %></div>
                                                    </div>
                                                </div>
                                                <div class="timeline-step current">
                                                    <div class="timeline-content">
                                                        <strong>Pick Up Order</strong>
                                                        <p>Pick up the order from the mill</p>
                                                    </div>
                                                </div>
                                                <div class="timeline-step">
                                                    <div class="timeline-content">
                                                        <strong>On The Way</strong>
                                                        <p>Start delivery to customer</p>
                                                    </div>
                                                </div>
                                                <div class="timeline-step">
                                                    <div class="timeline-content">
                                                        <strong>Delivered</strong>
                                                        <p>Complete delivery and get confirmation</p>
                                                    </div>
                                                </div>
                                            </div>
                                            <% } %>
                                            
                                            <div class="order-actions">
                                                <% if ("processing".equals(status)) { %>
                                                    <button class="btn btn-success btn-sm" 
                                                            onclick="updateOrderStatus(<%= rs.getInt("id") %>, 'picked')">
                                                        ✅ Pick Up
                                                    </button>
                                                    <button class="btn btn-warning btn-sm" 
                                                            onclick="openNavigation(<%= rs.getInt("id") %>, '<%= rs.getString("delivery_address") %>')">
                                                        🗺️ Start Navigation
                                                    </button>
                                                    <button class="btn btn-info btn-sm" 
                                                            onclick="contactCustomer('<%= rs.getString("customer_phone") %>', '<%= rs.getString("customer_name") %>')">
                                                        📞 Contact Customer
                                                    </button>
                                                <% } else if ("picked".equals(status)) { %>
                                                    <button class="btn btn-warning btn-sm" 
                                                            onclick="updateOrderStatus(<%= rs.getInt("id") %>, 'on-the-way')">
                                                        🚚 Start Delivery
                                                    </button>
                                                    <button class="btn btn-info btn-sm" 
                                                            onclick="getRouteGuidance(<%= rs.getInt("id") %>)">
                                                        📍 Get Route
                                                    </button>
                                                <% } else if ("on-the-way".equals(status)) { %>
                                                    <button class="btn btn-success btn-sm" 
                                                            onclick="openDeliveryProofModal(<%= rs.getInt("id") %>)">
                                                        ✅ Mark Delivered
                                                    </button>
                                                    <button class="btn btn-danger btn-sm" 
                                                            onclick="reportIssue(<%= rs.getInt("id") %>)">
                                                        ⚠️ Report Issue
                                                    </button>
                                                <% } else if ("delivered".equals(status)) { %>
                                                    <button class="btn btn-primary btn-sm" 
                                                            onclick="viewDeliveryProof(<%= rs.getInt("id") %>)">
                                                        📸 View Proof
                                                    </button>
                                                    <button class="btn btn-info btn-sm" 
                                                            onclick="viewCustomerFeedback(<%= rs.getInt("id") %>)">
                                                        ⭐ Feedback
                                                    </button>
                                                <% } %>
                                                
                                                <button class="btn btn-outline btn-sm" 
                                                        onclick="viewOrderDetails(<%= rs.getInt("id") %>)">
                                                    👁️ View Details
                                                </button>
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
                                        <p>You have no <%= statusFilter %> orders at the moment.</p>
                                        <% if ("assigned".equals(statusFilter)) { %>
                                            <p>Check back soon for new assignments.</p>
                                        <% } %>
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

                <!-- Map Tab -->
                <div id="mapTab" class="tab-content <%= "map".equals(tab) ? "active" : "" %>">
                    <h2>Delivery Map</h2>
                    <p class="alert alert-info">View your delivery locations and optimize your route.</p>
                    
                    <div class="delivery-map">
                        <div class="map-placeholder">
                            <div>🗺️</div>
                            <h3>Delivery Map</h3>
                            <p>Interactive map will be displayed here</p>
                            <p><small>Requires location permissions</small></p>
                            <button class="btn btn-primary" onclick="initMap()">Load Map</button>
                        </div>
                    </div>
                    
                    <div class="route-summary">
                        <h3>Today's Route Summary</h3>
                        <div class="route-stats">
                            <div class="route-stat">
                                <div class="route-stat-value" id="totalStops">0</div>
                                <div class="route-stat-label">Total Stops</div>
                            </div>
                            <div class="route-stat">
                                <div class="route-stat-value" id="totalDistance">0 km</div>
                                <div class="route-stat-label">Total Distance</div>
                            </div>
                            <div class="route-stat">
                                <div class="route-stat-value" id="estimatedTime">0 min</div>
                                <div class="route-stat-label">Estimated Time</div>
                            </div>
                            <div class="route-stat">
                                <div class="route-stat-value" id="completedStops">0</div>
                                <div class="route-stat-label">Completed</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="navigation-panel">
                        <h3>Navigation Controls</h3>
                        <div class="navigation-controls">
                            <div class="nav-btn" onclick="startNavigation()">
                                <div>🚗</div>
                                <div>Start Navigation</div>
                            </div>
                            <div class="nav-btn" onclick="optimizeRoute()">
                                <div>⚡</div>
                                <div>Optimize Route</div>
                            </div>
                            <div class="nav-btn" onclick="getTrafficInfo()">
                                <div>🚦</div>
                                <div>Traffic Info</div>
                            </div>
                            <div class="nav-btn" onclick="shareLocation()">
                                <div>📍</div>
                                <div>Share Location</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="orders-grid" style="margin-top: 2rem;">
                        <h3>Today's Delivery Stops</h3>
                        <div id="todaysStops">
                            <!-- Today's stops will be loaded here -->
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT o.*, u.full_name as customer_name, u.phone as customer_phone, " +
                                                "u.address as customer_address " +
                                                "FROM orders o " +
                                                "JOIN users u ON o.customer_id = u.id " +
                                                "WHERE o.assigned_driver = ? " +
                                                "AND DATE(o.order_date) = CURDATE() " +
                                                "AND o.order_status IN ('processing', 'picked', 'on-the-way') " +
                                                "ORDER BY o.order_date";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    stmt.setInt(1, driverId);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    int stopCount = 0;
                                    
                                    while (rs.next()) {
                                        stopCount++;
                            %>
                                        <div class="order-card">
                                            <div class="order-header">
                                                <div class="order-number">Stop #<%= stopCount %></div>
                                                <span class="order-status status-<%= rs.getString("order_status") %>">
                                                    <%= rs.getString("order_status") %>
                                                </span>
                                            </div>
                                            <div class="order-body">
                                                <div class="customer-info">
                                                    <div class="customer-details">
                                                        <h4><%= rs.getString("customer_name") %></h4>
                                                        <p>📍 <%= rs.getString("customer_address") %></p>
                                                        <p>📞 <%= rs.getString("customer_phone") %></p>
                                                    </div>
                                                </div>
                                                <div class="order-actions">
                                                    <button class="btn btn-primary btn-sm" 
                                                            onclick="navigateToStop('<%= rs.getString("customer_address") %>')">
                                                        🗺️ Navigate
                                                    </button>
                                                    <button class="btn btn-info btn-sm" 
                                                            onclick="contactCustomer('<%= rs.getString("customer_phone") %>', '<%= rs.getString("customer_name") %>')">
                                                        📞 Call
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                            <%
                                    }
                                    
                                    if (stopCount == 0) {
                            %>
                                        <div class="empty-state">
                                            <div class="empty-state-icon">📍</div>
                                            <h3>No Stops Today</h3>
                                            <p>You have no delivery stops scheduled for today.</p>
                                        </div>
                            <%
                                    }
                                    
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </div>
                    </div>
                </div>

                <!-- Earnings Tab -->
                <div id="earningsTab" class="tab-content <%= "earnings".equals(tab) ? "active" : "" %>">
                    <h2>Earnings & Payments</h2>
                    
                    <div class="stats-grid">
                        <div class="stat-card">
                            <div class="stat-number"><%= totalEarnings %> Birr</div>
                            <div class="stat-label">Total Earnings</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="weeklyEarnings">0 Birr</div>
                            <div class="stat-label">This Week</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="pendingEarnings">0 Birr</div>
                            <div class="stat-label">Pending</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="averageDelivery">0 Birr</div>
                            <div class="stat-label">Avg. per Delivery</div>
                        </div>
                    </div>
                    
                    <div class="route-summary">
                        <h3>Earnings Breakdown</h3>
                        <div class="performance-chart">
                            <p>Earnings chart will be displayed here</p>
                        </div>
                    </div>
                    
                    <div class="navigation-panel">
                        <h3>Payment Information</h3>
                        <div class="form-group">
                            <p><strong>Payment Method:</strong> <%= user.getPaymentMethod() != null ? user.getPaymentMethod() : "Not Set" %></p>
                            <p><strong>Account:</strong> <%= user.getPaymentAccount() != null ? user.getPaymentAccount() : "Not Set" %></p>
                            <p><strong>Next Payout:</strong> <span id="nextPayout">End of Month</span></p>
                        </div>
                        <div class="navigation-controls">
                            <div class="nav-btn" onclick="requestPayout()">
                                <div>💰</div>
                                <div>Request Payout</div>
                            </div>
                            <div class="nav-btn" onclick="updatePaymentInfo()">
                                <div>⚙️</div>
                                <div>Update Payment</div>
                            </div>
                            <div class="nav-btn" onclick="downloadStatement()">
                                <div>📄</div>
                                <div>Download Statement</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="settings-card" style="margin-top: 2rem;">
                        <h3>Recent Earnings</h3>
                        <table class="earnings-table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Order #</th>
                                    <th>Customer</th>
                                    <th>Amount</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="earningsTable">
                                <!-- Earnings will be loaded from database -->
                                <%
                                    try {
                                        Connection conn = DatabaseConnection.getConnection();
                                        String sql = "SELECT o.order_number, o.order_date, o.total_price, " +
                                                    "u.full_name as customer_name, o.payment_status " +
                                                    "FROM orders o " +
                                                    "JOIN users u ON o.customer_id = u.id " +
                                                    "WHERE o.assigned_driver = ? " +
                                                    "AND o.order_status = 'delivered' " +
                                                    "ORDER BY o.order_date DESC LIMIT 10";
                                        PreparedStatement stmt = conn.prepareStatement(sql);
                                        stmt.setInt(1, driverId);
                                        ResultSet rs = stmt.executeQuery();
                                        
                                        while (rs.next()) {
                                %>
                                            <tr>
                                                <td><%= rs.getTimestamp("order_date").toString().substring(0, 10) %></td>
                                                <td><%= rs.getString("order_number") %></td>
                                                <td><%= rs.getString("customer_name") %></td>
                                                <td><%= rs.getBigDecimal("total_price") %> Birr</td>
                                                <td>
                                                    <span class="payment-status payment-<%= rs.getString("payment_status") %>">
                                                        <%= rs.getString("payment_status") %>
                                                    </span>
                                                </td>
                                                <td>
                                                    <button class="btn btn-sm btn-info" 
                                                            onclick="viewEarningDetails('<%= rs.getString("order_number") %>')">
                                                        👁️ View
                                                    </button>
                                                </td>
                                            </tr>
                                <%
                                        }
                                        
                                        if (!rs.isBeforeFirst()) {
                                %>
                                            <tr>
                                                <td colspan="6" style="text-align: center; padding: 2rem;">
                                                    No earnings history yet
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
                                            <td colspan="6" style="text-align: center; color: #e74c3c;">
                                                Error loading earnings
                                            </td>
                                        </tr>
                                <%
                                    }
                                %>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Performance Tab -->
                <div id="performanceTab" class="tab-content <%= "performance".equals(tab) ? "active" : "" %>">
                    <h2>Performance Dashboard</h2>
                    
                    <div class="stats-grid">
                        <div class="stat-card">
                            <div class="stat-number"><%= totalDeliveries %></div>
                            <div class="stat-label">Total Deliveries</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="onTimeRate">98%</div>
                            <div class="stat-label">On Time Rate</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="ratingScore">4.8</div>
                            <div class="stat-label">Rating</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number" id="avgDeliveryTime">45 min</div>
                            <div class="stat-label">Avg. Delivery Time</div>
                        </div>
                    </div>
                    
                    <div class="settings-card">
                        <h3>Customer Rating</h3>
                        <div class="rating-stars">
                            <span class="star filled">★</span>
                            <span class="star filled">★</span>
                            <span class="star filled">★</span>
                            <span class="star filled">★</span>
                            <span class="star">★</span>
                        </div>
                        <p style="margin-top: 1rem;">Based on <strong id="totalRatings">125</strong> customer reviews</p>
                    </div>
                    
                    <div class="settings-card" style="margin-top: 2rem;">
                        <h3>Recent Feedback</h3>
                        <div id="feedbackList">
                            <!-- Feedback will be loaded here -->
                            <div class="timeline">
                                <div class="timeline-step">
                                    <div class="timeline-content">
                                        <strong>Excellent Service!</strong>
                                        <div class="rating-stars">
                                            <span class="star filled">★</span>
                                            <span class="star filled">★</span>
                                            <span class="star filled">★</span>
                                            <span class="star filled">★</span>
                                            <span class="star filled">★</span>
                                        </div>
                                        <p>"Driver was very punctual and polite. Delivery was perfect!"</p>
                                        <div class="timeline-time">- Customer #3456, Yesterday</div>
                                    </div>
                                </div>
                                <div class="timeline-step">
                                    <div class="timeline-content">
                                        <strong>Good Service</strong>
                                        <div class="rating-stars">
                                            <span class="star filled">★</span>
                                            <span class="star filled">★</span>
                                            <span class="star filled">★</span>
                                            <span class="star filled">★</span>
                                            <span class="star">★</span>
                                        </div>
                                        <p>"Everything was good, just a bit late due to traffic."</p>
                                        <div class="timeline-time">- Customer #3455, 2 days ago</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="settings-card" style="margin-top: 2rem;">
                        <h3>Performance Metrics</h3>
                        <div class="performance-chart">
                            <p>Performance metrics chart will be displayed here</p>
                        </div>
                        <div class="navigation-controls" style="margin-top: 1rem;">
                            <div class="nav-btn" onclick="viewDetailedMetrics()">
                                <div>📊</div>
                                <div>Detailed Metrics</div>
                            </div>
                            <div class="nav-btn" onclick="comparePerformance()">
                                <div>📈</div>
                                <div>Compare</div>
                            </div>
                            <div class="nav-btn" onclick="downloadPerformanceReport()">
                                <div>📄</div>
                                <div>Download Report</div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Vehicle Tab -->
                <div id="vehicleTab" class="tab-content <%= "vehicle".equals(tab) ? "active" : "" %>">
                    <h2>Vehicle Information</h2>
                    
                    <div class="vehicle-info">
                        <div class="vehicle-icon">🚗</div>
                        <div class="vehicle-details">
                            <%
                                try {
                                    Connection conn = DatabaseConnection.getConnection();
                                    String sql = "SELECT d.*, u.full_name FROM driver_details d " +
                                                "JOIN users u ON d.driver_id = u.id " +
                                                "WHERE d.driver_id = ?";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    stmt.setInt(1, driverId);
                                    ResultSet rs = stmt.executeQuery();
                                    
                                    if (rs.next()) {
                            %>
                                        <h4><%= rs.getString("car_type") %> - <%= rs.getString("car_number") %></h4>
                                        <p><strong>License Number:</strong> <%= rs.getString("license_number") %></p>
                                        <p><strong>Driver:</strong> <%= rs.getString("full_name") %></p>
                                        <p><strong>Status:</strong> <span class="order-status status-completed">Active</span></p>
                            <%
                                    }
                                    rs.close();
                                    stmt.close();
                                    conn.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            %>
                        </div>
                    </div>
                    
                    <div class="settings-card" style="margin-top: 2rem;">
                        <h3>Vehicle Maintenance</h3>
                        <div class="form-group">
                            <p><strong>Last Service:</strong> <span id="lastService">2 weeks ago</span></p>
                            <p><strong>Next Service Due:</strong> <span id="nextServiceDue">In 2 weeks</span></p>
                            <p><strong>Mileage:</strong> <span id="currentMileage">45,678 km</span></p>
                        </div>
                        <div class="navigation-controls">
                            <div class="nav-btn" onclick="logMaintenance()">
                                <div>🔧</div>
                                <div>Log Maintenance</div>
                            </div>
                            <div class="nav-btn" onclick="requestService()">
                                <div>🛠️</div>
                                <div>Request Service</div>
                            </div>
                            <div class="nav-btn" onclick="viewMaintenanceHistory()">
                                <div>📋</div>
                                <div>View History</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="settings-card" style="margin-top: 2rem;">
                        <h3>Fuel Tracking</h3>
                        <div class="form-group">
                            <p><strong>Current Fuel Level:</strong> <span id="fuelLevel">75%</span></p>
                            <p><strong>Last Refuel:</strong> <span id="lastRefuel">Yesterday</span></p>
                            <p><strong>Fuel Efficiency:</strong> <span id="fuelEfficiency">12 km/L</span></p>
                        </div>
                        <div class="navigation-controls">
                            <div class="nav-btn" onclick="logFuelRefill()">
                                <div>⛽</div>
                                <div>Log Refill</div>
                            </div>
                            <div class="nav-btn" onclick="viewFuelHistory()">
                                <div>📊</div>
                                <div>Fuel History</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="settings-card" style="margin-top: 2rem;">
                        <h3>Vehicle Documents</h3>
                        <div class="delivery-proof">
                            <img src="https://via.placeholder.com/150" alt="Insurance" class="proof-image" onclick="viewDocument('insurance')">
                            <img src="https://via.placeholder.com/150" alt="Registration" class="proof-image" onclick="viewDocument('registration')">
                            <img src="https://via.placeholder.com/150" alt="License" class="proof-image" onclick="viewDocument('license')">
                            <img src="https://via.placeholder.com/150" alt="PUC" class="proof-image" onclick="viewDocument('puc')">
                        </div>
                        <div class="navigation-controls" style="margin-top: 1rem;">
                            <div class="nav-btn" onclick="uploadDocument()">
                                <div>📤</div>
                                <div>Upload Document</div>
                            </div>
                            <div class="nav-btn" onclick="renewDocument()">
                                <div>🔄</div>
                                <div>Renew Document</div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Settings Tab -->
                <div id="settingsTab" class="tab-content <%= "settings".equals(tab) ? "active" : "" %>">
                    <h2>Driver Settings</h2>
                    
                    <div class="settings-grid">
                        <div class="settings-card">
                            <h3>Profile Settings</h3>
                            <form id="profileForm" action="driver" method="POST">
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
                            <form id="passwordForm" action="driver" method="POST">
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
                            <h3>Vehicle Settings</h3>
                            <form id="vehicleForm" action="driver" method="POST">
                                <input type="hidden" name="action" value="updateVehicle">
                                
                                <%
                                    try {
                                        Connection conn = DatabaseConnection.getConnection();
                                        String sql = "SELECT * FROM driver_details WHERE driver_id = ?";
                                        PreparedStatement stmt = conn.prepareStatement(sql);
                                        stmt.setInt(1, driverId);
                                        ResultSet rs = stmt.executeQuery();
                                        
                                        if (rs.next()) {
                                %>
                                        <div class="form-row">
                                            <div class="form-group">
                                                <label for="carNumber">Car Number *</label>
                                                <input type="text" id="carNumber" name="carNumber" 
                                                       value="<%= rs.getString("car_number") %>" required>
                                            </div>
                                            <div class="form-group">
                                                <label for="carType">Car Type *</label>
                                                <input type="text" id="carType" name="carType" 
                                                       value="<%= rs.getString("car_type") %>" required>
                                            </div>
                                        </div>
                                        
                                        <div class="form-group">
                                            <label for="licenseNumber">License Number *</label>
                                            <input type="text" id="licenseNumber" name="licenseNumber" 
                                                   value="<%= rs.getString("license_number") != null ? rs.getString("license_number") : "" %>" required>
                                        </div>
                                <%
                                        }
                                        rs.close();
                                        stmt.close();
                                        conn.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                %>
                                
                                <div class="form-group">
                                    <button type="submit" class="btn btn-primary">Update Vehicle</button>
                                </div>
                            </form>
                        </div>
                        
                        <div class="settings-card">
                            <h3>Availability Settings</h3>
                            <form id="availabilityForm" action="driver" method="POST">
                                <input type="hidden" name="action" value="updateAvailability">
                                
                                <div class="form-group">
                                    <label for="workingHours">Working Hours</label>
                                    <div class="form-row">
                                        <div class="form-group">
                                            <input type="time" id="startTime" name="startTime" value="08:00" required>
                                        </div>
                                        <div class="form-group">
                                            <input type="time" id="endTime" name="endTime" value="18:00" required>
                                        </div>
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
                                    <label for="maxDeliveries">Max Deliveries Per Day</label>
                                    <input type="number" id="maxDeliveries" name="maxDeliveries" min="1" max="50" value="20">
                                </div>
                                
                                <div class="form-group">
                                    <button type="submit" class="btn btn-primary">Save Settings</button>
                                </div>
                            </form>
                            
                            <div style="margin-top: 2rem;">
                                <h4>Current Status</h4>
                                <p id="currentStatusDisplay">Status: <span class="order-status status-completed">Online</span></p>
                                <button class="btn btn-warning" onclick="toggleOnlineStatus()">Go Offline</button>
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

    <!-- Delivery Proof Modal -->
    <div id="deliveryProofModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Delivery Proof</h2>
                <button class="close-modal" onclick="closeModal('deliveryProofModal')">&times;</button>
            </div>
            <form id="deliveryProofForm" action="driver" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="action" value="submitDeliveryProof">
                <input type="hidden" id="proofOrderId" name="orderId">
                
                <div class="form-group">
                    <label for="deliveryStatus">Delivery Status *</label>
                    <select id="deliveryStatus" name="status" required>
                        <option value="">Select Status</option>
                        <option value="delivered">Delivered Successfully</option>
                        <option value="failed">Delivery Failed</option>
                        <option value="rescheduled">Rescheduled</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label for="deliveryNotes">Delivery Notes</label>
                    <textarea id="deliveryNotes" name="notes" rows="3" 
                              placeholder="Any notes about the delivery..."></textarea>
                </div>
                
                <div class="form-group">
                    <label for="deliveryTime">Actual Delivery Time</label>
                    <input type="datetime-local" id="deliveryTime" name="deliveryTime" required>
                </div>
                
                <div class="form-group">
                    <label for="proofImages">Upload Proof Images</label>
                    <input type="file" id="proofImages" name="proofImages" accept="image/*" multiple>
                    <small style="color: #666;">Upload photos of delivered goods or customer signature</small>
                </div>
                
                <div class="delivery-proof" id="proofPreview">
                    <!-- Image previews will be shown here -->
                </div>
                
                <div class="form-group">
                    <label for="customerSignature">Customer Signature (Optional)</label>
                    <div style="border: 2px dashed #ddd; height: 150px; border-radius: 8px; margin-top: 0.5rem; cursor: pointer;" 
                         onclick="startSignature()" id="signaturePad">
                        <div style="text-align: center; padding: 3rem; color: #666;">
                            Tap to add customer signature
                        </div>
                    </div>
                    <input type="hidden" id="customerSignature" name="signature">
                </div>
                
                <div class="form-group">
                    <label for="otpVerification">OTP Verification (if applicable)</label>
                    <input type="text" id="otpVerification" name="otp" 
                           placeholder="Enter OTP received from customer">
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-success">Submit Delivery Proof</button>
                    <button type="button" class="btn" onclick="closeModal('deliveryProofModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Navigation Modal -->
    <div id="navigationModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Navigation</h2>
                <button class="close-modal" onclick="closeModal('navigationModal')">&times;</button>
            </div>
            <div id="navigationContent">
                <!-- Navigation instructions will be loaded here -->
            </div>
            <div class="navigation-controls" style="margin-top: 1rem;">
                <div class="nav-btn" onclick="startGoogleMaps()">
                    <div>🗺️</div>
                    <div>Open Google Maps</div>
                </div>
                <div class="nav-btn" onclick="getAlternativeRoute()">
                    <div>🔄</div>
                    <div>Alternative Route</div>
                </div>
                <div class="nav-btn" onclick="shareETA()">
                    <div>📤</div>
                    <div>Share ETA</div>
                </div>
            </div>
        </div>
    </div>

    <!-- Issue Report Modal -->
    <div id="issueReportModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Report Delivery Issue</h2>
                <button class="close-modal" onclick="closeModal('issueReportModal')">&times;</button>
            </div>
            <form id="issueReportForm" action="driver" method="POST">
                <input type="hidden" name="action" value="reportIssue">
                <input type="hidden" id="issueOrderId" name="orderId">
                
                <div class="form-group">
                    <label for="issueType">Issue Type *</label>
                    <select id="issueType" name="issueType" required>
                        <option value="">Select Issue Type</option>
                        <option value="address">Wrong Address</option>
                        <option value="customer">Customer Not Available</option>
                        <option value="vehicle">Vehicle Problem</option>
                        <option value="traffic">Heavy Traffic</option>
                        <option value="damage">Product Damaged</option>
                        <option value="other">Other</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label for="issueDescription">Description *</label>
                    <textarea id="issueDescription" name="description" rows="4" required
                              placeholder="Describe the issue in detail..."></textarea>
                </div>
                
                <div class="form-group">
                    <label for="issueImages">Upload Photos (Optional)</label>
                    <input type="file" id="issueImages" name="images" accept="image/*" multiple>
                </div>
                
                <div class="form-group">
                    <label for="suggestedSolution">Suggested Solution</label>
                    <select id="suggestedSolution" name="solution">
                        <option value="">Select Solution</option>
                        <option value="reschedule">Reschedule Delivery</option>
                        <option value="return">Return to Mill</option>
                        <option value="contact">Contact Operator</option>
                        <option value="wait">Wait for Customer</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label for="estimatedDelay">Estimated Delay (minutes)</label>
                    <input type="number" id="estimatedDelay" name="delay" min="0" max="480" value="0">
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-danger">Report Issue</button>
                    <button type="button" class="btn" onclick="closeModal('issueReportModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>

    <!-- View Proof Modal -->
    <div id="viewProofModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Delivery Proof</h2>
                <button class="close-modal" onclick="closeModal('viewProofModal')">&times;</button>
            </div>
            <div id="proofContent">
                <!-- Proof content will be loaded here -->
            </div>
        </div>
    </div>

    <!-- Payout Request Modal -->
    <div id="payoutModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Request Payout</h2>
                <button class="close-modal" onclick="closeModal('payoutModal')">&times;</button>
            </div>
            <form id="payoutForm" action="driver" method="POST">
                <input type="hidden" name="action" value="requestPayout">
                
                <div class="form-group">
                    <label>Available Balance</label>
                    <div class="alert alert-info">
                        <strong id="availableBalance">0 Birr</strong> available for payout
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="payoutAmount">Payout Amount *</label>
                    <input type="number" id="payoutAmount" name="amount" 
                           min="100" max="100000" step="100" required>
                    <small style="color: #666;">Minimum payout: 100 Birr</small>
                </div>
                
                <div class="form-group">
                    <label for="payoutMethod">Payout Method *</label>
                    <select id="payoutMethod" name="method" required>
                        <option value="">Select Method</option>
                        <option value="CBE">CBE</option>
                        <option value="Telebirr">Telebirr</option>
                        <option value="Bank">Bank Transfer</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label for="payoutAccount">Account Number *</label>
                    <input type="text" id="payoutAccount" name="account" required>
                </div>
                
                <div class="form-group">
                    <label for="payoutNotes">Notes (Optional)</label>
                    <textarea id="payoutNotes" name="notes" rows="2" 
                              placeholder="Any additional notes..."></textarea>
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-success">Request Payout</button>
                    <button type="button" class="btn" onclick="closeModal('payoutModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Maintenance Log Modal -->
    <div id="maintenanceModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Log Maintenance</h2>
                <button class="close-modal" onclick="closeModal('maintenanceModal')">&times;</button>
            </div>
            <form id="maintenanceForm" action="driver" method="POST">
                <input type="hidden" name="action" value="logMaintenance">
                
                <div class="form-group">
                    <label for="maintenanceType">Maintenance Type *</label>
                    <select id="maintenanceType" name="type" required>
                        <option value="">Select Type</option>
                        <option value="oil">Oil Change</option>
                        <option value="tire">Tire Replacement</option>
                        <option value="brake">Brake Service</option>
                        <option value="engine">Engine Service</option>
                        <option value="general">General Maintenance</option>
                        <option value="other">Other</option>
                    </select>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="maintenanceDate">Date *</label>
                        <input type="date" id="maintenanceDate" name="date" required>
                    </div>
                    <div class="form-group">
                        <label for="maintenanceCost">Cost (Birr)</label>
                        <input type="number" id="maintenanceCost" name="cost" min="0" step="100">
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="maintenanceMileage">Mileage (km)</label>
                    <input type="number" id="maintenanceMileage" name="mileage" min="0">
                </div>
                
                <div class="form-group">
                    <label for="maintenanceDescription">Description</label>
                    <textarea id="maintenanceDescription" name="description" rows="3" 
                              placeholder="Describe the maintenance work done..."></textarea>
                </div>
                
                <div class="form-group">
                    <label for="maintenanceReceipt">Upload Receipt (Optional)</label>
                    <input type="file" id="maintenanceReceipt" name="receipt" accept="image/*">
                </div>
                
                <div class="form-group">
                    <label for="nextMaintenance">Next Maintenance Due (km)</label>
                    <input type="number" id="nextMaintenance" name="nextDue" min="0" value="10000">
                </div>
                
                <div class="form-group">
                    <button type="submit" class="btn btn-primary">Log Maintenance</button>
                    <button type="button" class="btn" onclick="closeModal('maintenanceModal')">Cancel</button>
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

        // Order Management Functions
        function searchOrders() {
            const searchTerm = document.getElementById('orderSearch').value.toLowerCase();
            const orderCards = document.querySelectorAll('.order-card');
            
            orderCards.forEach(card => {
                const text = card.textContent.toLowerCase();
                card.style.display = text.includes(searchTerm) ? 'block' : 'none';
            });
        }

        function filterByPriority() {
            const priority = document.getElementById('priorityFilter').value;
            const orderCards = document.querySelectorAll('.order-card');
            
            orderCards.forEach(card => {
                const cardPriority = card.getAttribute('data-priority');
                card.style.display = (priority === 'all' || cardPriority === priority) ? 'block' : 'none';
            });
        }

        function updateOrderStatus(orderId, newStatus) {
            let confirmMessage = '';
            switch(newStatus) {
                case 'picked':
                    confirmMessage = 'Mark order as picked up from mill?';
                    break;
                case 'on-the-way':
                    confirmMessage = 'Start delivery to customer?';
                    break;
                case 'delivered':
                    openDeliveryProofModal(orderId);
                    return;
            }
            
            if (confirm(confirmMessage)) {
                fetch('driver?action=updateOrderStatus&orderId=' + orderId + '&status=' + newStatus, {
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

        function openNavigation(orderId, address) {
            document.getElementById('navigationContent').innerHTML = `
                <h3>Navigate to Delivery</h3>
                <p><strong>Destination:</strong> ${address}</p>
                <div class="delivery-timeline">
                    <div class="timeline-step current">
                        <div class="timeline-content">
                            <strong>Step 1: Get Directions</strong>
                            <p>Open navigation app for turn-by-turn directions</p>
                        </div>
                    </div>
                    <div class="timeline-step">
                        <div class="timeline-content">
                            <strong>Step 2: Start Journey</strong>
                            <p>Begin your journey to the delivery location</p>
                        </div>
                    </div>
                    <div class="timeline-step">
                        <div class="timeline-content">
                            <strong>Step 3: Arrive at Location</strong>
                            <p>Park safely and prepare for delivery</p>
                        </div>
                    </div>
                </div>
            `;
            openModal('navigationModal');
        }

        function contactCustomer(phone, name) {
            if (confirm(`Contact customer ${name} at ${phone}?`)) {
                window.location.href = 'tel:' + phone;
            }
        }

        function getRouteGuidance(orderId) {
            fetch('driver?action=getRouteGuidance&orderId=' + orderId)
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        alert(`Route guidance:\nDistance: ${data.distance}\nEstimated Time: ${data.time}\nInstructions: ${data.instructions}`);
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error getting route guidance');
                });
        }

        function openDeliveryProofModal(orderId) {
            document.getElementById('proofOrderId').value = orderId;
            document.getElementById('deliveryTime').value = new Date().toISOString().slice(0, 16);
            openModal('deliveryProofModal');
        }

        function reportIssue(orderId) {
            document.getElementById('issueOrderId').value = orderId;
            openModal('issueReportModal');
        }

        function viewOrderDetails(orderId) {
            fetch('driver?action=getOrderDetails&orderId=' + orderId)
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

        function viewDeliveryProof(orderId) {
            fetch('driver?action=getDeliveryProof&orderId=' + orderId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('proofContent').innerHTML = html;
                    openModal('viewProofModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading delivery proof');
                });
        }

        function viewCustomerFeedback(orderId) {
            fetch('driver?action=getCustomerFeedback&orderId=' + orderId)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('orderDetailsContent').innerHTML = html;
                    openModal('orderDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading customer feedback');
                });
        }

        // Map Functions
        function initMap() {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(function(position) {
                    const lat = position.coords.latitude;
                    const lng = position.coords.longitude;
                    
                    // In a real implementation, this would load Google Maps
                    document.querySelector('.map-placeholder').innerHTML = `
                        <div>✅</div>
                        <h3>Location Detected</h3>
                        <p>Latitude: ${lat.toFixed(6)}</p>
                        <p>Longitude: ${lng.toFixed(6)}</p>
                        <button class="btn btn-primary" onclick="loadTodayStops()">Load Today's Stops</button>
                    `;
                    
                    updateRouteStats();
                }, function(error) {
                    alert('Error getting location: ' + error.message);
                });
            } else {
                alert('Geolocation is not supported by this browser.');
            }
        }

        function loadTodayStops() {
            fetch('driver?action=getTodayStops')
                .then(response => response.json())
                .then(stops => {
                    if (stops && stops.length > 0) {
                        document.getElementById('totalStops').textContent = stops.length;
                        document.getElementById('completedStops').textContent = stops.filter(s => s.status === 'delivered').length;
                        
                        // Calculate total distance (simplified)
                        const totalDistance = stops.length * 5; // Assume 5km average per stop
                        document.getElementById('totalDistance').textContent = totalDistance + ' km';
                        
                        // Calculate estimated time (simplified)
                        const estimatedTime = stops.length * 30; // Assume 30 minutes per stop
                        document.getElementById('estimatedTime').textContent = estimatedTime + ' min';
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                });
        }

        function startNavigation() {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(function(position) {
                    const lat = position.coords.latitude;
                    const lng = position.coords.longitude;
                    
                    // Open Google Maps with current location
                    window.open(`https://www.google.com/maps/dir/${lat},${lng}/`, '_blank');
                });
            } else {
                window.open('https://www.google.com/maps', '_blank');
            }
        }

        function optimizeRoute() {
            fetch('driver?action=optimizeRoute')
                .then(response => response.json())
                .then(route => {
                    if (route) {
                        alert(`Optimized route calculated!\nTotal distance saved: ${route.distanceSaved} km\nEstimated time saved: ${route.timeSaved} minutes`);
                        updateRouteStats();
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error optimizing route');
                });
        }

        function getTrafficInfo() {
            alert('Traffic information would be displayed here.\nIn a real implementation, this would connect to traffic API.');
        }

        function shareLocation() {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(function(position) {
                    const lat = position.coords.latitude;
                    const lng = position.coords.longitude;
                    const message = `My current location: https://maps.google.com/?q=${lat},${lng}`;
                    
                    if (navigator.share) {
                        navigator.share({
                            title: 'My Location',
                            text: message,
                            url: `https://maps.google.com/?q=${lat},${lng}`
                        });
                    } else {
                        prompt('Copy this location link:', message);
                    }
                });
            } else {
                alert('Location sharing not supported');
            }
        }

        function navigateToStop(address) {
            const encodedAddress = encodeURIComponent(address);
            window.open(`https://www.google.com/maps/dir/?api=1&destination=${encodedAddress}`, '_blank');
        }

        function updateRouteStats() {
            // This would be populated with real data from API
        }

        // Earnings Functions
        function requestPayout() {
            fetch('driver?action=getAvailableBalance')
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        document.getElementById('availableBalance').textContent = data.balance + ' Birr';
                        document.getElementById('payoutAmount').max = data.balance;
                        document.getElementById('payoutAmount').value = data.balance;
                        openModal('payoutModal');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading balance');
                });
        }

        function updatePaymentInfo() {
            alert('Redirect to payment settings');
            window.location.href = '?tab=settings';
        }

        function downloadStatement() {
            fetch('driver?action=downloadStatement')
                .then(response => response.blob())
                .then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'earnings-statement-' + new Date().toISOString().split('T')[0] + '.pdf';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error downloading statement');
                });
        }

        function viewEarningDetails(orderNumber) {
            fetch('driver?action=getEarningDetails&orderNumber=' + orderNumber)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('orderDetailsContent').innerHTML = html;
                    openModal('orderDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading earning details');
                });
        }

        // Performance Functions
        function viewDetailedMetrics() {
            fetch('driver?action=getDetailedMetrics')
                .then(response => response.text())
                .then(html => {
                    document.getElementById('orderDetailsContent').innerHTML = html;
                    openModal('orderDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading metrics');
                });
        }

        function comparePerformance() {
            alert('Performance comparison would be displayed here');
        }

        function downloadPerformanceReport() {
            fetch('driver?action=downloadPerformanceReport')
                .then(response => response.blob())
                .then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'performance-report-' + new Date().toISOString().split('T')[0] + '.pdf';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error downloading report');
                });
        }

        // Vehicle Functions
        function logMaintenance() {
            document.getElementById('maintenanceDate').value = new Date().toISOString().split('T')[0];
            openModal('maintenanceModal');
        }

        function requestService() {
            alert('Service request would be sent to admin');
        }

        function viewMaintenanceHistory() {
            fetch('driver?action=getMaintenanceHistory')
                .then(response => response.text())
                .then(html => {
                    document.getElementById('orderDetailsContent').innerHTML = html;
                    openModal('orderDetailsModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading maintenance history');
                });
        }

        function logFuelRefill() {
            alert('Fuel refill logging form would open');
        }

        function viewFuelHistory() {
            alert('Fuel history would be displayed');
        }

        function viewDocument(type) {
            fetch('driver?action=getDocument&type=' + type)
                .then(response => response.text())
                .then(html => {
                    document.getElementById('proofContent').innerHTML = html;
                    openModal('viewProofModal');
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error loading document');
                });
        }

        function uploadDocument() {
            alert('Document upload form would open');
        }

        function renewDocument() {
            alert('Document renewal request would be sent to admin');
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

        function toggleOnlineStatus() {
            const statusElement = document.getElementById('statusIndicator');
            const statusText = document.getElementById('statusText');
            const statusDisplay = document.getElementById('currentStatusDisplay');
            const button = document.querySelector('#settingsTab .btn-warning');
            
            if (statusElement.classList.contains('offline')) {
                // Go online
                statusElement.classList.remove('offline');
                statusElement.style.backgroundColor = 'var(--success-color)';
                statusText.textContent = 'Online';
                statusDisplay.innerHTML = 'Status: <span class="order-status status-completed">Online</span>';
                button.textContent = 'Go Offline';
                
                fetch('driver?action=setOnlineStatus&status=online', { method: 'POST' });
            } else {
                // Go offline
                statusElement.classList.add('offline');
                statusElement.style.backgroundColor = 'var(--accent-color)';
                statusText.textContent = 'Offline';
                statusDisplay.innerHTML = 'Status: <span class="order-status status-pending">Offline</span>';
                button.textContent = 'Go Online';
                
                fetch('driver?action=setOnlineStatus&status=offline', { method: 'POST' });
            }
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
            
            // Vehicle form validation
            const vehicleForm = document.getElementById('vehicleForm');
            if (vehicleForm) {
                vehicleForm.addEventListener('submit', function(e) {
                    const carNumber = document.getElementById('carNumber').value;
                    const licenseNumber = document.getElementById('licenseNumber').value;
                    
                    if (!carNumber.match(/^[A-Z0-9]+$/)) {
                        e.preventDefault();
                        alert('Car number must contain only letters and numbers');
                        return;
                    }
                    
                    if (licenseNumber.length < 5) {
                        e.preventDefault();
                        alert('License number must be at least 5 characters');
                        return;
                    }
                });
            }
            
            // Load today's stops count for map
            if (window.location.href.includes('tab=map')) {
                loadTodayStops();
            }
            
            // Set current time for delivery proof
            const deliveryTime = document.getElementById('deliveryTime');
            if (deliveryTime) {
                deliveryTime.value = new Date().toISOString().slice(0, 16);
            }
            
            // Initialize online status
            updateOnlineStatus();
        });

        function updateOnlineStatus() {
            fetch('driver?action=getOnlineStatus')
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        const statusElement = document.getElementById('statusIndicator');
                        const statusText = document.getElementById('statusText');
                        const statusDisplay = document.getElementById('currentStatusDisplay');
                        const button = document.querySelector('#settingsTab .btn-warning');
                        
                        if (data.status === 'online') {
                            statusElement.classList.remove('offline');
                            statusElement.style.backgroundColor = 'var(--success-color)';
                            statusText.textContent = 'Online';
                            if (statusDisplay) {
                                statusDisplay.innerHTML = 'Status: <span class="order-status status-completed">Online</span>';
                            }
                            if (button) button.textContent = 'Go Offline';
                        } else {
                            statusElement.classList.add('offline');
                            statusElement.style.backgroundColor = 'var(--accent-color)';
                            statusText.textContent = 'Offline';
                            if (statusDisplay) {
                                statusDisplay.innerHTML = 'Status: <span class="order-status status-pending">Offline</span>';
                            }
                            if (button) button.textContent = 'Go Online';
                        }
                    }
                })
                .catch(error => console.error('Error loading status:', error));
        }

        // Auto-refresh orders
        setInterval(() => {
            if (window.location.href.includes('tab=orders')) {
                fetch('driver?action=checkNewOrders')
                    .then(response => response.json())
                    .then(data => {
                        if (data.newOrders > 0) {
                            const badge = document.querySelector('.sidebar-menu a[href*="tab=orders"] .badge');
                            if (badge) {
                                const currentCount = parseInt(badge.textContent) || 0;
                                badge.textContent = currentCount + data.newOrders;
                                
                                // Show notification
                                if (Notification.permission === 'granted') {
                                    new Notification('New Delivery Assignment', {
                                        body: `You have ${data.newOrders} new delivery order(s)`,
                                        icon: '/favicon.ico'
                                    });
                                }
                            }
                        }
                    });
            }
            
            // Update online status
            updateOnlineStatus();
        }, 30000); // Check every 30 seconds

        // Request notification permission
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }

        // Close modal when clicking outside
        window.onclick = function(event) {
            if (event.target.classList.contains('modal')) {
                event.target.style.display = 'none';
            }
        }

        // Helper function for priority determination
        function determinePriority(orderDate, address) {
            const now = new Date();
            const orderTime = new Date(orderDate);
            const hoursDiff = (now - orderTime) / (1000 * 60 * 60);
            
            if (hoursDiff > 4) return 'high';
            if (address.toLowerCase().includes('hospital') || address.toLowerCase().includes('emergency')) return 'high';
            if (hoursDiff > 2) return 'medium';
            return 'low';
        }
     // Update delivery status
        function updateStatus(orderId, status) {
            if (confirm('Update order status to ' + status + '?')) {
                fetch('driver?action=updateDeliveryStatus&orderId=' + orderId + '&status=' + status, {
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
        }
        // Start signature capture (simplified)
        function startSignature() {
            const signature = prompt('Enter customer name for signature verification:');
            if (signature) {
                document.getElementById('customerSignature').value = signature;
                document.getElementById('signaturePad').innerHTML = `
                    <div style="text-align: center; padding: 3rem; color: var(--success-color);">
                        ✓ Signature recorded: ${signature}
                    </div>
                `;
            }
        }

        // Preview proof images
        document.getElementById('proofImages')?.addEventListener('change', function(e) {
            const preview = document.getElementById('proofPreview');
            preview.innerHTML = '';
            
            Array.from(e.target.files).forEach(file => {
                const reader = new FileReader();
                reader.onload = function(e) {
                    const img = document.createElement('img');
                    img.src = e.target.result;
                    img.className = 'proof-image';
                    preview.appendChild(img);
                };
                reader.readAsDataURL(file);
            });
        });
    </script>
</body>
</html>

<%!
    // Helper method to determine priority
    private String determinePriority(java.sql.Timestamp orderDate, String address) {
        long now = System.currentTimeMillis();
        long orderTime = orderDate.getTime();
        long hoursDiff = (now - orderTime) / (1000 * 60 * 60);
        
        if (hoursDiff > 4) return "high";
        if (address != null && (address.toLowerCase().contains("hospital") || address.toLowerCase().contains("emergency"))) return "high";
        if (hoursDiff > 2) return "medium";
        return "low";
    }
%>