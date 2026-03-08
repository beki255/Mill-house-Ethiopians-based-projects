package fxml;
import java.sql.*;
import java.util.*;



public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/mill_house_management";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "password1234"; // Change to your MySQL password
    
    private static Connection connection;
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }
    public static Connection getConnection() {
        try {
            // Always create a fresh connection - don't reuse closed connections
            if (connection != null && !connection.isClosed()) {
                // Test if connection is still valid
                if (connection.isValid(2)) {
                    System.out.println("DEBUG: Returning existing valid connection");
                    return connection;
                } else {
                    System.out.println("DEBUG: Connection invalid, creating new one");
                    connection.close();
                    connection = null;
                }
            }
            
            // Create new connection
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("DEBUG: New database connection created");
            
            // Set connection properties to prevent premature closing
            connection.setAutoCommit(true);
            
            return connection;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
            connection = null; // Reset connection
            return null;
        }
    }
    public static void updateOrderStatusFlow() {
        String[] updates = {
            // Ensure proper status flow
            "UPDATE orders SET status = 'Ready for Delivery' WHERE status = 'Processing Complete'",
            "UPDATE orders SET status = 'Processing' WHERE status = 'Processing' AND assigned_to IS NOT NULL",
            "UPDATE orders SET status = 'Assigned to Driver' WHERE assigned_driver IS NOT NULL AND status IN ('Ready for Delivery', 'Processing Complete')",
            "UPDATE orders SET status = 'Pending' WHERE status IS NULL OR status = '' OR status NOT IN ('Pending', 'Assigned to Mill', 'Processing', 'Ready for Delivery', 'Assigned to Driver', 'On Route', 'Delivered', 'Cancelled')",
            
            // Set default values for new columns
            "UPDATE orders SET assigned_driver = NULL WHERE assigned_driver = ''",
            "UPDATE orders SET assigned_to = NULL WHERE assigned_to = ''"
        };
        
        try (Connection conn = getConnection()) {
            System.out.println("🔄 Updating order status flow...");
            
            for (String sql : updates) {
                try (Statement stmt = conn.createStatement()) {
                    int updated = stmt.executeUpdate(sql);
                    System.out.println("✅ Updated " + updated + " orders with: " + 
                                     sql.substring(0, Math.min(sql.length(), 60)) + "...");
                } catch (SQLException e) {
                    System.err.println("⚠️ Update skipped: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Order status flow updated successfully!");
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to update order status flow: " + e.getMessage());
        }
    }
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
    public static Connection getFreshConnection() throws SQLException {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignore
            }
            connection = null;
        }
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        System.out.println("DEBUG: Fresh database connection created");
        connection.setAutoCommit(true);
        return connection;
    }
    
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("✅ Database connection successful!");
                
                // Test minimal tables exist
                String[] essentialTables = {"users", "customers", "inventory", "settings", "staff_members"};
                DatabaseMetaData meta = conn.getMetaData();
                
                for (String table : essentialTables) {
                    ResultSet tables = meta.getTables(null, null, table, null);
                    if (tables.next()) {
                        System.out.println("✅ " + table + " table exists.");
                    } else {
                        System.out.println("❌ " + table + " table missing!");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
        }
    }
    
    // ADD THIS METHOD TO UPDATE DATABASE SCHEMA
    public static void updateDatabaseSchema() {
        String[] schemaUpdates = {
        		// In DatabaseConnection.java - updateDatabaseSchema() method
        		// Add these lines to the schemaUpdates array:
        		// Add tax rate setting
        		// Add to updateDatabaseSchema() method:
        		// Electric meter readings table
        		// Ensure price settings exist in key-value format
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('wheat_price', '45.00')",
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('teff_price', '85.00')",
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('corn_price', '40.00')",
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('barley_price', '42.00')",
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('low_stock_threshold', '200')",
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('tax_rate', '15.0')",
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('delivery_fee_per_kg', '5.00')",
        		"CREATE TABLE IF NOT EXISTS electric_meter_readings (" +
        		"  id INT PRIMARY KEY AUTO_INCREMENT," +
        		"  reading_date DATE NOT NULL UNIQUE," +
        		"  meter_reading DECIMAL(10,2) NOT NULL," +
        		"  recorded_by VARCHAR(100) NOT NULL," +
        		"  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        		"  INDEX idx_date (reading_date)" +
        		")",

        		// Electricity cost settings
        		"CREATE TABLE IF NOT EXISTS electricity_settings (" +
        		"  id INT PRIMARY KEY AUTO_INCREMENT," +
        		"  cost_per_unit DECIMAL(10,2) DEFAULT 5.00," + // ETB per unit
        		"  updated_by VARCHAR(100)," +
        		"  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
        		")",

        		// Insert default electricity cost
        		"INSERT IGNORE INTO electricity_settings (cost_per_unit) VALUES (5.00)",
        		"CREATE TABLE IF NOT EXISTS settings (setting_key VARCHAR(50) PRIMARY KEY, value VARCHAR(255))",
        		"INSERT IGNORE INTO settings (setting_key, value) VALUES ('tax_rate', '15.0')",

        		"ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS processed_by VARCHAR(100)",
        		"ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS payment_date TIMESTAMP NULL",
        		"ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP",

        		// Add unique constraint to prevent duplicate salary processing
        		"ALTER TABLE salary_payments DROP INDEX IF EXISTS unique_staff_month",
        		"ALTER TABLE salary_payments ADD UNIQUE KEY IF NOT EXISTS unique_staff_month (staff_id, DATE_FORMAT(payment_month, '%Y-%m'))",

        		// Add last salary fields to staff_members table
        		"ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS last_salary_date DATE NULL",
        		"ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS last_salary_status VARCHAR(20) DEFAULT 'UNPAID'",
        		// Add this to the schemaUpdates array in DatabaseConnection.java
        		"ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS processed_by VARCHAR(100) AFTER status",
        		"ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP",

        		// Ensure unique constraint to prevent duplicate salary processing
        		"ALTER TABLE salary_payments ADD UNIQUE KEY IF NOT EXISTS unique_staff_month (staff_id, DATE_FORMAT(payment_month, '%Y-%m'))",

        		// Add last salary fields to staff_members table for quick reference
        		"ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS last_salary_date DATE NULL",
        		"ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS last_salary_status VARCHAR(20) DEFAULT 'UNPAID'",
        		 "ALTER TABLE orders MODIFY COLUMN status VARCHAR(50)",
        		// Add to the schemaUpdates array in DatabaseConnection.java (around line 90):
        		 "ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_completed_date TIMESTAMP NULL",
        	        
        	        // Ensure all status values are consistent
        	        "UPDATE orders SET status = 'Delivered' WHERE status = 'delivered' OR status = 'Deliverd'",
        	        "UPDATE orders SET status = 'Cancelled' WHERE status = 'canceled' OR status = 'Canceled'",
        	        "UPDATE orders SET status = 'Pending' WHERE status = 'pending' OR status = '' OR status IS NULL",
        	     // Update 1: Add payment columns to orders table (simplified)
        	        "ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT 'Cash on Delivery'",
        	        "ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_amount DECIMAL(10,2) DEFAULT 0.00",
         // Add staff_id column to drivers table
        	        "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS staff_id INT AFTER id",
            
        	        "CREATE TABLE IF NOT EXISTS admin_notifications (" +
        	        		"  id INT PRIMARY KEY AUTO_INCREMENT, " +
        	        		"  title VARCHAR(200) NOT NULL, " +
        	        		"  message TEXT NOT NULL, " +
        	        		"  sender VARCHAR(100), " +
        	        		"  priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM', " +
        	        		"  status ENUM('UNREAD', 'READ', 'RESOLVED') DEFAULT 'UNREAD', " +
        	        		"  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        	        		"  read_date TIMESTAMP NULL, " +
        	        		"  resolved_date TIMESTAMP NULL" +
        	        		")",
        	        		// Fix: SIMPLIFIED delivery_logs table creation
        	        		"CREATE TABLE IF NOT EXISTS delivery_logs (" +
        	        		"  id INT PRIMARY KEY AUTO_INCREMENT," +
        	        		"  order_id INT NOT NULL," +
        	        		"  driver_name VARCHAR(100) NOT NULL," +
        	        		"  assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        	        		"  delivered_date TIMESTAMP NULL," +
        	        		"  status VARCHAR(50) DEFAULT 'Assigned'" +
        	        		")",
        	        		// Create driver_orders view
        	        		"CREATE OR REPLACE VIEW driver_orders AS " +
        	        		"SELECT o.*, d.name as assigned_driver_name, d.id as driver_id " +
        	        		"FROM orders o " +
        	        		"LEFT JOIN drivers d ON o.assigned_driver = d.name " +
        	        		"WHERE o.assigned_driver IS NOT NULL",

        	        		// Create customers table if it doesn't exist
        	        		"CREATE TABLE IF NOT EXISTS customers (" +
        	        		"  id INT PRIMARY KEY AUTO_INCREMENT," +
        	        		"  name VARCHAR(100) NOT NULL," +
        	        		"  phone VARCHAR(20) UNIQUE NOT NULL," +
        	        		"  delivery_address TEXT," +
        	        		"  credit_balance_ETB DECIMAL(10,2) DEFAULT 0.00," +
        	        		"  password VARCHAR(100)," +
        	        		"  registration_type ENUM('PORTAL', 'OPERATOR') DEFAULT 'PORTAL'," +
        	        		"  registered_by VARCHAR(100)," +
        	        		"  registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        	        		")",
            "ALTER TABLE transactions DROP FOREIGN KEY transactions_ibfk_2",
            "ALTER TABLE transactions ADD CONSTRAINT transactions_ibfk_2 FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE SET NULL",
            
            "ALTER TABLE orders DROP FOREIGN KEY orders_ibfk_1",
            "ALTER TABLE orders ADD CONSTRAINT orders_ibfk_1 FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE",
            
            "ALTER TABLE salary_payments DROP FOREIGN KEY salary_payments_ibfk_1",
            "ALTER TABLE salary_payments ADD CONSTRAINT salary_payments_ibfk_1 FOREIGN KEY (staff_id) REFERENCES staff_members(id) ON DELETE CASCADE",
            
            // Update 3: Add delivery fee to settings
            "ALTER TABLE settings " +
            "ADD COLUMN delivery_fee_per_kg DECIMAL(10,2) DEFAULT 5.00",
            "ALTER TABLE users ADD COLUMN password_changed BOOLEAN DEFAULT FALSE",
            
         // Ensure inventory and settings tables exist
            "CREATE TABLE IF NOT EXISTS inventory (" +
            "  id INT PRIMARY KEY AUTO_INCREMENT, " +
            "  grain_type VARCHAR(100) UNIQUE NOT NULL, " +
            "  current_stock_kg DECIMAL(10,2) DEFAULT 0.00, " +
            "  last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")",
            
        
            "ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(100) AFTER customer_id",
           
            "CREATE TABLE IF NOT EXISTS salary_payments (" +
            "  id INT PRIMARY KEY AUTO_INCREMENT," +
            "  staff_id INT NOT NULL," +
            "  staff_name VARCHAR(100) NOT NULL," +
            "  payment_month DATE NOT NULL," +
            "  base_salary DECIMAL(10,2) NOT NULL," +
            "  bonus DECIMAL(10,2) DEFAULT 0.00," +
            "  deductions DECIMAL(10,2) DEFAULT 0.00," +
            "  net_salary DECIMAL(10,2) NOT NULL," +
            "  status ENUM('PAID', 'UNPAID', 'PENDING') DEFAULT 'UNPAID'," +
            "  payment_method VARCHAR(50)," +
            "  payment_date TIMESTAMP NULL," +
            "  processed_by VARCHAR(100)," +
            "  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  UNIQUE KEY unique_staff_month (staff_id, payment_month)" +
            ")",

         
            "ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) AFTER status",
            // Fix foreign key constraints - use IF EXISTS
            "ALTER TABLE transactions DROP FOREIGN KEY IF EXISTS transactions_ibfk_2",
            "ALTER TABLE transactions MODIFY COLUMN operator_id INT NULL",
            "ALTER TABLE transactions ADD CONSTRAINT transactions_ibfk_2 FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE SET NULL",
            
            "ALTER TABLE orders DROP FOREIGN KEY IF EXISTS orders_ibfk_1",
            "ALTER TABLE orders ADD CONSTRAINT orders_ibfk_1 FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE",
            
            "ALTER TABLE salary_payments DROP FOREIGN KEY IF EXISTS salary_payments_ibfk_1",
            "ALTER TABLE salary_payments ADD CONSTRAINT salary_payments_ibfk_1 FOREIGN KEY (staff_id) REFERENCES staff_members(id) ON DELETE CASCADE",
         // Add status tracking columns
            "ALTER TABLE orders ADD COLUMN IF NOT EXISTS status_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
            "ALTER TABLE orders ADD COLUMN IF NOT EXISTS driver_notes TEXT",

            // Create customer notifications table
            "CREATE TABLE IF NOT EXISTS customer_notifications (" +
            "  id INT PRIMARY KEY AUTO_INCREMENT," +
            "  customer_id INT NOT NULL," +
            "  order_id INT," +
            "  message TEXT NOT NULL," +
            "  status ENUM('UNREAD', 'READ') DEFAULT 'UNREAD'," +
            "  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE" +
            ")",

            // Add driver phone to drivers table
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS phone VARCHAR(20) AFTER name",
            // Update orders table to populate customer_name from customers table
            "UPDATE orders o JOIN customers c ON o.customer_id = c.id SET o.customer_name = c.name WHERE o.customer_name IS NULL",
            
            // Add trigger to keep customer_name updated
            "DROP TRIGGER IF EXISTS update_order_customer_name",
            "CREATE TRIGGER update_order_customer_name AFTER UPDATE ON customers " +
            "FOR EACH ROW " +
            "BEGIN " +
            "  UPDATE orders SET customer_name = NEW.name WHERE customer_id = NEW.id; " +
            "END",
            // Check if customer_name column exists, if not add it
            "SET @dbname = DATABASE();",
            "SET @tablename = 'orders';",
            "SET @columnname = 'customer_name';",
            "SET @preparedStatement = (SELECT IF(",
            "  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS",
            "    WHERE table_schema = @dbname",
            "    AND table_name = @tablename",
            "    AND column_name = @columnname) > 0,",
            "  \"SELECT 'Column already exists'\",",
            "  \"ALTER TABLE orders ADD COLUMN customer_name VARCHAR(100) AFTER customer_id\"",
            "));",
            "PREPARE alterIfNotExists FROM @preparedStatement;",
            "EXECUTE alterIfNotExists;",
            "DEALLOCATE PREPARE alterIfNotExists;",
            
            // Update existing orders with customer names (if column was just added)
            "UPDATE orders o JOIN customers c ON o.customer_id = c.id SET o.customer_name = c.name WHERE o.customer_name IS NULL OR o.customer_name = '';",
            
            // Create trigger for future updates
            "DROP TRIGGER IF EXISTS update_order_customer_name;",
            "CREATE TRIGGER update_order_customer_name AFTER UPDATE ON customers " +
            "FOR EACH ROW " +
            "BEGIN " +
            "  UPDATE orders SET customer_name = NEW.name WHERE customer_id = NEW.id; " +
            "END;"


        };
        
        try (Connection conn = getConnection()) {
            System.out.println("🔄 Updating database schema...");
            
            for (String sql : schemaUpdates) {
                try (Statement stmt = conn.createStatement()) {
                    System.out.println("Executing: " + sql.substring(0, Math.min(sql.length(), 50)) + "...");
                    stmt.executeUpdate(sql);
                    System.out.println("✅ Success");
                } catch (SQLException e) {
                    // Ignore if column/table already exists
                    if (!e.getMessage().contains("Duplicate column") && 
                        !e.getMessage().contains("already exists")) {
                        System.err.println("❌ Schema update failed: " + e.getMessage());
                    } else {
                        System.out.println("✅ Already exists");
                    }
                }
            }
            
            System.out.println("✅ Database schema updated successfully!");
            
            // Add some sample data if tables are empty
            initializeSampleData();
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to update database schema: " + e.getMessage());
        }
    }
    public static boolean isValidConnection(Connection conn) {
        if (conn == null) {
            return false;
        }
        
        try {
            return !conn.isClosed() && conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public static Connection getValidConnection() throws SQLException {
        Connection conn = getConnection();
        
        // Test the connection
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
        }
        
        return conn;
    }
 // Add this method to DatabaseConnection class
    public static void ensureDeliveryLogsTable() {
        try (Connection conn = getConnection()) {
            String createTableSql = 
                "CREATE TABLE IF NOT EXISTS delivery_logs (" +
                "  id INT PRIMARY KEY AUTO_INCREMENT," +
                "  order_id INT NOT NULL," +
                "  driver_name VARCHAR(100) NOT NULL," +
                "  assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  delivered_date TIMESTAMP NULL," +  // Ensure this column exists
                "  status VARCHAR(50) DEFAULT 'Assigned'" +
                ")";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createTableSql);
                System.out.println("✅ Ensured delivery_logs table exists with delivered_date column");
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring delivery_logs table: " + e.getMessage());
        }
    }
    public static void addColumnIfNotExists(String tableName, String columnDefinition) {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition;
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("✅ Added column to " + tableName);
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate column")) {
                    System.out.println("✅ Column already exists in " + tableName);
                } else {
                    throw e;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to add column to " + tableName + ": " + e.getMessage());
        }
    }
    public static void repairDeliveryLogsTable() {
        try (Connection conn = getConnection()) {
            System.out.println("🛠️ Repairing delivery_logs table structure...");
            
            // Check if table exists
            String checkTableSql = "SHOW TABLES LIKE 'delivery_logs'";
            try (Statement stmt = conn.createStatement(); 
                 ResultSet rs = stmt.executeQuery(checkTableSql)) {
                
                if (!rs.next()) {
                    // Table doesn't exist, create it SIMPLIFIED
                    System.out.println("Creating delivery_logs table...");
                    String createTableSql = 
                        "CREATE TABLE delivery_logs (" +
                        "  id INT PRIMARY KEY AUTO_INCREMENT," +
                        "  order_id INT NOT NULL," +
                        "  driver_name VARCHAR(100) NOT NULL," +
                        "  assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "  delivered_date TIMESTAMP NULL," +
                        "  status VARCHAR(50) DEFAULT 'Assigned'" +
                        ")";
                    
                    try (Statement createStmt = conn.createStatement()) {
                        createStmt.executeUpdate(createTableSql);
                        System.out.println("✅ Created simplified delivery_logs table");
                    }
                } else {
                    System.out.println("Table exists, checking structure...");
                    
                    // Check if driver_name column exists
                    String checkColumnSql = 
                        "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = 'delivery_logs' " +
                        "AND COLUMN_NAME = 'driver_name'";
                    
                    try (Statement checkColStmt = conn.createStatement();
                         ResultSet colRs = checkColStmt.executeQuery(checkColumnSql)) {
                        
                        if (colRs.next() && colRs.getInt("count") == 0) {
                            System.out.println("Adding driver_name column...");
                            // Add driver_name column
                            String addColumnSql = "ALTER TABLE delivery_logs ADD COLUMN driver_name VARCHAR(100) NOT NULL AFTER order_id";
                            try (Statement alterStmt = conn.createStatement()) {
                                alterStmt.executeUpdate(addColumnSql);
                                System.out.println("✅ Added driver_name column");
                            }
                        } else {
                            System.out.println("✅ driver_name column already exists");
                        }
                    }
                }
                
            }
            
            System.out.println("✅ delivery_logs table repair completed");
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to repair delivery_logs table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void setupAutoRefresh() {
        // Refresh grain types every 5 minutes
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                GrainTypeManager.refreshGrainTypes();
                System.out.println("Auto-refreshed grain types");
            }
        }, 300000, 300000); // 5 minutes
    }
    private static void initializeSampleData() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM inventory");
            if (rs.next() && rs.getInt("count") == 0) {
                System.out.println("📦 Initializing sample inventory data...");
                
                String[][] sampleGrains = {
                    {"Wheat", "150.0", "45.00"},
                    {"Teff", "800.0", "85.00"},
                    {"Corn", "0.0", "40.00"},
                    {"Barley", "600.0", "42.00"}
                };
                
                try (
                    PreparedStatement inv = conn.prepareStatement(
                        "INSERT IGNORE INTO inventory (grain_type, current_stock_kg) VALUES (?, ?)"
                    );
                    PreparedStatement set = conn.prepareStatement(
                        "INSERT IGNORE INTO settings (grain_type, price_per_kg) VALUES (?, ?)"
                    )
                ) {
                    for (String[] g : sampleGrains) {
                        inv.setString(1, g[0]);
                        inv.setDouble(2, Double.parseDouble(g[1]));
                        inv.executeUpdate();
                        
                        set.setString(1, g[0]);
                        set.setDouble(2, Double.parseDouble(g[2]));
                        set.executeUpdate();
                    }
                }
                
                System.out.println("✅ Sample inventory data added");
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to initialize sample data: " + e.getMessage());
        }
    }
}
