package com.mill.system;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        
        System.out.println("AuthFilter: Path = " + path);
        
        // Allow public resources
        if (path.startsWith("/index.html") || 
            path.startsWith("/style.css") ||
            path.startsWith("/auth") ||
            path.startsWith("/logout.jsp") ||
            path.equals("/") ||
            path.startsWith("/WEB-INF") ||
            path.contains(".css") ||
            path.contains(".js") ||
            path.contains(".jpg") ||
            path.contains(".png") ||
            path.startsWith("/uploads/") ||
            path.contains(".ico")) {
            System.out.println("AuthFilter: Allowing public resource");
            chain.doFilter(request, response);
            return;
        }
        
        // Check for secure pages or API endpoints
        if (path.endsWith(".jsp") || 
            path.contains("/admin") || 
            path.contains("/customer") || 
            path.contains("/operator") || 
            path.contains("/driver") ||
            path.contains(".jsp")) {
            
            HttpSession session = httpRequest.getSession(false);
            System.out.println("AuthFilter: Session = " + session);
            
            if (session == null) {
                System.out.println("AuthFilter: No session found, redirecting to login");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html?error=No session");
                return;
            }
            
            Integer userId = (Integer) session.getAttribute("userId");
            String userType = (String) session.getAttribute("userType");
            
            System.out.println("AuthFilter: userId = " + userId + ", userType = " + userType);
            
            if (userId == null) {
                System.out.println("AuthFilter: No userId in session");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html?error=Session expired");
                return;
            }
            
            // Validate user type for specific pages
            if (path.contains("/customer.jsp") && !"customer".equals(userType)) {
                System.out.println("AuthFilter: Access denied - wrong user type for customer.jsp");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html?error=Access denied");
                return;
            }
            
            if (path.contains("/admin.jsp") && !"admin".equals(userType)) {
                System.out.println("AuthFilter: Access denied - wrong user type for admin.jsp");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html?error=Access denied");
                return;
            }
            
            if (path.contains("/driver.jsp") && !"driver".equals(userType)) {
                System.out.println("AuthFilter: Access denied - wrong user type for driver.jsp");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html?error=Access denied");
                return;
            }
            
            if (path.contains("/operator.jsp") && !"operator".equals(userType)) {
                System.out.println("AuthFilter: Access denied - wrong user type for operator.jsp");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html?error=Access denied");
                return;
            }
            
            System.out.println("AuthFilter: Access granted to " + path);
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("AuthFilter initialized");
    }
    
    @Override
    public void destroy() {
        System.out.println("AuthFilter destroyed");
    }
}