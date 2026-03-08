package fxml.admin.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;

import fxml.DatabaseConnection;
import fxml.CurrentUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

public class SalaryProcessingTabController {

    // Form fields
    @FXML private ComboBox<StaffComboItem> staffCombo;
    @FXML private TextField staffNameField, staffRoleField, baseSalaryField;
    @FXML private DatePicker salaryMonthPicker;
    @FXML private TextField bonusField, deductionsField;
    @FXML private Label netSalaryLabel, salarySummaryLabel;
    @FXML private ComboBox<String> paymentMethodCombo, statusCombo;
    
    // Table
    @FXML private TableView<ProcessedSalary> processedSalariesTable;
    
    // Data
    private ObservableList<StaffComboItem> staffList = FXCollections.observableArrayList();
    private ObservableList<ProcessedSalary> processedSalaries = FXCollections.observableArrayList();
    
    // Current selected staff
    private StaffComboItem selectedStaff = null;
    
    @FXML 
    private void initialize() {
        System.out.println("=== SALARY PROCESSING TAB INITIALIZED ===");
        
        // Check if user is Admin
        if (!"ADMIN".equals(CurrentUser.getInstance().getRole())) {
            showAlert("Access Denied", "Only Admin users can process salaries", Alert.AlertType.ERROR);
            return;
        }
        
        // Setup date picker (default to current month)
        salaryMonthPicker.setValue(LocalDate.now());
        
        // Setup combo boxes
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
            "Bank Transfer", "Cash", "Check", "Mobile Money", "Direct Deposit"
        ));
        paymentMethodCombo.setValue("Bank Transfer");
        
        statusCombo.setItems(FXCollections.observableArrayList(
            "PAID", "UNPAID", "PENDING"
        ));
        statusCombo.setValue("PAID");
        
        // Setup staff combo box
        setupStaffComboBox();
        
        // Setup processed salaries table
        setupProcessedSalariesTable();
        
        // Load initial data
        loadStaffList();
        loadProcessedSalaries();
        
        // Set default values
        bonusField.setText("0.00");
        deductionsField.setText("0.00");
    }
    
    private void setupStaffComboBox() {
        staffCombo.setItems(staffList);
        staffCombo.setCellFactory(param -> new ListCell<StaffComboItem>() {
            @Override
            protected void updateItem(StaffComboItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getRole() + " (ETB " + 
                           String.format("%.2f", item.getBaseSalary()) + ")");
                }
            }
        });
        
        staffCombo.setButtonCell(new ListCell<StaffComboItem>() {
            @Override
            protected void updateItem(StaffComboItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getRole());
                }
            }
        });
        
        // Add listener to staff selection
        staffCombo.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedStaff = newSelection;
                    loadStaffDetails(newSelection);
                }
            }
        );
    }
    
    private void loadStaffList() {
        staffList.clear();
        
        // Fetch active staff members with their base salaries
        String sql = "SELECT id, name, role, monthly_salary FROM staff_members " +
                    "WHERE status = 'Active' " +
                    "ORDER BY name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                StaffComboItem staff = new StaffComboItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("role"),
                    rs.getDouble("monthly_salary")
                );
                staffList.add(staff);
                count++;
            }
            
            System.out.println("Loaded " + count + " active staff members for salary processing");
            
        } catch (SQLException e) {
            System.err.println("Error loading staff list: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load staff list: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void loadStaffDetails(StaffComboItem staff) {
        // Populate read-only fields
        staffNameField.setText(staff.getName());
        staffRoleField.setText(staff.getRole());
        baseSalaryField.setText(String.format("%.2f", staff.getBaseSalary()));
        
        // Calculate and display net salary
        calculateNetSalary();
    }
    
    @FXML
    private void handleCalculateNetSalary() {
        calculateNetSalary();
    }
    
    private void calculateNetSalary() {
        if (selectedStaff == null) {
            netSalaryLabel.setText("0.00");
            return;
        }
        
        try {
            double baseSalary = selectedStaff.getBaseSalary();
            double bonus = parseDoubleField(bonusField.getText());
            double deductions = parseDoubleField(deductionsField.getText());
            
            double netSalary = baseSalary + bonus - deductions;
            netSalaryLabel.setText(String.format("%.2f", netSalary));
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for bonus and deductions", Alert.AlertType.ERROR);
        }
    }
    

    
    @FXML
    private void handleProcessSalary() {
        // BASIC VALIDATION
        if (selectedStaff == null) {
            showAlert("Error", "Please select staff", Alert.AlertType.ERROR);
            return;
        }
        
        if (salaryMonthPicker.getValue() == null) {
            showAlert("Error", "Select month", Alert.AlertType.ERROR);
            return;
        }
        
        // CALCULATE
        double bonus = parseDoubleField(bonusField.getText());
        double deductions = parseDoubleField(deductionsField.getText());
        double netSalary = selectedStaff.getBaseSalary() + bonus - deductions;
        
        // PROCESS
        processSalaryInDatabase(selectedStaff, salaryMonthPicker.getValue(), 
                               selectedStaff.getBaseSalary(), bonus, deductions, 
                               netSalary, paymentMethodCombo.getValue(), 
                               statusCombo.getValue());
    }
    
    private boolean isSalaryAlreadyProcessed(int staffId, LocalDate month) {
        YearMonth yearMonth = YearMonth.from(month);
        String monthStr = yearMonth.toString(); // Format: YYYY-MM
        
        String sql = "SELECT COUNT(*) as count FROM salary_payments " +
                    "WHERE staff_id = ? AND DATE_FORMAT(payment_month, '%Y-%m') = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, staffId);
            ps.setString(2, monthStr);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking duplicate salary: " + e.getMessage());
        }
        
        return false;
    }
    
    private void processSalaryInDatabase(StaffComboItem staff, LocalDate month, 
            double baseSalary, double bonus, double deductions,
            double netSalary, String paymentMethod, String status) {

// SIMPLE INSERT - no transactions
String sql = "INSERT INTO salary_payments " +
"(staff_id, staff_name, payment_month, base_salary, " +
"bonus, deductions, net_salary, payment_method, status) " +
"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

try (Connection conn = DatabaseConnection.getConnection();
PreparedStatement ps = conn.prepareStatement(sql)) {

ps.setInt(1, staff.getId());
ps.setString(2, staff.getName());
ps.setDate(3, Date.valueOf(month.withDayOfMonth(1)));
ps.setDouble(4, baseSalary);
ps.setDouble(5, bonus);
ps.setDouble(6, deductions);
ps.setDouble(7, netSalary);
ps.setString(8, paymentMethod);
ps.setString(9, status);

ps.executeUpdate();

// SUCCESS
showAlert("Success", 
"Salary processed for " + staff.getName() + 
"\nNet Salary: ETB " + String.format("%.2f", netSalary),
Alert.AlertType.INFORMATION);

loadProcessedSalaries();
clearForm();

} catch (SQLException e) {
showAlert("Error", "Failed to save: " + e.getMessage(), Alert.AlertType.ERROR);
}
}
    private double parseDoubleField(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return 0.0;
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    private boolean checkColumnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = ? " +
                    "AND COLUMN_NAME = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }
    
    private void updateStaffSalaryStatus(Connection conn, int staffId, LocalDate month, String status) throws SQLException {
        // Update staff member's last salary info
        String sql = "UPDATE staff_members SET last_salary_date = ?, last_salary_status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(month));
            ps.setString(2, status);
            ps.setInt(3, staffId);
            ps.executeUpdate();
        }
    }
    
    private void setupProcessedSalariesTable() {
        processedSalariesTable.getColumns().clear();
        
        TableColumn<ProcessedSalary, String> nameCol = new TableColumn<>("Staff Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("staffName"));
        nameCol.setPrefWidth(150);
        
        TableColumn<ProcessedSalary, String> periodCol = new TableColumn<>("Period");
        periodCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(getMonthYearString(cell.getValue().getPaymentMonth())));
        periodCol.setPrefWidth(100);
        
        TableColumn<ProcessedSalary, String> baseCol = new TableColumn<>("Base Salary");
        baseCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(String.format("ETB %.2f", cell.getValue().getBaseSalary())));
        baseCol.setPrefWidth(120);
        
        TableColumn<ProcessedSalary, String> netCol = new TableColumn<>("Net Salary");
        netCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(String.format("ETB %.2f", cell.getValue().getNetSalary())));
        netCol.setPrefWidth(120);
        
        TableColumn<ProcessedSalary, String> methodCol = new TableColumn<>("Payment Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        methodCol.setPrefWidth(120);
        
        TableColumn<ProcessedSalary, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(column -> new TableCell<ProcessedSalary, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "PAID":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "PENDING":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "UNPAID":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });
        
        TableColumn<ProcessedSalary, String> dateCol = new TableColumn<>("Processed Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("processedDate"));
        dateCol.setPrefWidth(150);
        
        processedSalariesTable.getColumns().addAll(nameCol, periodCol, baseCol, netCol, methodCol, statusCol, dateCol);
        processedSalariesTable.setItems(processedSalaries);
    }
    
    private void loadProcessedSalaries() {
        processedSalaries.clear();
        
        String sql = "SELECT * FROM salary_payments ORDER BY created_date DESC LIMIT 20";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                ProcessedSalary salary = new ProcessedSalary(
                    rs.getInt("id"),
                    rs.getString("staff_name"),
                    rs.getDate("payment_month").toLocalDate(),
                    rs.getDouble("base_salary"),
                    rs.getDouble("bonus"),
                    rs.getDouble("deductions"),
                    rs.getDouble("net_salary"),
                    rs.getString("payment_method"),
                    rs.getString("status"),
                    rs.getString("created_date") // Just use created_date
                );
                processedSalaries.add(salary);
            }
            
            salarySummaryLabel.setText("Records: " + processedSalaries.size());
            
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefreshStaffList() {
        loadStaffList();
        showAlert("Refreshed", "Staff list updated", Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void handleRefreshProcessedSalaries() {
        loadProcessedSalaries();
    }
    
    private void clearForm() {
        staffCombo.getSelectionModel().clearSelection();
        staffNameField.clear();
        staffRoleField.clear();
        baseSalaryField.clear();
        bonusField.setText("0.00");
        deductionsField.setText("0.00");
        netSalaryLabel.setText("0.00");
        salaryMonthPicker.setValue(LocalDate.now());
        selectedStaff = null;
    }
    
    private String getMonthYearString(LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        return yearMonth.toString(); // Returns "YYYY-MM"
    }
    
    public void refresh() {
        loadStaffList();
        loadProcessedSalaries();
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
    
    // Helper classes
    public static class StaffComboItem {
        private int id;
        private String name;
        private String role;
        private double baseSalary;
        
        public StaffComboItem(int id, String name, String role, double baseSalary) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.baseSalary = baseSalary;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public String getRole() { return role; }
        public double getBaseSalary() { return baseSalary; }
        
        @Override
        public String toString() {
            return name + " - " + role;
        }
    }
    
    public static class ProcessedSalary {
        private int id;
        private String staffName;
        private LocalDate paymentMonth;
        private double baseSalary;
        private double bonus;
        private double deductions;
        private double netSalary;
        private String paymentMethod;
        private String status;
        private String processedDate;
        private String processedBy;
        
        public ProcessedSalary(int id, String staffName, LocalDate paymentMonth, 
                double baseSalary, double bonus, double deductions,
                double netSalary, String paymentMethod, String status,
                String createdDate) {
this.id = id;
this.staffName = staffName;
this.paymentMonth = paymentMonth;
this.baseSalary = baseSalary;
this.bonus = bonus;
this.deductions = deductions;
this.netSalary = netSalary;
this.paymentMethod = paymentMethod;
this.status = status;
this.processedDate = createdDate; // Use created_date directly
}
        
        // Getters for PropertyValueFactory
        public int getId() { return id; }
        public String getStaffName() { return staffName; }
        public LocalDate getPaymentMonth() { return paymentMonth; }
        public double getBaseSalary() { return baseSalary; }
        public double getBonus() { return bonus; }
        public double getDeductions() { return deductions; }
        public double getNetSalary() { return netSalary; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
        public String getProcessedDate() { return processedDate; }
        public String getProcessedBy() { return processedBy; }
        public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    }
}