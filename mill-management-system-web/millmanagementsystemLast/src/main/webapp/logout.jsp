<%@ page import="com.mill.system.*" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Get current session
    HttpSession currentSession = request.getSession(false);
    
    // Log logout activity
    if (currentSession != null) {
        User user = (User) currentSession.getAttribute("user");
        String userType = (String) currentSession.getAttribute("userType");
        
        if (user != null) {
            try {
                // Log the logout in database (optional)
                java.sql.Connection conn = DatabaseConnection.getConnection();
                String sql = "INSERT INTO user_logs (user_id, action, ip_address, user_agent) VALUES (?, 'logout', ?, ?)";
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, user.getId());
                stmt.setString(2, request.getRemoteAddr());
                stmt.setString(3, request.getHeader("User-Agent"));
                stmt.executeUpdate();
                stmt.close();
                conn.close();
            } catch (Exception e) {
                // Log to server log if database logging fails
                e.printStackTrace();
            }
        }
        
        // Invalidate session
        currentSession.invalidate();
    }
    
    // Clear any cookies if needed
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("rememberMe") || 
                cookie.getName().equals("sessionToken") ||
                cookie.getName().equals("userPrefs")) {
                cookie.setValue("");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }
    }
    
    // Redirect to login page with logout message
    response.sendRedirect("index.html?message=You have been successfully logged out");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Logging Out - Mill Management System</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Arial', sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .logout-container {
            background: white;
            padding: 3rem;
            border-radius: 15px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            text-align: center;
            max-width: 500px;
            width: 90%;
            animation: fadeIn 0.5s ease;
        }
        
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(-20px); }
            to { opacity: 1; transform: translateY(0); }
        }
        
        .logo {
            font-size: 2.5rem;
            color: #667eea;
            margin-bottom: 1.5rem;
        }
        
        h1 {
            color: #333;
            margin-bottom: 1rem;
            font-size: 2rem;
        }
        
        .message {
            color: #666;
            margin-bottom: 2rem;
            line-height: 1.6;
        }
        
        .spinner {
            width: 50px;
            height: 50px;
            border: 5px solid #f3f3f3;
            border-top: 5px solid #667eea;
            border-radius: 50%;
            margin: 0 auto 2rem;
            animation: spin 1s linear infinite;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        
        .redirect-message {
            color: #999;
            font-size: 0.9rem;
            margin-top: 1.5rem;
        }
        
        .login-link {
            display: inline-block;
            margin-top: 1.5rem;
            padding: 0.8rem 2rem;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            transition: all 0.3s ease;
        }
        
        .login-link:hover {
            background: #5a67d8;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        
        .security-info {
            background: #f8f9fa;
            padding: 1rem;
            border-radius: 8px;
            margin-top: 2rem;
            font-size: 0.85rem;
            color: #666;
            text-align: left;
        }
        
        .security-info h3 {
            color: #333;
            margin-bottom: 0.5rem;
            font-size: 1rem;
        }
        
        .security-info ul {
            padding-left: 1.2rem;
        }
        
        .security-info li {
            margin-bottom: 0.3rem;
        }
        
        @media (max-width: 480px) {
            .logout-container {
                padding: 2rem 1.5rem;
            }
            
            h1 {
                font-size: 1.5rem;
            }
        }
    </style>
    <script>
        // Redirect after 3 seconds if JavaScript is enabled
        setTimeout(function() {
            window.location.href = 'index.html?message=You have been successfully logged out';
        }, 3000);
        
        // Clear any local storage or session storage
        window.onload = function() {
            // Clear any sensitive data from localStorage
            localStorage.removeItem('userData');
            localStorage.removeItem('authToken');
            localStorage.removeItem('cartData');
            
            // Clear sessionStorage
            sessionStorage.clear();
            
            // Clear any service worker caches if used
            if ('caches' in window) {
                caches.keys().then(function(cacheNames) {
                    cacheNames.forEach(function(cacheName) {
                        caches.delete(cacheName);
                    });
                });
            }
        };
    </script>
</head>
<body>
    <div class="logout-container">
        <div class="logo">🔒</div>
        <h1>Logging Out</h1>
        
        <div class="spinner"></div>
        
        <div class="message">
            You are being securely logged out from the Mill Management System.<br>
            Please wait while we clear your session...
        </div>
        
        <div class="redirect-message">
            You will be redirected to the login page in a few seconds.
        </div>
        
        <a href="index.html" class="login-link">Go to Login Page</a>
        
        <div class="security-info">
            <h3>Security Information:</h3>
            <ul>
                <li>Your session has been terminated</li>
                <li>All temporary data has been cleared</li>
                <li>You can safely close this browser tab</li>
                <li>For security, clear your browser cache if using a public computer</li>
            </ul>
        </div>
    </div>
</body>
</html>